package ar.uba.fi.ingsoft1.product_example.agenda;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AgendaUpdateDTO(
    @NotNull Integer bloqueMinutos,
    @NotNull Integer mesesAnticipacion,
    @NotNull @Size(min = 1) List<@Valid RangoHorarioDTO> rangos
) {}
