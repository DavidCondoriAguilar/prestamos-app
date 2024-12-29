package com.prestamosrapidos.prestamos_app.repository;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByClienteId(Long clienteId); // Obtener todos los préstamos de un cliente
    List<Prestamo> findByEstado(String estado); // Filtrar préstamos por estado (APROBADO, PENDIENTE, RECHAZADO)
}
