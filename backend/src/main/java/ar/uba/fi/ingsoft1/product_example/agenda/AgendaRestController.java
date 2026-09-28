package ar.uba.fi.ingsoft1.product_example.agenda;

import ar.uba.fi.ingsoft1.product_example.common.SecurityUtils;
import ar.uba.fi.ingsoft1.product_example.common.exception.ItemNotFoundException;
import ar.uba.fi.ingsoft1.product_example.servicio.ServicioPublicoDTO;
import ar.uba.fi.ingsoft1.product_example.servicio.ServicioRepository;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoPublicoDTO;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoService;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/agenda")
@Tag(name = "3 - Agenda")
class AgendaRestController {

    private final AgendaService agendaService;
    private final TurnoService turnoService;
    private final ServicioRepository servicioRepository;

    @Autowired
    AgendaRestController(AgendaService agendaService, TurnoService turnoService, ServicioRepository servicioRepository) {
        this.agendaService = agendaService;
        this.turnoService = turnoService;
        this.servicioRepository = servicioRepository;
    }

    @GetMapping("/{agenda_id}")
    @Operation(summary = "Get full agenda (owner or admin only)")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "403", description = "Not the agenda owner", content = @Content)
    @ApiResponse(responseCode = "404", description = "Agenda not found", content = @Content)
    public AgendaDetailDTO getAgenda(@PathVariable("agenda_id") Long agendaId) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        AgendaDTO agenda = agendaService.getAgendaDTOById(agendaId);
        if (!role.getImpliedRoles().contains(UserRole.SUPER_ADMIN)
                && !agendaService.isOwner(agendaId, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta agenda");
        }
        var turnos = turnoService.getTurnosByAgendaId(agenda.id());
        return new AgendaDetailDTO(agenda, turnos);
    }

    @GetMapping("/mi-agenda")
    @Operation(summary = "Get the logged-in professional's own agenda with turnos")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "403", description = "Only professionals can access their agenda", content = @Content)
    @ApiResponse(responseCode = "404", description = "Agenda not found", content = @Content)
    public AgendaDetailDTO getMiAgenda() throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        if (!role.getImpliedRoles().contains(UserRole.PROFESIONAL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo profesionales tienen agenda");
        }
        AgendaDTO agenda = agendaService.getAgendaDTOByProfesionalUsername(username);
        var turnos = turnoService.getTurnosByAgendaId(agenda.id());
        return new AgendaDetailDTO(agenda, turnos);
    }

    @GetMapping("/profesional/{profesional_id}")
    @Operation(summary = "Get public agenda for a profesional (slot availability, no client data)")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "404", description = "Agenda not found", content = @Content)
    public AgendaPublicaDetailDTO getAgendaByProfesionalId(@PathVariable("profesional_id") Long profesionalId) throws ItemNotFoundException {
        AgendaDTO agenda = agendaService.getAgendaDTOByProfesionalId(profesionalId);
        var turnosPublicos = turnoService.getTurnosByAgendaId(agenda.id()).stream()
                .map(TurnoPublicoDTO::fromTurnoDTO)
                .toList();
        var servicios = servicioRepository.findByAgenda_Id(agenda.id()).stream()
                .map(s -> new ServicioPublicoDTO(s.getId(), s.getNombre(), s.getPrecio(), s.isActivo()))
                .toList();
        return new AgendaPublicaDetailDTO(agenda, turnosPublicos, servicios);
    }

    @PutMapping("/{agenda_id}")
    @Operation(summary = "Update agenda configuration. Use ?conflictos=cancelar to cancel conflicting turnos, or ?conflictos=mantener to keep them.")
    @ApiResponse(responseCode = "403", description = "Only professionals can modify the agenda", content = @Content)
    @ApiResponse(responseCode = "404", description = "Agenda not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Existing turnos conflict with the new schedule", content = @Content)
    public ResponseEntity<?> updateAgenda(
            @PathVariable("agenda_id") Long agendaId,
            @Valid @NonNull @RequestBody AgendaUpdateDTO data,
            @RequestParam(required = false) String conflictos
    ) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        if (!role.getImpliedRoles().contains(UserRole.PROFESIONAL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo profesionales pueden modificar la agenda");
        }
        try {
            AgendaDTO result = agendaService.updateAgenda(agendaId, data, username, role, conflictos);
            return ResponseEntity.ok(result);
        } catch (AgendaConflictoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AgendaConflictoDTO(e.getTurnosConflictivos()));
        }
    }
}
