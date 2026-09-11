package com.lumura.primeraApi.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Component
public class RateLimitFilter implements Filter {

    static final long WINDOW_MS = 60_000;
    static final long MAX_ENTRIES = 10_000;
    static final long LIMPIEZA_MS = WINDOW_MS;

    private static final int MAX_LOGIN = 10;
    private static final int MAX_ADMIN = 60;
    private static final int MAX_GENERAL = 120;

    // estado por IP por clase de ruta: [contador, inicioDeVentanaMs]
    private final ConcurrentHashMap<String, long[]> loginRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> adminRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> generalRequests = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    // Proxies inversos confiables: solo de ellos se acepta la IP del cliente en
    // X-Forwarded-For/X-Real-IP (lo evita que un cliente directo falsee su IP).
    private final Set<String> proxiesConfiable;

    private volatile long ultimaLimpiezaMs = 0;

    @Autowired
    public RateLimitFilter(@Value("${app.rate-limit.proxy-ips:127.0.0.1,::1,0:0:0:0:0:0:0:1}") String proxyIps) {
        this(System::currentTimeMillis, proxyIps);
    }

    RateLimitFilter(LongSupplier clock) {
        this(clock, "127.0.0.1,::1,0:0:0:0:0:0:0:1");
    }

    RateLimitFilter(LongSupplier clock, String proxyIps) {
        this.clock = clock;
        this.proxiesConfiable = Set.of(proxyIps.split(","));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String uri = httpReq.getRequestURI();
        long now = clock.getAsLong();

        ConcurrentHashMap<String, long[]> bucket = bucketDe(uri);
        long[] estado = bucket.compute(clienteIp(httpReq), (ip, val) -> {
            if (val == null || now - val[1] >= WINDOW_MS) {
                return new long[]{1, now};
            }
            val[0]++;
            return val;
        });

        if (estado[0] > maxRequestsFor(uri)) {
            HttpServletResponse httpRes = (HttpServletResponse) response;
            httpRes.setStatus(429);
            httpRes.setContentType("application/json");
            httpRes.setCharacterEncoding("UTF-8");
            httpRes.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intenta de nuevo más tarde.\"}");
            return;
        }

        chain.doFilter(request, response);
        limpiarSiEsNecesario(now);
    }

    private int maxRequestsFor(String uri) {
        if (esLogin(uri)) {
            return MAX_LOGIN;
        }
        if (uri.startsWith("/api/admin")) {
            return MAX_ADMIN;
        }
        return MAX_GENERAL;
    }

    private ConcurrentHashMap<String, long[]> bucketDe(String uri) {
        if (esLogin(uri)) {
            return loginRequests;
        }
        if (uri.startsWith("/api/admin")) {
            return adminRequests;
        }
        return generalRequests;
    }

    private boolean esLogin(String uri) {
        return uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/register");
    }

    // IP real del cliente: detrás de un proxy inverso confiable (Cloudflare, nginx) la IP
    // original llega como primer valor de X-Forwarded-For (X-Real-IP es el fallback de
    // algunos proxies). Solo se confía en estas cabeceras cuando el peer inmediato coincide
    // con un proxy de la lista configurada (por defecto loopback, el caso del túnel local).
    String clienteIp(HttpServletRequest request) {
        if (esProxyConfiable(request.getRemoteAddr())) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null) {
                String primera = xff.split(",")[0].trim();
                if (!primera.isEmpty()) return primera;
            }
            String xri = request.getHeader("X-Real-IP");
            if (xri != null && !xri.trim().isEmpty()) return xri.trim();
        }
        return request.getRemoteAddr();
    }

    boolean esProxyConfiable(String peer) {
        return peer != null && proxiesConfiable.contains(peer);
    }

    private void limpiarSiEsNecesario(long now) {
        boolean expiroPeriodo = now - ultimaLimpiezaMs >= LIMPIEZA_MS;
        boolean sobreLimite = obsoletos(loginRequests, now) + obsoletos(adminRequests, now) + obsoletos(generalRequests, now) > MAX_ENTRIES;
        if (!expiroPeriodo && !sobreLimite) {
            return;
        }
        purgar(loginRequests, now);
        purgar(adminRequests, now);
        purgar(generalRequests, now);
        ultimaLimpiezaMs = now;
    }

    int totalEntradas() {
        return loginRequests.size() + adminRequests.size() + generalRequests.size();
    }

    private int obsoletos(ConcurrentHashMap<String, long[]> mapa, long now) {
        int n = 0;
        for (long[] val : mapa.values()) {
            if (now - val[1] >= WINDOW_MS) n++;
        }
        return n;
    }

    private void purgar(ConcurrentHashMap<String, long[]> mapa, long now) {
        List<String> expirados = new ArrayList<>();
        mapa.forEach((ip, val) -> {
            if (now - val[1] >= WINDOW_MS) expirados.add(ip);
        });
        expirados.forEach(mapa::remove);
    }
}