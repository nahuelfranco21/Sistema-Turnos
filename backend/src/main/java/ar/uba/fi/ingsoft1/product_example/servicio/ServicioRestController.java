package ar.uba.fi.ingsoft1.product_example.servicio;

import ar.uba.fi.ingsoft1.product_example.agenda.AgendaService;
import ar.uba.fi.ingsoft1.product_example.common.SecurityUtils;
import ar.uba.fi.ingsoft1.product_example.common.exception.ItemNotFoundException;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/servicio")
@Tag(name = "4 - Servicio")
class ServicioRestController {

    private final ServicioRepository servicioRepository;
    private final AgendaService agendaService;

    @Autowired
    ServicioRestController(ServicioRepository servicioRepository, AgendaService agendaService) {
        this.servicioRepository = servicioRepository;
        this.agendaService = agendaService;
    }

    @GetMapping("/agenda/{agenda_id}")
    @ResponseStatus(HttpStatus.OK)
    public List<ServicioPublicoDTO> getServiciosByAgenda(@PathVariable("agenda_id") Long agendaId) throws ItemNotFoundException {
        agendaService.getAgendaById(agendaId);
        return servicioRepository.findByAgenda_Id(agendaId).stream()
                .map(s -> new ServicioPublicoDTO(s.getId(), s.getNombre(), s.getPrecio(), s.isActivo()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioPublicoDTO createServicio(@Valid @NonNull @RequestBody ServicioCreateDTO data) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        if (!role.getImpliedRoles().contains(UserRole.PROFESIONAL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo profesionales pueden crear servicios");
        }
        if (!agendaService.isOwner(data.agendaId(), username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes crear servicios en una agenda que no te pertenece");
        }
        var agenda = agendaService.getAgendaById(data.agendaId());
        var servicio = new Servicio(agenda, data.nombre(), 0, data.precio());
        servicioRepository.save(servicio);
        return new ServicioPublicoDTO(servicio.getId(), servicio.getNombre(), servicio.getPrecio(), servicio.isActivo());
    }

    @PatchMapping("/{id}/toggle-activo")
    @ResponseStatus(HttpStatus.OK)
    public ServicioPublicoDTO toggleActivo(@PathVariable Long id) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        if (!role.getImpliedRoles().contains(UserRole.PROFESIONAL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo profesionales pueden modificar servicios");
        }
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Servicio", id));
        if (!role.getImpliedRoles().contains(UserRole.SUPER_ADMIN)
                && !servicio.getAgenda().getProfesional().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar servicios de otra agenda");
        }
        servicio.setActivo(!servicio.isActivo());
        servicioRepository.save(servicio);
        return new ServicioPublicoDTO(servicio.getId(), servicio.getNombre(), servicio.getPrecio(), servicio.isActivo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteServicio(@PathVariable Long id) throws ItemNotFoundException {
        String username = SecurityUtils.getCurrentUsername();
        UserRole role = SecurityUtils.getCurrentRole();
        if (!role.getImpliedRoles().contains(UserRole.PROFESIONAL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo profesionales pueden eliminar servicios");
        }
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Servicio", id));
        if (!role.getImpliedRoles().contains(UserRole.SUPER_ADMIN)
                && !servicio.getAgenda().getProfesional().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes eliminar servicios de otra agenda");
        }
        servicioRepository.deleteById(id);
    }
}
