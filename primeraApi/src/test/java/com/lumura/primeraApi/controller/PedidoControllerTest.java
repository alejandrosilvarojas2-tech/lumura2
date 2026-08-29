package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Carrito;
import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Compra;
import com.lumura.primeraApi.entity.DetalleCompra;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private CompraRepository compraRepository;
    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private CatalogoRepository catalogoRepository;
    @Mock
    private DetalleCompraRepository detalleCompraRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private PedidoController pedidoController;

    private static final String TOKEN_USER = "Bearer token-user";
    private static final String TOKEN_OTRO = "Bearer token-otro";

    private void mockToken(String auth, Integer userId) {
        String token = auth.substring(7);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
    }

    private Carrito itemCarrito(Integer idCatalogo, String nombre, int cantidad) {
        Carrito item = new Carrito();
        item.setIdCatalogo(idCatalogo);
        item.setArticulo(nombre);
        item.setCantidad(cantidad);
        return item;
    }

    private Catalogo producto(Integer id, String nombre, String precio) {
        return producto(id, nombre, precio, 10);
    }

    private Catalogo producto(Integer id, String nombre, String precio, int stock) {
        Catalogo p = new Catalogo();
        p.setIdCatalogo(id);
        p.setArticulo(nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        return p;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> bodyPago() {
        Map<String, Object> m = Map.of(
                "articulo", "INTENTO DE FRAUDE",
                "total", "1",
                "metodo_pago", "Efectivo",
                "direccion_entrega", "Calle 1 #2-3");
        return (Map<String, String>) (Map<?, ?>) m;
    }

    @Test
    void crear_calculaTotalDesdeCarrito_eIgnoraElDelCliente() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findByIdUsuario(1)).thenReturn(List.of(
                itemCarrito(2, "Jeans Slim Fit", 2),
                itemCarrito(3, "Chaqueta Deportiva", 1)));
        when(catalogoRepository.findById(2)).thenReturn(Optional.of(producto(2, "Jeans Slim Fit", "89900")));
        when(catalogoRepository.findById(3)).thenReturn(Optional.of(producto(3, "Chaqueta Deportiva", "129900")));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setIdCompra(99);
            return c;
        });

        ResponseEntity<?> res = pedidoController.crear(TOKEN_USER, bodyPago());

        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> bodyRta = (Map<String, Object>) res.getBody();
        assertEquals(99, bodyRta.get("id"));
        assertEquals(new BigDecimal("309700"), bodyRta.get("total")); // 89900x2 + 129900

        ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository).save(captor.capture());
        Compra guardada = captor.getValue();
        assertEquals(new BigDecimal("309700"), guardada.getTotal(), "el total del cliente debe ignorarse");
        assertEquals(3, guardada.getCantidadObjetos());
        assertEquals("pendiente", guardada.getEstadoPedido());
        assertTrue(guardada.getHistorialEnvio().startsWith("PENDIENTE@"));

        ArgumentCaptor<List<DetalleCompra>> captorDetalles = ArgumentCaptor.forClass(List.class);
        verify(detalleCompraRepository).saveAll(captorDetalles.capture());
        List<DetalleCompra> detalles = captorDetalles.getValue();
        assertEquals(2, detalles.size());
        assertEquals(2, detalles.get(0).getIdCatalogo());
        assertEquals(new BigDecimal("89900"), detalles.get(0).getPrecioUnitario());
        assertEquals(2, detalles.get(0).getCantidad());
        assertEquals(99, detalles.get(0).getIdCompra());

        verify(carritoRepository).deleteByIdUsuario(1);
    }

    @Test
    void crear_carritoVacio_retorna400() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findByIdUsuario(1)).thenReturn(List.of());

        ResponseEntity<?> res = pedidoController.crear(TOKEN_USER, bodyPago());

        assertEquals(400, res.getStatusCode().value());
        verify(compraRepository, never()).save(any());
    }

    @Test
    void crear_descuentaStockDeCadaProducto() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findByIdUsuario(1)).thenReturn(List.of(
                itemCarrito(2, "Jeans Slim Fit", 3)));
        Catalogo jeans = producto(2, "Jeans Slim Fit", "89900", 5);
        when(catalogoRepository.findById(2)).thenReturn(Optional.of(jeans));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setIdCompra(100);
            return c;
        });

        ResponseEntity<?> res = pedidoController.crear(TOKEN_USER, bodyPago());

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Catalogo> captor = ArgumentCaptor.forClass(Catalogo.class);
        verify(catalogoRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getStock()); // 5 - 3
        verify(detalleCompraRepository).saveAll(any());
    }

    @Test
    void crear_conTarjetaValida_generaReferenciaYGuardaMetodoPago() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findByIdUsuario(1)).thenReturn(List.of(itemCarrito(2, "Jeans Slim Fit", 1)));
        when(catalogoRepository.findById(2)).thenReturn(Optional.of(producto(2, "Jeans Slim Fit", "89900", 5)));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setIdCompra(101);
            return c;
        });

        Map<String, Object> m = new java.util.HashMap<>();
        m.put("metodo_pago", "Tarjeta");
        m.put("direccion_entrega", "Calle 1");
        m.put("numero_tarjeta", "4111 1111 1111 1111");
        m.put("mes_expiracion", "12");
        m.put("anio_expiracion", "28");
        m.put("cvv", "123");
        ResponseEntity<?> res = pedidoController.crear(TOKEN_USER, (Map<String, String>) (Map<?, ?>) m);

        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> bodyRta = (Map<String, Object>) res.getBody();
        assertTrue(((String) bodyRta.get("referencia_pago")).startsWith("SIM-"));
        ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository).save(captor.capture());
        assertEquals("Tarjeta", captor.getValue().getMetodoPago());
    }

    @Test
    void crear_conTarjetaInvalida_rechazaPagoSinCrearPedido() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findByIdUsuario(1)).thenReturn(List.of(itemCarrito(2, "Jeans Slim Fit", 1)));
        when(catalogoRepository.findById(2)).thenReturn(Optional.of(producto(2, "Jeans Slim Fit", "89900", 5)));

        Map<String, Object> m = new java.util.HashMap<>();
        m.put("metodo_pago", "Tarjeta");
        m.put("numero_tarjeta", "1234 5678 9012 3456");
        m.put("mes_expiracion", "12");
        m.put("anio_expiracion", "28");
        m.put("cvv", "123");
        ResponseEntity<?> res = pedidoController.crear(TOKEN_USER, (Map<String, String>) (Map<?, ?>) m);

        assertEquals(400, res.getStatusCode().value());
        verify(compraRepository, never()).save(any());
        verify(detalleCompraRepository, never()).saveAll(any());
        verify(carritoRepository, never()).deleteByIdUsuario(any());
    }

    @Test
    void crear_stockInsuficiente_rechazaPedidoCompleto() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findByIdUsuario(1)).thenReturn(List.of(
                itemCarrito(2, "Jeans Slim Fit", 3)));
        when(catalogoRepository.findById(2)).thenReturn(Optional.of(producto(2, "Jeans Slim Fit", "89900", 2)));

        ResponseEntity<?> res = pedidoController.crear(TOKEN_USER, bodyPago());

        assertEquals(400, res.getStatusCode().value());
        verify(compraRepository, never()).save(any());
        verify(detalleCompraRepository, never()).saveAll(any());
        verify(catalogoRepository, never()).save(any());
        verify(carritoRepository, never()).deleteByIdUsuario(any());
    }

    @Test
    void crear_sinToken_retorna401() {
        ResponseEntity<?> res = pedidoController.crear(null, bodyPago());

        assertEquals(401, res.getStatusCode().value());
        verify(carritoRepository, never()).findByIdUsuario(any());
    }

    @Test
    void pedidos_propios_incluyenDetalles() {
        mockToken(TOKEN_USER, 1);
        Compra compra = new Compra();
        compra.setIdCompra(5);
        compra.setIdUsuario(1);
        when(compraRepository.findByIdUsuarioOrderByFechaPedidoDesc(1)).thenReturn(List.of(compra));
        DetalleCompra detalle = new DetalleCompra();
        detalle.setIdCompra(5);
        detalle.setArticulo("Camiseta");
        when(detalleCompraRepository.findByIdCompraIn(List.of(5))).thenReturn(List.of(detalle));

        ResponseEntity<?> res = pedidoController.pedidos(TOKEN_USER, 1);

        assertEquals(200, res.getStatusCode().value());
        List<Compra> pedidos = (List<Compra>) res.getBody();
        assertEquals(1, pedidos.get(0).getDetalles().size());
    }

    @Test
    void pedidos_propios_incluyenVendedorEnDetalles() {
        mockToken(TOKEN_USER, 1);
        Compra compra = new Compra();
        compra.setIdCompra(5);
        compra.setIdUsuario(1);
        when(compraRepository.findByIdUsuarioOrderByFechaPedidoDesc(1)).thenReturn(List.of(compra));

        Catalogo producto = new Catalogo();
        producto.setIdCatalogo(17);
        producto.setArticulo("Zapatillas");
        producto.setIdAliado(39);
        when(catalogoRepository.findAllById(any())).thenReturn(List.of(producto));

        Usuario vendedor = new Usuario();
        vendedor.setIdUsuario(39);
        vendedor.setRol("ALIADO");
        vendedor.setNombreUsuario("aguacate - sandra rojas");
        vendedor.setCorreoUsuario("sandrarojasmoda@gmail.com");
        vendedor.setTelefono("3182244198");
        vendedor.setNombreNegocio("aguacate");
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(vendedor));

        DetalleCompra detalle = new DetalleCompra();
        detalle.setIdCompra(5);
        detalle.setIdCatalogo(17);
        detalle.setArticulo("Zapatillas");
        when(detalleCompraRepository.findByIdCompraIn(List.of(5))).thenReturn(List.of(detalle));

        ResponseEntity<?> res = pedidoController.pedidos(TOKEN_USER, 1);

        assertEquals(200, res.getStatusCode().value());
        List<Compra> pedidos = (List<Compra>) res.getBody();
        Map<?, ?> infoVendedor = (Map<?, ?>) pedidos.get(0).getDetalles().get(0).getVendedor();
        assertEquals("aguacate - sandra rojas", infoVendedor.get("vendedor_nombre"));
        assertEquals("sandrarojasmoda@gmail.com", infoVendedor.get("vendedor_correo"));
        assertEquals("aguacate", infoVendedor.get("vendedor_negocio"));
    }

    @Test
    void pedidos_deOtroUsuario_retorna403() {
        mockToken(TOKEN_USER, 1);

        ResponseEntity<?> res = pedidoController.pedidos(TOKEN_USER, 7);

        assertEquals(403, res.getStatusCode().value());
        verify(compraRepository, never()).findByIdUsuarioOrderByFechaPedidoDesc(any());
    }

    private Compra compraDe(Integer id, Integer duenio, String estado) {
        Compra c = new Compra();
        c.setIdCompra(id);
        c.setIdUsuario(duenio);
        c.setEstadoPedido(estado);
        return c;
    }

    @Test
    void cancelar_pedidoPendientePropio_loMarcaCancelado() {
        mockToken(TOKEN_USER, 1);
        when(compraRepository.findById(5)).thenReturn(Optional.of(compraDe(5, 1, "pendiente")));
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = pedidoController.cancelar(TOKEN_USER, 5);

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository).save(captor.capture());
        assertEquals("cancelado", captor.getValue().getEstadoPedido());
        assertTrue(captor.getValue().getHistorialEnvio().contains("CANCELADO@"));
    }

    @Test
    void cancelar_pedidoAjeno_retorna403() {
        mockToken(TOKEN_USER, 1);
        when(compraRepository.findById(5)).thenReturn(Optional.of(compraDe(5, 9, "pendiente")));

        ResponseEntity<?> res = pedidoController.cancelar(TOKEN_USER, 5);

        assertEquals(403, res.getStatusCode().value());
        verify(compraRepository, never()).save(any());
    }

    @Test
    void cancelar_pedidoEntregado_retorna400() {
        mockToken(TOKEN_USER, 1);
        when(compraRepository.findById(5)).thenReturn(Optional.of(compraDe(5, 1, "entregado")));

        ResponseEntity<?> res = pedidoController.cancelar(TOKEN_USER, 5);

        assertEquals(400, res.getStatusCode().value());
        verify(compraRepository, never()).save(any());
    }

    @Test
    void cancelar_pedidoInexistente_retorna404() {
        mockToken(TOKEN_USER, 1);
        when(compraRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> res = pedidoController.cancelar(TOKEN_USER, 99);

        assertEquals(404, res.getStatusCode().value());
    }
}
