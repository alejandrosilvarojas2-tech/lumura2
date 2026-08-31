package com.lumura.primeraApi.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Envío de correos transaccionales de LUMURA.
 *
 * En modo demo (app.mail.enabled=false, el valor por defecto) los correos NO se
 * envían: se registran en el log para poder verificar el flujo sin infraestructura
 * SMTP. Para activarlos definir variables de entorno (SMTP_HOST/PORT/USER/PASSWORD,
 * MAIL_ENABLED=true, MAIL_FROM).
 *
 * Los métodos son @Async: el envío nunca bloquea la respuesta HTTP y, si falla,
 * solo se registra el error sin propagarse (no rompe la acción de negocio).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.mail.from:no-reply@lumura.com}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Envía (o registra en modo log) un correo a un único destinatario. */
    @Async
    public void enviar(String destinatario, String asunto, String cuerpoHtml) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("[MAIL] Destinatario vacío, correo omitido (asunto: {})", asunto);
            return;
        }
        String textoPlano = fromHtml(cuerpoHtml);
        if (!enabled) {
            log.info("[MAIL][DEMO] (deshabilitado) Para: {} | Asunto: {} | Cuerpo: {}",
                    destinatario, asunto, textoPlano);
            return;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(textoPlano, cuerpoHtml);
            mailSender.send(mime);
            log.info("[MAIL] Enviado a {} | Asunto: {}", destinatario, asunto);
        } catch (Exception e) {
            log.error("[MAIL] Error enviando a {} | Asunto: {} :: {}", destinatario, asunto, e.getMessage(), e);
        }
    }

    // ---- Plantillas simples (cabecera LUMURA) -----------------------------

    public static String plantilla(String titulo, String cuerpoHtml) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:auto;border:1px solid #e5e5e5;border-radius:8px;overflow:hidden\">"
                + "<div style=\"background:#5b2a86;color:#fff;padding:16px 24px;font-size:22px;font-weight:bold\">LUMURA</div>"
                + "<div style=\"padding:24px\">"
                + "<h2 style=\"color:#5b2a86;margin-top:0\">" + titulo + "</h2>"
                + cuerpoHtml
                + "</div>"
                + "<div style=\"background:#f5f5f5;padding:12px 24px;color:#888;font-size:12px\">"
                + "Este es un mensaje automático de LUMURA. Por favor no respondas a este correo."
                + "</div>"
                + "</div>";
    }

    public static String enlace(String url, String texto) {
        try {
            String enc = URLEncoder.encode(url, StandardCharsets.UTF_8).replace("%3A", ":").replace("%2F", "/");
            return "<a href=\"" + enc + "\" style=\"display:inline-block;background:#5b2a86;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none\">" + texto + "</a>";
        } catch (Exception e) {
            return "<a href=\"" + url + "\">" + texto + "</a>";
        }
    }

    private static String fromHtml(String html) {
        String s = html.replaceAll("<br\\s*/?>", "\n").replaceAll("</p>", "\n").replaceAll("</div>", "\n");
        return s.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">").trim();
    }
}
