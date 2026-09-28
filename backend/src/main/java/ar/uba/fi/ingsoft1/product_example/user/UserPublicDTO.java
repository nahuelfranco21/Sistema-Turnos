package ar.uba.fi.ingsoft1.product_example.user;

public record UserPublicDTO(Long id, String nombre, String apellido, String email, String profesion, String sector, String ubicacion, String descripcion, String fotoPerfil) {
    public static UserPublicDTO fromUser(User user) {
        return new UserPublicDTO(
                user.getId(),
                user.getNombre(),
                user.getApellido(),
                user.getUsername(),
                user.getProfesion(),
                user.getSector() != null ? user.getSector().name() : null,
                user.getUbicacion(),
                user.getDescripcion(),
                user.getFotoPerfil()
        );
    }
}
