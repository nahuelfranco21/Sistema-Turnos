package ar.uba.fi.ingsoft1.product_example.turno;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoUpdateDTO(
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime bloqueHorario,
    Long clienteId,
    String nombreCliente
) {}
