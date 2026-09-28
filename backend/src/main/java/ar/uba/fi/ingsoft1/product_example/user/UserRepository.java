package ar.uba.fi.ingsoft1.product_example.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query(nativeQuery = true, value =
        "SELECT u.* FROM users u WHERE u.role = :role AND u.email <> :excludeUsername AND (" +
            "TRANSLATE(LOWER(u.nombre), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :q || '%'), 'áéíóúñü', 'aeiounu') OR " +
            "TRANSLATE(LOWER(u.apellido), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :q || '%'), 'áéíóúñü', 'aeiounu') OR " +
            "TRANSLATE(LOWER(u.email), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :q || '%'), 'áéíóúñü', 'aeiounu')) " +
            "AND (:servicio = '' OR u.id IN (" +
            "  SELECT a.profesional_id FROM agenda a, servicios s WHERE s.agenda_id = a.id AND TRANSLATE(LOWER(s.nombre), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :servicio || '%'), 'áéíóúñü', 'aeiounu')" +
            "))"
    )
    List<User> searchByRoleAndQuery(
        @Param("role") String role,
        @Param("q") String q,
        @Param("excludeUsername") String excludeUsername,
        @Param("servicio") String servicio
    );

    @Query(nativeQuery = true, value =
        "SELECT u.* FROM users u WHERE u.role = :role AND u.email <> :excludeUsername AND (" +
            "TRANSLATE(LOWER(u.nombre), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :q || '%'), 'áéíóúñü', 'aeiounu') OR " +
            "TRANSLATE(LOWER(u.apellido), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :q || '%'), 'áéíóúñü', 'aeiounu') OR " +
            "TRANSLATE(LOWER(u.email), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :q || '%'), 'áéíóúñü', 'aeiounu')) " +
            "AND (:sector IS NULL OR u.sector = :sector) " +
            "AND (:profesion = '' OR TRANSLATE(LOWER(u.profesion), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :profesion || '%'), 'áéíóúñü', 'aeiounu')) " +
            "AND (:servicio = '' OR u.id IN (" +
            "  SELECT a.profesional_id FROM agenda a, servicios s WHERE s.agenda_id = a.id AND TRANSLATE(LOWER(s.nombre), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :servicio || '%'), 'áéíóúñü', 'aeiounu')" +
            "))"
    )
    List<User> searchByRoleAndQueryWithFilters(
        @Param("role") String role,
        @Param("q") String q,
        @Param("excludeUsername") String excludeUsername,
        @Param("sector") String sector,
        @Param("profesion") String profesion,
        @Param("servicio") String servicio
    );

    @Query(nativeQuery = true, value =
        "SELECT u.* FROM users u WHERE u.role = :role AND u.email <> :excludeUsername " +
            "AND (:sector IS NULL OR u.sector = :sector) " +
            "AND (:profesion = '' OR TRANSLATE(LOWER(u.profesion), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :profesion || '%'), 'áéíóúñü', 'aeiounu')) " +
            "AND (:servicio = '' OR u.id IN (" +
            "  SELECT a.profesional_id FROM agenda a, servicios s WHERE s.agenda_id = a.id AND TRANSLATE(LOWER(s.nombre), 'áéíóúñü', 'aeiounu') LIKE TRANSLATE(LOWER('%' || :servicio || '%'), 'áéíóúñü', 'aeiounu')" +
            "))"
    )
    List<User> searchByRoleAndFiltersOnly(
        @Param("role") String role,
        @Param("excludeUsername") String excludeUsername,
        @Param("sector") String sector,
        @Param("profesion") String profesion,
        @Param("servicio") String servicio
    );
}
