package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.model.EstadoModel;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface PrestamoService {
    PrestamoModel crearPrestamo(PrestamoModel prestamoModel);
    PrestamoModel actualizarPrestamo(Long id, PrestamoModel prestamoModel);
    PrestamoModel obtenerPrestamoPorId(Long id);
    List<PrestamoModel> obtenerTodosLosPrestamos();
    List<PrestamoModel> obtenerPrestamosPorCliente(Long clienteId);
    List<PrestamoModel> obtenerPrestamosPorEstado(String estado);
    void eliminarPrestamo(Long id);
    BigDecimal calcularInteresTotal(Long prestamoId);
    BigDecimal calcularMontoRestante(Long prestamoId);
/* <<<<<<<<<<<<<<  ✨ Windsurf Command ⭐ >>>>>>>>>>>>>>>> */
    /**
     * Actualiza el estado de un pr &eacute;stamo.
     *
     * @param id          Identificador &uacute;nico del pr &eacute;stamo.
     * @param nuevoEstado Nuevo estado del pr &eacute;stamo.
     * @return El pr &eacute;stamo con el nuevo estado.
     */
/* <<<<<<<<<<  c0130dba-02dd-48ee-a650-fad4bfca2f54  >>>>>>>>>>> */
    PrestamoModel actualizarEstado(Long id, EstadoModel nuevoEstado);
    @Transactional
    void verificarYActualizarEstado(Prestamo prestamo);
}
