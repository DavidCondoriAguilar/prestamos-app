package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.entity.ErrorResponse;
import com.prestamosrapidos.prestamos_app.model.CuentaModel;
import com.prestamosrapidos.prestamos_app.service.CuentaService;
import com.prestamosrapidos.prestamos_app.exception.CuentaNotFoundException;
import com.prestamosrapidos.prestamos_app.exception.ClienteNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    /**
     * Crear una nueva cuenta
     *
     * @param cuentaModel los datos de la cuenta a crear
     * @return la cuenta creada
     */
    @PostMapping
    public Object crearCuenta(@RequestBody @Valid CuentaModel cuentaModel,
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("Cuenta no encontrada: " + ex.getMessage()));
        } catch (ClienteNotFoundException ex) {
            // Manejo de excepción específica de cliente
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("Cliente no encontrado: " + ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            // Manejo de errores de argumentos no válidos
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("Argumento inválido: " + ex.getMessage()));
        } catch (Exception ex) {
            // Manejo de errores inesperados
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Error interno: " + ex.getMessage()));
        }
    }

    private ErrorResponse buildErrorResponse(String message) {
        return ErrorResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Obtener una cuenta por el ID del cliente
     *
     * @param clienteId el ID del cliente
     * @return la cuenta asociada al clientestatus
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<CuentaModel> obtenerCuentaPorCliente(@PathVariable Long clienteId) {
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
    public ResponseEntity<Void> eliminarCuenta(@PathVariable Long id) {
        try {
            cuentaService.eliminarCuenta(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (CuentaNotFoundException ex) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
