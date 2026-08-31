package com.lumura.primeraApi.util;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRETO_VALIDO = "s".repeat(32);

    @Test
    void tokenGenerado_esValidoYContieneClaims() {
        JwtUtil jwt = new JwtUtil(SECRETO_VALIDO, 86400000L, "dev");

        String token = jwt.generateToken(7, "user@test.com", "USER");

        assertTrue(jwt.validateToken(token));
        assertEquals(7, jwt.getUserIdFromToken(token));
        assertEquals("USER", jwt.getRolFromToken(token));
    }

    @Test
    void tokenAlterado_noEsValido() {
        JwtUtil jwt = new JwtUtil(SECRETO_VALIDO, 86400000L, "dev");
        String token = jwt.generateToken(1, "a@b.com", "USER");
        String alterado = token.substring(0, token.length() - 3) + "xxx";

        assertFalse(jwt.validateToken(alterado));
    }

    @Test
    void tokenIncluyeVersionDeRevocacion() {
        JwtUtil jwt = new JwtUtil(SECRETO_VALIDO, 86400000L, "dev");

        String token = jwt.generateToken(12, "a@b.com", "USER", 3);

        assertTrue(jwt.validateToken(token));
        assertEquals(3, jwt.getTokenVersion(token));
    }

    @Test
    void tokenSinVersion_tomaVersion0() {
        JwtUtil jwt = new JwtUtil(SECRETO_VALIDO, 86400000L, "dev");

        String token = jwt.generateToken(2, "x@y.com", "ADMIN");

        assertEquals(0, jwt.getTokenVersion(token));
    }

    @Test
    void secretoDistinto_rechazaTokensDeOtraInstancia() {
        JwtUtil emisor = new JwtUtil(SECRETO_VALIDO, 86400000L, "dev");
        JwtUtil verificador = new JwtUtil("x".repeat(40), 86400000L, "dev");

        String token = emisor.generateToken(1, "a@b.com", "ADMIN");

        assertFalse(verificador.validateToken(token));
    }

    @Test
    void prod_sinSecreto_noArranca() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtUtil("", 86400000L, "prod"));

        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void prod_secretoCorto_noArranca() {
        assertThrows(IllegalStateException.class,
                () -> new JwtUtil("corto", 86400000L, "prod"));
    }

    @Test
    void dev_sinSecreto_generaEferimeroFuncional() {
        JwtUtil jwt = new JwtUtil("", 86400000L, "dev");

        String token = jwt.generateToken(3, "ana@test.com", "ALIADO");

        assertTrue(jwt.validateToken(token));
        assertEquals("ALIADO", jwt.getRolFromToken(token));
    }

    @Test
    void perfilDevConEspacios_trataProdPorNombre() {
        assertThrows(IllegalStateException.class,
                () -> new JwtUtil("", 86400000L, "dev,prod"));
    }
}
