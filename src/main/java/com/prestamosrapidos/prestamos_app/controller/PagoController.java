package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.service.PagoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Validated
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/{prestamoId}")
    public ResponseEntity<PagoModel> registrarPago(@RequestBody @Valid PagoModel pagoModel) {
        PagoModel pagoRegistrado = pagoService.registrarPago(pagoModel);
        return ResponseEntity.status(201).body(pagoRegistrado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoModel> obtenerPagoPorId(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(pagoService.obtenerPagoPorId(id));
    }

    @GetMapping("/prestamo/{prestamoId}")
    public ResponseEntity<List<PagoModel>> obtenerPagosPorPrestamo(@PathVariable @Min(1) Long prestamoId) {
        return ResponseEntity.ok(pagoService.obtenerPagosPorPrestamo(prestamoId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPago(@PathVariable @Min(1) Long id) {
        pagoService.eliminarPago(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/monto-restante/{prestamoId}")
    public ResponseEntity<BigDecimal> calcularMontoRestante(@PathVariable Long prestamoId) {
        BigDecimal montoRestante = pagoService.calcularMontoRestante(prestamoId);
        return ResponseEntity.ok(montoRestante);
    }
}
