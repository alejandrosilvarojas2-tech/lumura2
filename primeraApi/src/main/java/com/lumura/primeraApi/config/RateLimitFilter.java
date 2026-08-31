package com.lumura.primeraApi.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Component
public class RateLimitFilter implements Filter {

    static final long WINDOW_MS = 60_000;
    static final long MAX_ENTRIES = 10_000;

    private static final int MAX_LOGIN = 10;
    private static final int MAX_ADMIN = 60;
    private static final int MAX_GENERAL = 120;

    // estado por IP por clase de ruta: [contador, inicioDeVentanaMs]
    private final ConcurrentHashMap<String, long[]> loginRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> adminRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> generalRequests = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public RateLimitFilter() {
        this(System::currentTimeMillis);
    }

    RateLimitFilter(LongSupplier clock) {
        this.clock = clock;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String uri = httpReq.getRequestURI();
        long now = clock.getAsLong();

        ConcurrentHashMap<String, long[]> bucket = bucketDe(uri);
        long[] estado = bucket.compute(httpReq.getRemoteAddr(), (ip, val) -> {
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

    private void limpiarSiEsNecesario(long now) {
        if (obsoletos(loginRequests, now) + obsoletos(adminRequests, now) + obsoletos(generalRequests, now) <= MAX_ENTRIES) {
            return;
        }
        purgar(loginRequests, now);
        purgar(adminRequests, now);
        purgar(generalRequests, now);
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