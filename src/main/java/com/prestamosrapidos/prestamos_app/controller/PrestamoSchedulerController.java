package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.scheduler.PrestamoScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
@Slf4j
public class PrestamoSchedulerController {

    private final PrestamoScheduler prestamoScheduler;
    private final PrestamoRepository prestamoRepository;

    @PostMapping("/calcular-mora")
    public ResponseEntity<Map<String, Object>> calcularMoraManual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Map<String, Object> response = new HashMap<>();
        LocalDate fechaCalculo = fecha != null ? fecha : LocalDate.now();
        
        try {
            log.info("=== SOLICITUD MANUAL DE CÁLCULO DE MORA ===");
            log.info("Fecha de cálculo: {}", fechaCalculo);
            
            // Ejecutar el cálculo de mora
            prestamoScheduler.calcularInteresMoratorioDiario();
            
            // Obtener estadísticas de préstamos actualizados
            List<Prestamo> prestamosVencidos = prestamoRepository.findByEstadoInAndFechaVencimientoBefore(
                List.of(EstadoPrestamo.VENCIDO.name(), EstadoPrestamo.EN_MORA.name(), EstadoPrestamo.APROBADO.name()),
                fechaCalculo
            );
            
            // Contar préstamos por estado
            long totalPrestamos = prestamosVencidos.size();
            long enMora = prestamosVencidos.stream()
                .filter(p -> p.getEstado() == EstadoPrestamo.EN_MORA)
                .count();
            long vencidos = prestamosVencidos.stream()
                .filter(p -> p.getEstado() == EstadoPrestamo.VENCIDO)
                .count();
            
            // Construir respuesta detallada
            response.put("status", "success");
            response.put("fechaCalculo", fechaCalculo.toString());
            response.put("totalPrestamosProcesados", totalPrestamos);
            response.put("prestamosEnMora", enMora);
            response.put("prestamosVencidos", vencidos);
            response.put("mensaje", "Cálculo de mora ejecutado exitosamente");
            
            log.info("=== CÁLCULO DE MORA COMPLETADO ===");
            log.info("Total préstamos procesados: {}", totalPrestamos);
            log.info("Préstamos en mora: {}", enMora);
            log.info("Préstamos vencidos: {}", vencidos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String errorMessage = "Error al ejecutar cálculo de mora: " + e.getMessage();
            log.error(errorMessage, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("fechaCalculo", LocalDate.now().toString());
            errorResponse.put("mensaje", errorMessage);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
