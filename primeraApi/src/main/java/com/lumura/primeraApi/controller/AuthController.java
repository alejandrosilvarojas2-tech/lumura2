package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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

        if (nombre == null || correo == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos obligatorios"));
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

        return ResponseEntity.ok(Map.of("mensaje", "Usuario registrado correctamente", "id", usuario.getIdUsuario()));
    }

    private String sha256(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String correo = body.get("correo_usuario");
        String password = body.get("password");

        if (correo == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan credenciales"));
        }

        Optional<Usuario> opt = usuarioRepository.findByCorreoUsuario(correo);
        if (opt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Correo o contraseña incorrectos"));
        }

        Usuario usuario = opt.get();
        String storedHash = usuario.getPasswordHash();

        if (!BCrypt.checkpw(password, storedHash)) {
            if (!storedHash.equals(sha256(password))) {
                return ResponseEntity.status(401).body(Map.of("error", "Correo o contraseña incorrectos"));
            }
            usuario.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            usuarioRepository.save(usuario);
        }

        if ("admin@lumura.com".equals(usuario.getCorreoUsuario()) && !"ADMIN".equals(usuario.getRol())) {
            usuario.setRol("ADMIN");
            usuarioRepository.save(usuario);
        }

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

        return ResponseEntity.ok(Map.of("mensaje", "Cuenta eliminada correctamente"));
    }

}
