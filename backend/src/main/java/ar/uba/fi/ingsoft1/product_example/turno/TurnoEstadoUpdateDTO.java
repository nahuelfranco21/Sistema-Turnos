package ar.uba.fi.ingsoft1.product_example.turno;

import jakarta.validation.constraints.NotNull;

public record TurnoEstadoUpdateDTO(
    @NotNull TurnoEstado estado
) {}
