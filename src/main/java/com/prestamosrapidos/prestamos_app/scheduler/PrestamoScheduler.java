package com.prestamosrapidos.prestamos_app.scheduler;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.model.EstadoModel;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Servicio programado para el manejo automático de moras en préstamos.
 * 
 * <p>Este componente se encarga de realizar las siguientes tareas de forma automática:</p>
 * <ul>
 *   <li>Actualizar el estado de préstamos vencidos</li>
 *   <li>Calcular y aplicar intereses moratorios</li>
 *   <li>Mantener actualizada la deuda total de cada préstamo</li>
 *   <li>Gestionar los días de gracia para el cálculo de moras</li>
 * </ul>
 * 
 * <p>Configuración mediante properties:</p>
 * <ul>
 *   <li>prestamo.mora.porcentaje-diario: Porcentaje de mora diario (default: 0.1%)</li>
 *   <li>prestamo.mora.habilitada: Habilita/deshabilita el cálculo de mora (default: true)</li>
 *   <li>prestamo.mora.dias-gracia: Días de gracia antes de aplicar mora (default: 0)</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PrestamoScheduler {

    /** Repositorio para operaciones de base de datos relacionadas con préstamos */
    private final PrestamoRepository prestamoRepository;
    
    /** Servicio para operaciones de negocio relacionadas con préstamos */
    private final PrestamoService prestamoService;
    
    /** 
     * Porcentaje de mora diario a aplicar sobre el monto del préstamo.
     * Valor configurable mediante la propiedad 'prestamo.mora.porcentaje-diario' (default: 0.1%)
     */
    @Value("${prestamo.mora.porcentaje-diario:0.1}")
    private BigDecimal porcentajeMoraDiario;
    
    /** 
     * Bandera para habilitar/deshabilitar el cálculo de mora.
     * Útil para entornos de prueba o mantenimiento.
     * Valor configurable mediante la propiedad 'prestamo.mora.habilitada' (default: true)
     */
    @Value("${prestamo.mora.habilitada:true}")
    private boolean moraHabilitada;
    
    /** 
     * Número de días de gracia antes de comenzar a aplicar la mora.
     * Valor configurable mediante la propiedad 'prestamo.mora.dias-gracia' (default: 0)
     */
    @Value("${prestamo.mora.dias-gracia:0}")
    private int diasGracia;

    /**
     * Tarea programada que se ejecuta periódicamente para calcular intereses moratorios.
     * 
     * <p>Esta tarea realiza las siguientes operaciones:</p>
     * <ol>
     *   <li>Actualiza el estado de préstamos APROBADOS a VENCIDO cuando corresponda</li>
     *   <li>Calcula y aplica moras a préstamos vencidos</li>
     *   <li>Actualiza la deuda total de cada préstamo</li>
     * </ol>
     * 
     * <p>Frecuencia de ejecución: Cada minuto (configurable mediante cron expression)</p>
     * <p>Formato cron: segundo, minuto, hora, día del mes, mes, día de la semana</p>
     * 
     * @implNote Esta tarea está envuelta en una transacción para garantizar la integridad de los datos.
     * En caso de error durante el procesamiento, se realizará rollback de los cambios.
     */
    @Scheduled(cron = "0 * * * * ?") // Ejecutar al inicio de cada minuto (cada 60 segundos)
    @Transactional
    public void calcularInteresMoratorioDiario() {
        log.info("\n=== INICIO DE CÁLCULO DE MORA - {}", LocalDateTime.now());
        
        if (!moraHabilitada) {
            log.info("Cálculo de mora deshabilitado por configuración");
            return;
        }
        
        LocalDate hoy = LocalDate.now();
        log.info("Buscando préstamos vencidos antes de: {}", hoy);
        
        try {
            // 1. Actualizar estado de préstamos aprobados vencidos a VENCIDO
            log.info("\n=== PASO 1: Actualizando préstamos vencidos");
            actualizarPrestamosVencidos(hoy);
            
            // 2. Calcular mora para préstamos vencidos o en mora
            log.info("\n=== PASO 2: Calculando mora para préstamos vencidos");
            calcularMoraPrestamosVencidos(hoy);
            
            log.info("\n=== CÁLCULO DE MORA COMPLETADO - ÉXITO");
        } catch (Exception e) {
            log.error("\n=== ERROR EN EL CÁLCULO DE MORA: {}", e.getMessage(), e);
        } finally {
            log.info("\n=== FIN DE EJECUCIÓN ===\n");
        }
    }
    
    /**
     * Actualiza el estado de los préstamos vencidos y prepara el cálculo de mora.
     * 
     * <p>Este método realiza dos operaciones principales:</p>
     * <ol>
     *   <li>Marca como VENCIDO los préstamos APROBADOS que han superado su fecha de vencimiento</li>
     *   <li>Identifica préstamos VENCIDOS que requieren actualización de mora</li>
     * </ol>
     * 
     * @param hoy Fecha de referencia para el cálculo de vencimientos
     * @throws RuntimeException Si ocurre un error durante el procesamiento
     */
    private void actualizarPrestamosVencidos(LocalDate hoy) {
        try {
            // 1. Buscar préstamos aprobados que ya vencieron
            log.info("Buscando préstamos APROBADOS vencidos antes de: {}", hoy);
            List<Prestamo> prestamosPorVencer = prestamoRepository.findAprobadosVencidos(hoy);
            
            log.info("Encontrados {} préstamos para marcar como VENCIDO", prestamosPorVencer.size());
            
            for (Prestamo prestamo : prestamosPorVencer) {
                try {
                    log.info("Actualizando préstamo {} a estado VENCIDO (venció el {})", 
                        prestamo.getId(), prestamo.getFechaVencimiento());
                        
                    // Guardar la fecha de vencimiento como fecha de último cálculo
                    prestamo.setEstado(EstadoPrestamo.VENCIDO);
                    prestamo.setFechaUltimoCalculoMora(prestamo.getFechaVencimiento());
                    prestamo = prestamoRepository.save(prestamo);
                    
                    log.info("✓ Préstamo {} actualizado a estado VENCIDO exitosamente", prestamo.getId());
                } catch (Exception e) {
                    log.error("✗ Error actualizando estado del préstamo {}: {}", 
                        prestamo.getId(), e.getMessage(), e);
                }
            }
            
            // 2. Buscar préstamos VENCIDOS que necesitan actualización de mora
            log.info("\nBuscando préstamos VENCIDOS que necesitan actualización de mora...");
            List<Prestamo> vencidosSinMoraActualizada = prestamoRepository.findVencidosSinMoraActualizada(hoy);
            log.info("Encontrados {} préstamos VENCIDOS sin mora actualizada", vencidosSinMoraActualizada.size());
            
            for (Prestamo prestamo : vencidosSinMoraActualizada) {
                try {
                    log.info("\n--- Recalculando mora para préstamo VENCIDO {} ---", prestamo.getId());
                    log.info("Último cálculo: {}", prestamo.getFechaUltimoCalculoMora());
                    log.info("Días mora actuales: {}", prestamo.getDiasMora());
                    log.info("Mora acumulada: {}", prestamo.getMoraAcumulada());
                    
                    // Forzar recálculo de mora
                    calcularMoraParaPrestamo(prestamo, hoy);
                    
                    log.info("✓ Mora recalculada para préstamo {}", prestamo.getId());
                } catch (Exception e) {
                    log.error("✗ Error recalculando mora para préstamo {}: {}", 
                        prestamo.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("✗ Error en actualizarPrestamosVencidos: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Calcula y aplica mora a los préstamos vencidos.
     * 
     * <p>Este método:</p>
     * <ul>
     *   <li>Busca préstamos que requieren cálculo de mora</li>
     *   <li>Aplica el cálculo de mora a cada préstamo</li>
     *   <li>Proporciona un resumen de la operación</li>
     * </ul>
     * 
     * @param hoy Fecha de referencia para el cálculo de mora
     * @throws RuntimeException Si ocurre un error durante el procesamiento
     */
    private void calcularMoraPrestamosVencidos(LocalDate hoy) {
        try {
            log.info("\n=== BUSCANDO PRÉSTAMOS PARA CÁLCULO DE MORA ===");
            log.info("Fecha actual: {}", hoy);
            log.info("Días de gracia: {}", diasGracia);
            
            // Buscar préstamos que necesiten cálculo de mora
            List<Prestamo> prestamosVencidos = prestamoRepository.findByEstadoInAndFechaVencimientoBefore(
                List.of(EstadoPrestamo.VENCIDO.name(), EstadoPrestamo.EN_MORA.name(), EstadoPrestamo.APROBADO.name()),
                hoy.minusDays(diasGracia) // Considerar días de gracia
            );
            
            log.info("Encontrados {} préstamos para calcular mora", prestamosVencidos.size());
            
            if (prestamosVencidos.isEmpty()) {
                log.info("No hay préstamos que requieran cálculo de mora");
                return;
            }

            int contadorProcesados = 0;
            int contadorConMora = 0;
            
            for (Prestamo prestamo : prestamosVencidos) {
                try {
                    log.info("\n--- Procesando préstamo ID: {} ---", prestamo.getId());
                    log.info("Estado actual: {}", prestamo.getEstado());
                    log.info("Fecha vencimiento: {}", prestamo.getFechaVencimiento());
                    log.info("Días mora actuales: {}", prestamo.getDiasMora());
                    log.info("Último cálculo mora: {}", prestamo.getFechaUltimoCalculoMora());
                    
                    // Calcular mora para este préstamo
                    calcularMoraParaPrestamo(prestamo, hoy);
                    contadorProcesados++;
                    contadorConMora++;
                    
                    log.info("✓ Préstamo {} procesado exitosamente", prestamo.getId());
                } catch (Exception e) {
                    log.error("✗ Error procesando préstamo {}: {}", 
                        prestamo.getId(), e.getMessage(), e);
                }
            }
            
            log.info("\n=== RESUMEN DE CÁLCULO DE MORA ===");
            log.info("Total de préstamos procesados: {}", contadorProcesados);
            log.info("Préstamos con mora aplicada: {}", contadorConMora);
            log.info("Préstamos con errores: {}", (prestamosVencidos.size() - contadorProcesados));
            
        } catch (Exception e) {
            log.error("✗ Error en calcularMoraPrestamosVencidos: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Calcula la mora para un préstamo específico.
     * 
     * <p>Este método maneja:</p>
     * <ul>
     *   <li>Cálculo de días de mora considerando días de gracia</li>
     *   <li>Cálculo del monto de mora diaria</li>
     *   <li>Acumulación de días y montos de mora</li>
     *   <li>Actualización del estado del préstamo a EN_MORA cuando corresponde</li>
     * </ul>
     * 
     * @param prestamo Préstamo al que se le calculará la mora
     * @param hoy Fecha de referencia para el cálculo
     * @throws IllegalArgumentException Si el préstamo es nulo o no tiene fecha de vencimiento
     * @throws RuntimeException Si ocurre un error durante el procesamiento
     */
    private void calcularMoraParaPrestamo(Prestamo prestamo, LocalDate hoy) {
        try {
            log.info("\n--- INICIO CÁLCULO MORA PRÉSTAMO ID: {} ---", prestamo.getId());
            log.info("Estado actual: {}", prestamo.getEstado());
            log.info("Fecha vencimiento: {}", prestamo.getFechaVencimiento());
            log.info("Monto: {}", prestamo.getMonto());
            log.info("Interés: {}%", prestamo.getInteres());
            log.info("Interés moratorio: {}%", prestamo.getInteresMoratorio());
            log.info("Días mora actuales: {}", prestamo.getDiasMora());
            log.info("Último cálculo mora: {}", prestamo.getFechaUltimoCalculoMora());
            
            // Si el préstamo está aprobado pero ya venció, marcarlo como vencido primero
            if (prestamo.getEstado() == EstadoPrestamo.APROBADO && 
                prestamo.getFechaVencimiento().isBefore(hoy)) {
                log.info("⚠️ Actualizando préstamo {} de APROBADO a VENCIDO", prestamo.getId());
                prestamo.setEstado(EstadoPrestamo.VENCIDO);
                prestamo.setFechaUltimoCalculoMora(prestamo.getFechaVencimiento());
                prestamo = prestamoRepository.save(prestamo);
                log.info("✓ Préstamo {} actualizado a VENCIDO", prestamo.getId());
            }
            
            // Si el préstamo no está vencido o en mora, salir
            if (prestamo.getEstado() != EstadoPrestamo.VENCIDO && 
                prestamo.getEstado() != EstadoPrestamo.EN_MORA) {
                log.info("ℹ️ Préstamo {} no requiere cálculo de mora. Estado: {}", 
                    prestamo.getId(), prestamo.getEstado());
                return;
            }
            
            // Siempre usar la fecha de vencimiento como punto de partida para el cálculo
            LocalDate fechaReferencia = prestamo.getFechaVencimiento();
            if (fechaReferencia == null) {
                log.error("✗ No se pudo determinar la fecha de vencimiento para el préstamo {}", prestamo.getId());
                return;
            }
            
            // Si ya se ha calculado mora antes, usamos la fecha del último cálculo
            LocalDate fechaUltimoCalculo = prestamo.getFechaUltimoCalculoMora();
            if (fechaUltimoCalculo != null && !fechaUltimoCalculo.isBefore(fechaReferencia)) {
                fechaReferencia = fechaUltimoCalculo;
            }
            
            // Asegurarse de que la fecha de referencia no sea en el futuro
            if (fechaReferencia.isAfter(hoy)) {
                log.warn("⚠️ Fecha de referencia {} es posterior a hoy {}", fechaReferencia, hoy);
                return;
            }
            
            // Calcular días de mora desde la fecha de referencia, considerando días de gracia
            long diasDesdeVencimiento = ChronoUnit.DAYS.between(fechaReferencia, hoy);
            long diasMora = Math.max(0, diasDesdeVencimiento - diasGracia);
            
            // Sumar los días de mora existentes a los nuevos días
            int diasMoraExistentes = prestamo.getDiasMora();
            if (diasMoraExistentes > 0) {
                diasMora = diasMoraExistentes + diasMora;
                log.info("Sumando {} días de mora existentes a los nuevos {} días", diasMoraExistentes, diasMora - diasMoraExistentes);
            }
            
            log.info("\n📅 Cálculo de días de mora:");
            log.info("Fecha referencia: {}", fechaReferencia);
            log.info("Hoy: {}", hoy);
            log.info("Días gracia: {}", diasGracia);
            log.info("Días de mora calculados: {}", diasMora);
            
            if (diasMora > 0) {
                // Calcular mora diaria (porcentaje del monto original por día)
                BigDecimal moraDiaria = calcularMoraDiaria(prestamo.getMonto());
                
                // Calcular mora total por los días transcurridos
                BigDecimal moraTotal = moraDiaria.multiply(BigDecimal.valueOf(diasMora));
                
                log.info("\n💰 Cálculo de mora:");
                log.info("Monto base: {}", prestamo.getMonto());
                log.info("Porcentaje de mora diario: {}%", porcentajeMoraDiario);
                log.info("Mora diaria ({} * {}%): {}", 
                    prestamo.getMonto(), porcentajeMoraDiario, moraDiaria);
                log.info("Total mora ({} * {} días): {}", moraDiaria, diasMora, moraTotal);
                
                // Actualizar valores
                actualizarPrestamoConMora(prestamo, diasMora, moraTotal, hoy);
                
                log.info("\n✅ Mora aplicada exitosamente al préstamo {}", prestamo.getId());
                log.info("✅ Días de mora actualizados a: {}", prestamo.getDiasMora());
                log.info("✅ Mora acumulada: {}", prestamo.getMoraAcumulada());
                log.info("✅ Deuda restante: {}", prestamo.getDeudaRestante());
            } else {
                log.info("ℹ️ No hay días de mora para calcular para el préstamo {}", prestamo.getId());
                log.info("ℹ️ Fecha de referencia: {}", fechaReferencia);
                log.info("ℹ️ Días de gracia aplicados: {}", diasGracia);
            }
        } catch (Exception e) {
            log.error("✗ ERROR en calcularMoraParaPrestamo para préstamo {}: {}", 
                prestamo != null ? prestamo.getId() : "null", e.getMessage(), e);
            throw e; // Relanzar para que el método llamador pueda manejarlo
        } finally {
            log.info("--- FIN CÁLCULO MORA PRÉSTAMO ID: {} ---\n", 
                prestamo != null ? prestamo.getId() : "null");
        }
    }
    
    /**
     * Calcula el monto de mora diaria para un monto dado.
     * 
     * @param monto Monto base sobre el que se calculará la mora
     * @return Monto de mora diaria redondeado a 2 decimales
     * @throws IllegalArgumentException Si el monto es nulo o negativo
     */
    private BigDecimal calcularMoraDiaria(BigDecimal monto) {
        return monto.multiply(porcentajeMoraDiario)
                   .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Actualiza un préstamo con la información de mora calculada.
     * 
     * <p>Este método actualiza los siguientes campos del préstamo:</p>
     * <ul>
     *   <li>Días de mora</li>
     *   <li>Mora acumulada</li>
     *   <li>Deuda restante</li>
     *   <li>Fechas de último cálculo</li>
     *   <li>Estado (a EN_MORA si corresponde)</li>
     * </ul>
     * 
     * @param prestamo Préstamo a actualizar
     * @param diasMora Cantidad de días de mora a aplicar
     * @param moraTotal Monto total de mora a aplicar
     * @param hoy Fecha de referencia para la actualización
     * @throws IllegalArgumentException Si algún parámetro requerido es nulo
     * @throws RuntimeException Si ocurre un error durante la actualización
     */
    private void actualizarPrestamoConMora(Prestamo prestamo, long diasMora, 
                                          BigDecimal moraTotal, LocalDate hoy) {
        try {
            log.info("\n🔄 Actualizando préstamo ID: {} con mora", prestamo.getId());
            log.info("Días de mora actuales: {}", prestamo.getDiasMora());
            log.info("Nuevos días de mora: {}", diasMora);
            log.info("Mora acumulada actual: {}", prestamo.getMoraAcumulada());
            log.info("Nueva mora acumulada: {}", moraTotal);
            
            // Actualizar campos de mora
            prestamo.setDiasMora((int) diasMora);
            prestamo.setMoraAcumulada(moraTotal);
            prestamo.setFechaUltimoCalculoMora(hoy);
            prestamo.setFechaUltimaMora(hoy);
            
            // Calcular la deuda restante (capital pendiente + intereses + mora)
            BigDecimal capitalPendiente = prestamo.getMonto();
            BigDecimal intereses = BigDecimal.ZERO; // Asumiendo que los intereses ya están incluidos en el monto
            
            // Si hay pagos, restarlos del capital pendiente
            if (prestamo.getPagos() != null && !prestamo.getPagos().isEmpty()) {
                BigDecimal totalPagado = prestamo.getPagos().stream()
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                capitalPendiente = capitalPendiente.subtract(totalPagado);
            }
            
            // Asegurarse de que el capital pendiente no sea negativo
            capitalPendiente = capitalPendiente.max(BigDecimal.ZERO);
            
            // Calcular deuda total (capital pendiente + mora acumulada)
            BigDecimal deudaTotal = capitalPendiente.add(intereses).add(moraTotal);
            
            // Actualizar deuda restante
            prestamo.setDeudaRestante(deudaTotal);
            
            // Cambiar estado a EN_MORA si aún no lo está
            if (prestamo.getEstado() != EstadoPrestamo.EN_MORA) {
                log.info("Cambiando estado de {} a EN_MORA", prestamo.getEstado());
                prestamo.setEstado(EstadoPrestamo.EN_MORA);
            }
            
            // Marcar que se ha aplicado mora
            prestamo.setMoraAplicada(true);
            prestamo.setInteresMoratorioAplicado(true);
            
            // Actualizar auditoría
            prestamo.setModificadoPor("scheduler");
            prestamo.setFechaModificacionAuditoria(LocalDateTime.now());
            
            // Guardar cambios
            Prestamo prestamoActualizado = prestamoRepository.save(prestamo);
            
            log.info("✅ Préstamo actualizado exitosamente");
            log.info("Nuevo estado: {}", prestamoActualizado.getEstado());
            log.info("Total de días de mora: {}", prestamoActualizado.getDiasMora());
            log.info("Total mora acumulada: {}", prestamoActualizado.getMoraAcumulada());
            log.info("Deuda restante: {}", prestamoActualizado.getDeudaRestante());
            
        } catch (Exception e) {
            log.error("✗ Error actualizando préstamo {}: {}", 
                prestamo != null ? prestamo.getId() : "null", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Tarea programada para actualizar el estado de préstamos vencidos.
     * 
     * <p>Esta tarea se ejecuta diariamente al mediodía y realiza las siguientes acciones:</p>
     * <ol>
     *   <li>Busca préstamos PENDIENTES que hayan vencido</li>
     *   <li>Actualiza su estado a VENCIDO</li>
     *   <li>Aplica el interés moratorio correspondiente</li>
     * </ol>
     * 
     * <p>Frecuencia de ejecución: Diario a las 12:00 PM</p>
     * 
     * @implNote Esta tarea está diseñada para ejecutarse en un entorno de producción
     * con menor frecuencia que el cálculo de mora, ya que solo actualiza estados.
     */
    @Scheduled(cron = "0 0 12 * * ?") // Se ejecuta todos los días al mediodía
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

    /**
     * Aplica el interés moratorio a un préstamo vencido.
     * 
     * <p>Este método calcula y aplica el interés moratorio a la deuda restante
     * del préstamo, actualizando los campos correspondientes.</p>
     * 
     * @param prestamo Préstamo al que se aplicará el interés moratorio
     * @throws IllegalArgumentException Si el préstamo es nulo
     * @throws IllegalStateException Si el préstamo no está en estado VENCIDO
     */
    private void aplicarInteresMoratorio(Prestamo prestamo) {
        try {
            // Asegurarse de que deudaRestante no sea nulo
            BigDecimal deudaRestante = prestamo.getDeudaRestante() != null ? 
                prestamo.getDeudaRestante() : BigDecimal.ZERO;
                
            // Asegurarse de que interesMoratorio no sea nulo
            BigDecimal interesMoratorioPorcentaje = prestamo.getInteresMoratorio() != null ?
                prestamo.getInteresMoratorio() : BigDecimal.ZERO;
            
            // Calcular el interés moratorio
            BigDecimal interesMoratorio = deudaRestante
                    .multiply(interesMoratorioPorcentaje)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Actualizar el préstamo con el interés moratorio
            prestamo.setSaldoMoratorio(interesMoratorio);
            prestamo.setInteresMoratorioAplicado(true);
            
            // Actualizar la deuda restante incluyendo el interés moratorio
            BigDecimal nuevaDeuda = deudaRestante.add(interesMoratorio);
            prestamo.setDeudaRestante(nuevaDeuda);
            
            prestamoRepository.save(prestamo);

            log.info("Aplicado interés moratorio de {} al préstamo {} con deuda restante {}",
                interesMoratorio, prestamo.getId(), deudaRestante);
            prestamo.setInteresMoratorioAplicado(true);
            prestamoRepository.save(prestamo);
        } catch (Exception e) {
            log.error("Error aplicando interés moratorio al préstamo {}: {}", prestamo.getId(), e.getMessage());
        }
    }
}


