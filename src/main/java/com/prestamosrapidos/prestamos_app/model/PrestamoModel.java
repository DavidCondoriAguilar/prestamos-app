package com.prestamosrapidos.prestamos_app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoModel {
    private Long id;
    private BigDecimal monto;
    private BigDecimal interes;
    private BigDecimal interesMoratorio;
    private BigDecimal deudaRestante;
    private FechasModel fechas;
    private String estado;
    private Long clienteId;
    private DesglosePagoModel desglosePago;
    private PagoDiarioModel pagoDiario;
    private List<PagoModel> pagos;
}
