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
import java.util.UUID;
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
        String direccionPuntoVenta = body.get("direccion");
        String categoriaProductos = body.get("categoria_productos");

        if (nombreNegocio == null || nombreNegocio.isBlank() || nit == null || nit.isBlank()
                || correo == null || password == null || contacto == null || contacto.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos obligatorios"));
        }
        if (direccionPuntoVenta == null || direccionPuntoVenta.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La dirección del punto de venta es obligatoria"));
        }
        if (categoriaProductos == null || categoriaProductos.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes indicar la categoría de productos a vender"));
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
        usuario.setDireccionUsuario(direccionPuntoVenta);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setRol("ALIADO");
        usuario.setNombreNegocio(nombreNegocio);
        usuario.setNit(nit);
        usuario.setPersonaContacto(contacto);
        usuario.setCategoriaProductos(categoriaProductos);
        usuarioRepository.save(usuario);

        log.info("Nuevo aliado registrado: {} ({}, NIT: {}, categoría: {})", nombreNegocio, correo, nit, categoriaProductos);
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

        // Validación de bloqueo: si venció, se desbloquea automáticamente en el siguiente intento
        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            if (usuario.getBloqueoHasta() != null && usuario.getBloqueoHasta().isAfter(LocalDateTime.now())) {
                log.warn("Login bloqueado - cuenta suspendida: {} (hasta {})", correo, usuario.getBloqueoHasta());
                return ResponseEntity.status(403).body(Map.of(
                        "error", "Cuenta bloqueada. Motivo: " + (usuario.getMotivoBloqueo() == null ? "No especificado" : usuario.getMotivoBloqueo())
                                + ". Vuelve a intentarlo después del " + usuario.getBloqueoHasta()
                ));
            }
            // bloqueo vencido → limpiar
            usuario.setBloqueado(false);
            usuario.setMotivoBloqueo(null);
            usuario.setBloqueoHasta(null);
            usuarioRepository.save(usuario);
            log.info("Bloqueo vencido, cuenta {} desbloqueada", correo);
        }

        log.info("Login exitoso: {} ({})", usuario.getNombreUsuario(), correo);
        int version = usuario.getTokenVersion() == null ? 0 : usuario.getTokenVersion();
        String token = jwtUtil.generateToken(usuario.getIdUsuario(), usuario.getCorreoUsuario(), usuario.getRol(), version);
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

    // Cierra la sesión revocando todos los tokens activos del usuario
    // (incrementa token_version; los tokens viejos dejan de ser aceptados).
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        }
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Token inválido"));
        }
        Integer userId = jwtUtil.getUserIdFromToken(token);
        usuarioRepository.incrementarTokenVersion(userId);
        log.info("Sesión cerrada (tokens revocados): userId={}", userId);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }

    @DeleteMapping("/cuenta")
    @Transactional
    public ResponseEntity<?> eliminarCuenta(@RequestHeader(value = "Authorization", required = false) String auth) {
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

        if ("ADMIN".equals(usuario.get().getRol())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede eliminar la cuenta admin"));
        }

        carritoRepository.deleteByIdUsuario(userId);
        compraRepository.deleteByIdUsuario(userId);
        usuarioRepository.deleteById(userId);

        log.info("Cuenta eliminada: userId={}", userId);
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta eliminada correctamente"));
    }

    @PutMapping("/cuenta")
    public ResponseEntity<?> actualizarCuenta(@RequestHeader(value = "Authorization", required = false) String auth,
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

    // Solicita un enlace de recuperación de contraseña.
    // En modo offline (sin SMTP configurado) devuelve el enlace directamente en la
    // respuesta para que el usuario pueda copiarlo; también se registra en el log.
    @PostMapping("/recuperar")
    @Transactional
    public ResponseEntity<?> recuperar(@RequestBody Map<String, String> body) {
        String correo = body.get("correo_usuario");
        if (correo == null || correo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes indicar el correo"));
        }
        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo no tiene un formato válido"));
        }

        Optional<Usuario> opt = usuarioRepository.findByCorreoUsuario(correo);
        // No revelar si el correo existe o no (mitigación de enumeración de usuarios).
        String token = UUID.randomUUID().toString().replace("-", "");
        if (opt.isPresent()) {
            Usuario usuario = opt.get();
            usuario.setResetToken(token);
            usuario.setResetTokenExpira(LocalDateTime.now().plusMinutes(30));
            usuarioRepository.save(usuario);
        }

        log.info("Recuperación solicitada para {} (token: {}).", correo, token);
        String enlace = "http://localhost:8080/reset-password?token=" + token;
        return ResponseEntity.ok(Map.of(
            "mensaje", "Si el correo está registrado, recibirás un enlace para restablecer la contraseña",
            "enlace_demo", enlace
        ));
    }

    // Confirma el restablecimiento con el token recibido por correo.
    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String nuevaPassword = body.get("nueva_password");
        String confirmarPassword = body.get("confirmar_password");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token no válido"));
        }
        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 6 caracteres"));
        }
        if (confirmarPassword == null || !nuevaPassword.equals(confirmarPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }

        Optional<Usuario> opt = usuarioRepository.findByResetToken(token);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token no válido o ya utilizado"));
        }
        Usuario usuario = opt.get();
        if (usuario.getResetTokenExpira() == null || usuario.getResetTokenExpira().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El enlace de recuperación ha expirado"));
        }

        usuario.setPasswordHash(BCrypt.hashpw(nuevaPassword, BCrypt.gensalt()));
        usuario.setResetToken(null);
        usuario.setResetTokenExpira(null);
        usuarioRepository.save(usuario);
        // Revoca todos los tokens previos: tras cambiar la contraseña,
        // cualquier sesión activa con el token antiguo debe morir.
        usuarioRepository.incrementarTokenVersion(usuario.getIdUsuario());

        log.info("Contraseña restablecida: userId={}", usuario.getIdUsuario());
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
    }

}
