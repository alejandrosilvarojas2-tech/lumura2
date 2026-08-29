package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Carrito;
import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.CarritoRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoControllerTest {

    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private CatalogoRepository catalogoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private CarritoController carritoController;

    private static final String TOKEN_USER = "Bearer token-user";
    private static final String TOKEN_OTRO = "Bearer token-otro";

    private void mockToken(String auth, Integer userId) {
        String token = auth.substring(7);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
    }

    private Catalogo producto(int id, String nombre, int stock) {
        Catalogo p = new Catalogo();
        p.setIdCatalogo(id);
        p.setArticulo(nombre);
        p.setStock(stock);
        p.setPrecio(new BigDecimal("45900"));
        return p;
    }

    private Usuario vendedor(int id, String nombre, String correo, String telefono) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombreUsuario(nombre);
        u.setCorreoUsuario(correo);
        u.setTelefono(telefono);
        return u;
    }

    @Test
    void agregar_conIdCatalogo_guardaFkYNombreCanonico() {
        mockToken(TOKEN_USER, 1);
        when(catalogoRepository.findById(2)).thenReturn(Optional.of(producto(2, "Jeans Slim Fit", 30)));

        ResponseEntity<?> res = carritoController.agregar(TOKEN_USER,
                Map.of("id_catalogo", "2", "articulo", "nombre-inventado", "cantidad", "1"));

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Carrito> captor = ArgumentCaptor.forClass(Carrito.class);
        verify(carritoRepository).save(captor.capture());
        Carrito guardado = captor.getValue();
        assertEquals(2, guardado.getIdCatalogo());
        assertEquals("Jeans Slim Fit", guardado.getArticulo());
        assertEquals(1, guardado.getIdUsuario());
    }

    @Test
    void agregar_productoInexistente_retorna400() {
        mockToken(TOKEN_USER, 1);
        when(catalogoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> res = carritoController.agregar(TOKEN_USER,
                Map.of("id_catalogo", "99", "cantidad", "1"));

        assertEquals(400, res.getStatusCode().value());
        verify(carritoRepository, never()).save(any());
    }

    @Test
    void agregar_stockInsuficiente_retorna400() {
        mockToken(TOKEN_USER, 1);
        when(catalogoRepository.findById(3)).thenReturn(Optional.of(producto(3, "Chaqueta", 5)));

        ResponseEntity<?> res = carritoController.agregar(TOKEN_USER,
                Map.of("id_catalogo", "3", "cantidad", "10"));

        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void listar_resuelvePrecioPorFkAunqueCambieElNombre() {
        mockToken(TOKEN_USER, 1);

        Carrito item = new Carrito();
        item.setIdCarrito(10);
        item.setIdUsuario(1);
        item.setIdCatalogo(5);
        item.setArticulo("Nombre viejo en carrito");
        item.setCantidad(2);
        when(carritoRepository.findByIdUsuario(1)).thenReturn(List.of(item));
        when(catalogoRepository.findAllById(List.of(5))).thenReturn(List.of(producto(5, "Nombre nuevo", 40)));

        ResponseEntity<?> res = carritoController.listar(TOKEN_USER, 1);

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> fila = ((List<Map<String, Object>>) res.getBody()).get(0);
        assertEquals(new BigDecimal("45900"), fila.get("precio"));
        assertEquals(5, fila.get("id_catalogo"));
    }

    @Test
    void listar_incluyeDatosDeContactoDelVendedor() {
        mockToken(TOKEN_USER, 1);

        Carrito item = new Carrito();
        item.setIdCarrito(11);
        item.setIdUsuario(1);
        item.setIdCatalogo(5);
        item.setArticulo("Camisa");
        item.setCantidad(1);
        when(carritoRepository.findByIdUsuario(1)).thenReturn(List.of(item));

        Catalogo prod = producto(5, "Camisa", 10);
        prod.setIdAliado(8);
        when(catalogoRepository.findAllById(List.of(5))).thenReturn(List.of(prod));
        when(usuarioRepository.findAllById(Set.of(8)))
                .thenReturn(List.of(vendedor(8, "Moda Express", "aliado@test.com", "3201112233")));

        ResponseEntity<?> res = carritoController.listar(TOKEN_USER, 1);

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> fila = ((List<Map<String, Object>>) res.getBody()).get(0);
        assertEquals("Moda Express", fila.get("vendedor_nombre"));
        assertEquals("aliado@test.com", fila.get("vendedor_correo"));
        assertEquals("3201112233", fila.get("vendedor_telefono"));
    }

    @Test
    void listar_deOtroUsuario_retorna403() {
        mockToken(TOKEN_USER, 1);

        ResponseEntity<?> res = carritoController.listar(TOKEN_USER, 7);

        assertEquals(403, res.getStatusCode().value());
        verify(carritoRepository, never()).findByIdUsuario(any());
    }

    private Carrito itemDe(Integer idCarrito, Integer duenio) {
        Carrito item = new Carrito();
        item.setIdCarrito(idCarrito);
        item.setIdUsuario(duenio);
        item.setIdCatalogo(2);
        item.setArticulo("Jeans Slim Fit");
        item.setCantidad(1);
        return item;
    }

    @Test
    void actualizar_itemAjeno_retorna403() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findById(20)).thenReturn(Optional.of(itemDe(20, 9)));

        ResponseEntity<?> res = carritoController.actualizar(TOKEN_USER, 20, Map.of("cantidad", "3"));

        assertEquals(403, res.getStatusCode().value());
        verify(carritoRepository, never()).save(any());
    }

    @Test
    void actualizar_cantidadValida_guarda() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findById(20)).thenReturn(Optional.of(itemDe(20, 1)));
        when(catalogoRepository.findById(2)).thenReturn(Optional.of(producto(2, "Jeans Slim Fit", 30)));

        ResponseEntity<?> res = carritoController.actualizar(TOKEN_USER, 20, Map.of("cantidad", "3"));

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Carrito> captor = ArgumentCaptor.forClass(Carrito.class);
        verify(carritoRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getCantidad());
    }

    @Test
    void eliminar_itemAjeno_retorna403() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findById(30)).thenReturn(Optional.of(itemDe(30, 9)));

        ResponseEntity<?> res = carritoController.eliminar(TOKEN_USER, 30);

        assertEquals(403, res.getStatusCode().value());
        verify(carritoRepository, never()).delete(any(Carrito.class));
    }

    @Test
    void eliminar_itemInexistente_retorna404() {
        mockToken(TOKEN_USER, 1);
        when(carritoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> res = carritoController.eliminar(TOKEN_USER, 99);

        assertEquals(404, res.getStatusCode().value());
    }
}
