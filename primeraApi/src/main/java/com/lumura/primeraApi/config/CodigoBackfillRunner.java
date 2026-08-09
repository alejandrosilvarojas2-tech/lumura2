package com.lumura.primeraApi.config;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CatalogoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CodigoBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CodigoBackfillRunner.class);

    private final CatalogoRepository catalogoRepository;

    public CodigoBackfillRunner(CatalogoRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    @Override
    public void run(String... args) {
        List<Catalogo> sinCodigo = catalogoRepository.findAll().stream()
                .filter(p -> p.getCodigo() == null || p.getCodigo().isBlank())
                .toList();
        for (Catalogo p : sinCodigo) {
            if (p.getIdCatalogo() == null) continue;
            p.setCodigo("LUM-" + String.format("%06d", p.getIdCatalogo()));
            catalogoRepository.save(p);
        }
        if (!sinCodigo.isEmpty()) {
            log.info("Códigos asignados a {} productos sin código", sinCodigo.size());
        }
    }
}
