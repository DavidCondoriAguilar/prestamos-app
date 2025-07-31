package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.model.AuthRequest;
import com.prestamosrapidos.prestamos_app.model.AuthResponse;
import com.prestamosrapidos.prestamos_app.model.RegisterRequest;

public interface AuthService {
    AuthResponse authenticate(AuthRequest request);
    AuthResponse register(RegisterRequest request);
}
