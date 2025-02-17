/*
package com.prestamosrapidos.prestamos_app.util;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class PrestamoHelper {

    */
/**
     * Calcula el monto restante de un préstamo basado en el total del préstamo y los pagos realizados.
     *
     * @param prestamo El préstamo para el cual se calcula el monto restante.
     * @return El monto restante del préstamo.
     *//*

    @Override
    public Double calcularMontoRestante(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

        // Calcular monto total inicial (monto + interés inicial)
        BigDecimal montoTotal = prestamo.getMonto()
                .add(prestamo.getMonto().multiply(prestamo.getInteres())
                        .divide(BigDecimal.valueOf(100)));

        // Sumar pagos realizados
        BigDecimal montoPagado = prestamo.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Saldo pendiente sin mora
        BigDecimal saldoPendiente = montoTotal.subtract(montoPagado);

        // Aplicar interés moratorio si está vencido y no se ha aplicado antes
        LocalDate hoy = LocalDate.now();
        boolean interesMoratorioAplicado = prestamo.getInteresMoratorioAplicado() != null
                ? prestamo.getInteresMoratorioAplicado()
                : false;

        if (prestamo.getFechaVencimiento() != null
                && hoy.isAfter(prestamo.getFechaVencimiento())
                && !interesMoratorioAplicado) {
            BigDecimal interesMoratorio = saldoPendiente.multiply(prestamo.getInteresMoratorio())
                    .divide(BigDecimal.valueOf(100));
            saldoPendiente = saldoPendiente.add(interesMoratorio);
        }

        // Si el saldo pendiente es menor o igual a cero, el préstamo está pagado
        if (saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
            prestamo.setEstado(EstadoPrestamo.PAGADO);
            prestamo.setSaldoMoratorio(BigDecimal.ZERO); // Resetear mora si se paga completo
            prestamoRepository.save(prestamo);
        }

        return saldoPendiente.doubleValue();
    }
}
*/
