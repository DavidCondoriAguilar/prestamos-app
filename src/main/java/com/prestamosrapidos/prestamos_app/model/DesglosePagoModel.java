package com.prestamosrapidos.prestamos_app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesglosePagoModel {
    private BigDecimal capital;
    private BigDecimal interesOrdinario;
    private BigDecimal moraAcumulada;
    private BigDecimal totalDeuda;
}
