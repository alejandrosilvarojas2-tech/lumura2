package com.lumura.primeraApi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private final AtomicLong reloj = new AtomicLong(0);

    private RateLimitFilter filtro;

    @BeforeEach
    void setUp() {
        filtro = new RateLimitFilter(reloj::get);
    }

    private HttpServletRequest req(String uri, String ip) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn(uri);
        when(r.getRemoteAddr()).thenReturn(ip);
        return r;
    }

    private HttpServletResponse res() throws Exception {
        HttpServletResponse r = mock(HttpServletResponse.class);
        when(r.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return r;
    }

    @Test
    void login_permitidoHasta10_yLa11vaDa429() throws Exception {
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filtro.doFilter(req("/api/auth/login", "10.0.0.1"), response, chain);
        }
        verify(chain, times(10)).doFilter(any(), any());

        filtro.doFilter(req("/api/auth/login", "10.0.0.1"), response, chain);

        verify(response).setStatus(429);
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void login_conOtrosEndpoints_noCompartenLimite() throws Exception {
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filtro.doFilter(req("/api/auth/login", "10.0.0.2"), response, chain);
        }
        // El límite 11 de login no afecta a /api/productos
        filtro.doFilter(req("/api/productos", "10.0.0.2"), response, chain);

        verify(chain, times(11)).doFilter(any(), any());
        verify(response, never()).setStatus(429);
    }

    @Test
    void ventanaSeReiniciaDespuesDeUnMinuto() throws Exception {
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filtro.doFilter(req("/api/auth/login", "10.0.0.3"), response, chain);
        }
        filtro.doFilter(req("/api/auth/login", "10.0.0.3"), response, chain);
        verify(response).setStatus(429);

        reloj.set(RateLimitFilter.WINDOW_MS + 1);
        filtro.doFilter(req("/api/auth/login", "10.0.0.3"), response, chain);
        verify(chain, times(11)).doFilter(any(), any());
    }

    @Test
    void ipsDistintas_sonIndependientes() throws Exception {
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filtro.doFilter(req("/api/auth/login", "10.0.0.4"), response, chain);
        }
        // El IP A ya está al límite: su 11ª petición da 429...
        filtro.doFilter(req("/api/auth/login", "10.0.0.4"), response, chain);
        // ...pero el IP B arranca con su propia ventana y pasa.
        filtro.doFilter(req("/api/auth/login", "10.0.0.5"), response, chain);

        verify(chain, times(11)).doFilter(any(), any());
        verify(response).setStatus(429);
    }

    @Test
    void traficoGeneral_noAgotaElBucketDeLogin() throws Exception {
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        // Una carga normal del SPA genera muchos requests estáticos en el mismo minuto...
        for (int i = 0; i < 30; i++) {
            filtro.doFilter(req("/", "10.0.0.7"), response, chain);
        }
        // ...pero el login (límite 10) tiene su propio bucket y debe pasar
        filtro.doFilter(req("/api/auth/login", "10.0.0.7"), response, chain);

        verify(chain, times(31)).doFilter(any(), any());
        verify(response, never()).setStatus(429);
    }

    @Test
    void rutaAdmin_limite60() throws Exception {
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 60; i++) {
            filtro.doFilter(req("/api/admin/usuarios", "10.0.0.6"), response, chain);
        }
        filtro.doFilter(req("/api/admin/usuarios", "10.0.0.6"), response, chain);

        verify(response).setStatus(429);
    }
}