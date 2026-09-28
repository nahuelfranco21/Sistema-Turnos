package ar.uba.fi.ingsoft1.product_example.agenda;

import ar.uba.fi.ingsoft1.product_example.servicio.ServicioPublicoDTO;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoPublicoDTO;

import java.util.List;

public record AgendaPublicaDetailDTO(
    AgendaDTO agenda,
    List<TurnoPublicoDTO> turnos,
    List<ServicioPublicoDTO> servicios
) {}
