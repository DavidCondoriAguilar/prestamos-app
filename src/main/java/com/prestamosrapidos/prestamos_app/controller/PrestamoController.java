package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.EstadoModel;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prestamos")
@RequiredArgsConstructor
@Validated
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    public ResponseEntity<PrestamoModel> crearPrestamo(@Valid @RequestBody PrestamoModel prestamoModel) {
        return ResponseEntity.ok(prestamoService.crearPrestamo(prestamoModel));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrestamoModel> actualizarPrestamo(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody PrestamoModel prestamoModel) {
        return ResponseEntity.ok(prestamoService.actualizarPrestamo(id, prestamoModel));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PrestamoModel> actualizarEstado(
            @PathVariable Long id,
            @RequestBody EstadoModel estadoRequest) {

        PrestamoModel prestamoActualizado = prestamoService.actualizarEstado(id, estadoRequest);
        return ResponseEntity.ok(prestamoActualizado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestamoModel> obtenerPrestamoPorId(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(prestamoService.obtenerPrestamoPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<PrestamoModel>> obtenerTodosLosPrestamos() {
        return ResponseEntity.ok(prestamoService.obtenerTodosLosPrestamos());
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PrestamoModel>> obtenerPrestamosPorCliente(@PathVariable @Min(1) Long clienteId) {
        return ResponseEntity.ok(prestamoService.obtenerPrestamosPorCliente(clienteId));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PrestamoModel>> obtenerPrestamosPorEstado(@PathVariable @NotBlank String estado) {
        return ResponseEntity.ok(prestamoService.obtenerPrestamosPorEstado(estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPrestamo(@PathVariable @Min(1) Long id) {
        prestamoService.eliminarPrestamo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/interes")
    public ResponseEntity<Double> calcularInteresTotal(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(prestamoService.calcularInteresTotal(id));
    }

    @GetMapping("/{id}/monto-restante")
    public ResponseEntity<Double> calcularMontoRestante(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.calcularMontoRestante(id));
    }
}
