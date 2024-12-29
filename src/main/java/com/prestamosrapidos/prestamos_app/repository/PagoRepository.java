package com.prestamosrapidos.prestamos_app.repository;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByPrestamoId(Long prestamoId); // Obtener todos los pagos de un préstamo

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.prestamo.id = :prestamoId")
    Double calcularTotalPagado(@Param("prestamoId") Long prestamoId);
}
