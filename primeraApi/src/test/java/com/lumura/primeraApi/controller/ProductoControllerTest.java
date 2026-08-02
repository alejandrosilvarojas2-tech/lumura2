package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CatalogoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private CatalogoRepository catalogoRepository;

    @InjectMocks
    private ProductoController productoController;

    private Catalogo producto() {
        Catalogo p = new Catalogo();
        p.setIdCatalogo(1);
        p.setArticulo("Camiseta Premium");
        p.setPrecio(new BigDecimal("59900.00"));
        p.setStock(10);
        return p;
    }

    @Test
    void listar_retornaTodosLosProductos() {
        when(catalogoRepository.findAll()).thenReturn(List.of(producto()));

        List<Catalogo> res = productoController.listar();

        assertEquals(1, res.size());
        assertEquals("Camiseta Premium", res.get(0).getArticulo());
    }

    @Test
    void detalle_productoExistente_retorna200() {
        when(catalogoRepository.findById(1)).thenReturn(Optional.of(producto()));

        ResponseEntity<Catalogo> res = productoController.detalle(1);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Camiseta Premium", res.getBody().getArticulo());
    }

    @Test
    void detalle_productoInexistente_retorna404() {
        when(catalogoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<Catalogo> res = productoController.detalle(99);

        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void listarPaginado_retornaEstructuraConDatos() {
        Page<Catalogo> page = new PageImpl<>(List.of(producto()), PageRequest.of(0, 12), 1);
        when(catalogoRepository.findAll(any(PageRequest.class))).thenReturn(page);

        ResponseEntity<?> res = productoController.listarPaginado(0, 12);

        assertEquals(200, res.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(1, ((List<?>) body.get("content")).size());
        assertEquals(1, body.get("totalPages"));
        assertEquals(0, body.get("currentPage"));
    }
}
