package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.EstadoModel;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/prestamos")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:5173", 
             allowedHeaders = "*",
             allowCredentials = "true")
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PrestamoModel> crearPrestamo(@Valid @RequestBody PrestamoModel prestamoModel) {
        PrestamoModel nuevoPrestamo = prestamoService.crearPrestamo(prestamoModel);
        log.info("Préstamo creado exitosamente con ID: {}", nuevoPrestamo.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPrestamo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PrestamoModel> actualizarPrestamo(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody PrestamoModel prestamoModel) {
        PrestamoModel prestamoActualizado = prestamoService.actualizarPrestamo(id, prestamoModel);
        log.info("Préstamo con ID {} actualizado exitosamente", id);
        return ResponseEntity.ok(prestamoActualizado);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PrestamoModel> actualizarEstado(
            @PathVariable Long id,
            @RequestBody EstadoModel estadoRequest) {
        PrestamoModel prestamoActualizado = prestamoService.actualizarEstado(id, estadoRequest);
        log.info("Estado del préstamo con ID {} actualizado a: {}", id, estadoRequest.getEstado());
        return ResponseEntity.ok(prestamoActualizado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PrestamoModel> obtenerPrestamoPorId(@PathVariable @Min(1) Long id) {
        PrestamoModel prestamo = prestamoService.obtenerPrestamoPorId(id);
        return ResponseEntity.ok(prestamo);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<PrestamoModel>> obtenerTodosLosPrestamos() {
        List<PrestamoModel> prestamos = prestamoService.obtenerTodosLosPrestamos();
        return ResponseEntity.ok(prestamos);
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<PrestamoModel>> obtenerPrestamosPorCliente(@PathVariable @Min(1) Long clienteId) {
        List<PrestamoModel> prestamos = prestamoService.obtenerPrestamosPorCliente(clienteId);
        return ResponseEntity.ok(prestamos);
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<PrestamoModel>> obtenerPrestamosPorEstado(@PathVariable @NotBlank String estado) {
        List<PrestamoModel> prestamos = prestamoService.obtenerPrestamosPorEstado(estado);
        return ResponseEntity.ok(prestamos);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarPrestamo(@PathVariable @Min(1) Long id) {
        prestamoService.eliminarPrestamo(id);
        log.info("Préstamo con ID {} eliminado exitosamente", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/interes")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BigDecimal> calcularInteresTotal(@PathVariable @Min(1) Long id) {
        BigDecimal interes = prestamoService.calcularInteresTotal(id);
        return ResponseEntity.ok(interes);
    }

    @GetMapping("/{id}/monto-restante")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BigDecimal> calcularMontoRestante(@PathVariable Long id) {
        BigDecimal montoRestante = prestamoService.calcularMontoRestante(id);
        return ResponseEntity.ok(montoRestante);
    }
}
