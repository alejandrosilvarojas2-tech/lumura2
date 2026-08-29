package com.lumura.primeraApi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagoSimuladoServiceTest {

    @Test
    void tarjetaVisaValidaAprobada() {
        assertEquals("APROBADO", PagoSimuladoService.realizarPago("4111 1111 1111 1111", "12", "28", "123"));
    }

    @Test
    void tarjetaMastercardValidaAprobada() {
        assertEquals("APROBADO", PagoSimuladoService.realizarPago("5500 0000 0000 0004", "05", "30", "1234"));
    }

    @Test
    void numeroInvalidoRechazado() {
        assertEquals("Tarjeta rechazada por la entidad", PagoSimuladoService.realizarPago("1234 5678 9012 3456", "12", "28", "123"));
    }

    @Test
    void numeroCortoRechazado() {
        assertEquals("Número de tarjeta inválido", PagoSimuladoService.realizarPago("123", "12", "28", "123"));
    }

    @Test
    void tarjetaVencidaRechazada() {
        assertEquals("Tarjeta vencida", PagoSimuladoService.realizarPago("4111 1111 1111 1111", "01", "20", "123"));
    }

    @Test
    void mesInvalidoRechazado() {
        assertEquals("Mes de expiración inválido", PagoSimuladoService.realizarPago("4111 1111 1111 1111", "13", "28", "123"));
    }

    @Test
    void cvvInvalidoRechazado() {
        assertEquals("CVV inválido", PagoSimuladoService.realizarPago("4111 1111 1111 1111", "12", "28", "12"));
    }

    @Test
    void datosIncompletos() {
        assertEquals("Datos de pago incompletos", PagoSimuladoService.realizarPago("", "12", "28", ""));
    }

    @Test
    void resultadoAprobadoTieneReferencia() {
        var r = PagoSimuladoService.resultado("APROBADO");
        assertTrue((Boolean) r.get("aprobado"));
        assertTrue(((String) r.get("referencia")).startsWith("SIM-"));
    }

    @Test
    void resultadoRechazadoSinReferencia() {
        var r = PagoSimuladoService.resultado("Tarjeta vencida");
        assertEquals(false, r.get("aprobado"));
        assertEquals(null, r.get("referencia"));
    }
}
