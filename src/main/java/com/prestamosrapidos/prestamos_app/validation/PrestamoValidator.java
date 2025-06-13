package com.prestamosrapidos.prestamos_app.validation;

import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PrestamoValidator {

    private static final BigDecimal MAX_INTERES = BigDecimal.valueOf(100);

    /**
     * Valida el modelo del préstamo asegurando que todos los campos requeridos cumplan con las reglas.
     *
     * @param prestamoModel El modelo del préstamo a validar.
     */
    public static void validarPrestamoModel(PrestamoModel prestamoModel) {
        if (prestamoModel == null) {
            throw new IllegalArgumentException("El modelo del préstamo no puede ser nulo.");
        }

        validarMonto(prestamoModel.getMonto());
        validarInteres(prestamoModel.getInteres());
        validarEstado(prestamoModel.getEstado());
        validarClienteId(prestamoModel.getClienteId());
        validarFechas(prestamoModel.getFechaCreacion(), prestamoModel.getFechaVencimiento());
        validarDeudaRestante(prestamoModel);
        validarCalculos(prestamoModel);
    }

    private static void validarDeudaRestante(PrestamoModel prestamoModel) {
        double deudaRestante = prestamoModel.getDeudaRestante();
        if (deudaRestante < 0) {
            throw new IllegalArgumentException("La deuda restante no puede ser negativa.");
        }
        if ("PAGADO".equals(prestamoModel.getEstado()) && 
            deudaRestante != 0) {
            throw new IllegalArgumentException("Un préstamo PAGADO debe tener deuda restante = 0");
        }
    }

    private static void validarCalculos(PrestamoModel prestamoModel) {
        if (prestamoModel.getMonto() != null && prestamoModel.getInteres() != null && 
            prestamoModel.getPagos() != null) {
            
            BigDecimal totalPagos = BigDecimal.ZERO;
            for (PagoModel pago : prestamoModel.getPagos()) {
                if (pago.getMontoPago() != null) {
                    totalPagos = totalPagos.add(pago.getMontoPago());
                }
            }

            BigDecimal montoConInteres = prestamoModel.getMonto().add(prestamoModel.getInteres());
            
            // Convertimos el double a BigDecimal para la comparación
            BigDecimal deudaRestante = BigDecimal.valueOf(prestamoModel.getDeudaRestante());
            BigDecimal deudaCalculada = montoConInteres.subtract(totalPagos);
            
            // Comparamos usando BigDecimal
            if (deudaRestante.compareTo(deudaCalculada) != 0) {
                throw new IllegalArgumentException("La deuda restante no coincide con el cálculo: " +
                    "(monto + interes) - total pagos = " + deudaCalculada);
            }
        }
    }

    private static void validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
    }

    private static void validarInteres(BigDecimal interes) {
        if (interes == null || interes.compareTo(BigDecimal.ZERO) < 0 || interes.compareTo(MAX_INTERES) > 0) {
            throw new IllegalArgumentException("El interés debe estar entre 0 y 100.");
        }
    }

    private static void validarEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("El estado no puede ser nulo o vacío.");
        }
        try {
            EstadoPrestamo.fromString(estado);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El estado proporcionado no es válido. Valores permitidos: " +
                    EstadoPrestamo.values());
        }
    }

    private static void validarClienteId(Long clienteId) {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El cliente ID debe ser un valor positivo.");
        }
    }

    private static void validarFechas(LocalDate fechaCreacion, LocalDate fechaVencimiento) {
        if (fechaVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria.");
        }

        if (fechaCreacion != null && fechaVencimiento.isBefore(fechaCreacion)) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de creación.");
        }

        if (fechaCreacion != null && fechaCreacion.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de creación no puede ser una fecha futura.");
        }
    }

}
