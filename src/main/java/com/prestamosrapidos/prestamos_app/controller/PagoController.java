package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.ErrorResponse;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.service.PagoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:5173", 
             allowedHeaders = "*",
             allowCredentials = "true")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> listarTodosLosPagosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
            Page<PagoModel> pagos = pagoService.obtenerTodosLosPagosPaginados(pageable);
            return ResponseEntity.ok(pagos);
        } catch (Exception ex) {
            log.error("Error al listar pagos paginados: {}", ex.getMessage(), ex);
            return buildErrorResponse("Error al listar pagos: " + ex.getMessage(), 
                                   HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{prestamoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registrarPago(
            @PathVariable Long prestamoId,
            @RequestBody @Valid PagoModel pagoModel) {
        try {
            // Aseguramos que el modelo reciba el id del préstamo desde la URL
            pagoModel.setPrestamoId(prestamoId);
            PagoModel pagoRegistrado = pagoService.registrarPago(pagoModel);
            return ResponseEntity.status(HttpStatus.CREATED).body(pagoRegistrado);
        } catch (Exception ex) {
            log.error("Error al registrar pago: {}", ex.getMessage(), ex);
            return buildErrorResponse("Error al registrar pago: " + ex.getMessage(), 
                                   HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> obtenerPagoPorId(@PathVariable @Min(1) Long id) {
        try {
            PagoModel pago = pagoService.obtenerPagoPorId(id);
            return ResponseEntity.ok(pago);
        } catch (Exception ex) {
            log.error("Error al obtener pago con id {}: {}", id, ex.getMessage(), ex);
            return buildErrorResponse("Pago no encontrado: " + ex.getMessage(), 
                                   HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/prestamo/{prestamoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> obtenerPagosPorPrestamo(@PathVariable @Min(1) Long prestamoId) {
        try {
            List<PagoModel> pagos = pagoService.obtenerPagosPorPrestamo(prestamoId);
            return ResponseEntity.ok(pagos);
        } catch (Exception ex) {
            log.error("Error al obtener pagos para préstamo {}: {}", prestamoId, ex.getMessage(), ex);
            return buildErrorResponse("Error al obtener pagos: " + ex.getMessage(), 
                                   HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarPago(@PathVariable @Min(1) Long id) {
        try {
            pagoService.eliminarPago(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error al eliminar pago con id {}: {}", id, ex.getMessage(), ex);
            return buildErrorResponse("No se pudo eliminar el pago: " + ex.getMessage(), 
                                   HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/monto-restante/{prestamoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> calcularMontoRestante(@PathVariable Long prestamoId) {
        try {
            BigDecimal montoRestante = pagoService.calcularMontoRestante(prestamoId);
            return ResponseEntity.ok(montoRestante);
        } catch (Exception ex) {
            log.error("Error al calcular monto restante para préstamo {}: {}", prestamoId, ex.getMessage(), ex);
            return buildErrorResponse("Error al calcular monto restante: " + ex.getMessage(), 
                                   HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, HttpStatus status) {
        log.error("Error en PagoController: {}", message);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }
}
