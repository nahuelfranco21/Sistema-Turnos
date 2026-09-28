package ar.uba.fi.ingsoft1.product_example.servicio;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "servicios")
@NoArgsConstructor
@Getter
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "agenda_id", nullable = false)
    private Agenda agenda;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int duracionMinutos;

    @Column(nullable = false)
    private double precio;

    @Column(nullable = false)
    private boolean activo = true;

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Servicio(Agenda agenda, String nombre, int duracionMinutos, double precio) {
        this.agenda = agenda;
        this.nombre = nombre;
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
        this.activo = true;
    }
}
