package com.prestamosrapidos.prestamos_app.model;

import lombok.*;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

public class ClienteModel {
    private Long id;
    private String nombre;
    private String correo;
    private CuentaModel cuenta;
    private List<PrestamoModel> prestamos;
}
