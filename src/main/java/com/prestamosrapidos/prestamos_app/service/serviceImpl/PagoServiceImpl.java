package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.repository.PagoRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PrestamoRepository prestamoRepository;

    @Override
    public PagoModel registrarPago(PagoModel pagoModel) {
        if (pagoModel.getPrestamoId() == null) {
            throw new IllegalArgumentException("El ID del préstamo no puede ser nulo");
        }

        if (pagoModel.getMontoPago() == null || pagoModel.getMontoPago().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        Prestamo prestamo = prestamoRepository.findById(pagoModel.getPrestamoId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        Double montoRestante = calcularMontoRestante(pagoModel.getPrestamoId());
        if (pagoModel.getMontoPago().compareTo(BigDecimal.valueOf(montoRestante)) > 0) {
            throw new IllegalArgumentException("El pago no puede exceder el monto restante del préstamo");
        }

        LocalDate fechaPago = LocalDate.now();

        Pago pagoEntidad = Pago.builder()
                .monto(pagoModel.getMontoPago())
                .fecha(fechaPago)
                .prestamo(prestamo)
                .build();

        Pago savedPago = pagoRepository.save(pagoEntidad);

        prestamo.getPagos().add(savedPago);
        prestamoRepository.save(prestamo);

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
                .fecha(LocalDate.now())
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }
}
