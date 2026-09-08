package com.lumura.primeraApi.service;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CatalogoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private CatalogoRepository catalogoRepository;

    @InjectMocks
    private CatalogoService catalogoService;

    @Test
    void crearProducto_valido_conAliado_asignaDatosYCodigoAleatorio() {
        when(catalogoRepository.save(any(Catalogo.class))).thenAnswer(inv -> inv.getArgument(0));

        Catalogo p = catalogoService.crearProducto(Map.of(
                "articulo", "Tenis Urbanos",
                "precio", "159900",
                "stock", "20",
                "categoria", "Zapatos",
                "descripcion", "Unos tenis muy cómodos"), 7);

        assertEquals("Tenis Urbanos", p.getArticulo());
        assertEquals("159900", p.getPrecio().toPlainString());
        assertEquals(20, p.getStock());
        assertEquals("Zapatos", p.getCategoria());
        assertEquals("activo", p.getEstado());
        assertEquals(7, p.getIdAliado());
        assertNotNull(p.getCodigo());
        assertFalse(p.getCodigo().isBlank());
        verify(catalogoRepository).save(any(Catalogo.class));
    }

    @Test
    void crearProducto_admin_sinAliado_generaCodigoLUMTrasGuardar() {
        when(catalogoRepository.save(any(Catalogo.class))).thenAnswer(inv -> {
            Catalogo c = inv.getArgument(0);
            c.setIdCatalogo(50);
            return c;
        });

        Catalogo p = catalogoService.crearProducto(Map.of("articulo", "Camiseta Negra", "precio", "49900", "stock", "10"), null);

        assertEquals("LUM-000050", p.getCodigo());
        assertEquals("49900", p.getPrecio().toPlainString());
        assertEquals(10, p.getStock());
        verify(catalogoRepository, times(2)).save(any(Catalogo.class));
    }

    @Test
    void crearProducto_conCodigoEnBody_usaElDelBody() {
        when(catalogoRepository.save(any(Catalogo.class))).thenAnswer(inv -> inv.getArgument(0));

        Catalogo p = catalogoService.crearProducto(Map.of("articulo", "Blusa Floral", "precio", "89900", "stock", "5", "codigo", "BLUSA01"), 3);

        assertEquals("BLUSA01", p.getCodigo());
        verify(catalogoRepository).save(any(Catalogo.class));
    }

    @Test
    void crearProducto_sinArticulo_lanzaYNoGuarda() {
        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.crearProducto(Map.of("precio", "49900"), 7));
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void crearProducto_precioInvalido_lanzaYNoGuarda() {
        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.crearProducto(Map.of("articulo", "X", "precio", "abc"), 7));
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void crearProducto_precioNegativo_lanzaYNoGuarda() {
        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.crearProducto(Map.of("articulo", "X", "precio", "-100"), 7));
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void crearProducto_stockFueraDeRango_lanzaYNoGuarda() {
        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.crearProducto(Map.of("articulo", "X", "precio", "100", "stock", "-5"), 7));
        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.crearProducto(Map.of("articulo", "X", "precio", "100", "stock", "99999"), 7));
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void crearProducto_stockInvalido_lanzaYNoGuarda() {
        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.crearProducto(Map.of("articulo", "X", "precio", "100", "stock", "abc"), 7));
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void crearProducto_bodyNull_lanzaYNoGuarda() {
        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.crearProducto(null, 7));
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void crearProducto_descuentoInvalido_lanzaYNoGuarda() {
        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.crearProducto(Map.of("articulo", "X", "precio", "100", "stock", "5", "precio_descuento", "abc"), 7));
        verify(catalogoRepository, never()).save(any());
    }
}