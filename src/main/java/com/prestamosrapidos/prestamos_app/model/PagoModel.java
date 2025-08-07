package com.prestamosrapidos.prestamos_app.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoModel {

    private Long id;
    @JsonAlias({"monto"})
    @NotNull(message = "El monto de pago no puede ser vacio")
    @Positive(message = "El monto del pago debe ser mayor a cero")
    private BigDecimal montoPago;
    
    @JsonAlias({"fechaPago"})
    private LocalDate fecha;
    private Long prestamoId;
}
