package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginDTO(
        @Email @NotBlank String email,
        @NotBlank String password
) implements UserCredentials {
    @Override
    public String username() {
        return email;
    }
}
