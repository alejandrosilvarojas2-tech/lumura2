package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
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
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Usuario registrado correctamente", "id", usuario.getIdUsuario()));
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
        if (!BCrypt.checkpw(password, usuario.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Correo o contraseña incorrectos"));
        }

        String token = jwtUtil.generateToken(usuario.getIdUsuario(), usuario.getCorreoUsuario());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "usuario", Map.of(
                "id", usuario.getIdUsuario(),
                "nombre", usuario.getNombreUsuario(),
                "correo", usuario.getCorreoUsuario()
            )
        ));
    }

}
