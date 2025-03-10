package com.prestamosrapidos.prestamos_app.repository;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByClienteId(Long clienteId);

    List<Prestamo> findByEstado(String estado);

    @Query("SELECT p FROM Prestamo p LEFT JOIN FETCH p.pagos WHERE p.id = :id")
    Optional<Prestamo> findByIdWithPagos(@Param("id") Long id);

    /*List<Prestamo> findByFechaVencimientoBefore(LocalDate fecha); // Buscar préstamos próximos a vencer*/

    /*List<Prestamo> findByFechaVencimientoLessThanEqualAndNotificadoFalse(LocalDate fecha);*/
    List<Prestamo> findByFechaVencimientoBeforeAndInteresMoratorioAplicadoFalse(LocalDate fecha);

    @Query("SELECT p FROM Prestamo p WHERE p.fechaVencimiento < :hoy AND p.estado NOT IN " +
            "(com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo.PAGADO)")
    List<Prestamo> findPrestamosVencidosNoPagados(LocalDate hoy);
}
