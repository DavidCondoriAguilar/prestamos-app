package com.prestamosrapidos.prestamos_app.exception;

public class ValidacionException extends RuntimeException {
  public ValidacionException(String mensaje) {
    super(mensaje);
  }
}