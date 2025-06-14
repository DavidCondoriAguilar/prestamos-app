package com.prestamosrapidos.prestamos_app.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum que representa los posibles estados de un préstamo.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoPrestamo {

    APROBADO("Aprobado"),
    PENDIENTE("Pendiente"),
    RECHAZADO("Rechazado"),
    PAGADO("Pagado"),
    VENCIDO("Vencido"),
    EN_MORA("En mora");

    /** Descripción textual del estado */
    private final String descripcion;

    /**
     * Convierte una descripción en texto a su correspondiente enum EstadoPrestamo.
     *
     * @param descripcion Descripción textual del estado
     * @return El enum EstadoPrestamo correspondiente
     * @throws IllegalArgumentException si la descripción no coincide con ningún estado
     */
    public static EstadoPrestamo fromString(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción no puede ser nula o vacía.");
        }

        String descripcionTrimmed = descripcion.trim();
        return Arrays.stream(values())
                .filter(estado -> estado.descripcion.equalsIgnoreCase(descripcionTrimmed))
                .findFirst()
                .orElseThrow(() -> {
                    // Agrega un mensaje más detallado para facilitar la depuración
                    String validValues = Arrays.stream(values())
                            .map(EstadoPrestamo::getDescripcion)
                            .collect(Collectors.joining(", ", "[", "]"));
                    return new IllegalArgumentException(
                            "Estado no válido: '" + descripcionTrimmed + "'. Los valores válidos son: " + validValues);
                });
    }

    /**
     * Obtiene las descripciones de todos los estados posibles.
     *
     * @return Un array con las descripciones válidas de los estados
     */
    public static String[] getValidEstados() {
        return Arrays.stream(values())
                .map(EstadoPrestamo::getDescripcion)
                .toArray(String[]::new);
    }
}