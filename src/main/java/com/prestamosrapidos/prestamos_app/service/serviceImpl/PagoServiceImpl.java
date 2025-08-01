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

    @Override
    public Page<PagoModel> obtenerTodosLosPagosPaginados(Pageable pageable) {
        return pagoRepository.findAll(pageable).map(this::convertirEntidadAModelo);
    }

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
    
    private BigDecimal calcularNuevoSaldo(BigDecimal saldoPendiente, BigDecimal montoPago, BigDecimal interesMoratorio) {
        BigDecimal nuevoSaldo = saldoPendiente.add(interesMoratorio).subtract(montoPago);
        BigDecimal tolerancia = BigDecimal.valueOf(0.01);
        return nuevoSaldo.compareTo(tolerancia.negate()) < 0 ? BigDecimal.ZERO : nuevoSaldo;
    }

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

    @Override
    public List<PagoModel> obtenerPagosPorPrestamo(Long prestamoId) {
        return pagoRepository.findByPrestamoId(prestamoId).stream()
                .map(this::convertirEntidadAModelo)
                .collect(Collectors.toList());
    }

    @Override
    public PagoModel obtenerPagoPorId(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        return convertirEntidadAModelo(pago);
    }

    @Override
    public void eliminarPago(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        pagoRepository.delete(pago);
    }

    @Override
    public BigDecimal calcularMontoRestante(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

        BigDecimal montoTotal = prestamo.getMonto()
                .add(prestamo.getMonto().multiply(prestamo.getInteres())
                        .divide(BigDecimal.valueOf(100)));

        BigDecimal montoPagado = prestamo.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoPendiente = montoTotal.subtract(montoPagado);

        // Aplicar interés moratorio si está vencido y no se ha aplicado antes
        LocalDate hoy = LocalDate.now();
        boolean interesMoratorioAplicado = prestamo.getInteresMoratorioAplicado() != null
                ? prestamo.getInteresMoratorioAplicado()
                : false;

        if (prestamo.getFechaVencimiento() != null
                && hoy.isAfter(prestamo.getFechaVencimiento())
                && !interesMoratorioAplicado) {
            BigDecimal interesMoratorio = saldoPendiente.multiply(prestamo.getInteresMoratorio())
                    .divide(BigDecimal.valueOf(100));
            saldoPendiente = saldoPendiente.add(interesMoratorio);
        }

        return saldoPendiente;
    }

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

    @Override
    public void verificarYActualizarEstado(Prestamo prestamo) {
        LocalDate hoy = LocalDate.now();

        // Verificar si el préstamo está completamente pagado
        BigDecimal deudaRestante = calcularMontoRestante(prestamo.getId());
        if (deudaRestante.compareTo(BigDecimal.ZERO) <= 0) {
            if (!prestamo.getEstado().equals(EstadoPrestamo.PAGADO)) {
                prestamo.setEstado(EstadoPrestamo.PAGADO);
                prestamoRepository.save(prestamo);
            }
            return;
        }

        if (prestamo.getFechaVencimiento() != null && hoy.isAfter(prestamo.getFechaVencimiento())) {
            if (!prestamo.getEstado().equals(EstadoPrestamo.VENCIDO)) {
                prestamo.setEstado(EstadoPrestamo.VENCIDO);
                prestamoRepository.save(prestamo);
            }
        } else {
            if (prestamo.getEstado().equals(EstadoPrestamo.VENCIDO)) {
                prestamo.setEstado(EstadoPrestamo.PENDIENTE);
                prestamoRepository.save(prestamo);
            }
        }
    }

    private PagoModel convertirEntidadAModelo(Pago pago) {
        return PagoModel.builder()
                .id(pago.getId()) 
                .montoPago(pago.getMonto())
                .fecha(pago.getFecha())
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }
    
    private BigDecimal calcularInteresOrdinario(Prestamo prestamo) {
        // Interés ordinario = monto * (tasa_interes / 100)
        return prestamo.getMonto()
                .multiply(prestamo.getInteres())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
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
    
    private BigDecimal calcularTotalPagado(Prestamo prestamo) {
        // Suma de todos los pagos realizados
        return prestamo.getPagos().stream()
                .map(Pago::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}