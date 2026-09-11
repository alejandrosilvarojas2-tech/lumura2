package com.lumura.primeraApi.util;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resuelve la ruta física de las carpetas de archivos subidos (uploads y uploads-licencias)
 * de forma independiente del directorio de trabajo del proceso (user.dir).
 *
 * Si el valor configurado es absoluto se respeta tal cual; si es relativo se ancla al
 * directorio del proyecto (detectado por la presencia de pom.xml a partir de la ubicación
 * de esta clase en el classpath), de modo que el arranque desde cualquier CWD (jar, IDE,
 * mvnw, Docker) siga sirviendo los archivos guardados en <proyecto>/uploads.
 */
public final class UploadPaths {

    private UploadPaths() {
    }

    /**
     * Ruta absoluta normalizada de la carpeta indicada (ej. "uploads", "uploads-licencias").
     * Si es absoluta se respeta; si no, se resuelve contra el directorio del proyecto.
     */
    public static Path directorio(String dir) {
        Path p = Paths.get(dir);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return directorioProyecto().resolve(p).normalize();
    }

    /**
     * Ubicación tipo "file:" apta para {@code ResourceHandlerRegistry.addResourceLocations}.
     */
    public static String resourceLocation(String dir) {
        return directorio(dir).toUri().toString();
    }

    /**
     * Directorio raíz del proyecto (el que contiene pom.xml). Se localiza subiendo desde la
     * ubicación de esta clase en el classpath (target/classes, target/test-classes o el jar);
     * como último recurso usa el directorio de trabajo.
     */
    static Path directorioProyecto() {
        try {
            URL location = UploadPaths.class.getProtectionDomain() != null
                    ? UploadPaths.class.getProtectionDomain().getCodeSource() != null
                    ? UploadPaths.class.getProtectionDomain().getCodeSource().getLocation()
                    : null
                    : null;
            Path origen = location != null ? aPath(location) : null;
            if (origen != null) {
                Path p = Files.isRegularFile(origen) ? origen.getParent() : origen;
                while (p != null) {
                    if (Files.isDirectory(p) && Files.exists(p.resolve("pom.xml"))) {
                        return p;
                    }
                    p = p.getParent();
                }
            }
        } catch (Exception ignored) {
            // fallback al CWD
        }
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    /**
     * Convierte una URL del classpath a {@link Path}. Maneja los dos formatos que deja
     * Spring Boot: el jar "fat" genera URLs {@code jar:file:/...jar!/BOOT-INF/classes/}
     * (que {@code Paths.get(uri)} no sabe convertir) y una URL {@code file:} normal
     * (dev con {@code target/classes}). Devuelve {@code null} si no es convertible.
     */
    static Path aPath(URL location) {
        try {
            URI uri = location.toURI();
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                String especifica = uri.getSchemeSpecificPart();
                int separador = especifica.indexOf("!/");
                if (separador >= 0) {
                    especifica = especifica.substring(0, separador);
                }
                return Paths.get(URI.create("file:" + especifica)).toAbsolutePath().normalize();
            }
            String s = location.toString();
            if (s.startsWith("jar:")) {
                int separador = s.indexOf("!/");
                if (separador >= 0) {
                    return Paths.get(URI.create(s.substring("jar:".length(), separador)))
                            .toAbsolutePath().normalize();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}