package ar.uba.fi.ingsoft1.product_example.turno;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.agenda.AgendaService;
import ar.uba.fi.ingsoft1.product_example.common.exception.ItemNotFoundException;
import ar.uba.fi.ingsoft1.product_example.notificacion.NotificacionService;
import ar.uba.fi.ingsoft1.product_example.servicio.Servicio;
import ar.uba.fi.ingsoft1.product_example.servicio.ServicioRepository;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserPublicDTO;
import ar.uba.fi.ingsoft1.product_example.user.UserRepository;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final AgendaService agendaService;
    private final UserRepository userRepository;
    private final ServicioRepository servicioRepository;
    private final NotificacionService notificacionService;
    private final TurnoSlotValidator slotValidator;
    private static final Logger log = LoggerFactory.getLogger(TurnoService.class);

    private static final Set<TurnoEstado> PENDING_ESTADOS = EnumSet.of(
        TurnoEstado.CONFIRMADO, TurnoEstado.OCUPADO_SIN_CONFIRMAR, TurnoEstado.REPROGRAMAR
    );

    TurnoService(
        TurnoRepository turnoRepository,
        AgendaService agendaService,
        UserRepository userRepository,
        ServicioRepository servicioRepository,
        NotificacionService notificacionService,
        TurnoSlotValidator slotValidator
    ) {
        this.turnoRepository = turnoRepository;
        this.agendaService = agendaService;
        this.userRepository = userRepository;
        this.servicioRepository = servicioRepository;
        this.notificacionService = notificacionService;
        this.slotValidator = slotValidator;
    }

    public TurnoDTO getTurno(
        Long turnoId,
        String username,
        UserRole currentRole
    ) throws ItemNotFoundException {
        Turno turno = turnoRepository
            .findById(turnoId)
            .orElseThrow(() -> new ItemNotFoundException("Turno", turnoId));

        Set<UserRole> roles = currentRole.getImpliedRoles();
        if (!roles.contains(UserRole.SUPER_ADMIN)) {
            boolean isProfesionalOwner = turno
                .getAgenda()
                .getProfesional()
                .getUsername()
                .equals(username);
            boolean isClienteOwner =
                turno.getCliente() != null &&
                turno.getCliente().getUsername().equals(username);
            if (!isProfesionalOwner && !isClienteOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        return TurnoDTO.fromTurno(turno);
    }

    public TurnoDTO createTurno(
        TurnoCreateDTO data,
        String currentUsername,
        UserRole currentRole
    ) throws ItemNotFoundException {
        Agenda agenda = agendaService.getAgendaById(data.agendaId());

        if (!agenda.getProfesional().isActive()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El profesional no está disponible");
        }

        if (currentRole == UserRole.CLIENTE) {
            LocalDate limite = LocalDate.now().plusMonths(agenda.getMesesAnticipacion());
            if (data.fecha().isAfter(limite)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede reservar con más de " + agenda.getMesesAnticipacion() + " mes(es) de anticipación"
                );
            }
        }

        slotValidator.validarSlot(
            data.agendaId(), data.fecha(), data.bloqueHorario(),
            agenda.getBloqueMinutos(), agenda.getRangos(), null
        );

        Long resolvedClienteId = data.clienteId();

        if (currentRole == UserRole.CLIENTE) {
            User currentUser = userRepository
                .findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            resolvedClienteId = currentUser.getId();

            List<Servicio> servicios = servicioRepository.findByAgenda_Id(agenda.getId());
            if (servicios.isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El profesional no tiene servicios configurados"
                );
            }
        }

        final Long finalClienteId = resolvedClienteId;
        if (finalClienteId != null) {
            User cliente = userRepository
                .findById(finalClienteId)
                .orElseThrow(() -> new ItemNotFoundException("User", finalClienteId));

            if (!cliente.isActive()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La cuenta del cliente está desactivada");
            }

            if (currentRole == UserRole.CLIENTE) {
                User currentUser = userRepository
                    .findByUsername(currentUsername)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                if (!currentUser.getId().equals(cliente.getId())) {
                    throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No puedes crear turnos para otro cliente"
                    );
                }
            }

            if (
                currentRole.getImpliedRoles().contains(UserRole.PROFESIONAL) &&
                !currentRole.getImpliedRoles().contains(UserRole.SUPER_ADMIN) &&
                !agenda.getProfesional().getUsername().equals(currentUsername)
            ) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puedes crear turnos en una agenda que no te pertenece"
                );
            }

            Servicio servicio = null;
            if (data.servicioId() != null) {
                servicio = servicioRepository
                    .findById(data.servicioId())
                    .orElseThrow(() -> new ItemNotFoundException("Servicio", data.servicioId()));
                if (!servicio.getAgenda().getId().equals(data.agendaId())) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El servicio no pertenece a esta agenda"
                    );
                }
            }
            Turno turno = new Turno(agenda, cliente, data.fecha(), data.bloqueHorario(),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR, servicio);
            turnoRepository.save(turno);
            return TurnoDTO.fromTurno(turno);
        }

        if (!currentRole.getImpliedRoles().contains(UserRole.PROFESIONAL)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el profesional puede crear bloques sin cliente"
            );
        }
        if (
            !currentRole.getImpliedRoles().contains(UserRole.SUPER_ADMIN) &&
            !agenda.getProfesional().getUsername().equals(currentUsername)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puedes crear bloques en una agenda que no te pertenece"
            );
        }

        if (data.nombreCliente() != null && !data.nombreCliente().isBlank()) {
            Turno turno = new Turno(agenda, null, data.fecha(), data.bloqueHorario(), TurnoEstado.CONFIRMADO);
            turno.setNombreCliente(data.nombreCliente());
            turnoRepository.save(turno);
            return TurnoDTO.fromTurno(turno);
        }

        Turno turno = new Turno(agenda, null, data.fecha(), data.bloqueHorario(), TurnoEstado.DESHABILITADO);
        turnoRepository.save(turno);
        return TurnoDTO.fromTurno(turno);
    }

    public TurnoDTO updateEstado(
        Long turnoId,
        TurnoEstadoUpdateDTO data,
        String currentUsername,
        UserRole currentRole
    ) throws ItemNotFoundException {
        Turno turno = turnoRepository
            .findById(turnoId)
            .orElseThrow(() -> new ItemNotFoundException("Turno", turnoId));
        TurnoEstado estadoAnterior = turno.getEstado();

        User profesional = turno.getAgenda().getProfesional();
        boolean isProfesionalOwner = profesional.getUsername().equals(currentUsername);
        boolean isClienteOwner =
            turno.getCliente() != null && turno.getCliente().getUsername().equals(currentUsername);

        Set<UserRole> roles = currentRole.getImpliedRoles();
        boolean cancelledByProfesional = false;
        TurnoEstado destino = data.estado();
        if (roles.contains(UserRole.SUPER_ADMIN) ||
            (roles.contains(UserRole.PROFESIONAL) && isProfesionalOwner)) {
            if (destino == TurnoEstado.CANCELADO && turno.getEstado() == TurnoEstado.CONFIRMADO) {
                turno.setEstado(TurnoEstado.REPROGRAMAR);
            } else {
                turno.setEstado(destino);
            }
            cancelledByProfesional = destino == TurnoEstado.CANCELADO;
        } else if (isClienteOwner && (destino == TurnoEstado.CONFIRMADO || destino == TurnoEstado.CANCELADO)) {
            turno.setEstado(destino);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este turno");
        }

        turnoRepository.save(turno);

        if (cancelledByProfesional && estadoAnterior != TurnoEstado.REPROGRAMAR && estadoAnterior != TurnoEstado.CANCELADO) {
            notificacionService.onTurnoCancelado(turno);
        }

        return TurnoDTO.fromTurno(turno);
    }

    public List<TurnoDTO> getTurnosByAgendaId(Long agendaId) {
        return turnoRepository.findByAgenda_Id(agendaId).stream().map(TurnoDTO::fromTurno).toList();
    }

    public List<TurnoDTO> getMisTurnos(String username) {
        return turnoRepository.findByCliente_Username(username).stream().map(TurnoDTO::fromTurno).toList();
    }

    public List<UserPublicDTO> getProfesionalesRecientes(String username) {
        return turnoRepository.findDistinctProfesionalesByClienteUsername(username).stream()
            .map(UserPublicDTO::fromUser).toList();
    }

    public List<TurnoDTO> getTurnosByClienteId(Long clienteId) {
        return turnoRepository.findByCliente_Id(clienteId).stream().map(TurnoDTO::fromTurno).toList();
    }

    public boolean hasPendingFutureTurnosByClienteId(Long clienteId) {
        return turnoRepository.existsByCliente_IdAndEstadoInAndFechaGreaterThanEqual(
            clienteId, PENDING_ESTADOS, LocalDate.now());
    }

    public boolean hasPendingFutureTurnosByProfesionalId(Long profesionalId) {
        return turnoRepository.existsByAgenda_Profesional_IdAndEstadoInAndFechaGreaterThanEqual(
            profesionalId, PENDING_ESTADOS, LocalDate.now());
    }

    public void deleteAllTurnosByClienteId(Long clienteId) {
        turnoRepository.deleteAll(turnoRepository.findByCliente_Id(clienteId));
    }

    public void deleteAllTurnosByAgendaId(Long agendaId) {
        turnoRepository.deleteAll(turnoRepository.findByAgenda_Id(agendaId));
    }

    public void deleteTurno(Long turnoId, String currentUsername, UserRole currentRole) throws ItemNotFoundException {
        Turno turno = turnoRepository.findById(turnoId)
            .orElseThrow(() -> new ItemNotFoundException("Turno", turnoId));

        User profesional = turno.getAgenda().getProfesional();
        boolean isProfesionalOwner = profesional.getUsername().equals(currentUsername);
        boolean isClienteOwner = turno.getCliente() != null && turno.getCliente().getUsername().equals(currentUsername);

        if (!currentRole.getImpliedRoles().contains(UserRole.SUPER_ADMIN) && !isProfesionalOwner && !isClienteOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar este turno");
        }

        turnoRepository.deleteById(turnoId);
    }

    public TurnoDTO modificarTurno(Long turnoId, TurnoUpdateDTO data, String currentUsername, UserRole currentRole)
        throws ItemNotFoundException {
        Turno turno = turnoRepository.findById(turnoId)
            .orElseThrow(() -> new ItemNotFoundException("Turno", turnoId));

        Set<UserRole> roles = currentRole.getImpliedRoles();
        boolean isProfesionalOwner = turno.getAgenda().getProfesional().getUsername().equals(currentUsername);

        if (!roles.contains(UserRole.SUPER_ADMIN) && !isProfesionalOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este turno");
        }

        if (turno.getEstado() == TurnoEstado.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede modificar un turno cancelado");
        }

        Agenda agenda = turno.getAgenda();
        slotValidator.validarSlot(
            agenda.getId(), data.fecha(), data.bloqueHorario(),
            agenda.getBloqueMinutos(), agenda.getRangos(), turnoId
        );

        Long resolvedClienteId = data.clienteId();

        if (resolvedClienteId != null) {
            User cliente = userRepository.findById(resolvedClienteId)
                .orElseThrow(() -> new ItemNotFoundException("User", resolvedClienteId));
            turno.setCliente(cliente);
            turno.setNombreCliente(null);
        } else if (data.nombreCliente() != null && !data.nombreCliente().isBlank()) {
            if (!roles.contains(UserRole.PROFESIONAL)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el profesional puede dejar un turno sin cliente");
            }
            turno.setCliente(null);
            turno.setNombreCliente(data.nombreCliente());
            if (turno.getEstado() == TurnoEstado.DESHABILITADO) {
                turno.setEstado(TurnoEstado.OCUPADO_SIN_CONFIRMAR);
            }
        } else {
            if (!roles.contains(UserRole.PROFESIONAL)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el profesional puede dejar un turno sin cliente");
            }
            turno.setCliente(null);
            turno.setNombreCliente(null);
            turno.setEstado(TurnoEstado.DESHABILITADO);
        }

        turno.setFecha(data.fecha());
        turno.setBloqueHorario(data.bloqueHorario());
        turnoRepository.save(turno);
        return TurnoDTO.fromTurno(turno);
    }

    public TurnoDTO reprogramarTurno(Long turnoId, TurnoReprogramarDTO data, String currentUsername, UserRole currentRole)
        throws ItemNotFoundException {
        Turno turno = turnoRepository.findById(turnoId)
            .orElseThrow(() -> new ItemNotFoundException("Turno", turnoId));

        if (turno.getEstado() != TurnoEstado.REPROGRAMAR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden reprogramar turnos en estado REPROGRAMAR");
        }

        boolean isClienteOwner = turno.getCliente() != null && turno.getCliente().getUsername().equals(currentUsername);
        if (!currentRole.getImpliedRoles().contains(UserRole.SUPER_ADMIN) && !isClienteOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el cliente del turno puede reprogramarlo");
        }

        Agenda agenda = turno.getAgenda();
        slotValidator.validarSlot(
            agenda.getId(), data.fecha(), data.bloqueHorario(),
            agenda.getBloqueMinutos(), agenda.getRangos(), turnoId
        );

        turno.setFecha(data.fecha());
        turno.setBloqueHorario(data.bloqueHorario());
        turno.setEstado(TurnoEstado.CONFIRMADO);
        turnoRepository.save(turno);
        return TurnoDTO.fromTurno(turno);
    }

    public List<TurnoDTO> cancelarEnLote(Long agendaId, LocalDate fecha, String currentUsername, UserRole currentRole)
        throws ItemNotFoundException {
        Agenda agenda = agendaService.getAgendaById(agendaId);

        Set<UserRole> roles = currentRole.getImpliedRoles();
        boolean isProfesionalOwner = agenda.getProfesional().getUsername().equals(currentUsername);
        if (!roles.contains(UserRole.SUPER_ADMIN) && !isProfesionalOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para cancelar turnos en esta agenda");
        }

        List<Turno> turnosDelDia = turnoRepository.findByAgendaIdAndFecha(agendaId, fecha).stream()
            .filter(t -> t.getEstado() != TurnoEstado.CANCELADO && t.getEstado() != TurnoEstado.REPROGRAMAR)
            .toList();

        for (Turno t : turnosDelDia) {
            TurnoEstado destino = t.getEstado() == TurnoEstado.CONFIRMADO ? TurnoEstado.REPROGRAMAR : TurnoEstado.CANCELADO;
            t.setEstado(destino);
            turnoRepository.save(t);
            if (destino == TurnoEstado.REPROGRAMAR) {
                notificacionService.onTurnoCancelado(t);
            }
        }

        return turnosDelDia.stream().map(TurnoDTO::fromTurno).toList();
    }
}
