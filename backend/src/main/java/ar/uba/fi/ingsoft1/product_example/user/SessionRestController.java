package ar.uba.fi.ingsoft1.product_example.user;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/sessions")
@Tag(name = "2 - Sessions")
class SessionRestController {

    private final UserService userService;

    @Autowired
    SessionRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(produces = "application/json")
    @Operation(summary = "Log in, creating a new session")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "401", description = "Invalid email or password supplied", content = @Content)
    public TokenDTO login(
            @Valid @NonNull @RequestBody UserLoginDTO data
    ) throws MethodArgumentNotValidException {
        return userService
                .loginUser(data)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"));
    }

    @PutMapping(produces = "application/json")
    @Operation(summary = "Refresh a session")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "401", description = "Invalid refresh token supplied", content = @Content)
    public TokenDTO refresh(
            @Valid @NonNull @RequestBody RefreshDTO data
    ) throws MethodArgumentNotValidException {
        return userService
                .refresh(data)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
    }

    @PostMapping("/recover")
    @Operation(summary = "Start password recovery process")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> recoverPassword(
            @Valid @NonNull @RequestBody PasswordRecoveryDTO data,
            @RequestHeader(name = "X-Frontend-URL", required = false) String frontendUrl
    ) {
        userService.startPasswordRecovery(data, frontendUrl);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using recovery token")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content)
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @NonNull @RequestBody ResetPasswordDTO data
    ) {
        try {
            userService.resetPassword(data.token(), data.newPassword());
            return ResponseEntity.ok(Map.of("message", "Contraseña restablecida correctamente"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getReason()));
        }
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify email address using token from email link")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content)
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam String token
    ) {
        try {
            userService.verifyEmail(token);
            return ResponseEntity.ok(Map.of("message", "Cuenta verificada correctamente"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getReason()));
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification link")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> resendVerification(
            @Valid @NonNull @RequestBody PasswordRecoveryDTO data,
            @RequestHeader(name = "X-Frontend-URL", required = false) String frontendUrl
    ) {
        userService.resendVerificationEmail(data.email(), frontendUrl);
        return ResponseEntity.ok().build();
    }
}
