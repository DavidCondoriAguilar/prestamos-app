package com.prestamosrapidos.prestamos_app.exception;

public class CuentaNotFoundException extends RuntimeException {


    public CuentaNotFoundException(String message) {
        super(message);
    }
}
