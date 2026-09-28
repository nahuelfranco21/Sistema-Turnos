package ar.uba.fi.ingsoft1.product_example.turno;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TurnoBulkCancelDTO(
    @NotNull Long agendaId,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha
) {}
