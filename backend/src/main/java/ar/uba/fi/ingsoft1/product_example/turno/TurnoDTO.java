package ar.uba.fi.ingsoft1.product_example.turno;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record TurnoDTO(
    Long id,
    Long agendaId,
    Long clienteId,
    String clienteNombre,
    String clienteApellido,
    Long profesionalId,
    String profesionalNombre,
    String profesionalApellido,
    String profesionalProfesion,
    String profesionalSector,
    String profesionalUbicacion,
    String nombreCliente,
    String clienteFotoPerfil,
    String profesionalFotoPerfil,
    Long servicioId,
    String servicioNombre,
    Double servicioPrecio,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
    @JsonFormat(pattern = "HH:mm") LocalTime bloqueHorario,
    TurnoEstado estado
) {
    public static TurnoDTO fromTurno(Turno turno) {
        var cliente = turno.getCliente();
        var profesional = turno.getAgenda().getProfesional();
        var servicio = turno.getServicio();
        String clienteNombre = cliente != null ? cliente.getNombre() : turno.getNombreCliente();
        String clienteApellido = cliente != null ? cliente.getApellido() : null;
        return new TurnoDTO(
            turno.getId(),
            turno.getAgenda().getId(),
            cliente != null ? cliente.getId() : null,
            clienteNombre,
            clienteApellido,
            profesional.getId(),
            profesional.getNombre(),
            profesional.getApellido(),
            profesional.getProfesion(),
            profesional.getSector() != null ? profesional.getSector().name() : null,
            profesional.getUbicacion(),
            turno.getNombreCliente(),
            cliente != null ? cliente.getFotoPerfil() : null,
            profesional.getFotoPerfil(),
            servicio != null ? servicio.getId() : null,
            servicio != null ? servicio.getNombre() : null,
            servicio != null ? servicio.getPrecio() : null,
            turno.getFecha(),
            turno.getBloqueHorario(),
            turno.getEstado()
        );
    }
}
