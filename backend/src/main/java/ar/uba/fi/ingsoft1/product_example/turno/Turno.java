package ar.uba.fi.ingsoft1.product_example.turno;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.servicio.Servicio;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity(name = "turno")
@NoArgsConstructor
@Getter
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "agenda_id", nullable = false)
    private Agenda agenda;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private User cliente;

    @Column(name = "nombre_cliente")
    private String nombreCliente;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime bloqueHorario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnoEstado estado;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Getter
    private LocalDateTime createdAt;

    @Column(name = "reminder_sent", nullable = false)
    @Getter
    private boolean reminderSent = false;

    @Column(name = "recordatorio_24h_enviado", nullable = false, columnDefinition = "boolean default false")
    private boolean recordatorio24hEnviado = false;


    public void setRecordatorio24hEnviado(boolean recordatorio24hEnviado) {
        this.recordatorio24hEnviado = recordatorio24hEnviado;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    public Turno(Agenda agenda, User cliente, LocalDate fecha, LocalTime bloqueHorario, TurnoEstado estado) {
        this.agenda = agenda;
        this.cliente = cliente;
        this.fecha = fecha;
        this.bloqueHorario = bloqueHorario;
        this.estado = estado;
    }

    public Turno(Agenda agenda, User cliente, LocalDate fecha, LocalTime bloqueHorario, TurnoEstado estado, Servicio servicio) {
        this(agenda, cliente, fecha, bloqueHorario, estado);
        this.servicio = servicio;
    }

    public void setEstado(TurnoEstado estado) {
        this.estado = estado;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setBloqueHorario(LocalTime bloqueHorario) {
        this.bloqueHorario = bloqueHorario;
    }

    public void setCliente(User cliente) {
        this.cliente = cliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }
}
