package ar.uba.fi.ingsoft1.product_example.Agenda;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.agenda.DiaSemana;
import ar.uba.fi.ingsoft1.product_example.agenda.RangoHorario;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgendaTest {

    private User profesional;

    @BeforeEach
    void setup() {
        profesional = new User(
                "prof@test.com", "hash", UserRole.PROFESIONAL,
                "Carlos", "Lopez", LocalDate.of(1985, 5, 15),
                "Profesosr", SectorTrabajo.EDUCACION
        );
    }

    private Agenda crearAgenda() {
        return new Agenda(profesional, 30, 1);
    }



    @Test
    void agendaGuardaProfesionalCorrectamente() {
        var agenda = crearAgenda();
        assertEquals(profesional, agenda.getProfesional());
    }

    @Test
    void agendaGuardaBloqueMinutosCorrectamente() {
        var agenda = crearAgenda();
        assertEquals(30, agenda.getBloqueMinutos());
    }

    @Test
    void agendaNuevaEmpiezaSinRangosDeDiasDeTrabajo() {

        var agenda = crearAgenda();
        assertTrue(agenda.getRangos().isEmpty());
    }

    @Test
    void addRangoAgregaUnRangoDeTrabajoALaAgenda() {
        var agenda = crearAgenda();
        agenda.addRango(DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(14, 0));

        assertEquals(1, agenda.getRangos().size());
    }

    @Test
    void addRangoGuardaElDiaDeTrabajoCorrectamente() {
        var agenda = crearAgenda();
        agenda.addRango(DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(14, 0));

        assertEquals(DiaSemana.LUNES, agenda.getRangos().get(0).getDia());
    }

    @Test
    void addRangoGuardaHoraInicioDeTrabajoCorrectamente() {
        var agenda = crearAgenda();
        agenda.addRango(DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(14, 0));

        assertEquals(LocalTime.of(10, 0), agenda.getRangos().get(0).getHoraInicio());
    }

    @Test
    void addRangoGuardaHoraFinDeTrabajoCorrectamente() {
        var agenda = crearAgenda();
        agenda.addRango(DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(14, 0));

        assertEquals(LocalTime.of(14, 0), agenda.getRangos().get(0).getHoraFin());
    }

    @Test
    void unaAgendaPuedeAlamcenarDistintosDiasDeTrabajoConDistintosRangos() {

        var agenda = crearAgenda();
        agenda.addRango(DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(14, 0));
        agenda.addRango(DiaSemana.LUNES, LocalTime.of(16, 0), LocalTime.of(20, 0));
        agenda.addRango(DiaSemana.MIERCOLES, LocalTime.of(10, 0), LocalTime.of(18, 0));

        assertEquals(3, agenda.getRangos().size());
    }

    @Test
    void updateConfigCambiaBloqueMinutosDeAtencionDeTrabajoDe30A60() {
        var agenda = crearAgenda();
        var nuevosRangos = List.of(
                new RangoHorario(agenda, DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(17, 0))
        );
        agenda.updateConfig(60, 1, nuevosRangos);

        assertNotEquals(30, agenda.getBloqueMinutos());
        assertEquals(60, agenda.getBloqueMinutos());
    }

    @Test
    void updateConfigReemplazaLosRangosExistentesPorLosNuevos() {
        var agenda = crearAgenda();
        var nuevosRangos = List.of(
                new RangoHorario(agenda, DiaSemana.SABADO, LocalTime.of(10, 0), LocalTime.of(14, 0))
        );
        agenda.addRango(DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(17, 0));
        agenda.addRango(DiaSemana.MARTES, LocalTime.of(9, 0), LocalTime.of(17, 0));

        agenda.updateConfig(30, 1, nuevosRangos);

        assertEquals(1, agenda.getRangos().size());
        assertEquals(DiaSemana.SABADO, agenda.getRangos().get(0).getDia());
        assertNotEquals(DiaSemana.LUNES, agenda.getRangos().get(0).getDia());
    }

    @Test
    void updateConfigConListaVaciaEliminaTodosLosRangosDeTrabajo() {
        var agenda = crearAgenda();

        agenda.addRango(DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(17, 0));
        agenda.addRango(DiaSemana.MARTES, LocalTime.of(9, 0), LocalTime.of(17, 0));
        agenda.updateConfig(30, 1, List.of());

        assertTrue(agenda.getRangos().isEmpty());
    }
}
