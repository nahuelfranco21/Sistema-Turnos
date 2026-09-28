package ar.uba.fi.ingsoft1.product_example.user;

public record UserAdminDTO(
        Long id,
        String nombre,
        String apellido,
        String email,
        String role,
        String profesion,
        String sector,
        String ubicacion,
        String fotoPerfil,
        String descripcion,
        Long agendaId,
        boolean verified,
        boolean active
) {
    public static UserAdminDTO fromUser(User user, Long agendaId) {
        return new UserAdminDTO(
                user.getId(),
                user.getNombre(),
                user.getApellido(),
                user.getUsername(),
                user.getRole().name(),
                user.getProfesion(),
                user.getSector() != null ? user.getSector().name() : null,
                user.getUbicacion(),
                user.getFotoPerfil(),
                user.getDescripcion(),
                agendaId,
                user.isVerified(),
                user.isActive()
        );
    }
}
