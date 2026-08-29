package com.lumura.primeraApi.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    public static final int MIN_SECRET_LENGTH = 32;

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${app.jwt.secret:}") String secret,
                   @Value("${app.jwt.expiration:86400000}") long expiration,
                   @Value("${spring.profiles.active:}") String activeProfile) {
        this.expiration = expiration;
        this.key = Keys.hmacShaKeyFor(resolveSecret(secret, activeProfile).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * El secreto NUNCA se toma de un valor fijo commiteado:
     * - prod: exige JWT_SECRET definido y de al menos 32 caracteres; de lo contrario no arranca.
     * - dev: si no está definido, genera uno efímero para esta ejecución (los tokens mueren al reiniciar).
     */
    private static String resolveSecret(String secret, String activeProfile) {
        boolean esProd = activeProfile != null && activeProfile.contains("prod");
        boolean vacio = secret == null || secret.isBlank();
        if (vacio) {
            if (esProd) {
                throw new IllegalStateException(
                        "JWT_SECRET no definido: el perfil 'prod' no arranca con un secreto vacío. "
                                + "Define la variable de entorno JWT_SECRET con al menos "
                                + MIN_SECRET_LENGTH + " caracteres.");
            }
            byte[] aleatorio = new byte[48];
            new SecureRandom().nextBytes(aleatorio);
            String efimero = HexFormat.of().formatHex(aleatorio);
            log.warn("JWT_SECRET sin definir en perfil dev — usando secreto EFIMERO de esta ejecucion; "
                    + "los tokens no seran validos tras reiniciar. Define JWT_SECRET para persistencia.");
            return efimero;
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            if (esProd) {
                throw new IllegalStateException("JWT_SECRET demasiado corto (" + secret.length()
                        + " caracteres): usa al menos " + MIN_SECRET_LENGTH + ".");
            }
            log.warn("JWT_SECRET corto ({} caracteres); minimo recomendado {}", secret.length(), MIN_SECRET_LENGTH);
        }
        return secret;
    }

    public String generateToken(Integer userId, String email, String rol) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Integer getUserIdFromToken(String token) {
        return Integer.parseInt(getClaims(token).getSubject());
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getRolFromToken(String token) {
        return getClaims(token).get("rol", String.class);
    }
}
