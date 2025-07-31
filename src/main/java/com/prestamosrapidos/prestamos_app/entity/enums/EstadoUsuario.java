package com.prestamosrapidos.prestamos_app.entity.enums;

import lombok.Getter;

@Getter
public enum EstadoUsuario {
    PENDIENTE_VERIFICACION(
        "PENDIENTE_VERIFICACION",
        "El usuario ha sido registrado pero no ha verificado su correo electrónico",
        false
    ),
    ACTIVO(
        "ACTIVO",
        "El usuario está activo y puede acceder al sistema",
        true
    ),
    SUSPENDIDO(
        "SUSPENDIDO",
        "El usuario ha sido suspendido temporalmente",
        false
    ),
    BLOQUEADO(
        "BLOQUEADO",
        "El usuario ha sido bloqueado por exceder intentos fallidos de inicio de sesión",
        false
    ),
    ELIMINADO(
        "ELIMINADO",
        "El usuario ha sido eliminado del sistema (borrado lógico)",
        false
    );

    private final String nombre;
    private final String descripcion;
    private final boolean activo;

    EstadoUsuario(String nombre, String descripcion, boolean activo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    public boolean isActivo() {
        return this.activo;
    }
}
