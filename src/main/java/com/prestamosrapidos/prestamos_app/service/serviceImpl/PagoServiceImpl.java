package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
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
        log.info("Iniciando registro de pago: {}", pagoModel);

        PagoValidator.validarPrestamoId(pagoModel.getPrestamoId());
        PagoValidator.validarMontoPago(pagoModel.getMontoPago());

        // Buscar el préstamo asociado
        Prestamo prestamo = prestamoRepository.findById(pagoModel.getPrestamoId())
                .orElseThrow(() -> {
                    log.error("Préstamo no encontrado con ID: {}", pagoModel.getPrestamoId());
                    return new RuntimeException("Préstamo no encontrado");
                });

        // Calcular el monto restante del préstamo
        BigDecimal montoRestante = BigDecimal.valueOf(calcularMontoRestante(pagoModel.getPrestamoId()));

        PagoValidator.validarPago(pagoModel, prestamo, montoRestante);

        Pago pagoEntidad = Pago.builder()
                .monto(pagoModel.getMontoPago())
                .prestamo(prestamo)
                .build();

        log.info("Guardando el pago en la base de datos: {}", pagoEntidad);
        Pago savedPago = pagoRepository.save(pagoEntidad);

        log.info("Pago guardado correctamente: ID={}, Monto={}, Fecha={}",
                savedPago.getId(), savedPago.getMonto(), savedPago.getFecha());

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

        double totalPagado = pagoRepository.calcularTotalPagado(prestamoId);
        BigDecimal montoRestante = prestamo.getMonto().subtract(BigDecimal.valueOf(totalPagado));

        return montoRestante.doubleValue();
    }

    private PagoModel convertirEntidadAModelo(Pago pago) {

        Double deudaRestante = prestamoHelper.calcularMontoRestante(pago.getPrestamo());

        return PagoModel.builder()
                .id(pago.getId())
                .montoPago(pago.getMonto())
                .fecha(pago.getFecha())
                .deudaRestante(deudaRestante)
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }
}