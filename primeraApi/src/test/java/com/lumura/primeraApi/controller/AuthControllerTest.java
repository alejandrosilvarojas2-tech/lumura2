package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.service.EmailService;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private EmailService emailService;

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
            "categoria_productos", "Vestidos",
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
    void registerAliado_guardaCategoriaProductosYDireccionPuntoVenta() {
        Map<String, String> body = Map.of(
            "nombre_negocio", "Zapatos del Valle",
            "nit", "890999111-2",
            "telefono", "3115557788",
            "persona_contacto", "Marcela",
            "correo_usuario", "zapatos@valle.com",
            "categoria_productos", "Zapatos",
            "direccion", "Av Siempre Viva 123",
            "password", "secreto123",
            "confirmar_password", "secreto123"
        );
        when(usuarioRepository.findByCorreoUsuario("zapatos@valle.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(2);
            return u;
        });

        ResponseEntity<?> res = authController.registerAliado(body);

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("Zapatos", captor.getValue().getCategoriaProductos());
        assertEquals("Av Siempre Viva 123", captor.getValue().getDireccionUsuario());
    }

    @Test
    void registerAliado_sinCategoriaNiDireccion_retorna400() {
        Map<String, String> base = new java.util.HashMap<>(Map.of(
            "nombre_negocio", "Moda Express",
            "nit", "900123456-7",
            "persona_contacto", "Carlos",
            "correo_usuario", "aliado2@negocio.com",
            "password", "secreto123",
            "confirmar_password", "secreto123"
        ));

        // Sin dirección y sin categoría
        assertEquals(400, authController.registerAliado(base).getStatusCode().value());

        // Con dirección pero sin categoría
        base.put("direccion", "Calle 1");
        assertEquals(400, authController.registerAliado(base).getStatusCode().value());

        verify(usuarioRepository, never()).save(any());
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
        when(jwtUtil.generateToken(1, "ana@correo.com", "USER", 0)).thenReturn("token-jwt");

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "ana@correo.com", "password", "secreto123"));

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("token-jwt", body.get("token"));
    }

    @Test
    void login_usaLaVersionActualDeRevocacion() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(9);
        usuario.setNombreUsuario("Ana");
        usuario.setCorreoUsuario("ana@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("secreto123", BCrypt.gensalt()));
        usuario.setRol("USER");
        usuario.setTokenVersion(4);

        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken(eq(9), eq("ana@correo.com"), eq("USER"), eq(4))).thenReturn("token-v4");

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "ana@correo.com", "password", "secreto123"));

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("token-v4", body.get("token"));
    }

    @Test
    void logout_conTokenValido_incrementaVersionYRevoca() {
        when(jwtUtil.validateToken("rev-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("rev-token")).thenReturn(7);
        when(usuarioRepository.incrementarTokenVersion(7)).thenReturn(1);

        ResponseEntity<?> res = authController.logout("Bearer rev-token");

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("Sesión cerrada correctamente", body.get("mensaje"));
        verify(usuarioRepository).incrementarTokenVersion(7);
    }

    @Test
    void logout_sinToken_retorna401() {
        ResponseEntity<?> res = authController.logout(null);

        assertEquals(401, res.getStatusCode().value());
        verify(usuarioRepository, never()).incrementarTokenVersion(any());
    }

    @Test
    void logout_tokenInvalido_retorna401() {
        when(jwtUtil.validateToken("malo")).thenReturn(false);

        ResponseEntity<?> res = authController.logout("Bearer malo");

        assertEquals(401, res.getStatusCode().value());
        verify(usuarioRepository, never()).incrementarTokenVersion(any());
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
    void login_cuentaBloqueada_retorna403ConMotivo() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombreUsuario("Ana");
        usuario.setCorreoUsuario("ana@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("secreto123", BCrypt.gensalt()));
        usuario.setRol("USER");
        usuario.setBloqueado(true);
        usuario.setMotivoBloqueo("Fraude");
        usuario.setBloqueoHasta(java.time.LocalDateTime.now().plusDays(7));

        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.of(usuario));

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "ana@correo.com", "password", "secreto123"));

        assertEquals(403, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertTrue(((String) body.get("error")).contains("Cuenta bloqueada"));
        assertTrue(((String) body.get("error")).contains("Fraude"));
        verify(jwtUtil, never()).generateToken(any(), anyString(), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_bloqueoVencido_desbloqueaYPermiteEntrar() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombreUsuario("Ana");
        usuario.setCorreoUsuario("ana@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("secreto123", BCrypt.gensalt()));
        usuario.setRol("USER");
        usuario.setBloqueado(true);
        usuario.setMotivoBloqueo("Fraude");
        usuario.setBloqueoHasta(java.time.LocalDateTime.now().minusDays(1));

        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(1, "ana@correo.com", "USER", 0)).thenReturn("token-jwt");

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "ana@correo.com", "password", "secreto123"));

        assertEquals(200, res.getStatusCode().value());
        assertFalse(usuario.getBloqueado());
        assertNull(usuario.getMotivoBloqueo());
        assertNull(usuario.getBloqueoHasta());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("token-jwt", body.get("token"));
    }

    @Test
    void login_correoInvalido_retorna400() {
        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "invalido", "password", "secreto123"));

        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void login_aliadoConMembresiaVencida_retorna403ConMensaje() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(2);
        usuario.setNombreUsuario("Luis");
        usuario.setCorreoUsuario("luis@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("secreto123", BCrypt.gensalt()));
        usuario.setRol("ALIADO");
        usuario.setMembresiaCodigo("MEM-2-BASICO-20260101-AAAA");
        usuario.setMembresiaPlan("basico");
        usuario.setMembresiaActivadaEn(java.time.LocalDateTime.now().minusDays(40));
        usuario.setMembresiaVence(java.time.LocalDateTime.now().minusDays(1));

        when(usuarioRepository.findByCorreoUsuario("luis@correo.com")).thenReturn(Optional.of(usuario));

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "luis@correo.com", "password", "secreto123"));

        assertEquals(403, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertTrue(((String) body.get("error")).contains("membresía vencida"));
        assertTrue(((String) body.get("error")).contains("Genera tu pago"));
        verify(jwtUtil, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    void login_aliadoConMembresiaVigente_incluyeDatosDeMembresia() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(2);
        usuario.setNombreUsuario("Luis");
        usuario.setCorreoUsuario("luis@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("secreto123", BCrypt.gensalt()));
        usuario.setRol("ALIADO");
        usuario.setMembresiaCodigo("MEM-2-BASICO-20260101-AAAA");
        usuario.setMembresiaPlan("basico");
        usuario.setMembresiaActivadaEn(java.time.LocalDateTime.now().minusDays(1));
        usuario.setMembresiaVence(java.time.LocalDateTime.now().plusDays(30));

        when(usuarioRepository.findByCorreoUsuario("luis@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken(2, "luis@correo.com", "ALIADO", 0)).thenReturn("token-jwt");

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "luis@correo.com", "password", "secreto123"));

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> user = (Map<?, ?>) body.get("usuario");
        Map<?, ?> membresia = (Map<?, ?>) user.get("membresia");
        assertNotNull(membresia);
        assertEquals("MEM-2-BASICO-20260101-AAAA", membresia.get("codigo"));
        assertEquals("basico", membresia.get("plan"));
        assertNotNull(membresia.get("vence"));
    }

    @Test
    void login_clienteSinMembresia_noIncluyeDatosDeMembresia() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(3);
        usuario.setNombreUsuario("Ana");
        usuario.setCorreoUsuario("ana@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("secreto123", BCrypt.gensalt()));
        usuario.setRol("USER");

        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken(3, "ana@correo.com", "USER", 0)).thenReturn("token-jwt");

        ResponseEntity<?> res = authController.login(Map.of("correo_usuario", "ana@correo.com", "password", "secreto123"));

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> user = (Map<?, ?>) body.get("usuario");
        assertFalse(user.containsKey("membresia"));
    }

    @Test
    void recuperar_usuarioExistente_generaTokenYEnlace() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setCorreoUsuario("ana@correo.com");
        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = authController.recuperar(Map.of("correo_usuario", "ana@correo.com"));

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        String enlace = (String) body.get("enlace_demo");
        assertNotNull(enlace);
        assertTrue(enlace.startsWith("http://localhost:8080/reset-password?token="));
        assertNotNull(usuario.getResetToken());
        assertNotNull(usuario.getResetTokenExpira());
    }

    @Test
    void recuperar_correoNoRegistrado_noFallaNiRevela() {
        ResponseEntity<?> res = authController.recuperar(Map.of("correo_usuario", "nadie@correo.com"));

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertTrue(((String) body.get("mensaje")).contains("Si el correo está registrado"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void recuperar_correoInvalido_retorna400() {
        ResponseEntity<?> res = authController.recuperar(Map.of("correo_usuario", "invalido"));
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void resetPassword_tokenValido_cambiaPasswordYLimpiaToken() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setCorreoUsuario("ana@correo.com");
        usuario.setPasswordHash(BCrypt.hashpw("vieja123", BCrypt.gensalt()));
        usuario.setResetToken("token123");
        usuario.setResetTokenExpira(java.time.LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByResetToken("token123")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = authController.resetPassword(Map.of(
                "token", "token123",
                "nueva_password", "nuevaPass1",
                "confirmar_password", "nuevaPass1"));

        assertEquals(200, res.getStatusCode().value());
        assertTrue(BCrypt.checkpw("nuevaPass1", usuario.getPasswordHash()));
        assertNull(usuario.getResetToken());
        assertNull(usuario.getResetTokenExpira());
    }

    @Test
    void resetPassword_revocaTokensPrevios() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(5);
        usuario.setCorreoUsuario("ana@correo.com");
        usuario.setResetToken("token123");
        usuario.setResetTokenExpira(java.time.LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByResetToken("token123")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.incrementarTokenVersion(5)).thenReturn(1);

        ResponseEntity<?> res = authController.resetPassword(Map.of(
                "token", "token123",
                "nueva_password", "nuevaPass1",
                "confirmar_password", "nuevaPass1"));

        assertEquals(200, res.getStatusCode().value());
        verify(usuarioRepository).incrementarTokenVersion(5);
    }

    @Test
    void resetPassword_tokenExpirado_retorna400() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setResetToken("token123");
        usuario.setResetTokenExpira(java.time.LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByResetToken("token123")).thenReturn(Optional.of(usuario));

        ResponseEntity<?> res = authController.resetPassword(Map.of(
                "token", "token123",
                "nueva_password", "nuevaPass1",
                "confirmar_password", "nuevaPass1"));

        assertEquals(400, res.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void resetPassword_tokenInexistente_retorna400() {
        when(usuarioRepository.findByResetToken("no-existe")).thenReturn(Optional.empty());

        ResponseEntity<?> res = authController.resetPassword(Map.of(
                "token", "no-existe",
                "nueva_password", "nuevaPass1",
                "confirmar_password", "nuevaPass1"));

        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void resetPassword_confirmacionNoCoincide_retorna400() {
        ResponseEntity<?> res = authController.resetPassword(Map.of(
                "token", "token123",
                "nueva_password", "nuevaPass1",
                "confirmar_password", "otra"));

        assertEquals(400, res.getStatusCode().value());
        verify(usuarioRepository, never()).findByResetToken(anyString());
    }

    @Test
    void register_enviaCorreoDeBienvenidaAlNuevoUsuario() {
        when(usuarioRepository.findByCorreoUsuario("ana@correo.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(1);
            return u;
        });

        ResponseEntity<?> res = authController.register(bodyValido);

        assertEquals(200, res.getStatusCode().value());
        verify(emailService).enviar(eq("ana@correo.com"), contains("Bienvenido"),
                contains("ana@correo.com"));
    }

    @Test
    void registerAliado_enviaCorreoDeBienvenidaAlNuevoAliado() {
        Map<String, String> body = Map.of(
            "nombre_negocio", "Moda Express",
            "nit", "900123456-7",
            "persona_contacto", "Carlos",
            "correo_usuario", "aliado@negocio.com",
            "categoria_productos", "Vestidos",
            "direccion", "Calle 45 #12-34",
            "password", "secreto123",
            "confirmar_password", "secreto123"
        );
        when(usuarioRepository.findByCorreoUsuario("aliado@negocio.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(1);
            return u;
        });

        ResponseEntity<?> res = authController.registerAliado(body);

        assertEquals(200, res.getStatusCode().value());
        verify(emailService).enviar(eq("aliado@negocio.com"), contains("Bienvenido"),
                contains("Moda Express"));
    }

    @Test
    void eliminarCuenta_enviaCorreoDeDespedidaAntesDeBorrar() {
        when(jwtUtil.validateToken("del-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("del-token")).thenReturn(3);
        Usuario u = new Usuario();
        u.setIdUsuario(3);
        u.setNombreUsuario("Ana");
        u.setCorreoUsuario("ana@correo.com");
        u.setRol("USER");
        when(usuarioRepository.findById(3)).thenReturn(Optional.of(u));

        ResponseEntity<?> res = authController.eliminarCuenta("Bearer del-token");

        assertEquals(200, res.getStatusCode().value());
        verify(emailService).enviar(eq("ana@correo.com"), contains("eliminada"), anyString());
        verify(usuarioRepository).deleteById(3);
        verify(carritoRepository).deleteByIdUsuario(3);
        verify(compraRepository).deleteByIdUsuario(3);
    }

    @Test
    void actualizarCuenta_enviaCorreoDeConfirmacionDeCambios() {
        when(jwtUtil.validateToken("upd-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("upd-token")).thenReturn(3);
        Usuario u = new Usuario();
        u.setIdUsuario(3);
        u.setNombreUsuario("Ana");
        u.setCorreoUsuario("ana@correo.com");
        u.setRol("USER");
        when(usuarioRepository.findById(3)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = authController.actualizarCuenta("Bearer upd-token",
                Map.of("telefono", "3001234567"));

        assertEquals(200, res.getStatusCode().value());
        verify(emailService).enviar(eq("ana@correo.com"), contains("actualizados"), anyString());
        assertEquals("3001234567", u.getTelefono());
    }

}
