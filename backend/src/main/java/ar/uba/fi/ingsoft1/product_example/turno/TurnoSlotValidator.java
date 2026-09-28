package ar.uba.fi.ingsoft1.product_example.turno;

import ar.uba.fi.ingsoft1.product_example.agenda.DiaSemana;
import ar.uba.fi.ingsoft1.product_example.agenda.RangoHorario;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TurnoSlotValidator {

    private final TurnoRepository turnoRepository;

    public TurnoSlotValidator(TurnoRepository turnoRepository) {
        this.turnoRepository = turnoRepository;
    }

    public void validarSlot(
        Long agendaId,
        LocalDate fecha,
        LocalTime bloqueHorario,
        int bloqueMinutos,
        List<RangoHorario> rangos,
        Long excludeTurnoId
    ) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se puede agendar en el pasado"
            );
        }

        int slotStart = bloqueHorario.getHour() * 60 + bloqueHorario.getMinute();
        int slotEnd = slotStart + bloqueMinutos;

        DiaSemana dia = DiaSemana.from(fecha.getDayOfWeek());
        boolean slotValido = rangos
            .stream()
            .filter(r -> r.getDia() == dia)
            .anyMatch(r -> {
                int rStart = r.getHoraInicio().getHour() * 60 + r.getHoraInicio().getMinute();
                int rEnd = r.getHoraFin().getHour() * 60 + r.getHoraFin().getMinute();
                if (slotStart < rStart || slotEnd > rEnd) return false;
                return (slotStart - rStart) % bloqueMinutos == 0;
            });
        if (!slotValido) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El horario no está alineado a la grilla de " + bloqueMinutos + " minutos"
            );
        }

        List<Turno> existentes = turnoRepository.findByAgendaIdAndFecha(agendaId, fecha);
        boolean solapa = existentes
            .stream()
            .filter(t -> !t.getId().equals(excludeTurnoId))
            .anyMatch(t -> {
                int existingStart = t.getBloqueHorario().getHour() * 60 + t.getBloqueHorario().getMinute();
                int existingEnd = existingStart + bloqueMinutos;
                return slotStart < existingEnd && slotEnd > existingStart;
            });
        if (solapa) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El bloque [" + bloqueHorario + " - " + bloqueHorario.plusMinutes(bloqueMinutos)
                    + "] se superpone con un turno existente"
            );
        }
    }
}
