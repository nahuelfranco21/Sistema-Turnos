package ar.uba.fi.ingsoft1.product_example.agenda;

import ar.uba.fi.ingsoft1.product_example.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "agenda")
@NoArgsConstructor
@Getter
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "profesional_id", nullable = false, unique = true)
    private User profesional;

    @Column(nullable = false)
    private Integer bloqueMinutos;

    @Column(nullable = false)
    private Integer mesesAnticipacion;

    @OneToMany(mappedBy = "agenda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RangoHorario> rangos = new ArrayList<>();

    public Agenda(User profesional, Integer bloqueMinutos, Integer mesesAnticipacion) {
        this.profesional = profesional;
        this.bloqueMinutos = bloqueMinutos;
        this.mesesAnticipacion = mesesAnticipacion;
    }

    public void addRango(DiaSemana dia, LocalTime horaInicio, LocalTime horaFin) {
        this.rangos.add(new RangoHorario(this, dia, horaInicio, horaFin));
    }

    public void updateConfig(Integer bloqueMinutos, Integer mesesAnticipacion, List<RangoHorario> newRangos) {
        this.bloqueMinutos = bloqueMinutos;
        this.mesesAnticipacion = mesesAnticipacion;
        this.rangos.clear();
        this.rangos.addAll(newRangos);
    }
}
