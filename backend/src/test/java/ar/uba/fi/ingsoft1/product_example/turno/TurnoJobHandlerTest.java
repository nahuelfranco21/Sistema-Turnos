package ar.uba.fi.ingsoft1.product_example.turno;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.agenda.DiaSemana;
import ar.uba.fi.ingsoft1.product_example.config.TurnoProperties;
import ar.uba.fi.ingsoft1.product_example.notificacion.NotificacionService;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TurnoJobHandlerTest {

    private TurnoJobHandler jobHandler;
    private TurnoRepository turnoRepository;
    private TurnoProperties turnoProperties;
    private NotificacionService notificacionService;

    private Agenda agenda;
    private User cliente;

    @BeforeEach
    void setup() {
        turnoRepository = mock(TurnoRepository.class);
        turnoProperties = mock(TurnoProperties.class);
        notificacionService = mock(NotificacionService.class);

        jobHandler = new TurnoJobHandler(turnoRepository, turnoProperties, notificacionService);

        User profesional = new User("prof@test.com", "hash", UserRole.PROFESIONAL,
            "Carlos", "Lopez", LocalDate.of(1985, 5, 15), "Profesor", SectorTrabajo.EDUCACION);
        cliente = new User("cli@test.com", "hash", UserRole.CLIENTE,
            "Maria", "Garcia", LocalDate.of(2000, 3, 20), null, null);
        agenda = new Agenda(profesional, 30, 1);
    }

    private Turno crearTurno() {
        return new Turno(agenda, cliente, LocalDate.now().plusDays(1), LocalTime.of(9, 0),
            TurnoEstado.OCUPADO_SIN_CONFIRMAR);
    }

    @Test
    void expirarTurnosSinPagoEliminaYNotifica() {
        var turno = crearTurno();
        when(turnoProperties.getExpirationHours()).thenReturn(24);
        when(turnoRepository.findByEstadoAndClienteIsNotNullAndCreatedAtBefore(
            eq(TurnoEstado.OCUPADO_SIN_CONFIRMAR), any())).thenReturn(List.of(turno));

        int cantidad = jobHandler.expirarTurnosSinPago();

        assertEquals(1, cantidad);
        verify(notificacionService, times(1)).onTurnoExpirado(turno);
        verify(turnoRepository, times(1)).delete(turno);
    }

    @Test
    void expirarTurnosSinPagoSinVencidosDevuelveCero() {
        when(turnoProperties.getExpirationHours()).thenReturn(24);
        when(turnoRepository.findByEstadoAndClienteIsNotNullAndCreatedAtBefore(
            eq(TurnoEstado.OCUPADO_SIN_CONFIRMAR), any())).thenReturn(List.of());

        int cantidad = jobHandler.expirarTurnosSinPago();

        assertEquals(0, cantidad);
        verify(notificacionService, never()).onTurnoExpirado(any());
    }

    @Test
    void recordatorioPagoPendienteEnviaYMarcaReminderSent() {
        var turno = crearTurno();
        when(turnoProperties.getReminderHours()).thenReturn(23);
        when(turnoRepository.findByEstadoAndClienteIsNotNullAndReminderSentFalseAndCreatedAtBefore(
            eq(TurnoEstado.OCUPADO_SIN_CONFIRMAR), any())).thenReturn(List.of(turno));
        when(notificacionService.onTurnoPagoPendienteRecordatorio(turno)).thenReturn(true);

        int cantidad = jobHandler.recordatorioPagoPendiente();

        assertEquals(1, cantidad);
        assertTrue(turno.isReminderSent());
        verify(turnoRepository, times(1)).save(turno);
    }

    @Test
    void recordatorioPagoPendienteSinPendientesDevuelveCero() {
        when(turnoProperties.getReminderHours()).thenReturn(23);
        when(turnoRepository.findByEstadoAndClienteIsNotNullAndReminderSentFalseAndCreatedAtBefore(
            eq(TurnoEstado.OCUPADO_SIN_CONFIRMAR), any())).thenReturn(List.of());

        int cantidad = jobHandler.recordatorioPagoPendiente();

        assertEquals(0, cantidad);
        verify(notificacionService, never()).onTurnoPagoPendienteRecordatorio(any());
    }

    @Test
    void enviarRecordatorios24HsSinTurnosDevuelveCero() {
        when(turnoRepository.findByEstadoAndClienteIsNotNullAndFechaAndRecordatorio24hEnviadoFalse(
            eq(TurnoEstado.CONFIRMADO), any())).thenReturn(List.of());

        int cantidad = jobHandler.enviarRecordatorios24Hs();

        assertEquals(0, cantidad);
        verify(notificacionService, never()).onRecordatorio24Hs(any());
    }

    @Test
    void enviarRecordatorios24HsEnviaYMarcaFlag() {
        LocalDate manana = LocalDate.now().plusDays(1);
        LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);

        Turno turno = new Turno(agenda, cliente, manana, ahora, TurnoEstado.CONFIRMADO);
        when(turnoProperties.getReminder24hWindowMinutes()).thenReturn(1440);
        when(turnoRepository.findByEstadoAndClienteIsNotNullAndFechaAndRecordatorio24hEnviadoFalse(
            eq(TurnoEstado.CONFIRMADO), eq(manana))).thenReturn(List.of(turno));
        when(notificacionService.onRecordatorio24Hs(turno)).thenReturn(true);

        int cantidad = jobHandler.enviarRecordatorios24Hs();

        assertEquals(1, cantidad);
        assertTrue(turno.isRecordatorio24hEnviado());
        verify(turnoRepository, times(1)).save(turno);
    }
}
