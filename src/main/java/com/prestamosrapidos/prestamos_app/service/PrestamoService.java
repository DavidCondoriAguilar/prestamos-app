package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PrestamoService {
    PrestamoModel crearPrestamo(PrestamoModel prestamoModel);
    PrestamoModel actualizarPrestamo(Long id, PrestamoModel prestamoModel);
    PrestamoModel obtenerPrestamoPorId(Long id);
    List<PrestamoModel> obtenerTodosLosPrestamos();
    List<PrestamoModel> obtenerPrestamosPorCliente(Long clienteId);
    List<PrestamoModel> obtenerPrestamosPorEstado(String estado);
    void eliminarPrestamo(Long id);
    Double calcularInteresTotal(Long prestamoId);
    Double calcularMontoRestante(Long prestamoId);

    @Transactional
    void verificarYActualizarEstado(Prestamo prestamo);
}
