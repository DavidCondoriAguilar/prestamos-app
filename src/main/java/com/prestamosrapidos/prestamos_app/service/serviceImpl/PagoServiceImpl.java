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
    public PagoModel registrarPago(PagoModel pago) {
        // Verificar si el prestamoId es nulo
        if (pago.getPrestamoId() == null) {
            throw new IllegalArgumentException("El ID del préstamo no puede ser nulo");
        }

        // Verificar si el montoPago es nulo o menor o igual a cero
        if (pago.getMontoPago() == null || pago.getMontoPago().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        // Obtener el préstamo asociado
        Prestamo prestamo = prestamoRepository.findById(pago.getPrestamoId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        // Validar que el pago no exceda el monto restante del préstamo
        Double montoRestante = calcularMontoRestante(pago.getPrestamoId());
        if (pago.getMontoPago().compareTo(BigDecimal.valueOf(montoRestante)) > 0) {
            throw new IllegalArgumentException("El pago no puede exceder el monto restante del préstamo");
        }

        // Establecer la fecha del pago, si no se proporciona se usa la fecha actual
        LocalDate fechaPago = (pago.getFecha() != null) ? pago.getFecha() : LocalDate.now();

        // Crear y guardar el objeto Pago
        Pago pagoEntidad = Pago.builder()
                .monto(pago.getMontoPago())
                .fecha(fechaPago)
                .prestamo(prestamo)
                .build();

        // Guardar el pago en la base de datos
        Pago savedPago = pagoRepository.save(pagoEntidad);

        // Convertir la entidad guardada a un modelo y devolverlo
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

        // Convertir totalPagado a BigDecimal para hacer la resta
        BigDecimal totalPagadoBD = BigDecimal.valueOf(totalPagado);

        // Calcular el monto restante
        BigDecimal montoRestante = prestamo.getMonto().subtract(totalPagadoBD);

        // Devolver el resultado como Double
        return montoRestante.doubleValue();
    }

    private PagoModel convertirEntidadAModelo(Pago pago) {
        return PagoModel.builder()
                .id(pago.getId())
                .montoPago(pago.getMonto())  // Usar BigDecimal directamente
                .fecha(pago.getFecha())
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }
}
