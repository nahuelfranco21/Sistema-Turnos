package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateFotoPerfilDTO(
        @NotBlank String fotoPerfil
) {}