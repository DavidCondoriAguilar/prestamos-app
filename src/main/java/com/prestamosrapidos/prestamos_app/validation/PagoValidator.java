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
     * @param saldoPendiente   saldoPendiente del préstamo
     */

    /*public static void validarMontoContraRestante(BigDecimal montoPago, BigDecimal saldoPendiente) {
        BigDecimal tolerancia = BigDecimal.valueOf(0.01); // Tolerancia para redondeos
        if (montoPago.subtract(saldoPendiente).abs().compareTo(tolerancia) > 0) {
            log.error("El monto del pago ({}) excede el saldo pendiente del préstamo ({}).",
                    montoPago, saldoPendiente);
            throw new IllegalArgumentException("El pago no puede exceder el saldo pendiente del préstamo.");
        }
    }*/

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
     * @param saldoPendiente  saldoPendiente del préstamo
     */
    public static void validarPago(PagoModel pagoModel, Prestamo prestamo, BigDecimal saldoPendiente) {
        validarPrestamoId(pagoModel.getPrestamoId());
        validarMontoPago(pagoModel.getMontoPago());
        validarExistenciaPrestamo(prestamo);
/*
        validarMontoContraRestante(pagoModel.getMontoPago(), saldoPendiente);
*/

        if (prestamo.getEstado() == EstadoPrestamo.PAGADO) {
            log.error("No se pueden registrar pagos para un préstamo ya pagado. Préstamo ID={}", prestamo.getId());
            throw new IllegalArgumentException("No se pueden registrar pagos para un préstamo ya pagado.");
        }

        if (prestamo.getEstado() == EstadoPrestamo.RECHAZADO) {
            log.error("No se pueden registrar pagos para un préstamo rechazado. Préstamo ID={}", prestamo.getId());
            throw new IllegalArgumentException("No se pueden registrar pagos para un préstamo rechazado.");
        }
    }


}
