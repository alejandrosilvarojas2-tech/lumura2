package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.DetalleCompra;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.DetalleCompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AliadoControllerTest {

    @Mock
    private CatalogoRepository catalogoRepository;
    @Mock
    private DetalleCompraRepository detalleCompraRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AliadoController aliadoController;

    private static final String TOKEN_ALIADO = "Bearer token-aliado";
    private static final String TOKEN_ADMIN = "Bearer token-admin";
    private static final String TOKEN_CLIENTE = "Bearer token-cliente";

    private void mockToken(String auth, Integer userId, String rol) {
        String token = auth.substring(7);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getRolFromToken(token)).thenReturn(rol);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
    }

    private Catalogo producto(String nombre, int stock, String precio) {
        Catalogo p = new Catalogo();
        p.setArticulo(nombre);
        p.setStock(stock);
        p.setPrecio(new BigDecimal(precio));
        return p;
    }

    private Catalogo productoDeAliado(Integer idCatalogo, Integer idAliado) {
        Catalogo p = producto("Camiseta", 10, "45900");
        p.setIdCatalogo(idCatalogo);
        p.setIdAliado(idAliado);
        return p;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> body(Object... kv) {
        Map<String, Object> m = new java.util.HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return (Map<String, String>) (Map<?, ?>) m;
    }

    private DetalleCompra venta(Integer idCompra, Integer idCatalogo, String articulo, int cantidad, String precio) {
        DetalleCompra d = new DetalleCompra();
        d.setIdCompra(idCompra);
        d.setIdCatalogo(idCatalogo);
        d.setArticulo(articulo);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(new BigDecimal(precio));
        return d;
    }

    @Test
    void productos_aliado_veSoloLosSuyos() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.findByIdAliado(7)).thenReturn(List.of(producto("Camiseta", 10, "1000")));

        ResponseEntity<?> res = aliadoController.productos(TOKEN_ALIADO);

        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, ((List<?>) res.getBody()).size());
        verify(catalogoRepository, never()).findAll();
    }

    @Test
    void productos_admin_veTodos() {
        mockToken(TOKEN_ADMIN, 4, "ADMIN");
        when(catalogoRepository.findAll()).thenReturn(List.of(
                producto("A", 1, "1000"), producto("B", 2, "2000")));

        ResponseEntity<?> res = aliadoController.productos(TOKEN_ADMIN);

        assertEquals(200, res.getStatusCode().value());
        assertEquals(2, ((List<?>) res.getBody()).size());
        verify(catalogoRepository, never()).findByIdAliado(any());
    }

    @Test
    void productos_cliente_retorna401() {
        String token = TOKEN_CLIENTE.substring(7);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getRolFromToken(token)).thenReturn("USER");

        ResponseEntity<?> res = aliadoController.productos(TOKEN_CLIENTE);

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void productos_sinToken_retorna401() {
        ResponseEntity<?> res = aliadoController.productos(null);

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void dashboard_calculaMetricasDelInventario() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.findByIdAliado(7)).thenReturn(List.of(
                producto("Camiseta", 10, "45900"),
                producto("Jeans", 5, "89900")));

        ResponseEntity<?> res = aliadoController.dashboard(TOKEN_ALIADO);

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(2L, body.get("total_productos"));
        assertEquals(15L, body.get("unidades_stock"));
        assertEquals(new BigDecimal("908500"), body.get("valor_inventario"));
    }

    @Test
    void dashboard_conProductoSinStockNoFalla() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        Catalogo sinDatos = new Catalogo();
        sinDatos.setArticulo("Sin stock definido");
        when(catalogoRepository.findByIdAliado(7)).thenReturn(List.of(sinDatos));

        ResponseEntity<?> res = aliadoController.dashboard(TOKEN_ALIADO);

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(0L, body.get("unidades_stock"));
        assertEquals(BigDecimal.ZERO, body.get("valor_inventario"));
    }

    // ---------- crearProducto ----------

    @Test
    void crearProducto_nuevo_seAsignaAlAliadoConCodigo() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.save(any(Catalogo.class))).thenAnswer(inv -> {
            Catalogo c = inv.getArgument(0);
            c.setIdCatalogo(50);
            return c;
        });

        ResponseEntity<?> res = aliadoController.crearProducto(TOKEN_ALIADO,
                body("articulo", "Tenis Urbanos", "precio", "159900", "stock", "20",
                        "categoria", "Zapatos", "descripcion", "Unos tenis muy cómodos"));

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Catalogo> captor = ArgumentCaptor.forClass(Catalogo.class);
        verify(catalogoRepository).save(captor.capture());
        assertEquals("Tenis Urbanos", captor.getValue().getArticulo());
        assertEquals(7, captor.getValue().getIdAliado());
        assertNotNull(captor.getValue().getCodigo()); // código autogenerado
    }

    @Test
    void crearProducto_sinArticulo_retorna400() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");

        ResponseEntity<?> res = aliadoController.crearProducto(TOKEN_ALIADO, body("precio", "100"));

        assertEquals(400, res.getStatusCode().value());
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void crearProducto_precioOStockInvalidos_retorna400() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");

        assertEquals(400, aliadoController.crearProducto(TOKEN_ALIADO, body("articulo", "X", "precio", "abc")).getStatusCode().value());
        assertEquals(400, aliadoController.crearProducto(TOKEN_ALIADO, body("articulo", "X", "precio", "100", "stock", "-5")).getStatusCode().value());
        assertEquals(400, aliadoController.crearProducto(TOKEN_ALIADO, body("articulo", "X", "stock", "99999")).getStatusCode().value());
        verify(catalogoRepository, never()).save(any());
    }

    // ---------- actualizarProducto ----------

    @Test
    void actualizarProducto_propio_actualizaCampos() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        Catalogo p = productoDeAliado(5, 7);
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(p));

        ResponseEntity<?> res = aliadoController.actualizarProducto(
                TOKEN_ALIADO, 5, body("articulo", "Camiseta Premium", "precio", "49900", "stock", "25"));

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Catalogo> captor = ArgumentCaptor.forClass(Catalogo.class);
        verify(catalogoRepository).save(captor.capture());
        assertEquals("Camiseta Premium", captor.getValue().getArticulo());
        assertEquals(new BigDecimal("49900"), captor.getValue().getPrecio());
        assertEquals(25, captor.getValue().getStock());
    }

    @Test
    void actualizarProducto_ajeno_retorna403() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(productoDeAliado(5, 9)));

        ResponseEntity<?> res = aliadoController.actualizarProducto(
                TOKEN_ALIADO, 5, body("stock", "10"));

        assertEquals(403, res.getStatusCode().value());
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void actualizarProducto_adminPuedeEditarCualquiera() {
        mockToken(TOKEN_ADMIN, 1, "ADMIN");
        Catalogo ajeno = productoDeAliado(5, 9);
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(ajeno));

        ResponseEntity<?> res = aliadoController.actualizarProducto(TOKEN_ADMIN, 5, body("stock", "3"));

        assertEquals(200, res.getStatusCode().value());
        verify(catalogoRepository).save(any(Catalogo.class));
    }

    @Test
    void actualizarProducto_precioNegativo_retorna400() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(productoDeAliado(5, 7)));

        ResponseEntity<?> res = aliadoController.actualizarProducto(
                TOKEN_ALIADO, 5, body("precio", "-100"));

        assertEquals(400, res.getStatusCode().value());
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void actualizarProducto_stockFueraDeRango_retorna400() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(productoDeAliado(5, 7)));

        assertEquals(400, aliadoController.actualizarProducto(TOKEN_ALIADO, 5, body("stock", "99999")).getStatusCode().value());
        assertEquals(400, aliadoController.actualizarProducto(TOKEN_ALIADO, 5, body("stock", "abc")).getStatusCode().value());
        verify(catalogoRepository, never()).save(any());
    }

    // ---------- eliminarProducto ----------

    @Test
    void eliminarProducto_propio_elimina() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(productoDeAliado(5, 7)));

        ResponseEntity<?> res = aliadoController.eliminarProducto(TOKEN_ALIADO, 5);

        assertEquals(200, res.getStatusCode().value());
        verify(catalogoRepository).deleteById(5);
    }

    @Test
    void eliminarProducto_ajeno_retorna403() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(productoDeAliado(5, 9)));

        ResponseEntity<?> res = aliadoController.eliminarProducto(TOKEN_ALIADO, 5);

        assertEquals(403, res.getStatusCode().value());
        verify(catalogoRepository, never()).deleteById(any());
    }

    @Test
    void eliminarProducto_inexistente_retorna404() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(catalogoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> res = aliadoController.eliminarProducto(TOKEN_ALIADO, 99);

        assertEquals(404, res.getStatusCode().value());
    }

    // ---------- ventas ----------

    @Test
    void ventas_calculaUnidadesEIngresosYPorProducto() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(detalleCompraRepository.findVentasDeAliado(7)).thenReturn(List.of(
                venta(20, 1, "Camiseta", 2, "45900"),
                venta(21, 1, "Camiseta", 1, "45900"),
                venta(22, 2, "Jeans", 1, "89900")));

        ResponseEntity<?> res = aliadoController.ventas(TOKEN_ALIADO);

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> bodyRta = (Map<?, ?>) res.getBody();
        assertEquals(4L, bodyRta.get("unidades_vendidas"));
        assertEquals(new BigDecimal("227600"), bodyRta.get("ingresos_totales")); // 45900x3 + 89900

        List<?> porProducto = (List<?>) bodyRta.get("ventas_por_producto");
        assertEquals(2, porProducto.size());

        List<?> ultimas = (List<?>) bodyRta.get("ultimas_ventas");
        assertEquals(3, ultimas.size());
    }

    @Test
    void ventas_sinVentas_retornaCeros() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(detalleCompraRepository.findVentasDeAliado(7)).thenReturn(List.of());

        ResponseEntity<?> res = aliadoController.ventas(TOKEN_ALIADO);

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> bodyRta = (Map<?, ?>) res.getBody();
        assertEquals(0L, bodyRta.get("unidades_vendidas"));
        assertEquals(BigDecimal.ZERO, bodyRta.get("ingresos_totales"));
        assertTrue(((List<?>) bodyRta.get("ventas_por_producto")).isEmpty());
    }

    @Test
    void ventas_cliente_retorna401() {
        String token = TOKEN_CLIENTE.substring(7);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getRolFromToken(token)).thenReturn("USER");

        ResponseEntity<?> res = aliadoController.ventas(TOKEN_CLIENTE);

        assertEquals(401, res.getStatusCode().value());
        verifyNoInteractions(detalleCompraRepository);
    }

    private Usuario usuarioAliado(Integer id) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setRol("ALIADO");
        return u;
    }

    @Test
    void obtenerLicencia_sinLicencia_retornaVacio() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuarioAliado(7)));

        ResponseEntity<?> res = aliadoController.obtenerLicencia(TOKEN_ALIADO);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("", ((Map<?, ?>) res.getBody()).get("licencia"));
    }

    @Test
    void obtenerLicencia_conLicencia_retornaUrl() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        Usuario u = usuarioAliado(7);
        u.setLicenciaDistribuidor("/uploads/licencia_7.jpg");
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(u));

        ResponseEntity<?> res = aliadoController.obtenerLicencia(TOKEN_ALIADO);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("/uploads/licencia_7.jpg", ((Map<?, ?>) res.getBody()).get("licencia"));
    }

    @Test
    void obtenerLicencia_cliente_retorna401() {
        String token = TOKEN_CLIENTE.substring(7);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getRolFromToken(token)).thenReturn("USER");

        ResponseEntity<?> res = aliadoController.obtenerLicencia(TOKEN_CLIENTE);

        assertEquals(401, res.getStatusCode().value());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void guardarLicencia_validUrl_guardaYLlamaSave() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");
        Usuario u = usuarioAliado(7);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(u));

        ResponseEntity<?> res = aliadoController.guardarLicencia(TOKEN_ALIADO, body("licencia", "/uploads/licencia_7.jpg"));

        assertEquals(200, res.getStatusCode().value());
        assertEquals("/uploads/licencia_7.jpg", u.getLicenciaDistribuidor());
        verify(usuarioRepository).save(u);
    }

    @Test
    void guardarLicencia_vacio_retorna400() {
        mockToken(TOKEN_ALIADO, 7, "ALIADO");

        ResponseEntity<?> res = aliadoController.guardarLicencia(TOKEN_ALIADO, body("licencia", "  "));

        assertEquals(400, res.getStatusCode().value());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void guardarLicencia_cliente_retorna401() {
        String token = TOKEN_CLIENTE.substring(7);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getRolFromToken(token)).thenReturn("USER");

        ResponseEntity<?> res = aliadoController.guardarLicencia(TOKEN_CLIENTE, body("licencia", "/uploads/x.jpg"));

        assertEquals(401, res.getStatusCode().value());
        verifyNoInteractions(usuarioRepository);
    }
}
