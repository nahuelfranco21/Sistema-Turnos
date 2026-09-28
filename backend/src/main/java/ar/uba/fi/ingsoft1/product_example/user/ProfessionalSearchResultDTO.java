package ar.uba.fi.ingsoft1.product_example.user;

import ar.uba.fi.ingsoft1.product_example.servicio.ServicioPublicoDTO;

import java.util.List;

public record ProfessionalSearchResultDTO(
    Long id,
    String nombre,
    String apellido,
    String email,
    String profesion,
    String sector,
    String ubicacion,
    String fotoPerfil,
    List<ServicioPublicoDTO> servicios
) {
    public static ProfessionalSearchResultDTO fromUser(User user, List<ServicioPublicoDTO> servicios) {
        return new ProfessionalSearchResultDTO(
                user.getId(),
                user.getNombre(),
                user.getApellido(),
                user.getUsername(),
                user.getProfesion(),
                user.getSector() != null ? user.getSector().name() : null,
                user.getUbicacion(),
                user.getFotoPerfil(),
                servicios
        );
    }
}
