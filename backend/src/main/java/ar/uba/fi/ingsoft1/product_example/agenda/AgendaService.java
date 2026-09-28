package ar.uba.fi.ingsoft1.product_example.agenda;

import ar.uba.fi.ingsoft1.product_example.common.exception.ItemNotFoundException;
import ar.uba.fi.ingsoft1.product_example.notificacion.NotificacionService;
import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoDTO;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoEstado;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoRepository;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoService;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRepository;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AgendaService {

    private final AgendaRepository agendaRepository;
    private final UserRepository userRepository;
    private final TurnoRepository turnoRepository;
    private final NotificacionService notificacionService;

    @Autowired
    public AgendaService(
        AgendaRepository agendaRepository,
        UserRepository userRepository,
        TurnoRepository turnoRepository,
        NotificacionService notificacionService
    ) {
        this.agendaRepository = agendaRepository;
        this.userRepository = userRepository;
        this.turnoRepository = turnoRepository;
        this.notificacionService = notificacionService;
    }

    public AgendaDTO getAgendaDTOById(Long agendaId)
        throws ItemNotFoundException {
        Agenda agenda = agendaRepository
            .findById(agendaId)
            .orElseThrow(() -> new ItemNotFoundException("Agenda", agendaId));
        return AgendaDTO.fromAgenda(agenda);
    }

    public AgendaDTO getAgendaDTOByProfesionalId(Long profesionalId)
        throws ItemNotFoundException {
        Agenda agenda = agendaRepository
            .findByProfesionalId(profesionalId)
            .orElseThrow(() ->
                new ItemNotFoundException("Agenda", profesionalId)
            );
        return AgendaDTO.fromAgenda(agenda);
    }

    public AgendaDTO getAgendaDTOByProfesionalUsername(String username) {
        return agendaRepository
            .findByProfesionalUsername(username)
            .map(AgendaDTO::fromAgenda)
            .orElseGet(() -> {
                User profesional = userRepository
                    .findByUsername(username)
                    .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED)
                    );
                createDefaultAgenda(profesional);
                return agendaRepository
                    .findByProfesionalUsername(username)
                    .map(AgendaDTO::fromAgenda)
                    .orElseThrow(() ->
                        new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error al crear agenda por defecto"
                        )
                    );
            });
    }

    public List<TurnoDTO> detectarConflictos(
        Long agendaId,
        AgendaUpdateDTO data
    ) {
        List<Turno> candidatos =
            turnoRepository.findByAgenda_IdAndClienteIsNotNullAndFechaGreaterThanEqualAndEstadoNot(
                agendaId,
                LocalDate.now(),
                TurnoEstado.CANCELADO
            );
        LocalDate nuevoLimite = LocalDate.now().plusMonths(
            data.mesesAnticipacion()
        );
        return candidatos
            .stream()
            .filter(
                t ->
                    !estaEnNuevosRangos(
                        t,
                        data.rangos(),
                        data.bloqueMinutos()
                    ) || t.getFecha().isAfter(nuevoLimite)
            )
            .map(TurnoDTO::fromTurno)
            .toList();
    }

    private boolean estaEnNuevosRangos(
        Turno turno,
        List<RangoHorarioDTO> rangos,
        int bloqueMinutos
    ) {
        DiaSemana dia = DiaSemana.from(turno.getFecha().getDayOfWeek());
        LocalTime inicio = turno.getBloqueHorario();
        LocalTime fin = inicio.plusMinutes(bloqueMinutos);
        return rangos
            .stream()
            .filter(r -> r.dia() == dia)
            .anyMatch(
                r ->
                    !inicio.isBefore(r.horaInicio()) &&
                    !fin.isAfter(r.horaFin())
            );
    }

    public AgendaDTO updateAgenda(
        Long agendaId,
        AgendaUpdateDTO data,
        String currentUsername,
        UserRole currentRole,
        String accion
    ) throws ItemNotFoundException {
        Agenda agenda = agendaRepository
            .findById(agendaId)
            .orElseThrow(() -> new ItemNotFoundException("Agenda", agendaId));
        if (
            !currentRole.getImpliedRoles().contains(UserRole.SUPER_ADMIN) &&
            !agenda.getProfesional().getUsername().equals(currentUsername)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puedes modificar una agenda que no te pertenece"
            );
        }

        if (accion == null) {
            List<TurnoDTO> conflictos = detectarConflictos(agendaId, data);
            if (!conflictos.isEmpty()) {
                throw new AgendaConflictoException(conflictos);
            }
        } else if ("cancelar".equals(accion)) {
            LocalDate nuevoLimite = LocalDate.now().plusMonths(
                data.mesesAnticipacion()
            );
            List<Turno> candidatos =
                turnoRepository.findByAgenda_IdAndClienteIsNotNullAndFechaGreaterThanEqualAndEstadoNot(
                    agendaId,
                    LocalDate.now(),
                    TurnoEstado.CANCELADO
                );
            candidatos
                .stream()
                .filter(
                    t ->
                        !estaEnNuevosRangos(
                            t,
                            data.rangos(),
                            data.bloqueMinutos()
                        ) || t.getFecha().isAfter(nuevoLimite)
                )
                .forEach(t -> {
                    t.setEstado(TurnoEstado.CANCELADO);
                    turnoRepository.save(t);
                    notificacionService.onTurnoCancelado(t);
                });
        }

        List<RangoHorario> newRangos = data
            .rangos()
            .stream()
            .map(r ->
                new RangoHorario(agenda, r.dia(), r.horaInicio(), r.horaFin())
            )
            .toList();
        agenda.updateConfig(
            data.bloqueMinutos(),
            data.mesesAnticipacion(),
            newRangos
        );
        agendaRepository.save(agenda);
        return AgendaDTO.fromAgenda(agenda);
    }

    public Agenda getAgendaById(Long agendaId) throws ItemNotFoundException {
        return agendaRepository
            .findById(agendaId)
            .orElseThrow(() -> new ItemNotFoundException("Agenda", agendaId));
    }

    public boolean isOwner(Long agendaId, String username)
        throws ItemNotFoundException {
        Agenda agenda = agendaRepository
            .findById(agendaId)
            .orElseThrow(() -> new ItemNotFoundException("Agenda", agendaId));
        return agenda.getProfesional().getUsername().equals(username);
    }

    public java.util.Optional<Long> findAgendaIdByProfesionalId(
        Long profesionalId
    ) {
        return agendaRepository
            .findByProfesionalId(profesionalId)
            .map(Agenda::getId);
    }

    public void deleteAgendaAndTurnosByProfesionalId(
        Long profesionalId,
        TurnoService turnoService
    ) {
        agendaRepository
            .findByProfesionalId(profesionalId)
            .ifPresent(agenda -> {
                turnoService.deleteAllTurnosByAgendaId(agenda.getId());
                agendaRepository.delete(agenda);
            });
    }

    public void createDefaultAgenda(User profesional) {
        agendaRepository.save(new Agenda(profesional, 30, 1));
    }
}
