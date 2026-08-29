package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Favorito;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.FavoritoRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritoControllerTest {

    @Mock
    private FavoritoRepository favoritoRepository;
    @Mock
    private CatalogoRepository catalogoRepository;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private FavoritoController favoritoController;

    private static final String TOKEN = "Bearer token-user";

    private void mockToken(int userId) {
        when(jwtUtil.validateToken("token-user")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-user")).thenReturn(userId);
    }

    private Favorito fav(int idUsuario, int idCatalogo) {
        Favorito f = new Favorito();
        f.setIdFavorito(1);
        f.setIdUsuario(idUsuario);
        f.setIdCatalogo(idCatalogo);
        return f;
    }

    @Test
    @SuppressWarnings("unchecked")
    void listar_conToken_retornaIds() {
        mockToken(1);
        when(favoritoRepository.findByIdUsuarioOrderByFechaAgregadoDesc(1))
                .thenReturn(List.of(fav(1, 3), fav(1, 7)));

        ResponseEntity<?> res = favoritoController.listar(TOKEN);

        assertEquals(200, res.getStatusCode().value());
        List<Integer> ids = (List<Integer>) res.getBody();
        assertEquals(List.of(3, 7), ids);
    }

    @Test
    void listar_sinToken_retorna401() {
        ResponseEntity<?> res = favoritoController.listar(null);

        assertEquals(401, res.getStatusCode().value());
        verifyNoInteractions(favoritoRepository);
    }

    @Test
    void agregar_productoValido_guardaConDatosCompletos() {
        mockToken(1);
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(new Catalogo()));
        when(favoritoRepository.findByIdUsuarioAndIdCatalogo(1, 5)).thenReturn(Optional.empty());

        ResponseEntity<?> res = favoritoController.agregar(TOKEN, Map.of("id_catalogo", "5"));

        assertEquals(200, res.getStatusCode().value());
        ArgumentCaptor<Favorito> captor = ArgumentCaptor.forClass(Favorito.class);
        verify(favoritoRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getIdUsuario());
        assertEquals(5, captor.getValue().getIdCatalogo());
        assertNotNull(captor.getValue().getFechaAgregado());
    }

    @Test
    void agregar_duplicado_esIdempotenteYNoGuardaOtraVez() {
        mockToken(1);
        when(catalogoRepository.findById(5)).thenReturn(Optional.of(new Catalogo()));
        when(favoritoRepository.findByIdUsuarioAndIdCatalogo(1, 5)).thenReturn(Optional.of(fav(1, 5)));

        ResponseEntity<?> res = favoritoController.agregar(TOKEN, Map.of("id_catalogo", "5"));

        assertEquals(200, res.getStatusCode().value());
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void agregar_productoInexistente_retorna400() {
        mockToken(1);
        when(catalogoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> res = favoritoController.agregar(TOKEN, Map.of("id_catalogo", "99"));

        assertEquals(400, res.getStatusCode().value());
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void agregar_sinIdOCampoInvalido_retorna400() {
        mockToken(1);

        assertEquals(400, favoritoController.agregar(TOKEN, Map.of()).getStatusCode().value());
        assertEquals(400, favoritoController.agregar(TOKEN, Map.of("id_catalogo", "abc")).getStatusCode().value());
        assertEquals(400, favoritoController.agregar(TOKEN, null).getStatusCode().value());
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void agregar_sinToken_retorna401() {
        ResponseEntity<?> res = favoritoController.agregar(null, Map.of("id_catalogo", "5"));

        assertEquals(401, res.getStatusCode().value());
        verifyNoInteractions(catalogoRepository, favoritoRepository);
    }

    @Test
    void eliminar_existente_loBorra() {
        mockToken(1);
        when(favoritoRepository.findByIdUsuarioAndIdCatalogo(1, 5)).thenReturn(Optional.of(fav(1, 5)));

        ResponseEntity<?> res = favoritoController.eliminar(TOKEN, 5);

        assertEquals(200, res.getStatusCode().value());
        verify(favoritoRepository).deleteByIdUsuarioAndIdCatalogo(1, 5);
    }

    @Test
    void eliminar_inexistente_retorna404() {
        mockToken(1);
        when(favoritoRepository.findByIdUsuarioAndIdCatalogo(1, 8)).thenReturn(Optional.empty());

        ResponseEntity<?> res = favoritoController.eliminar(TOKEN, 8);

        assertEquals(404, res.getStatusCode().value());
        verify(favoritoRepository, never()).deleteByIdUsuarioAndIdCatalogo(any(), any());
    }
}
