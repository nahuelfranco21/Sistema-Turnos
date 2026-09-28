package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailDTO(
    @NotBlank @Email(message = "Ingresá un email válido") String email
) {}
