package ar.uba.fi.ingsoft1.product_example.turno;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.agenda.DiaSemana;
import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoEstado;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TurnoTest {

    private Agenda agenda;
    private User cliente;

    @BeforeEach
    void setup() {
        User profesional = new User(
                "prof@test.com", "hash", UserRole.PROFESIONAL,
                "Carlos", "Lopez", LocalDate.of(1985, 5, 15),
                "Profesor", SectorTrabajo.EDUCACION
        );
        agenda = new Agenda(profesional, 30, 1);

        cliente = new User(
                "cliente@test.com", "hash", UserRole.CLIENTE,
                "María", "García", LocalDate.of(2000, 3, 20), null, null
        );
    }
    private Turno crearTurno() {
        return new Turno(
                agenda,
                cliente,
                LocalDate.of(2025, 6, 16),
                LocalTime.of(9, 0),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR
        );
    }

    @Test
    void turnoGuardaAgendaCorrectamente() {
        var turno = crearTurno();
        assertEquals(agenda, turno.getAgenda());
    }

    @Test
    void turnoGuardaClienteCorrectamente() {
        var turno = crearTurno();
        assertEquals(cliente, turno.getCliente());
    }

    @Test
    void turnoGuardaFechaCorrectamente() {
        var turno = crearTurno();
        assertEquals(LocalDate.of(2025, 6, 16), turno.getFecha());
    }

    @Test
    void turnoGuardaBloqueHorarioCorrectamente() {
        var turno = crearTurno();
        assertEquals(LocalTime.of(9, 0), turno.getBloqueHorario());
    }

    @Test
    void turnoGuardaEstadoInicialCorrectamenteHastaQueElProfesinalConfirmeTurno() {

        var turno = crearTurno();
        assertEquals(TurnoEstado.OCUPADO_SIN_CONFIRMAR, turno.getEstado());
    }

    @Test
    void setEstadoCambiaDeOcupadoSinConfirmarAConfirmado() {

        var turno = crearTurno();
        turno.setEstado(TurnoEstado.CONFIRMADO);

        assertEquals(TurnoEstado.CONFIRMADO, turno.getEstado());
    }

    @Test
    void setEstadoCambiaADeshabilitado() {

        var turno = crearTurno();
        turno.setEstado(TurnoEstado.DESHABILITADO);

        assertEquals(TurnoEstado.DESHABILITADO, turno.getEstado());
    }

    @Test
    void unTurnoConfirmadoPuedeCambiarADeshabilitado() {

        var turno = crearTurno();

        turno.setEstado(TurnoEstado.CONFIRMADO);
        turno.setEstado(TurnoEstado.DESHABILITADO);
        assertNotEquals(TurnoEstado.CONFIRMADO, turno.getEstado());
        assertEquals(TurnoEstado.DESHABILITADO, turno.getEstado());
    }
    
}