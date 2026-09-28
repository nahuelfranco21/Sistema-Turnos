package ar.uba.fi.ingsoft1.product_example.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoEstado;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {

    private EmailService emailService;
    private JavaMailSender mailSender;

    private static final String FRONTEND_URL = "http://localhost:5173";

    private User profesional;
    private User cliente;
    private Agenda agenda;
    private Turno turno;

    @BeforeEach
    void setup() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailService(mailSender, FRONTEND_URL, "sistematurnos06@gmail.com");

        profesional = new User(
            "prof@test.com",
            "hash",
            UserRole.PROFESIONAL,
            "Carlos",
            "Lopez",
            LocalDate.of(1985, 5, 15),
            "Médico",
            SectorTrabajo.SALUD
        );
        cliente = new User(
            "cli@test.com",
            "hash",
            UserRole.CLIENTE,
            "Maria",
            "Garcia",
            LocalDate.of(2000, 3, 20),
            null,
            null
        );

        agenda = new Agenda(profesional, 30, 1);
        turno = new Turno(
            agenda,
            cliente,
            LocalDate.of(2026, 7, 10),
            LocalTime.of(9, 0),
            TurnoEstado.REPROGRAMAR
        );
    }

    private SimpleMailMessage capturarMensaje() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(
            SimpleMailMessage.class
        );
        verify(mailSender, times(1)).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendPasswordRecoveryEmailUsaFrontendUrlDelHeader() {
        emailService.sendPasswordRecoveryEmail(
            "user@test.com",
            "token123",
            "http://custom-url.com"
        );
        SimpleMailMessage msg = capturarMensaje();
        assertNotNull(msg.getTo());
        assertEquals("user@test.com", msg.getTo()[0]);
        assertTrue(
            msg
                .getText()
                .contains("http://custom-url.com/reset-password?token=token123")
        );
    }

    @Test
    void sendPasswordRecoveryEmailUsaFrontendUrlPorDefecto() {
        emailService.sendPasswordRecoveryEmail(
            "user@test.com",
            "token123",
            null
        );
        SimpleMailMessage msg = capturarMensaje();
        assertTrue(
            msg
                .getText()
                .contains(FRONTEND_URL + "/reset-password?token=token123")
        );
    }

    @Test
    void sendPasswordRecoveryEmailTieneAsuntoYDestinatario() {
        emailService.sendPasswordRecoveryEmail("user@test.com", "abc", null);
        SimpleMailMessage msg = capturarMensaje();
        assertEquals("Recuperación de Contraseña", msg.getSubject());
        assertEquals("user@test.com", msg.getTo()[0]);
    }

    @Test
    void sendVerificationEmailIncludeEnlaceConToken() {
        emailService.sendVerificationEmail(
            "user@test.com",
            "verif-token",
            null
        );
        SimpleMailMessage msg = capturarMensaje();
        assertTrue(
            msg
                .getText()
                .contains(FRONTEND_URL + "/verify-email?token=verif-token")
        );
    }

    @Test
    void sendVerificationEmailUsaHeaderUrlSiEstaPresente() {
        emailService.sendVerificationEmail(
            "user@test.com",
            "verif-token",
            "http://otra-url.com"
        );
        SimpleMailMessage msg = capturarMensaje();
        assertTrue(
            msg
                .getText()
                .contains("http://otra-url.com/verify-email?token=verif-token")
        );
    }

    @Test
    void sendVerificationEmailTieneAsuntoYDestinatario() {
        emailService.sendVerificationEmail("user@test.com", "t", null);
        SimpleMailMessage msg = capturarMensaje();
        assertEquals("Verificá tu cuenta en Sistema Turnos", msg.getSubject());
        assertEquals("user@test.com", msg.getTo()[0]);
    }

    @Test
    void sendTurnoCancelledEnviaAlEmailDelCliente() {
        emailService.sendTurnoCancelledByProfesionalEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertEquals("cli@test.com", msg.getTo()[0]);
    }

    @Test
    void sendTurnoCancelledContieneNombreDelClienteYProfesional() {
        emailService.sendTurnoCancelledByProfesionalEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertTrue(msg.getText().contains("Maria"));
        assertTrue(msg.getText().contains("Carlos"));
    }

    @Test
    void sendTurnoCancelledContieneEnlaceReprogramar() {
        emailService.sendTurnoCancelledByProfesionalEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertTrue(msg.getText().contains("/reprogramar/"));
    }

    @Test
    void sendTurnoExpiredEnviaAlEmailDelCliente() {
        emailService.sendTurnoExpiredEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertEquals("cli@test.com", msg.getTo()[0]);
    }

    @Test
    void sendTurnoExpiredContieneNombreDelCliente() {
        emailService.sendTurnoExpiredEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertTrue(msg.getText().contains("Maria"));
    }

    @Test
    void sendTurnoPaymentReminderEnviaAlEmailDelCliente() {
        emailService.sendTurnoPaymentReminderEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertEquals("cli@test.com", msg.getTo()[0]);
    }

    @Test
    void sendTurnoPaymentReminderContieneNombreDelCliente() {
        emailService.sendTurnoPaymentReminderEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertTrue(msg.getText().contains("Maria"));
    }

    @Test
    void sendRecordatorio24HsEmailEnviaAlEmailDelCliente() {
        emailService.sendRecordatorio24HsEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertEquals("cli@test.com", msg.getTo()[0]);
    }

    @Test
    void sendRecordatorio24HsEmailContieneNombreDelClienteYProfesional() {
        emailService.sendRecordatorio24HsEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertTrue(msg.getText().contains("Maria"));
        assertTrue(msg.getText().contains("Carlos"));
    }

    @Test
    void sendRecordatorio24HsEmailTieneAsuntoCorrespondiente() {
        emailService.sendRecordatorio24HsEmail(turno);
        SimpleMailMessage msg = capturarMensaje();
        assertEquals("Recordatorio: tenés un turno mañana", msg.getSubject());
    }
}
