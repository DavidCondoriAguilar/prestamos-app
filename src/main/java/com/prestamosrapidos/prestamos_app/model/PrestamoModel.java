package com.prestamosrapidos.prestamos_app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoModel {
    private Long id;
    private BigDecimal monto;
    private BigDecimal interes;
    private LocalDate fechaCreacion;
    private LocalDate fechaVencimiento;
    private String estado;
    private Long clienteId;
    private double deudaRestante;
    private List<PagoModel> pagos;

}
