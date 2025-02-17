package com.prestamosrapidos.prestamos_app.exception;

public class PagoNotFoundException extends RuntimeException {
    public PagoNotFoundException(String maximo) {
        super("Monto excede el máximo permitido: " + maximo);
    }

}
