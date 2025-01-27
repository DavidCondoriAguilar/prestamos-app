package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.repository.PagoRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PagoService;
import com.prestamosrapidos.prestamos_app.util.PrestamoHelper;
import com.prestamosrapidos.prestamos_app.validation.PagoValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PrestamoRepository prestamoRepository;
    private final PrestamoHelper prestamoHelper;

    @Override
    @Transactional
    public PagoModel registrarPago(PagoModel pagoModel) {
        PagoValidator.validarPrestamoId(pagoModel.getPrestamoId());
        PagoValidator.validarMontoPago(pagoModel.getMontoPago());

        Prestamo prestamo = prestamoRepository.findById(pagoModel.getPrestamoId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado con ID: " + pagoModel.getPrestamoId()));

        BigDecimal montoRestante = BigDecimal.valueOf(calcularMontoRestante(pagoModel.getPrestamoId()));

        if (pagoModel.getMontoPago().compareTo(BigDecimal.valueOf(montoRestante.doubleValue())) > 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante.");
        }

        PagoValidator.validarPago(pagoModel, prestamo, montoRestante);

        // Crear la entidad de pago
        Pago pagoEntidad = Pago.builder()
                .monto(pagoModel.getMontoPago())
                .prestamo(prestamo)
                .build();

        Pago savedPago = pagoRepository.save(pagoEntidad);

        // Actualizar el estado del préstamo basado en el saldo pendiente
        BigDecimal nuevoSaldoPendiente = montoRestante.subtract(pagoModel.getMontoPago());
        if (nuevoSaldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            prestamo.setEstado(EstadoPrestamo.PAGADO);
        }
        prestamoRepository.save(prestamo);

        // Retornar el modelo de pago
        return convertirEntidadAModelo(savedPago);
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
    public Double calcularMontoRestante(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        // Calcular monto total inicial (monto + interés inicial)
        BigDecimal montoTotal = prestamo.getMonto()
                .add(prestamo.getMonto().multiply(prestamo.getInteres())
                        .divide(BigDecimal.valueOf(100)));

        // Sumar pagos realizados
        BigDecimal montoPagado = prestamo.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Saldo pendiente sin mora
        BigDecimal saldoPendiente = montoTotal.subtract(montoPagado);

        // Aplicar interés moratorio si está vencido y no se ha aplicado antes
        LocalDate hoy = LocalDate.now();
        if (prestamo.getFechaVencimiento() != null
                && hoy.isAfter(prestamo.getFechaVencimiento())
                && !prestamo.getInteresMoratorioAplicado()) {

            BigDecimal interesMoratorio = saldoPendiente.multiply(prestamo.getInteresMoratorio())
                    .divide(BigDecimal.valueOf(100));

            saldoPendiente = saldoPendiente.add(interesMoratorio);

            // Actualizar estado del préstamo
            prestamo.setInteresMoratorioAplicado(true);
            prestamo.setFechaUltimoInteres(hoy);
            prestamoRepository.save(prestamo);
        }

        return saldoPendiente.doubleValue();
    }

    private PagoModel convertirEntidadAModelo(Pago pago) {

        Double deudaRestante = prestamoHelper.calcularMontoRestante(pago.getPrestamo());

        return PagoModel.builder()
                .id(pago.getId())
                .montoPago(pago.getMonto())
                .fecha(pago.getFecha())
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }
}