package com.prestamosrapidos.prestamos_app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaModel {

    private Long id;
    private String numeroCuenta;
    private Double saldo;
    private Long clienteId;
}
