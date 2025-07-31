package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.AuthRequest;
import com.prestamosrapidos.prestamos_app.model.AuthResponse;
import com.prestamosrapidos.prestamos_app.model.RegisterRequest;
import com.prestamosrapidos.prestamos_app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping({"/api/auth", "/auth"})
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"},
             allowCredentials = "true",
             allowedHeaders = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest request) {
        try {
            AuthResponse response = authService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            log.error("Error de autenticación para el usuario: " + request.getUsername(), ex);
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, 
                "Usuario o contraseña incorrectos", 
                ex
            );
        } catch (Exception ex) {
            log.error("Error inesperado durante el login", ex);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error interno del servidor", 
                ex
            );
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.error("Error en el registro: " + ex.getMessage(), ex);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                ex.getMessage(), 
                ex
            );
        } catch (Exception ex) {
            log.error("Error inesperado durante el registro", ex);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error interno del servidor durante el registro", 
                ex
            );
        }
    }
}
