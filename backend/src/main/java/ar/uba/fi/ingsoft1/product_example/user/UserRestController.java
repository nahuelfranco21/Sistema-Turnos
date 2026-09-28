package ar.uba.fi.ingsoft1.product_example.user;

import ar.uba.fi.ingsoft1.product_example.common.SecurityUtils;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@Tag(name = "1 - Users")
class UserRestController {

    private final UserService userService;

    @Autowired
    UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    @Operation(summary = "Get public info of a user by ID")
    ResponseEntity<UserPublicDTO> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (
            org.springframework.security.core.userdetails.UsernameNotFoundException e
        ) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                e.getMessage()
            );
        }
    }

    @GetMapping(path = "/me", produces = "application/json")
    @Operation(summary = "Get profile of the authenticated user")
    ResponseEntity<UserProfileDTO> getMyProfile(Principal principal) {
        String username = extractUsername(principal);
        if (username == null) throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED
        );
        return ResponseEntity.ok(userService.getUserProfile(username));
    }

    @GetMapping(produces = "application/json")
    @Operation(
        summary = "Search users by role and query string. Searching for CLIENTEs requires PROFESIONAL role or higher."
    )
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<List<ProfessionalSearchResultDTO>> searchUsers(
        @RequestParam UserRole role,
        @RequestParam(defaultValue = "") String q,
        @RequestParam(required = false) String sector,
        @RequestParam(defaultValue = "") String profesion,
        @RequestParam(defaultValue = "") String servicio
    ) {
        UserRole callerRole = SecurityUtils.getCurrentRole();
        if (
            role == UserRole.CLIENTE &&
            !callerRole.getImpliedRoles().contains(UserRole.PROFESIONAL)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo profesionales pueden buscar clientes"
            );
        }
        return ResponseEntity.ok(
            userService.searchUsers(role, q, sector, profesion, servicio)
        );
    }

    @PostMapping(produces = "application/json")
    @Operation(summary = "Create a new user")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(
        responseCode = "409",
        description = "User already exists",
        content = @Content
    )
    ResponseEntity<TokenDTO> signUp(
        @Valid @NonNull @RequestBody UserCreateDTO data
    ) throws MethodArgumentNotValidException {
        return userService
            .createUser(data)
            .map(tk -> ResponseEntity.status(HttpStatus.CREATED).body(tk))
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El email ya está registrado"
                )
            );
    }

    @PutMapping(path = "/password", produces = "application/json")
    @Operation(summary = "Update password for logged user")
    public ResponseEntity<Map<String, String>> updatePassword(
        Principal principal,
        @Valid @NonNull @RequestBody UpdatePasswordDTO data
    ) {
        String email = extractUsername(principal);
        try {
            boolean ok = userService.updatePassword(
                email,
                data.currentPassword(),
                data.newPassword()
            );
            if (!ok) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", "Contraseña actual incorrecta")
                );
            }
            return ResponseEntity.ok(
                Map.of("message", "Contraseña actualizada")
            );
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", e.getMessage())
            );
        }
    }

    @PutMapping(path = "/email", produces = "application/json")
    @Operation(summary = "Update email for logged user")
    public ResponseEntity<Map<String, String>> updateEmail(
        Principal principal,
        @Valid @NonNull @RequestBody UpdateEmailDTO data,
        @org.springframework.web.bind.annotation.RequestHeader(
            value = "X-Frontend-URL",
            required = false
        ) String frontendUrl
    ) {
        String currentEmail = extractUsername(principal);
        if (currentEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Usuario no autenticado")
            );
        }

        try {
            boolean ok = userService.updateEmail(
                currentEmail,
                data.email(),
                frontendUrl
            );
            if (!ok) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "El email ya está registrado")
                );
            }
            return ResponseEntity.ok(
                Map.of(
                    "message",
                    "Email actualizado. Verificá tu nueva dirección de correo."
                )
            );
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", e.getMessage())
            );
        }
    }

    @PutMapping(path = "/profesion", produces = "application/json")
    @Operation(summary = "Update profesion and sector for logged user")
    public ResponseEntity<Map<String, String>> updateProfesion(
        Principal principal,
        @Valid @NonNull @RequestBody UpdateProfesionDTO data
    ) {
        String currentEmail = extractUsername(principal);
        if (currentEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Usuario no autenticado")
            );
        }

        try {
            userService.updateProfesion(
                currentEmail,
                data.profesion(),
                data.sector(),
                data.ubicacion()
            );
            return ResponseEntity.ok(
                Map.of("message", "Profesión actualizada")
            );
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", e.getMessage())
            );
        }
    }

    @PutMapping(path = "/descripcion", produces = "application/json")
    @Operation(summary = "Update description for logged professional user")
    public ResponseEntity<Map<String, String>> updateDescripcion(
        Principal principal,
        @Valid @NonNull @RequestBody UpdateDescripcionDTO data
    ) {
        String email = extractUsername(principal);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Usuario no autenticado")
            );
        }
        try {
            userService.updateDescripcion(email, data.descripcion());
            return ResponseEntity.ok(
                Map.of("message", "Descripción actualizada")
            );
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", e.getMessage())
            );
        }
    }

    @PutMapping(path = "/foto", produces = "application/json")
    @Operation(summary = "Update profile photo for logged user")
    public ResponseEntity<Map<String, String>> updateFotoPerfil(
        Principal principal,
        @Valid @NonNull @RequestBody UpdateFotoPerfilDTO data
    ) {
        String email = extractUsername(principal);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Usuario no autenticado")
            );
        }
        userService.updateFotoPerfil(email, data.fotoPerfil());
        return ResponseEntity.ok(Map.of("message", "Foto actualizada"));
    }

    private String extractUsername(Principal principal) {
        if (principal instanceof Authentication auth) {
            Object p = auth.getPrincipal();
            if (p instanceof JwtUserDetails jd) return jd.username();
            if (p instanceof UserDetails ud) return ud.getUsername();
        }
        return principal == null ? null : principal.getName();
    }
}
