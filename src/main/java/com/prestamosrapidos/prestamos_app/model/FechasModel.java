package com.prestamosrapidos.prestamos_app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FechasModel {
    private LocalDate creacion;
    private LocalDate vencimiento;
    private int diasMora;
}
