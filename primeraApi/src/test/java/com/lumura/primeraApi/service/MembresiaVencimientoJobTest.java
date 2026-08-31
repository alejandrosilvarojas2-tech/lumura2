package com.lumura.primeraApi.service;

import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembresiaVencimientoJobTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private MembresiaVencimientoJob job;

    private Usuario aliado(Integer id, String correo, String plan, LocalDateTime vence) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setRol("ALIADO");
        u.setNombreUsuario("Aliado" + id);
        u.setCorreoUsuario(correo);
        u.setMembresiaCodigo("MEM-" + id + "-" + plan.toUpperCase() + "-X");
        u.setMembresiaPlan(plan);
        u.setMembresiaVence(vence);
        return u;
    }

    @Test
    void vigilarVencimientos_enviaRecordatorioAPorVencer() {
        LocalDateTime ahora = LocalDateTime.now();
        Usuario porVencer = aliado(1, "a@lumura.com", "basico", ahora.plusDays(2));
        when(usuarioRepository.findByRolAndMembresiaVenceBetween(eq("ALIADO"), any(), any()))
                .thenReturn(List.of(porVencer));
        when(usuarioRepository.findByRolAndMembresiaVenceBefore(eq("ALIADO"), any())).thenReturn(List.of());

        job.vigilarVencimientos();

        verify(emailService).enviar(eq("a@lumura.com"), contains("vence pronto"), anyString());
        // No envía aviso de bloqueo
        verify(emailService, never()).enviar(anyString(), contains("venció"), anyString());
    }

    @Test
    void vigilarVencimientos_enviaAvisoDeBloqueoAVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        Usuario vencido = aliado(2, "b@lumura.com", "medio", ahora.minusDays(1));
        when(usuarioRepository.findByRolAndMembresiaVenceBetween(eq("ALIADO"), any(), any())).thenReturn(List.of());
        when(usuarioRepository.findByRolAndMembresiaVenceBefore(eq("ALIADO"), any())).thenReturn(List.of(vencido));

        job.vigilarVencimientos();

        verify(emailService).enviar(eq("b@lumura.com"), contains("venció"), contains("bloqueada automáticamente"));
    }
}
