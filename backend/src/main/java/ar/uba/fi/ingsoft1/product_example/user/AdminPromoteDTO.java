package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.NotBlank;

public record AdminPromoteDTO(
        @NotBlank String profesion,
        @NotBlank String sector,
        @NotBlank String ubicacion
) {}
