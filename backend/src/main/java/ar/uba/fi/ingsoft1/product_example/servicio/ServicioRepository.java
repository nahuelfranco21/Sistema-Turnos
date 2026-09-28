package ar.uba.fi.ingsoft1.product_example.servicio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    List<Servicio> findByAgenda_Id(Long agendaId);

    @Query(nativeQuery = true, value =
        "SELECT s.* FROM servicios s WHERE s.agenda_id = :agendaId AND TRANSLATE(LOWER(s.nombre), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :nombre || '%'), 'áéíóúñü', 'aeiounu')"
    )
    List<Servicio> findByAgendaIdAndNombreContaining(
        @Param("agendaId") Long agendaId,
        @Param("nombre") String nombre
    );
}
