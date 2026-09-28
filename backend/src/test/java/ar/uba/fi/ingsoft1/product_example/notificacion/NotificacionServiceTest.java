package ar.uba.fi.ingsoft1.product_example.notificacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.email.EmailService;
import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoEstado;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificacionServiceTest {

    private NotificacionService notificacionService;
    private EmailService emailService;

    private User profesional;
    private User cliente;
    private Agenda agenda;
    private Turno turno;

    @BeforeEach
    void setup() {
        emailService = mock(EmailService.class);
        notificacionService = new NotificacionService(emailService);

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
            LocalDate.now().plusDays(2),
            LocalTime.of(9, 0),
            TurnoEstado.REPROGRAMAR
        );
    }

    @Test
    void onTurnoCanceladoConClienteEnviaEmail() {
        notificacionService.onTurnoCancelado(turno);
        verify(emailService, times(1)).sendTurnoCancelledByProfesionalEmail(
            turno
        );
    }

    @Test
    void onTurnoCanceladoConTurnoNuloNoEnviaEmail() {
        notificacionService.onTurnoCancelado(null);
        verify(emailService, never()).sendTurnoCancelledByProfesionalEmail(
            any()
        );
    }

    @Test
    void onTurnoCanceladoSinClienteNoEnviaEmail() {
        Turno turnoSinCliente = new Turno(
            agenda,
            null,
            LocalDate.now().plusDays(2),
            LocalTime.of(9, 0),
            TurnoEstado.DESHABILITADO
        );
        notificacionService.onTurnoCancelado(turnoSinCliente);
        verify(emailService, never()).sendTurnoCancelledByProfesionalEmail(
            any()
        );
    }

    @Test
    void onTurnoCanceladoNoFallaAunqueEmailServiceLanceExcepcion() {
        doThrow(new RuntimeException("SMTP error"))
            .when(emailService)
            .sendTurnoCancelledByProfesionalEmail(turno);
        assertDoesNotThrow(() -> notificacionService.onTurnoCancelado(turno));
    }

    @Test
    void onTurnoExpiradoConClienteEnviaEmail() {
        notificacionService.onTurnoExpirado(turno);
        verify(emailService, times(1)).sendTurnoExpiredEmail(turno);
    }

    @Test
    void onTurnoExpiradoConTurnoNuloNoEnviaEmail() {
        notificacionService.onTurnoExpirado(null);
        verify(emailService, never()).sendTurnoExpiredEmail(any());
    }

    @Test
    void onTurnoExpiradoSinClienteNoEnviaEmail() {
        Turno turnoSinCliente = new Turno(
            agenda,
            null,
            LocalDate.now().plusDays(2),
            LocalTime.of(9, 0),
            TurnoEstado.DESHABILITADO
        );
        notificacionService.onTurnoExpirado(turnoSinCliente);
        verify(emailService, never()).sendTurnoExpiredEmail(any());
    }

    @Test
    void onTurnoExpiradoNoFallaAunqueEmailServiceLanceExcepcion() {
        doThrow(new RuntimeException("SMTP error"))
            .when(emailService)
            .sendTurnoExpiredEmail(turno);
        assertDoesNotThrow(() -> notificacionService.onTurnoExpirado(turno));
    }

    @Test
    void onTurnoPagoPendienteConClienteEnviaEmailYDevuelveTrue() {
        boolean resultado =
            notificacionService.onTurnoPagoPendienteRecordatorio(turno);
        verify(emailService, times(1)).sendTurnoPaymentReminderEmail(turno);
        assertTrue(resultado);
    }

    @Test
    void onTurnoPagoPendienteConTurnoNuloDevuelveFalse() {
        boolean resultado =
            notificacionService.onTurnoPagoPendienteRecordatorio(null);
        verify(emailService, never()).sendTurnoPaymentReminderEmail(any());
        assertFalse(resultado);
    }

    @Test
    void onTurnoPagoPendienteSinClienteDevuelveFalse() {
        Turno turnoSinCliente = new Turno(
            agenda,
            null,
            LocalDate.now().plusDays(2),
            LocalTime.of(9, 0),
            TurnoEstado.OCUPADO_SIN_CONFIRMAR
        );
        boolean resultado =
            notificacionService.onTurnoPagoPendienteRecordatorio(
                turnoSinCliente
            );
        assertFalse(resultado);
    }

    @Test
    void onTurnoPagoPendienteDevuelveFalseSiEmailFalla() {
        doThrow(new RuntimeException("SMTP error"))
            .when(emailService)
            .sendTurnoPaymentReminderEmail(turno);
        boolean resultado =
            notificacionService.onTurnoPagoPendienteRecordatorio(turno);
        assertFalse(resultado);
    }

    @Test
    void onRecordatorio24HsConClienteEnviaEmailYDevuelveTrue() {
        boolean resultado = notificacionService.onRecordatorio24Hs(turno);
        verify(emailService, times(1)).sendRecordatorio24HsEmail(turno);
        assertTrue(resultado);
    }

    @Test
    void onRecordatorio24HsConTurnoNuloDevuelveFalse() {
        boolean resultado = notificacionService.onRecordatorio24Hs(null);
        verify(emailService, never()).sendRecordatorio24HsEmail(any());
        assertFalse(resultado);
    }

    @Test
    void onRecordatorio24HsSinClienteDevuelveFalse() {
        Turno turnoSinCliente = new Turno(
            agenda,
            null,
            LocalDate.now().plusDays(1),
            LocalTime.of(10, 0),
            TurnoEstado.CONFIRMADO
        );
        boolean resultado = notificacionService.onRecordatorio24Hs(
            turnoSinCliente
        );
        assertFalse(resultado);
    }

    @Test
    void onRecordatorio24HsDevuelveFalseSiEmailFalla() {
        doThrow(new RuntimeException("SMTP error"))
            .when(emailService)
            .sendRecordatorio24HsEmail(turno);
        boolean resultado = notificacionService.onRecordatorio24Hs(turno);
        assertFalse(resultado);
    }
}
