package ar.uba.fi.ingsoft1.product_example.agenda;

import java.time.DayOfWeek;

public enum DiaSemana {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;

    public static DiaSemana from(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> LUNES;
            case TUESDAY   -> MARTES;
            case WEDNESDAY -> MIERCOLES;
            case THURSDAY  -> JUEVES;
            case FRIDAY    -> VIERNES;
            case SATURDAY  -> SABADO;
            case SUNDAY    -> DOMINGO;
        };
    }
}
