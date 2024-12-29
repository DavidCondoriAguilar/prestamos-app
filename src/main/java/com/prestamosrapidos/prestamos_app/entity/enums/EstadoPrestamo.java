package com.prestamosrapidos.prestamos_app.entity.enums;

import lombok.Getter;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;

/**
 * Enum que representa los posibles estados de un préstamo.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoPrestamo {
    APROBADO("Aprobado"),
    PENDIENTE("Pendiente"),
    RECHAZADO("Rechazado"),
    PAGADO("Pagado");

    /** Descripción textual del estado */
    private final String descripcion;

    /**
     * Convierte una descripción en texto a su correspondiente enum EstadoPrestamo.
     * @param descripcion Descripción textual del estado
     * @return El enum EstadoPrestamo correspondiente
     * @throws IllegalArgumentException si la descripción no coincide con ningún estado
     */
    public static EstadoPrestamo fromString(String descripcion) {
        return Arrays.stream(values())
                .filter(estado -> estado.descripcion.equalsIgnoreCase(descripcion))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado no válido: " + descripcion));
    }
}
