package com.prestamosrapidos.prestamos_app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String rol;
    private String nombre;
    private String apellidos;
    
    // Constructor que mantiene compatibilidad con código existente
    public AuthResponse(String token) {
        this.token = token;
    }
}
