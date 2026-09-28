package ar.uba.fi.ingsoft1.product_example.Reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.agenda.DiaSemana;
import ar.uba.fi.ingsoft1.product_example.reserva.Reserva;
import ar.uba.fi.ingsoft1.product_example.reserva.ReservaEstado;
import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoEstado;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservaTest {

    private User cliente;
    private Turno turno;

    @BeforeEach
    void setup() {
        User profesional = new User(
            "prof@test.com",
            "hash",
            UserRole.PROFESIONAL,
            "Carlos",
            "Lopez",
            LocalDate.of(1985, 5, 15),
            "Profesor",
            SectorTrabajo.EDUCACION
        );
        Agenda agenda = new Agenda(profesional, 30, 1);

        cliente = new User(
            "cliente@test.com",
            "hash",
            UserRole.CLIENTE,
            "María",
            "García",
            LocalDate.of(2000, 3, 20),
            null,
            null
        );
        turno = new Turno(
            agenda,
            cliente,
            LocalDate.of(2025, 6, 16),
            LocalTime.of(9, 0),
            TurnoEstado.OCUPADO_SIN_CONFIRMAR
        );
    }

    @Test
    void unaReservaEmpiezaEnEstadoPendiente() {
        var reserva = new Reserva(cliente, turno);

        assertEquals(ReservaEstado.PENDIENTE, reserva.getEstado());
        assertNotEquals(ReservaEstado.CONFIRMADO, reserva.getEstado());
        assertNotEquals(ReservaEstado.CANCELADO, reserva.getEstado());
    }

    @Test
    void unaReservaConfirmadaNoEstaPendiente() {
        var reserva = new Reserva(cliente, turno);
        reserva.confirmar();

        assertNotEquals(ReservaEstado.PENDIENTE, reserva.getEstado());
    }

    @Test
    void unaReservaCanceladaNoEstaPendiente() {
        var reserva = new Reserva(cliente, turno);
        reserva.cancelar();

        assertNotEquals(ReservaEstado.PENDIENTE, reserva.getEstado());
    }

    @Test
    void laReservaTieneAlClienteCorrecto() {
        var reserva = new Reserva(cliente, turno);

        assertEquals(cliente, reserva.getCliente());
    }

    @Test
    void laReservaTieneElTurnoCorrecto() {
        var reserva = new Reserva(cliente, turno);

        assertEquals(turno, reserva.getTurno());
    }

    @Test
    void confirmarReservaCambiaEstadoAConfirmado() {
        var reserva = new Reserva(cliente, turno);
        reserva.confirmar();

        assertEquals(ReservaEstado.CONFIRMADO, reserva.getEstado());
    }

    @Test
    void unaReservaSePuedeConfirmarSiSeCanceloAnteriormente() {
        var reserva = new Reserva(cliente, turno);
        reserva.cancelar();
        reserva.confirmar();

        assertEquals(ReservaEstado.CONFIRMADO, reserva.getEstado());
        assertNotEquals(ReservaEstado.CANCELADO, reserva.getEstado());
    }

    @Test
    void cancelarReservaCambiaEstadoACancelado() {
        var reserva = new Reserva(cliente, turno);
        reserva.cancelar();

        assertEquals(ReservaEstado.CANCELADO, reserva.getEstado());
    }

    @Test
    void unaReservaConfirmadaSePuedeCancelar() {
        var reserva = new Reserva(cliente, turno);
        reserva.confirmar();
        reserva.cancelar();

        assertEquals(ReservaEstado.CANCELADO, reserva.getEstado());
        assertNotEquals(ReservaEstado.CONFIRMADO, reserva.getEstado());
    }
}
