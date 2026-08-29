package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Compra;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private CompraRepository compraRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CatalogoRepository catalogoRepository;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AdminController adminController;

    private static final String TOKEN_ADMIN = "Bearer token-admin";
    private static final String TOKEN_USER = "Bearer token-user";
    private static final String TOKEN_ALIADO = "Bearer token-aliado";

    private void mockToken(String auth, String rol) {
        String token = auth.substring(7);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getRolFromToken(token)).thenReturn(rol);
    }

    @Test
    void dashboard_conTokenAdmin_retornaKPIs() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        when(catalogoRepository.count()).thenReturn(5L);
        when(usuarioRepository.count()).thenReturn(10L);
        when(compraRepository.count()).thenReturn(3L);
        when(compraRepository.sumTotal()).thenReturn(new BigDecimal("150000"));

        ResponseEntity<?> res = adminController.dashboard(TOKEN_ADMIN);

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(5L, body.get("total_productos"));
        assertEquals("150000", body.get("ingresos").toString());
    }

    @Test
    void dashboard_sinToken_retorna401() {
        ResponseEntity<?> res = adminController.dashboard(null);

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void dashboard_conTokenNoAdmin_retorna401() {
        mockToken(TOKEN_USER, "USER");

        ResponseEntity<?> res = adminController.dashboard(TOKEN_USER);

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void crearProducto_conAliado_retorna200() {
        mockToken(TOKEN_ALIADO, "ALIADO");
        when(catalogoRepository.save(any(Catalogo.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = adminController.crearProducto(TOKEN_ALIADO,
            Map.of("articulo", "Camiseta Negra", "precio", "49900", "stock", "10"));

        assertEquals(200, res.getStatusCode().value());
        Catalogo creado = (Catalogo) res.getBody();
        assertEquals("Camiseta Negra", creado.getArticulo());
        assertEquals("activo", creado.getEstado());
    }

    @Test
    void crearProducto_sinArticulo_retorna400() {
        mockToken(TOKEN_ADMIN, "ADMIN");

        ResponseEntity<?> res = adminController.crearProducto(TOKEN_ADMIN, Map.of("precio", "49900"));

        assertEquals(400, res.getStatusCode().value());
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void crearProducto_sinToken_retorna401() {
        ResponseEntity<?> res = adminController.crearProducto(null, Map.of("articulo", "X"));

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void actualizarProducto_actualizaStock() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        Catalogo p = new Catalogo();
        p.setIdCatalogo(1);
        p.setArticulo("Camiseta");
        p.setStock(5);
        when(catalogoRepository.findById(1)).thenReturn(Optional.of(p));
        when(catalogoRepository.save(any(Catalogo.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = adminController.actualizarProducto(TOKEN_ADMIN, 1, Map.of("stock", "100"));

        assertEquals(200, res.getStatusCode().value());
        assertEquals(100, ((Catalogo) res.getBody()).getStock());
    }

    @Test
    void actualizarProducto_inexistente_retorna404() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        when(catalogoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> res = adminController.actualizarProducto(TOKEN_ADMIN, 99, Map.of("stock", "5"));

        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void eliminarProducto_existente_retorna200() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        when(catalogoRepository.existsById(1)).thenReturn(true);

        ResponseEntity<?> res = adminController.eliminarProducto(TOKEN_ADMIN, 1);

        assertEquals(200, res.getStatusCode().value());
        verify(catalogoRepository, times(1)).deleteById(1);
    }

    @Test
    void eliminarProducto_noAdmin_retorna401() {
        mockToken(TOKEN_ALIADO, "ALIADO");

        ResponseEntity<?> res = adminController.eliminarProducto(TOKEN_ALIADO, 1);

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void eliminarUsuario_adminNoEliminable_retorna400() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        Usuario admin = new Usuario();
        admin.setIdUsuario(1);
        admin.setCorreoUsuario("admin@lumura.com");
        admin.setRol("ADMIN");
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        ResponseEntity<?> res = adminController.eliminarUsuario(TOKEN_ADMIN, 1);

        assertEquals(400, res.getStatusCode().value());
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void actualizarPedido_aEnviado_guardaGuiaTransportadoraYAgregaHistorial() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        Compra compra = new Compra();
        compra.setIdCompra(5);
        compra.setEstadoPedido("pendiente");
        compra.setHistorialEnvio("PENDIENTE@2026-08-27T10:00:00");
        when(compraRepository.findById(5)).thenReturn(Optional.of(compra));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = adminController.actualizarPedido(TOKEN_ADMIN, 5,
                Map.of("estado_pedido", "enviado", "numero_guia", "GUIA-123", "transportadora", "Interrapidisimo"));

        assertEquals(200, res.getStatusCode().value());
        assertEquals("enviado", compra.getEstadoPedido());
        assertEquals("GUIA-123", compra.getNumeroGuia());
        assertEquals("Interrapidisimo", compra.getTransportadora());
        assertTrue(compra.getHistorialEnvio().contains("PENDIENTE@"));
        assertTrue(compra.getHistorialEnvio().contains("ENVIADO@"));
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("GUIA-123", body.get("numero_guia"));
        assertEquals("Interrapidisimo", body.get("transportadora"));
    }

    @Test
    void actualizarPedido_estadoInvalido_retorna400() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        ResponseEntity<?> res = adminController.actualizarPedido(TOKEN_ADMIN, 5, Map.of("estado_pedido", "perdido"));
        assertEquals(400, res.getStatusCode().value());
        verify(compraRepository, never()).save(any());
    }

    @Test
    void listarUsuarios_excluyeAdminAutenticado() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        when(jwtUtil.getUserIdFromToken("token-admin")).thenReturn(1);

        Usuario admin = new Usuario();
        admin.setIdUsuario(1);
        admin.setRol("ADMIN");
        admin.setCorreoUsuario("admin@lumura.com");
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(2);
        cliente.setRol("USER");
        cliente.setCorreoUsuario("cliente@lumura.com");
        when(usuarioRepository.findAll()).thenReturn(List.of(admin, cliente));

        ResponseEntity<?> res = adminController.listarUsuarios(TOKEN_ADMIN);

        assertEquals(200, res.getStatusCode().value());
        List<?> body = (List<?>) res.getBody();
        assertEquals(1, body.size());
        Map<?, ?> unico = (Map<?, ?>) body.get(0);
        assertEquals(2, unico.get("id_usuario"));
        assertEquals("cliente@lumura.com", unico.get("correo_usuario"));
    }

    @Test
    void bloquearUsuario_valido_retorna200() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(2);
        cliente.setRol("USER");
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = adminController.bloquearUsuario(TOKEN_ADMIN, 2, Map.of("motivo", "Fraude", "dias", "7"));

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(true, body.get("bloqueado"));
        assertEquals("Fraude", body.get("motivo_bloqueo"));
        assertTrue(cliente.getBloqueado());
        assertEquals("Fraude", cliente.getMotivoBloqueo());
        assertNotNull(cliente.getBloqueoHasta());
        assertTrue(cliente.getBloqueoHasta().isAfter(LocalDateTime.now()));
    }

    @Test
    void bloquearUsuario_esAdmin_retorna400() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        Usuario otroAdmin = new Usuario();
        otroAdmin.setIdUsuario(1);
        otroAdmin.setRol("ADMIN");
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(otroAdmin));

        ResponseEntity<?> res = adminController.bloquearUsuario(TOKEN_ADMIN, 1, Map.of("motivo", "X", "dias", "3"));

        assertEquals(400, res.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void bloquearUsuario_sinMotivo_retorna400() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(2);
        cliente.setRol("USER");
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));

        ResponseEntity<?> res = adminController.bloquearUsuario(TOKEN_ADMIN, 2, Map.of("dias", "3"));

        assertEquals(400, res.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void bloquearUsuario_diasInvalido_retorna400() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(2);
        cliente.setRol("USER");
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));

        ResponseEntity<?> res = adminController.bloquearUsuario(TOKEN_ADMIN, 2, Map.of("motivo", "X", "dias", "0"));

        assertEquals(400, res.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void bloquearUsuario_inexistente_retorna404() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> res = adminController.bloquearUsuario(TOKEN_ADMIN, 99, Map.of("motivo", "X", "dias", "3"));

        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void desbloquearUsuario_retorna200() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(2);
        cliente.setRol("USER");
        cliente.setBloqueado(true);
        cliente.setMotivoBloqueo("Fraude");
        cliente.setBloqueoHasta(LocalDateTime.now().plusDays(7));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = adminController.desbloquearUsuario(TOKEN_ADMIN, 2);

        assertEquals(200, res.getStatusCode().value());
        assertFalse(cliente.getBloqueado());
        assertNull(cliente.getMotivoBloqueo());
        assertNull(cliente.getBloqueoHasta());
    }

    @Test
    void desbloquearUsuario_inexistente_retorna404() {
        mockToken(TOKEN_ADMIN, "ADMIN");
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> res = adminController.desbloquearUsuario(TOKEN_ADMIN, 99);

        assertEquals(404, res.getStatusCode().value());
    }
}
