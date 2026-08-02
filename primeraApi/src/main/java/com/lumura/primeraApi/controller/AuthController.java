package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final CompraRepository compraRepository;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository,
                          CarritoRepository carritoRepository,
                          CompraRepository compraRepository,
                          JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.compraRepository = compraRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String nombre = body.get("nombre_usuario");
        String correo = body.get("correo_usuario");
        String password = body.get("password");

        if (nombre == null || nombre.isBlank() || correo == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos obligatorios"));
        }
        if (nombre.trim().length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre debe tener al menos 2 caracteres"));
        }
        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo no tiene un formato válido"));
        }
        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 6 caracteres"));
        }
        String confirmarPassword = body.get("confirmar_password");
        if (confirmarPassword == null || !password.equals(confirmarPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }

        if (usuarioRepository.findByCorreoUsuario(correo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo ya está registrado"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombre);
        usuario.setCorreoUsuario(correo);
        usuario.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        usuario.setTelefono(body.get("telefono"));
        if (body.containsKey("edad")) usuario.setEdad(Integer.parseInt(body.get("edad")));
        usuario.setDireccionUsuario(body.get("direccion_usuario"));
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setRol("USER");
        usuarioRepository.save(usuario);

        log.info("Nuevo usuario registrado: {} ({})", nombre, correo);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario registrado correctamente", "id", usuario.getIdUsuario()));
    }

    @PostMapping("/register-aliado")
    public ResponseEntity<?> registerAliado(@RequestBody Map<String, String> body) {
        String nombreNegocio = body.get("nombre_negocio");
        String nit = body.get("nit");
        String telefono = body.get("telefono");
        String contacto = body.get("persona_contacto");
        String correo = body.get("correo_usuario");
        String password = body.get("password");
        String confirmarPassword = body.get("confirmar_password");

        if (nombreNegocio == null || nombreNegocio.isBlank() || nit == null || nit.isBlank()
                || correo == null || password == null || contacto == null || contacto.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos obligatorios"));
        }
        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo no tiene un formato válido"));
        }
        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 6 caracteres"));
        }
        if (confirmarPassword == null || !password.equals(confirmarPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }
        if (usuarioRepository.findByCorreoUsuario(correo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo ya está registrado"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombreNegocio + " - " + contacto);
        usuario.setCorreoUsuario(correo);
        usuario.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        usuario.setTelefono(telefono);
        usuario.setDireccionUsuario(body.get("direccion"));
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setRol("ALIADO");
        usuarioRepository.save(usuario);

        log.info("Nuevo aliado registrado: {} ({}, NIT: {})", nombreNegocio, correo, nit);
        return ResponseEntity.ok(Map.of("mensaje", "Aliado registrado correctamente", "id", usuario.getIdUsuario()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String correo = body.get("correo_usuario");
        String password = body.get("password");

        if (correo == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan credenciales"));
        }
        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo no tiene un formato válido"));
        }

        Optional<Usuario> opt = usuarioRepository.findByCorreoUsuario(correo);
        if (opt.isEmpty()) {
            log.warn("Login fallido - correo no encontrado: {}", correo);
            return ResponseEntity.status(401).body(Map.of("error", "Correo o contraseña incorrectos"));
        }

        Usuario usuario = opt.get();
        String storedHash = usuario.getPasswordHash();

        if (!BCrypt.checkpw(password, storedHash)) {
            log.warn("Login fallido - contraseña incorrecta: {}", correo);
            return ResponseEntity.status(401).body(Map.of("error", "Correo o contraseña incorrectos"));
        }

        if ("admin@lumura.com".equals(usuario.getCorreoUsuario()) && !"ADMIN".equals(usuario.getRol())) {
            usuario.setRol("ADMIN");
            usuarioRepository.save(usuario);
        }

        log.info("Login exitoso: {} ({})", usuario.getNombreUsuario(), correo);
        String token = jwtUtil.generateToken(usuario.getIdUsuario(), usuario.getCorreoUsuario(), usuario.getRol());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "usuario", Map.of(
                "id", usuario.getIdUsuario(),
                "nombre", usuario.getNombreUsuario(),
                "correo", usuario.getCorreoUsuario(),
                "rol", usuario.getRol(),
                "direccion", usuario.getDireccionUsuario() != null ? usuario.getDireccionUsuario() : ""
            )
        ));
    }

    @DeleteMapping("/cuenta")
    @Transactional
    public ResponseEntity<?> eliminarCuenta(@RequestHeader("Authorization") String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        }
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Token inválido"));
        }
        Integer userId = jwtUtil.getUserIdFromToken(token);
        Optional<Usuario> usuario = usuarioRepository.findById(userId);
        if (usuario.isEmpty()) return ResponseEntity.notFound().build();

        if ("admin@lumura.com".equals(usuario.get().getCorreoUsuario())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede eliminar la cuenta admin"));
        }

        carritoRepository.deleteByIdUsuario(userId);
        compraRepository.deleteByIdUsuario(userId);
        usuarioRepository.deleteById(userId);

        log.info("Cuenta eliminada: userId={}", userId);
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta eliminada correctamente"));
    }

    @PutMapping("/cuenta")
    public ResponseEntity<?> actualizarCuenta(@RequestHeader("Authorization") String auth,
                                               @RequestBody Map<String, String> body) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        }
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Token inválido"));
        }
        Integer userId = jwtUtil.getUserIdFromToken(token);
        Optional<Usuario> opt = usuarioRepository.findById(userId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Usuario usuario = opt.get();
        if (body.containsKey("nombre_usuario")) usuario.setNombreUsuario(body.get("nombre_usuario"));
        if (body.containsKey("telefono")) usuario.setTelefono(body.get("telefono"));
        if (body.containsKey("direccion_usuario")) usuario.setDireccionUsuario(body.get("direccion_usuario"));
        usuarioRepository.save(usuario);

        log.info("Datos actualizados: userId={}", userId);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Datos actualizados correctamente",
            "usuario", Map.of(
                "id", usuario.getIdUsuario(),
                "nombre", usuario.getNombreUsuario(),
                "correo", usuario.getCorreoUsuario(),
                "rol", usuario.getRol(),
                "direccion", usuario.getDireccionUsuario() != null ? usuario.getDireccionUsuario() : ""
            )
        ));
    }

}
