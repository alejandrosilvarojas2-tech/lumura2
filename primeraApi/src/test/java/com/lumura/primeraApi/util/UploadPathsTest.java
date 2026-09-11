package com.lumura.primeraApi.util;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class UploadPathsTest {

    @Test
    void directorio_relativo_seAnclaAlProyecto_yNoAlCwd() {
        Path dir = UploadPaths.directorio("uploads");

        assertTrue(dir.isAbsolute(), "debe resolverse a una ruta absoluta");
        assertEquals(Paths.get("uploads").getFileName(), dir.getFileName(), "debe apuntar a la carpeta uploads");
        // El directorio del proyecto (el que contiene pom.xml) es ancestro del uploads resuelto.
        Path proyecto = UploadPaths.directorioProyecto();
        assertTrue(Files.exists(proyecto.resolve("pom.xml")), "debe detectar el pom.xml del proyecto");
        assertTrue(dir.startsWith(proyecto), "uploads debe vivir dentro del directorio del proyecto");
    }

    @Test
    void directorio_absoluto_seRespetaTalCual() {
        String absoluto = Paths.get(System.getProperty("user.dir")).resolve("custom").toString();
        Path dir = UploadPaths.directorio(absoluto);

        assertEquals(Paths.get(absoluto).toAbsolutePath().normalize(), dir);
    }

    @Test
    void resourceLocation_devuelveURLFileValida() {
        String location = UploadPaths.resourceLocation("uploads");

        assertTrue(location.startsWith("file:"), "debe ser una URL de tipo file:");
        assertTrue(location.contains("uploads"), "debe contener la carpeta uploads");
    }

    @Test
    void aPath_extraeJarDeURLFatSpringBoot() throws Exception {
        URL fat = new URL("jar:file:/C:/Users/User/Desktop/lumura/primeraApi/target/"
                + "primeraApi-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes/");

        Path p = UploadPaths.aPath(fat);

        assertNotNull(p, "debe extraer el jar de la URL jar:file:...!/BOOT-INF/classes/");
        assertEquals(Paths.get("C:/Users/User/Desktop/lumura/primeraApi/target/"
                + "primeraApi-0.0.1-SNAPSHOT.jar").toAbsolutePath().normalize(), p);
    }

    @Test
    void aPath_aceptaURLFileDirectaDeClasses() throws Exception {
        URL clases = new URL("file:/C:/Users/User/Desktop/lumura/primeraApi/target/classes/");

        Path p = UploadPaths.aPath(clases);

        assertNotNull(p);
        assertTrue(p.toString().contains("target"), "debe apuntar a target/classes");
        assertFalse(p.toString().contains("!"), "no debe quedar el separador del jar");
    }
}