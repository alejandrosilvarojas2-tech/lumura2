package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private CompraRepository compraRepository;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    private Map<String, String> bodyValido;

    @BeforeEach
    void setUp() {
        bodyValido = Map.of(
            "nombre_usuario", "Ana",
            "correo_usuario", "ana@correo.com",
            "password", "secreto123",
            "confirmar_password", "secreto123"
        );
    }

    @Test
    void register_guardaUsuarioConBCryptYAsignaRolUSER() {
        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(1);
            return u;
        });

        ResponseEntity<?> res = authController.register(bodyValido);

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertNotEquals("secreto123", guardado.getPasswordHash());
        assertTrue(guardado.getPasswordHash().startsWith("$2"));
        assertEquals("USER", guardado.getRol());
    }

    @Test
    void register_correoDuplicado_retorna400() {
        Usuario existente = new Usuario();
        existente.setCorreoUsuario("ana@correo.com");
        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.of(existente));

        ResponseEntity<?> res = authController.register(bodyValido);

        assertEquals(400, res.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void register_correoInvalido_retorna400() {
        Map<String, String> body = new java.util.HashMap<>(bodyValido);
        body.put("correo_usuario", "correo-invalido");

        ResponseEntity<?> res = authController.register(body);

        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void register_passwordCorta_retorna400() {
        Map<String, String> body = new java.util.HashMap<>(bodyValido);
        body.put("password", "123");
        body.put("confirmar_password", "123");

        ResponseEntity<?> res = authController.register(body);

        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void register_passwordNoCoincide_retorna400() {
        Map<String, String> body = new java.util.HashMap<>(bodyValido);
        body.put("confirmar_password", "otra-clave");

        ResponseEntity<?> res = authController.register(body);

        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void registerAliado_guardaUsuarioConRolALIADO() {
        Map<String, String> body = Map.of(
            "nombre_negocio", "Moda Express",
            "nit", "900123456-7",
            "telefono", "3001234567",
            "persona_contacto", "Carlos",
            "correo_usuario", "aliado@negocio.com",
            "password", "secreto123",
            "confirmar_password", "secreto123",
            "direccion", "Calle 45 #12-34"
        );
        when(usuarioRepository.findByCorreoUsuario("aliado@negocio.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(1);
            return u;
        });

        ResponseEntity<?> res = authController.registerAliado(body);

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("ALIADO", captor.getValue().getRol());
    }

    @Test
    void login_conCredencialesCorrectas_retornaToken() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombreUsuario("Ana");
        usuario.setCorreoUsuario("ana@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("secreto123", BCrypt.gensalt()));
        usuario.setRol("USER");

        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken(1, "ana@correo.com", "USER")).thenReturn("token-jwt");

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "ana@correo.com", "password", "secreto123"));

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("token-jwt", body.get("token"));
    }

    @Test
    void login_conPasswordIncorrecta_retorna401() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombreUsuario("Ana");
        usuario.setCorreoUsuario("ana@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("secreto123", BCrypt.gensalt()));
        usuario.setRol("USER");

        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.of(usuario));

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "ana@correo.com", "password", "clave-incorrecta"));

        assertEquals(401, res.getStatusCode().value());
        verify(jwtUtil, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    void login_correoNoRegistrado_retorna401() {
        when(usuarioRepository.findByCorreoUsuario("nadie@correo.com")).thenReturn(Optional.empty());

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "nadie@correo.com", "password", "secreto123"));

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void login_correoInvalido_retorna400() {
        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "invalido", "password", "secreto123"));

        assertEquals(400, res.getStatusCode().value());
    }
}
