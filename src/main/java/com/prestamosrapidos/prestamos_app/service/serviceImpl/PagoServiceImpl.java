package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.exception.RecursoNoEncontradoException;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.repository.PagoRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PagoService;
import com.prestamosrapidos.prestamos_app.validation.PagoValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PrestamoRepository prestamoRepository;

    private static final Logger log = LoggerFactory.getLogger(PagoServiceImpl.class);

    /**
     * Obtiene una página de todos los pagos registrados en el sistema.
     *
     * @param pageable Configuración de paginación y ordenamiento
     * @return Page<PagoModel> Página con los modelos de pago
     */
    @Override
    public Page<PagoModel> obtenerTodosLosPagosPaginados(Pageable pageable) {
        return pagoRepository.findAll(pageable).map(this::convertirEntidadAModelo);
    }

    /**
     * Registra un nuevo pago en el sistema y actualiza el estado del préstamo correspondiente.
     * Realiza validaciones sobre el monto del pago y calcula automáticamente intereses y moras.
     *
     * @param pagoModel DTO con la información del pago a registrar
     * @return PagoModel El pago registrado con su ID generado
     * @throws RecursoNoEncontradoException Si el préstamo asociado no existe
     * @throws IllegalArgumentException Si el monto del pago no es válido
     */
    @Override
    @Transactional
    public PagoModel registrarPago(PagoModel pagoModel) {
        log.info("Iniciando registro de pago: {}", pagoModel);
        
        // 1. Obtener y validar el préstamo
        Prestamo prestamo = prestamoRepository.findById(pagoModel.getPrestamoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

        // 2. Calcular deuda actual incluyendo intereses y mora
        BigDecimal interesOrdinario = calcularInteresOrdinario(prestamo);
        BigDecimal moraAcumulada = calcularMoraAcumulada(prestamo);
        BigDecimal totalPagado = calcularTotalPagado(prestamo);
        
        BigDecimal deudaTotal = prestamo.getMonto()
                .add(interesOrdinario)
                .add(moraAcumulada);
                
        BigDecimal deudaPendiente = deudaTotal.subtract(totalPagado)
                .max(BigDecimal.ZERO);
                
        log.info("Deuda total: {}, Pagado: {}, Pendiente: {}", 
                deudaTotal, totalPagado, deudaPendiente);

        // 3. Validar pago
        PagoValidator.validarPago(pagoModel, prestamo, deudaPendiente);

        // 4. Crear y guardar el pago
        Pago pago = Pago.builder()
                .monto(pagoModel.getMontoPago())
                .fecha(pagoModel.getFecha() != null ? pagoModel.getFecha() : LocalDate.now())
                .prestamo(prestamo)
                .build();

        prestamo.addPago(pago);
        
        // 5. Actualizar deuda y estado del préstamo
        BigDecimal nuevoTotalPagado = totalPagado.add(pagoModel.getMontoPago());
        boolean estaPagado = nuevoTotalPagado.compareTo(deudaTotal) >= 0;
        
        if (estaPagado) {
            prestamo.setEstado(EstadoPrestamo.PAGADO);
            prestamo.setDeudaRestante(BigDecimal.ZERO);
            log.info("Préstamo {} marcado como PAGADO", prestamo.getId());
        } else {
            BigDecimal nuevoSaldo = deudaTotal.subtract(nuevoTotalPagado);
            prestamo.setDeudaRestante(nuevoSaldo);
            // Actualizar estado según vencimiento
            LocalDate hoy = LocalDate.now();
            if (prestamo.getFechaVencimiento() != null && hoy.isAfter(prestamo.getFechaVencimiento())) {
                prestamo.setEstado(EstadoPrestamo.EN_MORA);
            } else {
                prestamo.setEstado(EstadoPrestamo.APROBADO);
            }
            log.info("Nuevo saldo del préstamo {}: {}", prestamo.getId(), nuevoSaldo);
        }

        // 6. Guardar cambios
        pago = pagoRepository.save(pago);
        prestamo = prestamoRepository.save(prestamo);
        
        log.info("Pago registrado exitosamente. ID: {}, Monto: {}", pago.getId(), pago.getMonto());
        return convertirEntidadAModelo(pago);
    }
    
    /**
     * Calcula el nuevo saldo después de aplicar un pago, considerando intereses moratorios.
     * Si el nuevo saldo es menor que una tolerancia de 0.01, se redondea a cero.
     *
     * @param saldoPendiente Saldo pendiente antes del pago
     * @param montoPago Monto del pago realizado
     * @param interesMoratorio Interés moratorio acumulado
     * @return BigDecimal El nuevo saldo pendiente
     */
    private BigDecimal calcularNuevoSaldo(BigDecimal saldoPendiente, BigDecimal montoPago, BigDecimal interesMoratorio) {
        BigDecimal nuevoSaldo = saldoPendiente.add(interesMoratorio).subtract(montoPago);
        BigDecimal tolerancia = BigDecimal.valueOf(0.01);
        return nuevoSaldo.compareTo(tolerancia.negate()) < 0 ? BigDecimal.ZERO : nuevoSaldo;
    }

    /**
     * Actualiza el estado de un préstamo basado en el saldo pendiente y la fecha actual.
     * Puede marcar el préstamo como PAGADO, VENCIDO o PENDIENTE según corresponda.
     *
     * @param prestamo El préstamo a actualizar
     * @param nuevoSaldo Nuevo saldo pendiente después del pago
     * @param hoy Fecha actual para validar vencimientos
     */
    private void actualizarEstadoPrestamo(Prestamo prestamo, BigDecimal nuevoSaldo, LocalDate hoy) {
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0) {

            prestamo.setEstado(EstadoPrestamo.PAGADO);
            prestamo.setSaldoMoratorio(BigDecimal.ZERO);
        } else {
            if (prestamo.getFechaVencimiento() != null && hoy.isAfter(prestamo.getFechaVencimiento())) {

                prestamo.setEstado(EstadoPrestamo.VENCIDO);
                prestamo.setSaldoMoratorio(nuevoSaldo);
            } else {
                prestamo.setEstado(EstadoPrestamo.PENDIENTE);
            }
        }
    }

    /**
     * Obtiene todos los pagos asociados a un préstamo específico.
     *
     * @param prestamoId ID del préstamo del cual se desean obtener los pagos
     * @return List<PagoModel> Lista de pagos del préstamo, ordenados por fecha
     */
    @Override
    public List<PagoModel> obtenerPagosPorPrestamo(Long prestamoId) {
        return pagoRepository.findByPrestamoId(prestamoId).stream()
                .map(this::convertirEntidadAModelo)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un pago específico por su ID.
     *
     * @param id ID del pago a buscar
     * @return PagoModel Datos del pago encontrado
     * @throws RuntimeException Si el pago no existe
     */
    @Override
    public PagoModel obtenerPagoPorId(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        return convertirEntidadAModelo(pago);
    }

    /**
     * Elimina un pago del sistema por su ID.
     * 
     * @param id ID del pago a eliminar
     * @throws RuntimeException Si el pago no existe
     */
    @Override
    public void eliminarPago(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        pagoRepository.delete(pago);
    }

    /**
     * Calcula el monto restante por pagar de un préstamo, incluyendo intereses ordinarios y moratorios.
     * Aplica automáticamente intereses moratorios si el préstamo está vencido.
     *
     * @param prestamoId ID del préstamo a consultar
     * @return BigDecimal Monto total pendiente de pago
     * @throws RecursoNoEncontradoException Si el préstamo no existe
     */
    @Override
    public BigDecimal calcularMontoRestante(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

        // Calculate total amount (principal + interest)
        BigDecimal montoTotal = prestamo.getMonto()
                .add(prestamo.getMonto().multiply(prestamo.getInteres())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

        // Get total paid amount from repository
        Double totalPagado = pagoRepository.calcularTotalPagado(prestamoId);
        BigDecimal montoPagado = totalPagado != null ? BigDecimal.valueOf(totalPagado) : BigDecimal.ZERO;

        // Calculate remaining amount
        BigDecimal saldoPendiente = montoTotal.subtract(montoPagado);

        // Apply late interest if the loan is overdue
        LocalDate hoy = LocalDate.now();
        if (prestamo.getFechaVencimiento() != null
                && hoy.isAfter(prestamo.getFechaVencimiento())
                && !prestamo.getEstado().equals(EstadoPrestamo.PAGADO)) {
            // Calculate days late
            long diasMora = ChronoUnit.DAYS.between(prestamo.getFechaVencimiento(), hoy);
            if (diasMora > 0) {
                // Calculate daily interest (0.1% = 0.001)
                BigDecimal interesDiario = saldoPendiente.multiply(BigDecimal.valueOf(0.001));
                BigDecimal interesMoratorio = interesDiario.multiply(BigDecimal.valueOf(diasMora));
                saldoPendiente = saldoPendiente.add(interesMoratorio);
            }
        }

        return saldoPendiente.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el interés moratorio acumulado para un préstamo hasta una fecha específica.
     * El cálculo se realiza solo si el préstamo está vencido.
     *
     * @param prestamo Préstamo sobre el cual calcular la mora
     * @param fechaActual Fecha hasta la cual calcular la mora
     * @return BigDecimal Monto de intereses moratorios calculados
     */
    private BigDecimal calcularInteresMoratorio(Prestamo prestamo, LocalDate fechaActual) {
        if (prestamo.getEstado() == EstadoPrestamo.PAGADO ||
                prestamo.getFechaVencimiento() == null ||
                !fechaActual.isAfter(prestamo.getFechaVencimiento())) {
            return BigDecimal.ZERO;
        }

        BigDecimal saldoPendiente = calcularMontoRestante(prestamo.getId());

        long diasVencidos = ChronoUnit.DAYS.between(prestamo.getFechaVencimiento(), fechaActual);
        BigDecimal interesMoratorioDiario = saldoPendiente
                .multiply(prestamo.getInteresMoratorio())
                .divide(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(365), 4, RoundingMode.HALF_UP);

        return interesMoratorioDiario.multiply(BigDecimal.valueOf(diasVencidos));
    }

    /**
     * Verifica y actualiza el estado de un préstamo basado en su saldo pendiente y fecha de vencimiento.
     * Puede cambiar el estado a PAGADO, VENCIDO o PENDIENTE según corresponda.
     *
     * @param prestamo Préstamo a verificar y actualizar
     */
    @Override
    public void verificarYActualizarEstado(Prestamo prestamo) {
        if (prestamo == null) {
            return;
        }

        // Get total paid amount from repository
        Double totalPagado = pagoRepository.calcularTotalPagado(prestamo.getId());
        BigDecimal montoPagado = totalPagado != null ? BigDecimal.valueOf(totalPagado) : BigDecimal.ZERO;

        // Calculate total amount (principal + interest)
        BigDecimal interes = prestamo.getMonto()
                .multiply(prestamo.getInteres())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal montoTotal = prestamo.getMonto().add(interes);

        // Calculate remaining amount
        BigDecimal saldoPendiente = montoTotal.subtract(montoPagado);

        // If the remaining amount is zero or negative, mark as PAID
        if (saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
            if (!EstadoPrestamo.PAGADO.equals(prestamo.getEstado())) {
                prestamo.setEstado(EstadoPrestamo.PAGADO);
                prestamoRepository.save(prestamo);
            }
            return;
        }

        // Only proceed with status updates if the loan isn't already marked as PAID
        if (!EstadoPrestamo.PAGADO.equals(prestamo.getEstado())) {
            // Check if the loan is overdue
            LocalDate hoy = LocalDate.now();
            if (prestamo.getFechaVencimiento() != null && hoy.isAfter(prestamo.getFechaVencimiento())) {
                if (!EstadoPrestamo.EN_MORA.equals(prestamo.getEstado())) {
                    prestamo.setEstado(EstadoPrestamo.EN_MORA);
                    prestamoRepository.save(prestamo);
                }
            } else if (EstadoPrestamo.EN_MORA.equals(prestamo.getEstado())) {
                // If it was in MORA but now it's not overdue anymore
                prestamo.setEstado(EstadoPrestamo.PENDIENTE);
                prestamoRepository.save(prestamo);
            }
        }
    }

    /**
     * Convierte una entidad Pago a su correspondiente DTO PagoModel.
     *
     * @param pago Entidad Pago a convertir
     * @return PagoModel DTO con los datos del pago
     */
    private PagoModel convertirEntidadAModelo(Pago pago) {
        return PagoModel.builder()
                .id(pago.getId()) 
                .montoPago(pago.getMonto())
                .fecha(pago.getFecha())
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }
    
    /**
     * Calcula el interés ordinario de un préstamo.
     * Fórmula: monto * (tasa_interés / 100)
     *
     * @param prestamo Préstamo sobre el cual calcular el interés
     * @return BigDecimal Monto de intereses ordinarios calculados
     */
    private BigDecimal calcularInteresOrdinario(Prestamo prestamo) {
        // Interés ordinario = monto * (tasa_interes / 100)
        return prestamo.getMonto()
                .multiply(prestamo.getInteres())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calcula la mora acumulada de un préstamo hasta la fecha actual.
     * La mora se calcula solo si el préstamo está vencido.
     * Fórmula: (monto * tasa_moratoria * días_vencido) / (100 * 365)
     *
     * @param prestamo Préstamo sobre el cual calcular la mora
     * @return BigDecimal Monto de mora acumulada, o cero si no aplica
     */
    private BigDecimal calcularMoraAcumulada(Prestamo prestamo) {
        if (prestamo.getFechaVencimiento() == null || 
                LocalDate.now().isBefore(prestamo.getFechaVencimiento())) {
            return BigDecimal.ZERO;
        }
        
        // Días de mora
        long diasMora = ChronoUnit.DAYS.between(
                prestamo.getFechaVencimiento(), 
                LocalDate.now()
        );
        
        if (diasMora <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Cálculo de mora diaria = (monto * tasa_moratoria) / (100 * 365)
        BigDecimal moraDiaria = prestamo.getMonto()
                .multiply(prestamo.getInteresMoratorio())
                .divide(BigDecimal.valueOf(365 * 100), 10, RoundingMode.HALF_UP);
                
        // Mora total = mora_diaria * días de mora
        return moraDiaria.multiply(BigDecimal.valueOf(diasMora))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calcula el monto total pagado de un préstamo sumando todos sus pagos registrados.
     *
     * @param prestamo Préstamo del cual calcular el total pagado
     * @return BigDecimal Suma total de todos los pagos realizados
     */
    private BigDecimal calcularTotalPagado(Prestamo prestamo) {
        // Suma de todos los pagos realizados
        return prestamo.getPagos().stream()
                .map(Pago::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}