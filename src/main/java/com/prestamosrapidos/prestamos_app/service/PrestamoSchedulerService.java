package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PrestamoSchedulerService {
    Map<String, Object> calcularMoraManual(LocalDate fechaCalculo);
    void calcularInteresMoratorioDiario();
    List<Prestamo> obtenerPrestamosVencidos(LocalDate fecha, List<EstadoPrestamo> estados);
}
