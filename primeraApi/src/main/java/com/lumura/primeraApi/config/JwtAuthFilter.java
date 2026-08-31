package com.lumura.primeraApi.config;

import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Valida el JWT de forma central y comprueba la versión de revocación:
 * un token emitido con una versión vieja (logout, cambio de contraseña,
 * bloqueo o desbloqueo por el admin) deja de ser aceptado de inmediato.
 * Las rutas públicas y las peticiones sin token pasan sin intervenir
 * (los controllers siguen devolviendo su propio 401 "Token requerido").
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/register-aliado",
            "/api/auth/recuperar", "/api/auth/reset-password", "/api/pago/procesar",
            "/api/productos"
    );

    private final ObjectProvider<UsuarioRepository> usuariosProvider;
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(ObjectProvider<UsuarioRepository> usuariosProvider, JwtUtil jwtUtil) {
        this.usuariosProvider = usuariosProvider;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String auth = request.getHeader("Authorization");

        if (!path.startsWith("/api/") || auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        if (PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            rechazar401(response, "Token inválido o expirado");
            return;
        }

        UsuarioRepository repositorio = usuariosProvider.getIfAvailable();
        if (repositorio == null) {
            // Contexto de pruebas sin repositorio (slices @WebMvcTest): el controller valida.
            chain.doFilter(request, response);
            return;
        }

        Optional<Usuario> usuario = repositorio.findById(jwtUtil.getUserIdFromToken(token));
        if (usuario.isEmpty()) {
            rechazar401(response, "Tu sesión expiró. Inicia sesión de nuevo");
            return;
        }

        int versionActual = usuario.get().getTokenVersion() == null ? 0 : usuario.get().getTokenVersion();
        if (jwtUtil.getTokenVersion(token) != versionActual) {
            rechazar401(response, "Tu sesión expiró. Inicia sesión de nuevo");
            return;
        }

        chain.doFilter(request, response);
    }

    private void rechazar401(HttpServletResponse response, String mensaje) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}