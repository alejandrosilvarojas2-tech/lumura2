package com.lumura.primeraApi.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

    // Imagen de bienvenida incrustada inline (CID) en el correo de registro.
    private static final String CID_BIENVENIDA = "lumuraBienvenida";
    private static final Resource IMG_BIENVENIDA = new ClassPathResource("email/lumura-bienvenida.jpg");

    // Imagen de confirmación de compra incrustada inline (CID) en el correo al comprador.
    public static final String CID_CONFIRMACION = "confirmacionCompra";
    public static final Resource IMG_CONFIRMACION = new ClassPathResource("email/lumura-confirmacion.jpg");

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
        Map<String, Resource> inline = new HashMap<>();
        // Compatibilidad: si el HTML referencia la imagen de bienvenida, se adjunta automáticamente.
        if (cuerpoHtml != null && cuerpoHtml.contains("cid:" + CID_BIENVENIDA)) {
            inline.put(CID_BIENVENIDA, IMG_BIENVENIDA);
        }
        enviar(destinatario, asunto, cuerpoHtml, inline);
    }

    /**
     * Envía (o registra en modo log) un correo con imágenes inline (CID). Cada recurso del mapa
     * se adjunta con addInline solo si su cid aparece referenciado en el HTML y el recurso existe;
     * si falta, se omite con una advertencia sin romper el envío.
     */
    @Async
    public void enviar(String destinatario, String asunto, String cuerpoHtml, Map<String, Resource> inline) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("[MAIL] Destinatario vacío, correo omitido (asunto: {})", asunto);
            return;
        }
        String textoPlano = fromHtml(cuerpoHtml);
        if (!enabled) {
            // Modo demo: registrar el correo SIN volcar el cuerpo completo (evita PII y datos
            // sensibles como tokens/códigos de membresía en los logs). Solo se anota a quién
            // iba el correo y su asunto.
            log.info("[MAIL][DEMO] (deshabilitado) Para: {} | Asunto: {}",
                    destinatario, asunto);
            return;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(textoPlano, cuerpoHtml);
            // Imágenes inline: solo se adjuntan si la plantilla las referencia y el recurso
            // existe. Si falta alguna, el correo se envía igual (sin romper el flujo).
            if (inline != null) {
                inline.forEach((cid, recurso) -> {
                    if (cuerpoHtml == null || !cuerpoHtml.contains("cid:" + cid)) return;
                    try {
                        if (recurso.exists()) {
                            helper.addInline(cid, recurso);
                        } else {
                            log.warn("[MAIL] Imagen inline no encontrada para cid:{}; se enviará sin ella", cid);
                        }
                    } catch (Exception ex) {
                        log.warn("[MAIL] No fue posible adjuntar la imagen cid:{} :: {}", cid, ex.getMessage());
                    }
                });
            }
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

    // Plantilla con una imagen incrustada (CID) en la cabecera morada; se usa en los
    // correos de registro y de confirmación de compra. La imagen debe adjuntarse con
    // addInline() al enviar.
    public static String plantillaEncabezadoImagen(String cidImg, int anchoMax, String titulo, String cuerpoHtml) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:auto;border:1px solid #e5e5e5;border-radius:8px;overflow:hidden\">"
                + "<div style=\"background:#5b2a86;text-align:center;padding:20px\">"
                + "<img src=\"cid:" + cidImg + "\" alt=\"LUMURA\" width=\"" + anchoMax + "\" style=\"display:block;width:100%;max-width:" + anchoMax + "px;height:auto;margin:0 auto;border-radius:8px\">"
                + "</div>"
                + "<div style=\"padding:24px\">"
                + "<h2 style=\"color:#5b2a86;margin-top:0\">" + titulo + "</h2>"
                + cuerpoHtml
                + "</div>"
                + "<div style=\"background:#f5f5f5;padding:12px 24px;color:#888;font-size:12px\">"
                + "Este es un mensaje automático de LUMURA. Por favor no respondas a este correo."
                + "</div>"
                + "</div>";
    }

    public static String plantillaConImagen(String titulo, String cuerpoHtml) {
        return plantillaEncabezadoImagen(CID_BIENVENIDA, 320, titulo, cuerpoHtml);
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
