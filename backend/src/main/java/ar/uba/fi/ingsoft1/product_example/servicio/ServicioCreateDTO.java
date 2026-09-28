package ar.uba.fi.ingsoft1.product_example.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServicioCreateDTO(
    @NotNull Long agendaId,
    @NotBlank String nombre,
    @NotNull Double precio
) {}
