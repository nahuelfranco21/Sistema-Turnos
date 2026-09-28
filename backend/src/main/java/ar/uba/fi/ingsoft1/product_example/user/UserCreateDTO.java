package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.function.Function;

public record UserCreateDTO(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String nombre,
        @NotBlank String apellido,
        @Past @NotNull LocalDate fechaNacimiento,
        boolean esProfesional,
        String profesion,
        SectorTrabajo sector,
        String ubicacion
) implements UserCredentials {
    @Override
    public String username() {
        return email;
    }

    public User asUser(Function<String, String> encryptPassword) {
        UserRole role = esProfesional ? UserRole.PROFESIONAL : UserRole.CLIENTE;

        String prof = esProfesional ? profesion : null;
        SectorTrabajo sec = esProfesional ? sector : null;
        User user = new User(email, encryptPassword.apply(password), role, nombre, apellido, fechaNacimiento, prof, sec);
        if (esProfesional && ubicacion != null) {
            user.setUbicacion(ubicacion);
        }
        return user;
    }
}
