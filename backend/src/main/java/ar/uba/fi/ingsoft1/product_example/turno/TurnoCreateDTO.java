package ar.uba.fi.ingsoft1.product_example.turno;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record TurnoCreateDTO(
    @NotNull Long agendaId,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime bloqueHorario,
    Long clienteId,
    String nombreCliente,
    Long servicioId
) {}
