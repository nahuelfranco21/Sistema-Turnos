package ar.uba.fi.ingsoft1.product_example.user;

import java.time.LocalDate;

public record UserProfileDTO(
    Long id,
    String nombre,
    String apellido,
    String email,
    String role,
    LocalDate fechaNacimiento,
    String profesion,
    String sector,
    String ubicacion,
    String fotoPerfil,
    String descripcion,
    boolean verified
) {
    public static UserProfileDTO fromUser(User user) {
        return new UserProfileDTO(
            user.getId(),
            user.getNombre(),
            user.getApellido(),
            user.getUsername(),
            user.getRole().name(),
            user.getFechaNacimiento(),
            user.getProfesion(),
            user.getSector() != null ? user.getSector().name() : null,
            user.getUbicacion(),
            user.getFotoPerfil(),
            user.getDescripcion(),
            user.isVerified()
        );
    }
}
