package ar.uba.fi.ingsoft1.product_example.agenda;

import ar.uba.fi.ingsoft1.product_example.turno.TurnoDTO;

import java.util.List;

public class AgendaConflictoException extends RuntimeException {
    private final List<TurnoDTO> turnosConflictivos;

    public AgendaConflictoException(List<TurnoDTO> turnosConflictivos) {
        super("La nueva agenda entra en conflicto con turnos existentes");
        this.turnosConflictivos = turnosConflictivos;
    }

    public List<TurnoDTO> getTurnosConflictivos() {
        return turnosConflictivos;
    }
}
