package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfesionDTO(
    @NotBlank String profesion,
    String sector,
    @NotBlank String ubicacion
) {}
