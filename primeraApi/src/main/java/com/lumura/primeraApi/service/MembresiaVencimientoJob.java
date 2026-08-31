package com.lumura.primeraApi.service;

import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Job programado que vigila el vencimiento de las membresías de distribuidor.
 *
 *  - Recordatorio: a partir de REMINDER_DIAS_DESCUENTO (5) días antes de vencer
 *    se envía un correo al aliado para que renueve/generé su pago.
 *  - Bloqueo: al cruzar la fecha de vencimiento (membresia_vence <= ahora, incluye
 *    el día de gracia) se envía un aviso de bloqueo automático.
 *
 * Los correos los despacha EmailService (modo log o real según MAIL_ENABLED).
 */
@Service
public class MembresiaVencimientoJob {

    private static final Logger log = LoggerFactory.getLogger(MembresiaVencimientoJob.class);

    private static final int REMINDER_DIAS = 5;
    private static final Set<Integer> recordados = new HashSet<>();

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public MembresiaVencimientoJob(UsuarioRepository usuarioRepository, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    // Corre cada 6 horas. Los recordatorios se deduplican por id en memoria (no spamear).
    @Scheduled(cron = "0 0 */6 * * *")
    public void vigilarVencimientos() {
        LocalDateTime ahora = LocalDateTime.now();

        // 1) Recordatorio antes de vencer (aliados con membresía vigente que vence pronto)
        List<Usuario> porRenovar = usuarioRepository
                .findByRolAndMembresiaVenceBetween("ALIADO", ahora, ahora.plusDays(REMINDER_DIAS));
        for (Usuario u : porRenovar) {
            if (u.getMembresiaCodigo() == null || u.getMembresiaVence() == null) continue;
            if (recordados.contains(u.getIdUsuario())) continue;
            recordados.add(u.getIdUsuario());
            emailService.enviar(u.getCorreoUsuario(),
                    "Tu membresía LUMURA vence pronto",
                    EmailService.plantilla("Renueva tu membresía, " + u.getNombreUsuario(),
                            "<p>Tu membresía de distribuidor (<b>"
                            + (u.getMembresiaPlan() != null ? u.getMembresiaPlan() : "activa")
                            + "</b>) <b>vence el " + u.getMembresiaVence().toLocalDate()
                            + "</b>.</p>"
                            + "<p>Genera el pago de tu membresía para seguir operando sin interrupciones "
                            + "y evitar el bloqueo automático de tu cuenta.</p>"));
            log.info("[MEMBRESIA] Recordatorio enviado a aliado {}", u.getIdUsuario());
        }

        // 2) Aviso de bloqueo automático (membresía vencida)
        List<Usuario> vencidos = usuarioRepository
                .findByRolAndMembresiaVenceBefore("ALIADO", ahora);
        for (Usuario u : vencidos) {
            if (u.getMembresiaCodigo() == null) continue;
            emailService.enviar(u.getCorreoUsuario(),
                    "Tu membresía LUMURA venció",
                    EmailService.plantilla("Cuenta bloqueada por membresía vencida, " + u.getNombreUsuario(),
                            "<p>Tu membresía de distribuidor venció el <b>" + u.getMembresiaVence().toLocalDate()
                            + "</b>.</p>"
                            + "<p>Tu cuenta ha sido <b>bloqueada automáticamente</b>. Genera el pago de tu "
                            + "membresía para reactivarla y continuar vendiendo en LUMURA.</p>"));
            recordados.remove(u.getIdUsuario());
            log.info("[MEMBRESIA] Aviso de bloqueo enviado a aliado {}", u.getIdUsuario());
        }
    }
}
