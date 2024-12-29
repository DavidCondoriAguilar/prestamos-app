package com.prestamosrapidos.prestamos_app.exception;

public class PrestamoNotFoundException extends RuntimeException {
    public PrestamoNotFoundException(String message) {
        super(message);
    }
}
