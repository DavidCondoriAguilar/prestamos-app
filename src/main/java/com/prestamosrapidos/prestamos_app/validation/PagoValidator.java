package com.prestamosrapidos.prestamos_app.validation;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
public class PagoValidator {

    /**
     * Valida que el ID del préstamo no sea nulo.
     *
     * @param prestamoId ID del préstamo
     */
    public static void validarPrestamoId(Long prestamoId) {
        if (prestamoId == null) {
            throw new IllegalArgumentException("El ID del préstamo no puede ser nulo.");
        }
    }

    /**
     * Valida que el monto del pago sea mayor a cero.
     *
     * @param montoPago Monto del pago
     */
    public static void validarMontoPago(BigDecimal montoPago) {
        if (montoPago == null || montoPago.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("El monto del pago es inválido: {}", montoPago);
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero.");
        }
    }

    /**
     * Valida que el monto del pago no exceda el saldo pendiente del préstamo.
     *
     * @param montoPago      Monto del pago
     * @param saldoPendiente Saldo pendiente del préstamo
     */
    public static void validarMontoContraRestante(BigDecimal montoPago, BigDecimal saldoPendiente) {
        if (montoPago.compareTo(saldoPendiente) > 0) {
            log.error("El monto del pago ({}) excede el saldo pendiente del préstamo ({}).", montoPago, saldoPendiente);
            throw new IllegalArgumentException("El pago no puede ser mayor que la deuda pendiente.");
        }
    }

    /**
     * Valida la existencia de un préstamo.
     *
     * @param prestamo Préstamo a validar
     */
    public static void validarExistenciaPrestamo(Prestamo prestamo) {
        if (prestamo == null) {
            log.error("El préstamo no fue encontrado.");
            throw new IllegalArgumentException("El préstamo no existe o no fue encontrado.");
        }
    }

    /**
     * Valida todas las reglas necesarias antes de registrar un pago.
     *
     * @param pagoModel      Modelo del pago
     * @param prestamo       Préstamo asociado
     * @param saldoPendiente Saldo pendiente del préstamo
     */
    public static void validarPago(PagoModel pagoModel, Prestamo prestamo, BigDecimal saldoPendiente) {
        validarPrestamoId(pagoModel.getPrestamoId());
        validarMontoPago(pagoModel.getMontoPago());
        validarExistenciaPrestamo(prestamo);
        
        log.info("Validando pago - Monto: {}, Saldo Pendiente: {}", pagoModel.getMontoPago(), saldoPendiente);
        
        // Validación más flexible para el monto del pago
        if (pagoModel.getMontoPago().compareTo(saldoPendiente) > 0) {
            log.warn("Pago mayor a la deuda. Ajustando monto de {} a {}", 
                    pagoModel.getMontoPago(), saldoPendiente);
            pagoModel.setMontoPago(saldoPendiente);
        }

        if (prestamo.getEstado() == EstadoPrestamo.PAGADO) {
            log.error("Intento de pago en préstamo pagado. Préstamo ID={}", prestamo.getId());
            throw new IllegalArgumentException("No se pueden registrar pagos en un préstamo ya pagado.");
        }

        if (prestamo.getEstado() == EstadoPrestamo.RECHAZADO) {
            log.error("Intento de pago en préstamo rechazado. Préstamo ID={}", prestamo.getId());
            throw new IllegalArgumentException("No se pueden registrar pagos en un préstamo rechazado.");
        }
    }
}
