package com.prestamosrapidos.prestamos_app.exception;

public class PagoNotFoundException extends RuntimeException {
    public PagoNotFoundException(String message) {
        super(message);
    }
}
