package ar.uba.fi.ingsoft1.product_example.agenda;

import ar.uba.fi.ingsoft1.product_example.turno.TurnoDTO;

import java.util.List;

public record AgendaConflictoDTO(List<TurnoDTO> turnosConflictivos) {}
