package ar.uba.fi.ingsoft1.product_example.agenda;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record RangoHorarioDTO(
    @NotNull DiaSemana dia,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horaFin
) {}
