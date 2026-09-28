package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordRecoveryDTO(
        @Email @NotBlank String email
) {
}
