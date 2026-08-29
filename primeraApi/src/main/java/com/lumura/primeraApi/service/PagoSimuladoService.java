package com.lumura.primeraApi.service;

import java.time.YearMonth;
import java.util.Map;

/**
 * Pasarela de pago simulada (modo offline/desarrollo).
 *
 * Valida los datos de una tarjeta de prueba mediante el algoritmo de Luhn y
 * devuelve una respuesta de autorización. NUNCA almacena ni el número ni el CVV.
 *
 * Tarjetas de prueba:
 *   - OK:   4111 1111 1111 1111 (Visa), 5500 0000 0000 0004 (Mastercard)
 *   - Rechazadas: números que no pasan Luhn.
 */
public class PagoSimuladoService {

    public static String realizarPago(String numeroTarjeta, String mesExpiracion, String anioExpiracion, String cvv) {
        // Validaciones de formato
        if (numeroTarjeta == null || cvv == null || mesExpiracion == null || anioExpiracion == null) {
            return "Datos de pago incompletos";
        }
        String numero = numeroTarjeta.trim().replaceAll("\\s", "").replaceAll("-", "");
        if (numero.isEmpty() || numeroTarjeta.trim().isEmpty() || mesExpiracion.trim().isEmpty()
                || anioExpiracion.trim().isEmpty() || cvv.trim().isEmpty()) {
            return "Datos de pago incompletos";
        }
        if (!numero.matches("\\d{13,19}")) return "Número de tarjeta inválido";
        if (!luhn(numero)) return "Tarjeta rechazada por la entidad";
        if (!cvv.matches("\\d{3,4}")) return "CVV inválido";

        // Mes/anio de expiración (formato MM / YY o MM / YYYY)
        int mes;
        try {
            mes = Integer.parseInt(mesExpiracion);
        } catch (NumberFormatException e) {
            return "Mes de expiración inválido";
        }
        int anio;
        try {
            anio = Integer.parseInt(anioExpiracion);
            if (anio < 100) anio += 2000;
        } catch (NumberFormatException e) {
            return "Año de expiración inválido";
        }
        if (mes < 1 || mes > 12) return "Mes de expiración inválido";
        if (YearMonth.now().isAfter(YearMonth.of(anio, mes))) return "Tarjeta vencida";

        return "APROBADO";
    }

    // Algoritmo de Luhn para validación de número de tarjeta
    private static boolean luhn(String numero) {
        int suma = 0;
        boolean doble = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (doble) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            suma += d;
            doble = !doble;
        }
        return suma % 10 == 0;
    }

    public static Map<String, Object> resultado(String estado) {
        Map<String, Object> r = new java.util.HashMap<>();
        r.put("aprobado", "APROBADO".equals(estado));
        r.put("detalle", estado);
        r.put("referencia", "APROBADO".equals(estado)
                ? "SIM-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : null);
        return r;
    }
}
