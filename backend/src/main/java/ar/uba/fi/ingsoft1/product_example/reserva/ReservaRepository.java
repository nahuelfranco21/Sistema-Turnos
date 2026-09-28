package ar.uba.fi.ingsoft1.product_example.reserva;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReservaRepository extends JpaRepository<Reserva, Long> {
}
