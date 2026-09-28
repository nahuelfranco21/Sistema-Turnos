package ar.uba.fi.ingsoft1.product_example.agenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AgendaRepository extends JpaRepository<Agenda, Long> {
    Optional<Agenda> findByProfesionalId(Long profesionalId);

    @Query("SELECT a FROM agenda a WHERE a.profesional.username = :username")
    Optional<Agenda> findByProfesionalUsername(@Param("username") String username);
}
