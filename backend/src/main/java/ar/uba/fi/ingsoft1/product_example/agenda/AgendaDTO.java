package ar.uba.fi.ingsoft1.product_example.agenda;

import java.util.List;

public record AgendaDTO(
    Long id,
    Long profesionalId,
    Integer bloqueMinutos,
    Integer mesesAnticipacion,
    List<RangoHorarioDTO> rangos
) {
    public static AgendaDTO fromAgenda(Agenda agenda) {
        List<RangoHorarioDTO> rangos = agenda.getRangos() == null ? List.of() :
            agenda.getRangos().stream()
                .map(r -> new RangoHorarioDTO(r.getDia(), r.getHoraInicio(), r.getHoraFin()))
                .toList();
        return new AgendaDTO(agenda.getId(), agenda.getProfesional().getId(), agenda.getBloqueMinutos(), agenda.getMesesAnticipacion(), rangos);
    }
}
