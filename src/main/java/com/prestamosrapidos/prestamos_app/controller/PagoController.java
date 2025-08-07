package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.service.PagoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Validated
public class PagoController {

    private final PagoService pagoService;

    /**
     * Endpoint para listar todos los pagos con paginación.
     *
     * @param page Número de página (comienza desde 0).
     * @param size Tamaño de la página (número de elementos por página).
     * @return Página de pagos en formato JSON.
     */
    @GetMapping({"", "/paginados"})
    public ResponseEntity<Page<PagoModel>> listarTodosLosPagosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        Page<PagoModel> pagos = pagoService.obtenerTodosLosPagosPaginados(pageable);

        return ResponseEntity.ok(pagos);
    }

    @PostMapping("/{prestamoId}")
    public ResponseEntity<PagoModel> registrarPago(
            @PathVariable Long prestamoId,
            @RequestBody @Valid PagoModel pagoModel) {

        // Aseguramos que el modelo reciba el id del préstamo desde la URL
        pagoModel.setPrestamoId(prestamoId);

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
