package com.lumura.primeraApi.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter implements Filter {

    private final ConcurrentHashMap<String, int[]> requests = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        long now = System.currentTimeMillis();

        int maxRequests;
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            maxRequests = 10;
        } else if (path.startsWith("/api/admin")) {
            maxRequests = 60;
        } else {
            maxRequests = 120;
        }

        int[] window = requests.compute(ip, (key, val) -> {
            if (val == null || now - val[1] > 60000) {
                return new int[]{1, (int) (now / 60000)};
            }
            val[0]++;
            return val;
        });

        if (window[0] > maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intenta de nuevo más tarde.\"}");
            return;
        }

        chain.doFilter(req, res);
    }
}
