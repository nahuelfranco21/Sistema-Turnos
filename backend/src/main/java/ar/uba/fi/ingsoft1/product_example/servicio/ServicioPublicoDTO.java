package ar.uba.fi.ingsoft1.product_example.servicio;

public record ServicioPublicoDTO(
    Long id,
    String nombre,
    double precio,
    boolean activo
) {}
