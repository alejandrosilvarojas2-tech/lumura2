package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.service.PagoSimuladoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint de la pasarela de pago SIMULADA (modo offline).
 * NO almacena datos de tarjeta. Solo valida y devuelve autorización.
 */
@RestController
@RequestMapping("/api/pago")
public class PagoController {

    @PostMapping("/procesar")
    public ResponseEntity<?> procesar(@RequestBody Map<String, String> body) {
        String numero = body.get("numero_tarjeta");
        String mes = body.get("mes_expiracion");
        String anio = body.get("anio_expiracion");
        String cvv = body.get("cvv");

        String estado = PagoSimuladoService.realizarPago(numero, mes, anio, cvv);
        if ("APROBADO".equals(estado)) {
            return ResponseEntity.ok(PagoSimuladoService.resultado(estado));
        }
        return ResponseEntity.badRequest().body(PagoSimuladoService.resultado(estado));
    }
}
