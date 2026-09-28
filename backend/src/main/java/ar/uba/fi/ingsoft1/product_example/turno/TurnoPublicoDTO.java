package ar.uba.fi.ingsoft1.product_example.turno;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record TurnoPublicoDTO(
    Long id,
    Long agendaId,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
    @JsonFormat(pattern = "HH:mm") LocalTime bloqueHorario,
    TurnoEstado estado
) {
    public static TurnoPublicoDTO fromTurno(Turno turno) {
        return new TurnoPublicoDTO(
            turno.getId(),
            turno.getAgenda().getId(),
            turno.getFecha(),
            turno.getBloqueHorario(),
            turno.getEstado()
        );
    }

    public static TurnoPublicoDTO fromTurnoDTO(TurnoDTO dto) {
        return new TurnoPublicoDTO(dto.id(), dto.agendaId(), dto.fecha(), dto.bloqueHorario(), dto.estado());
    }
}
