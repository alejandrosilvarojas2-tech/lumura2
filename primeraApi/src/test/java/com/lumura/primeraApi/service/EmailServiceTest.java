package com.lumura.primeraApi.service;

import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void isEnabled_esFalseEnModoDemo() {
        EmailService svc = new EmailService(mailSender, false, "no-reply@lumura.com");
        assertFalse(svc.isEnabled());
        // En modo demo no toca el mail sender real
        svc.enviar("a@b.co", "asunto", "<p>hola</p>");
        verifyNoInteractions(mailSender);
    }

    @Test
    void isEnabled_esTrueCuandoSeConfigura() {
        EmailService svc = new EmailService(mailSender, true, "no-reply@lumura.com");
        assertTrue(svc.isEnabled());
    }

    @Test
    void enviar_conDestinatarioVacio_omiteYNoLanza() {
        EmailService svc = new EmailService(mailSender, true, "no-reply@lumura.com");
        assertDoesNotThrow(() -> svc.enviar("   ", "asunto", "<p>x</p>"));
        assertDoesNotThrow(() -> svc.enviar(null, "asunto", "<p>x</p>"));
        verifyNoInteractions(mailSender);
    }

    @Test
    void plantilla_generaHtmlConCabeceraLumura() {
        String html = EmailService.plantilla("Titulo", "<p>cuerpo</p>");
        assertTrue(html.contains("LUMURA"));
        assertTrue(html.contains("Titulo"));
        assertTrue(html.contains("cuerpo"));
    }

    @Test
    void plantillaConImagen_incluyeCidDeBienvenida() {
        String html = EmailService.plantillaConImagen("Titulo", "<p>cuerpo</p>");
        assertTrue(html.contains("cid:lumuraBienvenida"));
        assertTrue(html.contains("LUMURA"));
        assertTrue(html.contains("cuerpo"));
    }

    @Test
    void enviar_conImagenInline_enviaMultipartSinLanzar() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        Session session = Session.getInstance(new Properties());
        MimeMessage mime = new MimeMessage(session);
        when(sender.createMimeMessage()).thenReturn(mime);
        EmailService svc = new EmailService(sender, true, "no-reply@lumura.com");

        assertDoesNotThrow(() -> svc.enviar("a@b.co", "asunto",
                EmailService.plantillaConImagen("Titulo", "<p>cuerpo</p>")));

        verify(sender).send(mime);
        assertTrue(mime.getContent() instanceof Multipart,
                "El correo con imagen inline debe componerse como mensaje multipart");
    }

    @Test
    void plantillaEncabezadoImagen_usaElCidYElAnchoIndicado() {
        String html = EmailService.plantillaEncabezadoImagen(
                EmailService.CID_CONFIRMACION, 560, "Titulo", "<p>cuerpo</p>");
        assertTrue(html.contains("cid:" + EmailService.CID_CONFIRMACION));
        assertTrue(html.contains("max-width:560px"));
        assertTrue(html.contains("LUMURA"));
        assertTrue(html.contains("cuerpo"));
    }

    @Test
    void enviar_conMultiplesImagenesInline_enviaMultipartSinLanzar() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        Session session = Session.getInstance(new Properties());
        MimeMessage mime = new MimeMessage(session);
        when(sender.createMimeMessage()).thenReturn(mime);
        EmailService svc = new EmailService(sender, true, "no-reply@lumura.com");

        Map<String, Resource> inline = Map.of(
                EmailService.CID_CONFIRMACION, EmailService.IMG_CONFIRMACION,
                "producto-7", new ClassPathResource("email/lumura-bienvenida.jpg"));
        String cuerpo = EmailService.plantillaEncabezadoImagen(
                EmailService.CID_CONFIRMACION, 560, "Titulo",
                "<p>Productos:</p><img src=\"cid:producto-7\">");

        assertDoesNotThrow(() -> svc.enviar("a@b.co", "asunto", cuerpo, inline));

        verify(sender).send(mime);
        assertTrue(mime.getContent() instanceof Multipart,
                "El correo con varias imágenes inline debe componerse como multipart");
    }

    @Test
    void enviar_conRecursoInlineInexistente_enviaIgualSinLanzar() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        Session session = Session.getInstance(new Properties());
        MimeMessage mime = new MimeMessage(session);
        when(sender.createMimeMessage()).thenReturn(mime);
        EmailService svc = new EmailService(sender, true, "no-reply@lumura.com");

        Map<String, Resource> inline = Map.of("producto-x",
                new ClassPathResource("email/no-existe.jpg"));
        String cuerpo = EmailService.plantilla("Titulo", "<p><img src=\"cid:producto-x\"></p>");

        assertDoesNotThrow(() -> svc.enviar("a@b.co", "asunto", cuerpo, inline),
                "un recurso faltante no debe impedir el envío");

        verify(sender).send(mime);
    }
}
