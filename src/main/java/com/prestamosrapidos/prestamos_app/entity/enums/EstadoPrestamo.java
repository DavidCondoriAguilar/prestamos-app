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
        // Verificar si la descripción proporcionada es válida
        EstadoPrestamo estado = Arrays.stream(values())
                .filter(estadoValor -> estadoValor.descripcion.equalsIgnoreCase(descripcion))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Estado no válido: '" + descripcion + "'. Los valores válidos son: "
                                + Arrays.toString(getValidEstados())));
        return estado;
    }

    /**
     * Obtiene las descripciones de todos los estados posibles.
     * @return Un array con las descripciones válidas de los estados
     */
    public static String[] getValidEstados() {
        return Arrays.stream(values())
                .map(EstadoPrestamo::getDescripcion)
                .toArray(String[]::new);
    }
}
