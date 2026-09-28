package ar.uba.fi.ingsoft1.product_example.reserva;

import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import ar.uba.fi.ingsoft1.product_example.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "reservas")
@NoArgsConstructor
@Getter
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private User cliente;

    @ManyToOne
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservaEstado estado;

    public Reserva(User cliente, Turno turno) {
        this.cliente = cliente;
        this.turno = turno;
        this.estado = ReservaEstado.PENDIENTE;
    }

    public void confirmar() {
        this.estado = ReservaEstado.CONFIRMADO;
    }

    public void cancelar() {
        this.estado = ReservaEstado.CANCELADO;
    }
}
