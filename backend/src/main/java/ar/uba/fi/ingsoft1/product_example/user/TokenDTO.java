package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record TokenDTO(
        @NotNull String accessToken,
        String refreshToken,
        @NotNull Set<UserRole> roles,
        boolean verified
) {
}
