package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Favorito;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.FavoritoRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    private static final Logger log = LoggerFactory.getLogger(FavoritoController.class);

    private final FavoritoRepository favoritoRepository;
    private final CatalogoRepository catalogoRepository;
    private final JwtUtil jwtUtil;

    public FavoritoController(FavoritoRepository favoritoRepository,
                              CatalogoRepository catalogoRepository,
                              JwtUtil jwtUtil) {
        this.favoritoRepository = favoritoRepository;
        this.catalogoRepository = catalogoRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer idUsuario = extraerUserId(auth);
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        List<Integer> ids = favoritoRepository.findByIdUsuarioOrderByFechaAgregadoDesc(idUsuario)
                .stream().map(Favorito::getIdCatalogo).toList();
        return ResponseEntity.ok(ids);
    }

    @PostMapping
    public ResponseEntity<?> agregar(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @RequestBody(required = false) Map<String, String> body) {
        Integer idUsuario = extraerUserId(auth);
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Integer idCatalogo = parsearIdCatalogo(body);
        if (idCatalogo == null) return ResponseEntity.badRequest().body(Map.of("error", "Producto inválido"));
        if (catalogoRepository.findById(idCatalogo).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Producto no encontrado en catálogo"));
        }
        if (favoritoRepository.findByIdUsuarioAndIdCatalogo(idUsuario, idCatalogo).isPresent()) {
            return ResponseEntity.ok(Map.of("mensaje", "El producto ya está en favoritos"));
        }
        Favorito fav = new Favorito();
        fav.setIdUsuario(idUsuario);
        fav.setIdCatalogo(idCatalogo);
        fav.setFechaAgregado(LocalDateTime.now());
        favoritoRepository.save(fav);
        log.info("Favorito agregado: producto {} (userId={})", idCatalogo, idUsuario);
        return ResponseEntity.ok(Map.of("mensaje", "Producto agregado a favoritos"));
    }

    @DeleteMapping("/{idCatalogo}")
    @Transactional
    public ResponseEntity<?> eliminar(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @PathVariable Integer idCatalogo) {
        Integer idUsuario = extraerUserId(auth);
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        if (favoritoRepository.findByIdUsuarioAndIdCatalogo(idUsuario, idCatalogo).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        favoritoRepository.deleteByIdUsuarioAndIdCatalogo(idUsuario, idCatalogo);
        log.info("Favorito eliminado: producto {} (userId={})", idCatalogo, idUsuario);
        return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado de favoritos"));
    }

    private Integer parsearIdCatalogo(Map<String, String> body) {
        if (body == null || body.get("id_catalogo") == null || body.get("id_catalogo").isBlank()) return null;
        try {
            return Integer.parseInt(body.get("id_catalogo").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extraerUserId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }
}
