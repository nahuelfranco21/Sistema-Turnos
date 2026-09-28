package ar.uba.fi.ingsoft1.product_example.turno;

import ar.uba.fi.ingsoft1.product_example.common.SecurityUtils;
import ar.uba.fi.ingsoft1.product_example.common.exception.ItemNotFoundException;
import ar.uba.fi.ingsoft1.product_example.user.UserPublicDTO;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/turno")
@Tag(name = "4 - Turnos")
class TurnoRestController {

    private final TurnoService turnoService;

    @Autowired
    TurnoRestController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @GetMapping("/mis-turnos")
    @Operation(summary = "Get turnos for the logged-in client")
    @ResponseStatus(HttpStatus.OK)
    public List<TurnoDTO> getMisTurnos() {
        String username = SecurityUtils.getCurrentUsername();
        return turnoService.getMisTurnos(username);
    }

    @GetMapping("/profesionales-recientes")
    @Operation(summary = "Get recent professionals for the logged-in client")
    @ResponseStatus(HttpStatus.OK)
    public List<UserPublicDTO> getProfesionalesRecientes() {
        String username = SecurityUtils.getCurrentUsername();
        return turnoService.getProfesionalesRecientes(username);
    }

    @GetMapping("/{turno_id}")
    @Operation(summary = "Get turno details")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "404", description = "Turno not found", content = @Content)
    public TurnoDTO getTurno(@PathVariable("turno_id") Long turnoId) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        return turnoService.getTurno(turnoId, username, role);
    }

    @PostMapping
    @Operation(summary = "Create a turno (reservation or block)")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "409", description = "Block already occupied", content = @Content)
    public TurnoDTO createTurno(
            @Valid @NonNull @RequestBody TurnoCreateDTO data
    ) throws ItemNotFoundException, MethodArgumentNotValidException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        return turnoService.createTurno(data, username, role);
    }

    @PutMapping("/{turno_id}")
    @Operation(summary = "Update turno estado")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "403", description = "Only professionals can update turnos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Turno not found", content = @Content)
    public TurnoDTO updateEstado(
            @PathVariable("turno_id") Long turnoId,
            @Valid @NonNull @RequestBody TurnoEstadoUpdateDTO data
    ) throws ItemNotFoundException, MethodArgumentNotValidException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        return turnoService.updateEstado(turnoId, data, username, role);
    }

    @DeleteMapping("/{turno_id}")
    @Operation(summary = "Delete a turno, freeing the block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "404", description = "Turno not found", content = @Content)
    public ResponseEntity<Void> deleteTurno(@PathVariable("turno_id") Long turnoId) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        turnoService.deleteTurno(turnoId, username, role);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{turno_id}/modificar")
    @Operation(summary = "Modify a turno's date, time and/or client")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "400", description = "Invalid date or time slot", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not authorized", content = @Content)
    @ApiResponse(responseCode = "404", description = "Turno not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Block already occupied", content = @Content)
    public TurnoDTO modificarTurno(
            @PathVariable("turno_id") Long turnoId,
            @Valid @NonNull @RequestBody TurnoUpdateDTO data
    ) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        return turnoService.modificarTurno(turnoId, data, username, role);
    }

    @PostMapping("/cancelar-en-lote")
    @Operation(summary = "Cancel all turnos for a given agenda and date (bulk cancel)")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "403", description = "Not authorized", content = @Content)
    @ApiResponse(responseCode = "404", description = "Agenda not found", content = @Content)
    public List<TurnoDTO> cancelarEnLote(
            @Valid @NonNull @RequestBody TurnoBulkCancelDTO data
    ) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        return turnoService.cancelarEnLote(data.agendaId(), data.fecha(), username, role);
    }

    @PutMapping("/{turno_id}/reprogramar")
    @Operation(summary = "Reschedule a turno that the professional marked as REPROGRAMAR")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "400", description = "Invalid date or time slot", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not authorized", content = @Content)
    @ApiResponse(responseCode = "404", description = "Turno not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Block already occupied", content = @Content)
    public TurnoDTO reprogramarTurno(
            @PathVariable("turno_id") Long turnoId,
            @Valid @NonNull @RequestBody TurnoReprogramarDTO data
    ) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        return turnoService.reprogramarTurno(turnoId, data, username, role);
    }
}
