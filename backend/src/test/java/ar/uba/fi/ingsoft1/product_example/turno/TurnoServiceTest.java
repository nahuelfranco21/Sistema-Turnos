package ar.uba.fi.ingsoft1.product_example.turno;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.agenda.AgendaService;
import ar.uba.fi.ingsoft1.product_example.agenda.DiaSemana;
import ar.uba.fi.ingsoft1.product_example.common.exception.ItemNotFoundException;
import ar.uba.fi.ingsoft1.product_example.notificacion.NotificacionService;
import ar.uba.fi.ingsoft1.product_example.servicio.ServicioRepository;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRepository;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TurnoServiceTest {

    private TurnoService turnoService;
    private TurnoRepository turnoRepository;
    private AgendaService agendaService;
    private UserRepository userRepository;
    private ServicioRepository servicioRepository;
    private NotificacionService notificacionService;
    private TurnoSlotValidator slotValidator;

    private Agenda agenda;
    private User profesional;
    private User cliente;

    private static LocalDate proximoDiaHabil(int daysAhead) {
        LocalDate d = LocalDate.now().plusDays(daysAhead);
        while (
            d.getDayOfWeek() == DayOfWeek.SATURDAY ||
            d.getDayOfWeek() == DayOfWeek.SUNDAY
        ) {
            d = d.plusDays(1);
        }
        return d;
    }

    @BeforeEach
    void setup() throws ItemNotFoundException {
        turnoRepository = mock(TurnoRepository.class);
        agendaService = mock(AgendaService.class);
        userRepository = mock(UserRepository.class);
        servicioRepository = mock(ServicioRepository.class);
        notificacionService = mock(NotificacionService.class);
        slotValidator = mock(TurnoSlotValidator.class);

        turnoService = new TurnoService(
            turnoRepository,
            agendaService,
            userRepository,
            servicioRepository,
            notificacionService,
            slotValidator
        );

        profesional = new User(
            "prof@test.com",
            "hash",
            UserRole.PROFESIONAL,
            "Carlos",
            "Lopez",
            LocalDate.of(1985, 5, 15),
            "Profesor",
            SectorTrabajo.EDUCACION
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
        List.of(
            DiaSemana.LUNES,
            DiaSemana.MARTES,
            DiaSemana.MIERCOLES,
            DiaSemana.JUEVES,
            DiaSemana.VIERNES
        ).forEach(dia ->
            agenda.addRango(dia, LocalTime.of(8, 0), LocalTime.of(17, 0))
        );

        when(userRepository.findByUsername("prof@test.com")).thenReturn(
            Optional.of(profesional)
        );
        when(userRepository.findByUsername("cli@test.com")).thenReturn(
            Optional.of(cliente)
        );
        when(agendaService.getAgendaById(any())).thenReturn(agenda);
    }

    private Turno crearTurno() {
        return new Turno(
            agenda,
            cliente,
            proximoDiaHabil(1),
            LocalTime.of(9, 0),
            TurnoEstado.OCUPADO_SIN_CONFIRMAR
        );
    }

    @Test
    void getMisTurnosDevuelveTurnosDelCliente() {
        when(turnoRepository.findByCliente_Username("cli@test.com")).thenReturn(
            List.of(crearTurno())
        );
        var turnos = turnoService.getMisTurnos("cli@test.com");
        assertEquals(1, turnos.size());
    }

    @Test
    void getMisTurnosVacioSiNoHayTurnos() {
        when(turnoRepository.findByCliente_Username("cli@test.com")).thenReturn(
            List.of()
        );
        var turnos = turnoService.getMisTurnos("cli@test.com");
        assertTrue(turnos.isEmpty());
    }

    @Test
    void getProfesionalesRecientesDevuelveLista() {
        when(
            turnoRepository.findDistinctProfesionalesByClienteUsername(
                "cli@test.com"
            )
        ).thenReturn(List.of(profesional));
        var profs = turnoService.getProfesionalesRecientes("cli@test.com");
        assertEquals(1, profs.size());
    }

    @Test
    void getProfesionalesRecientesVacioSiNoHay() {
        when(
            turnoRepository.findDistinctProfesionalesByClienteUsername(
                "cli@test.com"
            )
        ).thenReturn(List.of());
        var profs = turnoService.getProfesionalesRecientes("cli@test.com");
        assertTrue(profs.isEmpty());
    }

    @Test
    void profesionalPuedeModificarTurno() throws ItemNotFoundException {
        var turno = crearTurno();
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));

        var dto = new TurnoUpdateDTO(
            proximoDiaHabil(2),
            LocalTime.of(10, 0),
            cliente.getId(),
            null
        );
        var resultado = turnoService.modificarTurno(
            1L,
            dto,
            "prof@test.com",
            UserRole.PROFESIONAL
        );

        assertNotNull(resultado);
        assertEquals(proximoDiaHabil(2), resultado.fecha());
        assertEquals(LocalTime.of(10, 0), resultado.bloqueHorario());
        verify(turnoRepository, times(1)).save(turno);
    }

    @Test
    void modificarTurnoInexistenteLanzaItemNotFound() {
        when(turnoRepository.findById(999L)).thenReturn(Optional.empty());
        var dto = new TurnoUpdateDTO(
            proximoDiaHabil(2),
            LocalTime.of(10, 0),
            null,
            null
        );
        assertThrows(ItemNotFoundException.class, () ->
            turnoService.modificarTurno(
                999L,
                dto,
                "prof@test.com",
                UserRole.PROFESIONAL
            )
        );
    }

    @Test
    void clienteNoPuedeModificarTurno() {
        var turno = crearTurno();
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        var dto = new TurnoUpdateDTO(
            proximoDiaHabil(2),
            LocalTime.of(10, 0),
            null,
            null
        );
        assertThrows(ResponseStatusException.class, () ->
            turnoService.modificarTurno(
                1L,
                dto,
                "cli@test.com",
                UserRole.CLIENTE
            )
        );
    }

    @Test
    void noSePuedeModificarTurnoCancelado() {
        var turno = crearTurno();
        turno.setEstado(TurnoEstado.CANCELADO);
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        var dto = new TurnoUpdateDTO(
            proximoDiaHabil(2),
            LocalTime.of(10, 0),
            null,
            null
        );
        assertThrows(ResponseStatusException.class, () ->
            turnoService.modificarTurno(
                1L,
                dto,
                "prof@test.com",
                UserRole.PROFESIONAL
            )
        );
    }

    @Test
    void cancelarEnLoteCancelaTurnosDelDia() throws ItemNotFoundException {
        var turno1 = crearTurno();
        var turno2 = new Turno(
            agenda,
            cliente,
            proximoDiaHabil(1),
            LocalTime.of(10, 0),
            TurnoEstado.OCUPADO_SIN_CONFIRMAR
        );
        when(
            turnoRepository.findByAgendaIdAndFecha(1L, proximoDiaHabil(1))
        ).thenReturn(List.of(turno1, turno2));
        when(agendaService.getAgendaById(1L)).thenReturn(agenda);

        var resultado = turnoService.cancelarEnLote(
            1L,
            proximoDiaHabil(1),
            "prof@test.com",
            UserRole.PROFESIONAL
        );
        assertEquals(2, resultado.size());
        verify(turnoRepository, times(2)).save(any());
    }

    @Test
    void cancelarEnLoteSinTurnosDevuelveListaVacia()
        throws ItemNotFoundException {
        when(
            turnoRepository.findByAgendaIdAndFecha(1L, proximoDiaHabil(1))
        ).thenReturn(List.of());
        when(agendaService.getAgendaById(1L)).thenReturn(agenda);

        var resultado = turnoService.cancelarEnLote(
            1L,
            proximoDiaHabil(1),
            "prof@test.com",
            UserRole.PROFESIONAL
        );
        assertTrue(resultado.isEmpty());
    }

    @Test
    void cancelarEnLoteSinPermisoLanzaExcepcion() throws ItemNotFoundException {
        when(agendaService.getAgendaById(1L)).thenReturn(agenda);
        assertThrows(ResponseStatusException.class, () ->
            turnoService.cancelarEnLote(
                1L,
                proximoDiaHabil(1),
                "otro@test.com",
                UserRole.PROFESIONAL
            )
        );
    }

    @Test
    void profesionalPuedeEliminarTurno() throws ItemNotFoundException {
        var turno = crearTurno();
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        turnoService.deleteTurno(1L, "prof@test.com", UserRole.PROFESIONAL);
        verify(turnoRepository, times(1)).deleteById(1L);
    }

    @Test
    void clientePuedeEliminarSuPropioTurno() throws ItemNotFoundException {
        var turno = crearTurno();
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        turnoService.deleteTurno(1L, "cli@test.com", UserRole.CLIENTE);
        verify(turnoRepository, times(1)).deleteById(1L);
    }

    @Test
    void otroClienteNoPuedeEliminarTurnoAjeno() {
        var turno = crearTurno();
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        assertThrows(ResponseStatusException.class, () ->
            turnoService.deleteTurno(1L, "otro@test.com", UserRole.CLIENTE)
        );
    }

    @Test
    void getTurnosByAgendaIdDevuelveTurnos() {
        when(turnoRepository.findByAgenda_Id(1L)).thenReturn(
            List.of(crearTurno())
        );
        var turnos = turnoService.getTurnosByAgendaId(1L);
        assertEquals(1, turnos.size());
    }

    @Test
    void getTurnosByClienteIdDevuelveTurnos() {
        when(turnoRepository.findByCliente_Id(1L)).thenReturn(
            List.of(crearTurno())
        );
        var turnos = turnoService.getTurnosByClienteId(1L);
        assertEquals(1, turnos.size());
    }

    @Test
    void clientePuedeReprogramarTurnoEnEstadoReprogramar()
        throws ItemNotFoundException {
        var turno = crearTurno();
        turno.setEstado(TurnoEstado.REPROGRAMAR);
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));

        var dto = new TurnoReprogramarDTO(
            proximoDiaHabil(3),
            LocalTime.of(10, 0)
        );
        var resultado = turnoService.reprogramarTurno(
            1L,
            dto,
            "cli@test.com",
            UserRole.CLIENTE
        );

        assertNotNull(resultado);
        assertEquals(TurnoEstado.CONFIRMADO, resultado.estado());
        assertEquals(proximoDiaHabil(3), resultado.fecha());
        verify(turnoRepository, times(1)).save(turno);
    }

    @Test
    void reprogramarTurnoConEstadoInvalidoLanzaExcepcion() {
        var turno = crearTurno();
        turno.setEstado(TurnoEstado.CONFIRMADO);
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));

        var dto = new TurnoReprogramarDTO(
            proximoDiaHabil(3),
            LocalTime.of(10, 0)
        );
        assertThrows(ResponseStatusException.class, () ->
            turnoService.reprogramarTurno(
                1L,
                dto,
                "cli@test.com",
                UserRole.CLIENTE
            )
        );
    }

    @Test
    void reprogramarTurnoOtroClienteNoPuedeReprogramar() {
        var turno = crearTurno();
        turno.setEstado(TurnoEstado.REPROGRAMAR);
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));

        var dto = new TurnoReprogramarDTO(
            proximoDiaHabil(3),
            LocalTime.of(10, 0)
        );
        assertThrows(ResponseStatusException.class, () ->
            turnoService.reprogramarTurno(
                1L,
                dto,
                "otro@test.com",
                UserRole.CLIENTE
            )
        );
    }

    @Test
    void clienteNoPuedeCrearTurnoConProfesionalSinServicios() {
        when(servicioRepository.findByAgenda_Id(any())).thenReturn(List.of());

        var dto = new TurnoCreateDTO(
            1L,
            proximoDiaHabil(1),
            LocalTime.of(9, 0),
            null,
            null,
            null
        );
        assertThrows(ResponseStatusException.class, () ->
            turnoService.createTurno(dto, "cli@test.com", UserRole.CLIENTE)
        );
    }

    @Test
    void profesionalPuedeCrearTurnoConNombreCliente()
        throws ItemNotFoundException {
        when(turnoRepository.findByAgendaIdAndFecha(any(), any())).thenReturn(
            List.of()
        );
        var dto = new TurnoCreateDTO(
            1L,
            proximoDiaHabil(1),
            LocalTime.of(9, 0),
            null,
            "Juan Externo",
            null
        );
        var resultado = turnoService.createTurno(
            dto,
            "prof@test.com",
            UserRole.PROFESIONAL
        );
        assertNotNull(resultado);
        assertEquals("Juan Externo", resultado.nombreCliente());
        verify(turnoRepository, times(1)).save(any());
    }

}
