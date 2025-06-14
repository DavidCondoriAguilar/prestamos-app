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

    @Query("SELECT p FROM Prestamo p WHERE p.estado IN :estados AND p.fechaVencimiento <= :fechaVencimiento " +
           "AND (p.estado <> 'PAGADO' AND p.estado <> 'CANCELADO')")
    List<Prestamo> findByEstadoInAndFechaVencimientoBefore(
        @Param("estados") List<String> estados, 
        @Param("fechaVencimiento") LocalDate fechaVencimiento
    );
    
    @Query("SELECT p FROM Prestamo p WHERE p.estado = 'APROBADO' AND p.fechaVencimiento <= :hoy")
    List<Prestamo> findAprobadosVencidos(@Param("hoy") LocalDate hoy);
    
    @Query("SELECT p FROM Prestamo p WHERE p.estado = 'VENCIDO' AND (p.fechaUltimoCalculoMora IS NULL OR p.fechaUltimoCalculoMora < :hoy)")
    List<Prestamo> findVencidosSinMoraActualizada(@Param("hoy") LocalDate hoy);

    /*List<Prestamo> findByFechaVencimientoBefore(LocalDate fecha); // Buscar préstamos próximos a vencer*/

    /*List<Prestamo> findByFechaVencimientoLessThanEqualAndNotificadoFalse(LocalDate fecha);*/
    List<Prestamo> findByFechaVencimientoBeforeAndInteresMoratorioAplicadoFalse(LocalDate fecha);

    @Query("SELECT p FROM Prestamo p WHERE p.fechaVencimiento < :hoy AND p.estado NOT IN " +
            "(com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo.PAGADO)")
    List<Prestamo> findPrestamosVencidosNoPagados(LocalDate hoy);
    
    List<Prestamo> findByEstadoAndFechaVencimientoBefore(String estado, LocalDate fechaVencimiento);
}
