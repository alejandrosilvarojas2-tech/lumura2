package com.lumura.primeraApi.config;

import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private JwtAuthFilter filtro;
    private UsuarioRepository repositorio;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        repositorio = mock(UsuarioRepository.class);
        jwtUtil = mock(JwtUtil.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<UsuarioRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(repositorio);
        filtro = new JwtAuthFilter(provider, jwtUtil);
    }

    private Usuario usuario(int id, String rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setRol(rol);
        u.setTokenVersion(0);
        return u;
    }

    private HttpServletRequest req(String uri) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn(uri);
        when(r.getHeader("Authorization")).thenReturn("Bearer token-de-prueba");
        return r;
    }

    private HttpServletResponse res() throws Exception {
        HttpServletResponse r = mock(HttpServletResponse.class);
        when(r.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return r;
    }

    @Test
    void aliadoConMembresiaVigente_pasa() throws Exception {
        Usuario u = usuario(1, "ALIADO");
        u.setMembresiaVence(LocalDateTime.now().plusDays(10));
        when(jwtUtil.validateToken("token-de-prueba")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-de-prueba")).thenReturn(1);
        when(jwtUtil.getTokenVersion("token-de-prueba")).thenReturn(0);
        when(repositorio.findById(1)).thenReturn(Optional.of(u));
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req("/api/aliado/productos"), response, chain);

        verify(chain).doFilter(any(), any());
        verify(response, never()).setStatus(403);
    }

    @Test
    void aliadoConMembresiaVencida_devuelve403YNoPasa() throws Exception {
        Usuario u = usuario(1, "ALIADO");
        u.setMembresiaVence(LocalDateTime.now().minusDays(1));
        when(jwtUtil.validateToken("token-de-prueba")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-de-prueba")).thenReturn(1);
        when(jwtUtil.getTokenVersion("token-de-prueba")).thenReturn(0);
        when(repositorio.findById(1)).thenReturn(Optional.of(u));
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req("/api/aliado/productos"), response, chain);

        verify(response).setStatus(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void aliadoVencido_puedeRenovarMembresia_aunqueEsteVencido() throws Exception {
        Usuario u = usuario(1, "ALIADO");
        u.setMembresiaVence(LocalDateTime.now().minusDays(1));
        when(jwtUtil.validateToken("token-de-prueba")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-de-prueba")).thenReturn(1);
        when(jwtUtil.getTokenVersion("token-de-prueba")).thenReturn(0);
        when(repositorio.findById(1)).thenReturn(Optional.of(u));
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req("/api/aliado/membresia"), response, chain);

        verify(chain).doFilter(any(), any());
        verify(response, never()).setStatus(403);
    }

    @Test
    void aliadoVencido_puedeCerrarSesion_aunqueEsteVencido() throws Exception {
        Usuario u = usuario(1, "ALIADO");
        u.setMembresiaVence(LocalDateTime.now().minusDays(1));
        when(jwtUtil.validateToken("token-de-prueba")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-de-prueba")).thenReturn(1);
        when(jwtUtil.getTokenVersion("token-de-prueba")).thenReturn(0);
        when(repositorio.findById(1)).thenReturn(Optional.of(u));
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req("/api/auth/logout"), response, chain);

        verify(chain).doFilter(any(), any());
        verify(response, never()).setStatus(403);
    }

    @Test
    void adminSinMembresia_noSeBloquea() throws Exception {
        when(jwtUtil.validateToken("token-de-prueba")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-de-prueba")).thenReturn(2);
        when(jwtUtil.getTokenVersion("token-de-prueba")).thenReturn(0);
        when(repositorio.findById(2)).thenReturn(Optional.of(usuario(2, "ADMIN")));
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req("/api/admin/dashboard"), response, chain);

        verify(chain).doFilter(any(), any());
        verify(response, never()).setStatus(403);
    }

    @Test
    void userSinMembresia_noSeBloquea() throws Exception {
        when(jwtUtil.validateToken("token-de-prueba")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-de-prueba")).thenReturn(3);
        when(jwtUtil.getTokenVersion("token-de-prueba")).thenReturn(0);
        when(repositorio.findById(3)).thenReturn(Optional.of(usuario(3, "USER")));
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req("/api/pedidos/3"), response, chain);

        verify(chain).doFilter(any(), any());
        verify(response, never()).setStatus(403);
    }

    @Test
    void aliadoVencido_sinTokenDeValido_devuelve401() throws Exception {
        when(jwtUtil.validateToken("token-de-prueba")).thenReturn(false);
        HttpServletResponse response = res();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req("/api/aliado/productos"), response, chain);

        verify(response).setStatus(401);
        verify(chain, never()).doFilter(any(), any());
    }
}