package com.prestamosrapidos.prestamos_app.validation;

import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.model.FechasModel;
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
            throw new IllegalArgumentException("El modelo de préstamo no puede ser nulo.");
        }

        validarMonto(prestamoModel.getMonto());
        validarInteres(prestamoModel.getInteres());
        
        // Si el estado está presente, validarlo. Si no, establecerlo como APROBADO por defecto
        if (prestamoModel.getEstado() != null && !prestamoModel.getEstado().trim().isEmpty()) {
            validarEstado(prestamoModel.getEstado());
        } else {
            prestamoModel.setEstado(EstadoPrestamo.APROBADO.toString());
        }
        
        validarClienteId(prestamoModel.getClienteId());
        
        // Inicializar objeto fechas si es nulo
        if (prestamoModel.getFechas() == null) {
            prestamoModel.setFechas(new FechasModel());
        }
        
        // Establecer fecha de creación por defecto si no está presente
        if (prestamoModel.getFechas().getCreacion() == null) {
            prestamoModel.getFechas().setCreacion(LocalDate.now());
        }
        
        // Validar las fechas
        validarFechas(
            prestamoModel.getFechas().getCreacion(),
            prestamoModel.getFechas().getVencimiento()
        );
        
        validarDeudaRestante(prestamoModel);
        validarCalculos(prestamoModel);
    }

    private static void validarDeudaRestante(PrestamoModel prestamoModel) {
        BigDecimal deudaRestante = prestamoModel.getDeudaRestante() != null ? 
            prestamoModel.getDeudaRestante() : BigDecimal.ZERO;
            
        if (deudaRestante.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La deuda restante no puede ser negativa.");
        }
        if ("PAGADO".equals(prestamoModel.getEstado()) && 
            deudaRestante.compareTo(BigDecimal.ZERO) != 0) {
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
            
            // Obtenemos la deuda restante del modelo (puede ser null)
            BigDecimal deudaRestante = prestamoModel.getDeudaRestante() != null ? 
                prestamoModel.getDeudaRestante() : BigDecimal.ZERO;
                
            // Calculamos la deuda esperada
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

    public static void validarFechas(LocalDate fechaCreacion, LocalDate fechaVencimiento) {
        // Si no hay fechas, no hay nada que validar
        if (fechaCreacion == null && fechaVencimiento == null) {
            return;
        }
        
        // Validar que la fecha de creación no sea futura
        if (fechaCreacion != null && fechaCreacion.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de creación no puede ser futura");
        }
        
        // Si solo se proporciona una de las dos fechas, no podemos hacer la comparación
        if (fechaCreacion == null || fechaVencimiento == null) {
            return;
        }
        
        // Validar que la fecha de vencimiento sea estrictamente posterior a la de creación
        if (fechaVencimiento.isBefore(fechaCreacion) || fechaVencimiento.isEqual(fechaCreacion)) {
            throw new IllegalArgumentException(String.format(
                "La fecha de vencimiento (%s) debe ser posterior a la fecha de creación (%s)", 
                fechaVencimiento, fechaCreacion));
        }
    }

}
