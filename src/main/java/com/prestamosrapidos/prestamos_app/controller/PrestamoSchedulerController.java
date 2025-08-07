package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.service.PrestamoSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
@Slf4j
public class PrestamoSchedulerController {

    private final PrestamoSchedulerService prestamoSchedulerService;

    @PostMapping("/calcular-mora")
    public ResponseEntity<Map<String, Object>> calcularMoraManual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        
        LocalDate fechaCalculo = fecha != null ? fecha : LocalDate.now();
        
        try {
            Map<String, Object> response = prestamoSchedulerService.calcularMoraManual(fechaCalculo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en el controlador al calcular mora: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "fechaCalculo", LocalDate.now().toString(),
                "mensaje", "Error al procesar la solicitud: " + e.getMessage()
            ));
        }
    }
}
