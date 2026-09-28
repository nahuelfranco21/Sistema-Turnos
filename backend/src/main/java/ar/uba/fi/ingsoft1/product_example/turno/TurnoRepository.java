package ar.uba.fi.ingsoft1.product_example.turno;

import ar.uba.fi.ingsoft1.product_example.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByAgenda_Id(Long agendaId);
    List<Turno> findByAgendaIdAndFecha(Long agendaId, LocalDate fecha);
    boolean existsByAgendaIdAndFechaAndBloqueHorario(Long agendaId, LocalDate fecha, LocalTime bloqueHorario);
    List<Turno> findByCliente_Username(String username);
    List<Turno> findByAgenda_IdAndClienteIsNotNullAndFechaGreaterThanEqualAndEstadoNot(
            Long agendaId, LocalDate fecha, TurnoEstado estado);
    List<Turno> findByCliente_Id(Long clienteId);
    List<Turno> findByEstadoAndClienteIsNotNullAndCreatedAtBefore(
            TurnoEstado estado, LocalDateTime threshold);
    List<Turno> findByEstadoAndClienteIsNotNullAndReminderSentFalseAndCreatedAtBefore(
            TurnoEstado estado, LocalDateTime threshold);

    List<Turno> findByEstadoAndClienteIsNotNullAndFechaAndRecordatorio24hEnviadoFalse(
            TurnoEstado estado, LocalDate fecha);

    boolean existsByCliente_IdAndEstadoInAndFechaGreaterThanEqual(
            Long clienteId, Collection<TurnoEstado> estados, LocalDate fecha);

    boolean existsByAgenda_Profesional_IdAndEstadoInAndFechaGreaterThanEqual(
            Long profesionalId, Collection<TurnoEstado> estados, LocalDate fecha);

    @Query("SELECT DISTINCT t.agenda.profesional FROM turno t WHERE t.cliente.username = :username")
    List<User> findDistinctProfesionalesByClienteUsername(@Param("username") String username);
}