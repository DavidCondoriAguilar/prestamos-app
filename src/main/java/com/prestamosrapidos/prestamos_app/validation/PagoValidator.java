package com.prestamosrapidos.prestamos_app.validation;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class PagoValidator {

    /**
     * Valida el ID del préstamo.
     *
     * @param prestamoId ID del préstamo
     */
    public static void validarPrestamoId(Long prestamoId) {
        if (prestamoId == null) {
            throw new IllegalArgumentException("El ID del préstamo no puede ser nulo.");
        }
    }

    /**
     * Valida el monto del pago.
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
     * Valida que el monto del pago no exceda el monto restante del préstamo.
     *
     * @param montoPago       Monto del pago
     * @param montoRestante   Monto restante del préstamo
     */
    public static void validarMontoContraRestante(BigDecimal montoPago, BigDecimal montoRestante) {
        if (montoPago.compareTo(montoRestante) > 0) {
            log.error("El monto del pago excede el monto restante. Monto Pago={}, Monto Restante={}",
                    montoPago, montoRestante);
            throw new IllegalArgumentException("El pago no puede exceder el monto restante del préstamo.");
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
     * Realiza todas las validaciones necesarias para registrar un pago.
     *
     * @param pagoModel      Modelo del pago
     * @param prestamo       Préstamo asociado
     * @param montoRestante  Monto restante del préstamo
     */
    public static void validarPago(PagoModel pagoModel, Prestamo prestamo, BigDecimal montoRestante) {
        validarPrestamoId(pagoModel.getPrestamoId());
        validarMontoPago(pagoModel.getMontoPago());
        validarExistenciaPrestamo(prestamo);
        validarMontoContraRestante(pagoModel.getMontoPago(), montoRestante);
    }
}
