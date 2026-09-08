package com.lumura.primeraApi.service;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CatalogoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
public class CatalogoService {

    private static final String CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final CatalogoRepository catalogoRepository;

    public CatalogoService(CatalogoRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    /**
     * Crea y guarda un producto de catálogo con las validaciones compartidas
     * por admin y aliado.
     *
     * @param body      campos del producto (articulo obligatorio, precio >= 0, stock 0..10000)
     * @param idAliado  id del aliado dueño, o null si lo crea un admin
     * @return el producto guardado
     * @throws IllegalArgumentException con mensaje de validación si los datos son inválidos
     */
    public Catalogo crearProducto(Map<String, String> body, Integer idAliado) {
        if (body == null) throw new IllegalArgumentException("Faltan datos del producto");

        String articulo = body.get("articulo");
        if (articulo == null || articulo.isBlank()) {
            throw new IllegalArgumentException("El nombre del artículo es obligatorio");
        }

        BigDecimal precio;
        try {
            precio = new BigDecimal(body.getOrDefault("precio", "0"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Precio inválido");
        }
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }

        int stock;
        try {
            stock = Integer.parseInt(body.getOrDefault("stock", "0"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Stock inválido");
        }
        if (stock < 0 || stock > 10000) {
            throw new IllegalArgumentException("El stock debe estar entre 0 y 10.000");
        }

        Catalogo p = new Catalogo();
        p.setArticulo(articulo.trim());
        p.setPrecio(precio);
        p.setCategoria(body.get("categoria"));
        p.setTalla(body.get("talla"));
        p.setColor(body.get("color"));
        p.setStock(stock);
        p.setDescripcion(body.get("descripcion"));
        p.setImagenUrl(body.get("imagen_url"));
        p.setFechaCreacion(LocalDateTime.now());
        p.setEstado(body.getOrDefault("estado", "activo"));
        p.setIdAliado(idAliado);

        String descuento = body.get("precio_descuento");
        if (descuento != null && !descuento.isBlank()) {
            try {
                p.setPrecioDescuento(new BigDecimal(descuento));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Descuento inválido");
            }
        }

        String codigo = body.get("codigo");
        if (codigo != null && !codigo.isBlank()) {
            p.setCodigo(codigo);
            return catalogoRepository.save(p);
        }
        if (idAliado == null) {
            Catalogo guardado = catalogoRepository.save(p);
            if (guardado.getIdCatalogo() != null) {
                guardado.setCodigo("LUM-" + String.format("%06d", guardado.getIdCatalogo()));
                return catalogoRepository.save(guardado);
            }
            return guardado;
        }
        p.setCodigo(codigoAleatorio());
        return catalogoRepository.save(p);
    }

    private String codigoAleatorio() {
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) sb.append(CARACTERES_CODIGO.charAt(rnd.nextInt(CARACTERES_CODIGO.length())));
        return sb.toString();
    }
}