package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrestamoServiceImpl implements PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final ClienteRepository clienteRepository;

    @Override
    @Transactional
    public PrestamoModel crearPrestamo(PrestamoModel prestamoModel) {
        if ("Rechazado".equalsIgnoreCase(prestamoModel.getEstado())) {
            throw new IllegalArgumentException("No se puede crear un préstamo con estado 'Rechazado'");
        }

        Cliente cliente = clienteRepository.findById(prestamoModel.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        EstadoPrestamo estado = EstadoPrestamo.fromString(prestamoModel.getEstado());

        Prestamo prestamo = Prestamo.builder()
                .monto(new BigDecimal(String.valueOf(prestamoModel.getMonto())))
                .interes(prestamoModel.getInteres() != null ? new BigDecimal(String.valueOf(prestamoModel.getInteres())) : BigDecimal.ZERO)
                .fechaCreacion(prestamoModel.getFechaCreacion() != null ? prestamoModel.getFechaCreacion() : LocalDate.now())
                .estado(estado)
                .cliente(cliente)
                .build();

        Prestamo savedPrestamo = prestamoRepository.save(prestamo);

        return convertirEntidadAModelo(savedPrestamo);
    }


    @Override
    public PrestamoModel actualizarPrestamo(Long id, PrestamoModel prestamoModel) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        prestamo.setMonto(new BigDecimal(String.valueOf(prestamoModel.getMonto())));
        prestamo.setInteres(prestamoModel.getInteres() != null ? new BigDecimal(String.valueOf(prestamoModel.getInteres())) : BigDecimal.ZERO); // Usamos el constructor de BigDecimal

        EstadoPrestamo estado = EstadoPrestamo.fromString(prestamoModel.getEstado());
        prestamo.setEstado(estado);

        Prestamo updatedPrestamo = prestamoRepository.save(prestamo);
        return convertirEntidadAModelo(updatedPrestamo);
    }

    @Override
    public PrestamoModel obtenerPrestamoPorId(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
        return convertirEntidadAModelo(prestamo);
    }

    @Override
    public List<PrestamoModel> obtenerTodosLosPrestamos() {
        return prestamoRepository.findAll().stream()
                .map(this::convertirEntidadAModelo)
                .collect(Collectors.toList());
    }

    @Override
    public List<PrestamoModel> obtenerPrestamosPorCliente(Long clienteId) {
        return prestamoRepository.findByClienteId(clienteId).stream()
                .map(this::convertirEntidadAModelo)
                .collect(Collectors.toList());
    }

    @Override
    public List<PrestamoModel> obtenerPrestamosPorEstado(String estado) {
        EstadoPrestamo estadoEnum = EstadoPrestamo.fromString(estado);
        return prestamoRepository.findByEstado(String.valueOf(estadoEnum)).stream()
                .map(this::convertirEntidadAModelo)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarPrestamo(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
        prestamoRepository.delete(prestamo);
    }

    @Override
    public Double calcularInteresTotal(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        BigDecimal monto = prestamo.getMonto();
        BigDecimal interes = prestamo.getInteres();

        BigDecimal interesTotal = monto.multiply(interes).divide(BigDecimal.valueOf(100));
        BigDecimal total = monto.add(interesTotal);

        return total.doubleValue();
    }
    public Double calcularMontoRestante(Long prestamoId) {
        // Obtener el préstamo por ID
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        // Calcular el monto total del préstamo
        BigDecimal montoTotal = prestamo.getMonto();
        // Sumar los pagos realizados
        BigDecimal montoPagado = prestamo.getPagos().stream()
                .map(Pago::getMonto) // Obtener el monto de cada pago
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Sumar todos los pagos

        // Calcular el monto restante
        BigDecimal montoRestante = montoTotal.subtract(montoPagado);
        return montoRestante.doubleValue();
    }


    private PrestamoModel convertirEntidadAModelo(Prestamo prestamo) {
        return PrestamoModel.builder()
                .id(prestamo.getId())
                .monto(BigDecimal.valueOf(prestamo.getMonto().doubleValue()))
                .interes(BigDecimal.valueOf(prestamo.getInteres().doubleValue()))
                .fechaCreacion(prestamo.getFechaCreacion())
                .estado(prestamo.getEstado().getDescripcion())
                .clienteId(prestamo.getCliente().getId())
                .deudaRestante(calcularMontoRestante(prestamo.getId()))
                .pagos(prestamo.getPagos() != null ?
                        prestamo.getPagos().stream().map(pago -> PagoModel.builder()
                                        .id(pago.getId())
                                        .montoPago(pago.getMonto())
                                        .fecha(pago.getFecha())
                                        .prestamoId(pago.getPrestamo().getId())
                                        .build())
                                .collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }

}
