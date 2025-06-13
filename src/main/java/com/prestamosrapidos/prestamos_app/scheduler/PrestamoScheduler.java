package com.prestamosrapidos.prestamos_app.scheduler;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.model.EstadoModel;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.serviceImpl.PrestamoServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PrestamoScheduler {

    private final PrestamoRepository prestamoRepository;
    private final PrestamoServiceImpl prestamoService;

    /**
     * Tarea programada que se ejecuta:
     * - Cada hora durante el día laboral (9 AM a 5 PM)
     * - Una vez al día por la noche (23:59:59)
     */
    @Scheduled(cron = "0 0 9-17 * * ?")
    @Scheduled(cron = "59 59 23 * * ?")
    @Transactional
    public void actualizarEstadosPrestamosVencidos() {
        log.info("Iniciando verificación de préstamos vencidos");
        
        try {
            // Buscar préstamos que están en estado PENDIENTE y cuya fecha de vencimiento sea hoy o anterior
            List<Prestamo> prestamosVencidos = prestamoRepository.findByEstadoInAndFechaVencimientoBefore(
                List.of("PENDIENTE"),
                LocalDate.now()
            );

            if (prestamosVencidos.isEmpty()) {
                log.debug("No se encontraron préstamos vencidos para actualizar");
                return;
            }

            for (Prestamo prestamo : prestamosVencidos) {
                try {
                    // Actualizar el estado del préstamo a VENCIDO
                    log.info("Actualizando préstamo {} a estado VENCIDO", prestamo.getId());
                    prestamoService.actualizarEstado(prestamo.getId(), new EstadoModel("VENCIDO"));

                    // Calcular y aplicar interés moratorio
                    if (!prestamo.getInteresMoratorioAplicado()) {
                        aplicarInteresMoratorio(prestamo);
                    }
                } catch (Exception e) {
                    log.error("Error procesando préstamo {}: {}", prestamo.getId(), e.getMessage());
                }
            }

            log.info("Proceso de actualización de préstamos vencidos completado");
        } catch (Exception e) {
            log.error("Error general en el scheduler: {}", e.getMessage());
        }
    }

    private void aplicarInteresMoratorio(Prestamo prestamo) {
        try {
            // Calcular el interés moratorio
            BigDecimal interesMoratorio = prestamo.getDeudaRestante()
                    .multiply(prestamo.getInteresMoratorio())
                    .divide(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            // Actualizar el préstamo con el interés moratorio
            prestamo.setSaldoMoratorio(interesMoratorio);
            prestamo.setInteresMoratorioAplicado(true);
            prestamoRepository.save(prestamo);

            log.info("Aplicado interés moratorio de {} al préstamo {} con deuda restante {}",
                interesMoratorio, prestamo.getId(), prestamo.getDeudaRestante());

            // Actualizar la deuda restante incluyendo el interés moratorio
            prestamo.setDeudaRestante(prestamo.getDeudaRestante().add(interesMoratorio));
            prestamo.setSaldoMoratorio(interesMoratorio);
            prestamo.setInteresMoratorioAplicado(true);
            prestamoRepository.save(prestamo);
        } catch (Exception e) {
            log.error("Error aplicando interés moratorio al préstamo {}: {}", prestamo.getId(), e.getMessage());
        }
    }
}


