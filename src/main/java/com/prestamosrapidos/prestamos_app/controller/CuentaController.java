package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.ErrorResponse;
import com.prestamosrapidos.prestamos_app.model.CuentaModel;
import com.prestamosrapidos.prestamos_app.service.CuentaService;
import com.prestamosrapidos.prestamos_app.exception.CuentaNotFoundException;
import com.prestamosrapidos.prestamos_app.exception.ClienteNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", 
             allowedHeaders = "*",
             allowCredentials = "true")
public class CuentaController {

    private final CuentaService cuentaService;

    /**
     * Crear una nueva cuenta
     *
     * @param cuentaModel los datos de la cuenta a crear
     * @return la cuenta creada
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearCuenta(@RequestBody @Valid CuentaModel cuentaModel,
                                      UriComponentsBuilder uriComponentsBuilder) {
        try {
            // Crear la nueva cuenta
            CuentaModel nuevaCuenta = cuentaService.crearCuenta(cuentaModel);

            // Construir la URI del recurso creado
            URI location = uriComponentsBuilder
                    .path("/cuentas/{id}")
                    .buildAndExpand(nuevaCuenta.getId())
                    .toUri();

            // Devolver respuesta con estado 201 Created
            return ResponseEntity.created(location).body(nuevaCuenta);
        } catch (CuentaNotFoundException ex) {
            // Manejo de excepción específica de cuenta
            return buildErrorResponse("Cuenta no encontrada: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (ClienteNotFoundException ex) {
            // Manejo de excepción específica de cliente
            return buildErrorResponse("Cliente no encontrado: " + ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException ex) {
            // Manejo de errores de argumentos no válidos
            return buildErrorResponse("Argumento inválido: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            // Manejo de errores inesperados
            return buildErrorResponse("Error interno: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, HttpStatus status) {
        log.error("Error en CuentaController: {}", message);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Obtener una cuenta por el ID del cliente
     *
     * @param clienteId el ID del cliente
     * @return la cuenta asociada al clientestatus
     */
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> obtenerCuentaPorCliente(@PathVariable Long clienteId) {
        try {
            CuentaModel cuenta = cuentaService.obtenerCuentaPorClienteId(clienteId);
            return new ResponseEntity<>(cuenta, HttpStatus.OK);
        } catch (CuentaNotFoundException ex) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Eliminar una cuenta por su ID
     *
     * @param id el ID de la cuenta
     * @return una respuesta indicando el éxito o fracaso
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarCuenta(@PathVariable Long id) {
        try {
            cuentaService.eliminarCuenta(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (CuentaNotFoundException ex) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
