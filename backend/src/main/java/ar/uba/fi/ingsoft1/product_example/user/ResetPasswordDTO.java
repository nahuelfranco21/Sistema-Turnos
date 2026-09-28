package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordDTO(
        @NotBlank String token,
        @NotBlank String newPassword
) {}
