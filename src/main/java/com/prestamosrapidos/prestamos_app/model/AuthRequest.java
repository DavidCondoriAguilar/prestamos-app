package com.prestamosrapidos.prestamos_app.model;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
