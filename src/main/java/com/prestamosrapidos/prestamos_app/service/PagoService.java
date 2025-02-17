package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;

import java.math.BigDecimal;
import java.util.List;

public interface PagoService {
    PagoModel registrarPago(PagoModel pagoModel);
    List<PagoModel> obtenerPagosPorPrestamo(Long prestamoId);
    PagoModel obtenerPagoPorId(Long id);
    void eliminarPago(Long id);
    BigDecimal calcularMontoRestante(Long prestamoId);

    void verificarYActualizarEstado(Prestamo prestamo);
}
