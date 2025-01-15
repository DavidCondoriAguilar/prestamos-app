package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.repository.PagoRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PagoService;
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

    @Override
    @Transactional
    public PagoModel registrarPago(PagoModel pagoModel) {
        log.info("Iniciando registro de pago: {}", pagoModel);

        // Validación del ID del préstamo
        if (pagoModel.getPrestamoId() == null) {
            log.error("El ID del préstamo es nulo.");
            throw new IllegalArgumentException("El ID del préstamo no puede ser nulo");
        }

        // Validación del monto del pago
        if (pagoModel.getMontoPago() == null || pagoModel.getMontoPago().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("El monto del pago es inválido: {}", pagoModel.getMontoPago());
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        // Buscar el préstamo asociado
        Prestamo prestamo = prestamoRepository.findById(pagoModel.getPrestamoId())
                .orElseThrow(() -> {
                    log.error("Préstamo no encontrado con ID: {}", pagoModel.getPrestamoId());
                    return new RuntimeException("Préstamo no encontrado");
                });

        log.info("Préstamo encontrado: ID={}, Monto Total={}, Monto Restante={}",
                prestamo.getId(), prestamo.getMonto(), calcularMontoRestante(prestamo.getId()));

        // Calcular el monto restante del préstamo
        Double montoRestante = calcularMontoRestante(pagoModel.getPrestamoId());
        if (pagoModel.getMontoPago().compareTo(BigDecimal.valueOf(montoRestante)) > 0) {
            log.error("El monto del pago excede el monto restante. Monto Pago={}, Monto Restante={}",
                    pagoModel.getMontoPago(), montoRestante);
            throw new IllegalArgumentException("El pago no puede exceder el monto restante del préstamo");
        }

        // Crear la entidad Pago
        Pago pagoEntidad = Pago.builder()
                .monto(pagoModel.getMontoPago())
                .prestamo(prestamo)
                .build();

        // Guardar el pago en la base de datos
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

        BigDecimal totalPagadoBD = BigDecimal.valueOf(totalPagado);

        BigDecimal montoRestante = prestamo.getMonto().subtract(totalPagadoBD);

        return montoRestante.doubleValue();
    }

    private PagoModel convertirEntidadAModelo(Pago pago) {
        return PagoModel.builder()
                .id(pago.getId())
                .montoPago(pago.getMonto())
                .fecha(pago.getFecha())
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }
}
