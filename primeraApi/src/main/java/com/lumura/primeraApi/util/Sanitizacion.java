package com.lumura.primeraApi.util;

import java.util.Locale;

/**
 * Sanitización de entrada compartida para evitar XSS almacenado en el catálogo.
 * Neutraliza los delimitadores de tag HTML (< y >) en textos libres y permite
 * solo URLs con esquemas seguros (http/https/relativas/data:image).
 */
public final class Sanitizacion {

    private Sanitizacion() {
    }

    public static String limpiarTexto(String s, int maxLongitud) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() > maxLongitud) t = t.substring(0, maxLongitud);
        return t.replace("<", "").replace(">", "");
    }

    public static boolean urlImagenPermitida(String url) {
        if (url == null) return true;
        String u = url.trim().toLowerCase(Locale.ROOT);
        if (u.isEmpty()) return true;
        if (u.startsWith("//")) return false;
        return u.startsWith("http://") || u.startsWith("https://")
                || u.startsWith("/") || u.startsWith("uploads/")
                || u.startsWith("images/") || u.startsWith("licencias/") || u.startsWith("data:image/");
    }
}