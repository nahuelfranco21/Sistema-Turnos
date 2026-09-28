package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordDTO(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {}
