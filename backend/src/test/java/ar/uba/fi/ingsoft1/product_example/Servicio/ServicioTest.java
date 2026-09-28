package ar.uba.fi.ingsoft1.product_example.Servicio;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.agenda.DiaSemana;
import ar.uba.fi.ingsoft1.product_example.servicio.Servicio;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicioTest {

    private Agenda agenda;

    @BeforeEach
    void setup() {
        User profesional = new User(
                "prof@test.com", "hash", UserRole.PROFESIONAL,
                "Carlos", "Lopez", LocalDate.of(1985, 5, 15),
                "Profesor", SectorTrabajo.EDUCACION
        );
        agenda = new Agenda(profesional, 30, 1);
    }

    @Test
    void servicioGuardaNombreCorrectamente() {
        var servicio = new Servicio(agenda, "Corte de pelo", 30, 1500.0);

        assertEquals("Corte de pelo", servicio.getNombre());
    }


    @Test
    void servicioGuardaDuracionCorrectamente() {

        var servicio = new Servicio(agenda, "Corte de pelo", 30, 1500.0);

        assertEquals(30, servicio.getDuracionMinutos());
    }

    @Test
    void servicioGuardaPrecioCorrectamente() {
        var servicio = new Servicio(agenda, "Corte de pelo", 30, 1500.0);

        assertEquals(1500.0, servicio.getPrecio());
    }

    @Test
    void servicioPertenecaALaAgendaCorrecta() {
        var servicio = new Servicio(agenda, "Corte de pelo", 30, 1500.0);

        assertEquals(agenda, servicio.getAgenda());
    }
}