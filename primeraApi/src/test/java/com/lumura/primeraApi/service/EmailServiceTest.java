package com.lumura.primeraApi.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

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
}
