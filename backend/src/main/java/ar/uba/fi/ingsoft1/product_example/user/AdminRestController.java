package ar.uba.fi.ingsoft1.product_example.user;

import ar.uba.fi.ingsoft1.product_example.common.SecurityUtils;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoDTO;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoService;
import ar.uba.fi.ingsoft1.product_example.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "5 - Admin")
class AdminRestController {

    private final AdminUserService adminUserService;
    private final TurnoService turnoService;

    @Autowired
    AdminRestController(AdminUserService adminUserService, TurnoService turnoService) {
        this.adminUserService = adminUserService;
        this.turnoService = turnoService;
    }

    private void requireSuperAdmin() {
        UserRole role = SecurityUtils.getCurrentRole();
        if (!role.getImpliedRoles().contains(UserRole.SUPER_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso restringido a superadmin");
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full user details (SUPER_ADMIN only)")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<UserAdminDTO> getUserAdminDetail(@PathVariable Long id) {
        requireSuperAdmin();
        return ResponseEntity.ok(adminUserService.getUserAdminDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user data (SUPER_ADMIN only)")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<Map<String, String>> updateUser(
            @PathVariable Long id,
            @Valid @NonNull @RequestBody AdminUpdateUserDTO data
    ) {
        requireSuperAdmin();
        adminUserService.adminUpdateUser(id, data);
        return ResponseEntity.ok(Map.of("message", "Usuario actualizado"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user, cancelling their active turnos (SUPER_ADMIN only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        requireSuperAdmin();
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Promote a CLIENTE to PROFESIONAL (SUPER_ADMIN only)")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<Map<String, String>> promoteUser(
            @PathVariable Long id,
            @Valid @NonNull @RequestBody AdminPromoteDTO data
    ) {
        requireSuperAdmin();
        adminUserService.promoteToProfesional(id, data);
        return ResponseEntity.ok(Map.of("message", "Usuario promovido a profesional"));
    }

    @GetMapping("/{id}/turnos")
    @Operation(summary = "Get all turnos for a client (SUPER_ADMIN only)")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<List<TurnoDTO>> getTurnosCliente(@PathVariable Long id) {
        requireSuperAdmin();
        return ResponseEntity.ok(turnoService.getTurnosByClienteId(id));
    }

    @PutMapping("/{id}/verified")
    @Operation(summary = "Set verified status for a user (SUPER_ADMIN only)")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<Map<String, String>> setVerified(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        requireSuperAdmin();
        Boolean verified = body.get("verified");
        if (verified == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Campo 'verified' requerido"));
        }
        adminUserService.adminSetVerified(id, verified);
        return ResponseEntity.ok(Map.of("message", "Estado de verificación actualizado"));
    }

    @PutMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a user (SUPER_ADMIN only)")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<Map<String, String>> setActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        requireSuperAdmin();
        Boolean active = body.get("active");
        if (active == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Campo 'active' requerido"));
        }
        adminUserService.setUserActive(id, active);
        return ResponseEntity.ok(Map.of("message", active ? "Cuenta activada" : "Cuenta desactivada"));
    }
}
