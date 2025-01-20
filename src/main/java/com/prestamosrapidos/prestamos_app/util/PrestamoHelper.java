package com.prestamosrapidos.prestamos_app.util;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PrestamoHelper {

    /**
     * Calcula el monto restante de un préstamo basado en el total del préstamo y los pagos realizados.
     *
     * @param prestamo El préstamo para el cual se calcula el monto restante.
     * @return El monto restante del préstamo.
     */
    public Double calcularMontoRestante(Prestamo prestamo) {
        if (prestamo == null) {
            throw new IllegalArgumentException("El préstamo no puede ser nulo");
        }

        BigDecimal montoTotal = prestamo.getMonto();
        List<Pago> pagos = prestamo.getPagos();
        BigDecimal montoPagado = BigDecimal.ZERO;

        if (pagos != null && !pagos.isEmpty()) {
            montoPagado = pagos.stream()
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal montoRestante = montoTotal.subtract(montoPagado);
        return montoRestante.doubleValue();
    }
}
