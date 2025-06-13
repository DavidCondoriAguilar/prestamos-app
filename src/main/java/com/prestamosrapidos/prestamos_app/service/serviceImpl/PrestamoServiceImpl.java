package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.exception.RecursoNoEncontradoException;
import com.prestamosrapidos.prestamos_app.model.EstadoModel;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import com.prestamosrapidos.prestamos_app.validation.PrestamoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrestamoServiceImpl implements PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final ClienteRepository clienteRepository;

    @Override
    public PrestamoModel crearPrestamo(PrestamoModel prestamoModel) {
        PrestamoValidator.validarPrestamoModel(prestamoModel);

        if ("Rechazado".equalsIgnoreCase(prestamoModel.getEstado())) {
            throw new IllegalArgumentException("No se puede crear un préstamo con estado 'Rechazado'");
        }

        Cliente cliente = clienteRepository.findById(prestamoModel.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        EstadoPrestamo estado = EstadoPrestamo.fromString(prestamoModel.getEstado());

        LocalDate fechaCreacion = prestamoModel.getFechaCreacion() != null
                ? prestamoModel.getFechaCreacion()
                : LocalDate.now();

        Prestamo prestamo = Prestamo.builder()
                .monto(prestamoModel.getMonto())
                .interes(prestamoModel.getInteres() != null ? prestamoModel.getInteres() : BigDecimal.ZERO)
                .interesMoratorio(
                        prestamoModel.getInteresMoratorio() != null
                                ? prestamoModel.getInteresMoratorio()
                                : BigDecimal.valueOf(10.00))
                .fechaCreacion(fechaCreacion.atStartOfDay())
                .fechaVencimiento(prestamoModel.getFechaVencimiento())
                .estado(estado)
                .cliente(cliente)
                .deudaRestante(prestamoModel.getMonto())
                .interesMoratorioAplicado(false)
                .build();

        Prestamo savedPrestamo = prestamoRepository.save(prestamo);

        return convertirEntidadAModelo(savedPrestamo);
    }

    @Override
    public PrestamoModel actualizarPrestamo(Long id, PrestamoModel prestamoModel) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        prestamo.setMonto(new BigDecimal(String.valueOf(prestamoModel.getMonto())));

        prestamo.setInteres(prestamoModel.getInteres() != null ?
                new BigDecimal(String.valueOf(prestamoModel.getInteres())) : BigDecimal.ZERO);

        prestamo.setInteresMoratorio(prestamoModel.getInteresMoratorio() != null
                ? prestamoModel.getInteresMoratorio()
                : BigDecimal.valueOf(10));

        if (prestamoModel.getFechaVencimiento() != null) {
            if (prestamoModel.getFechaVencimiento().isBefore(ChronoLocalDate.from(prestamo.getFechaCreacion()))) {
                throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de creación");
            }
            prestamo.setFechaVencimiento(prestamoModel.getFechaVencimiento());
        }

        EstadoPrestamo estado = EstadoPrestamo.fromString(prestamoModel.getEstado());
        prestamo.setEstado(estado);

        Prestamo updatedPrestamo = prestamoRepository.save(prestamo);
        return convertirEntidadAModelo(updatedPrestamo);
    }

    @Override
    public PrestamoModel actualizarEstado(Long id, EstadoModel nuevoEstado) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

        EstadoPrestamo nuevoEstadoEnum = EstadoPrestamo.fromString(nuevoEstado.getEstado());
        EstadoPrestamo estadoActual = prestamo.getEstado();

        // Validaciones de estado
        if (nuevoEstadoEnum == EstadoPrestamo.PAGADO) {
            if (prestamo.getDeudaRestante() != null && 
                prestamo.getDeudaRestante().compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("No se puede marcar como PAGADO un préstamo con deuda restante");
            }
        }

        if (nuevoEstadoEnum == EstadoPrestamo.VENCIDO) {
            if (prestamo.getFechaVencimiento() == null || 
                prestamo.getFechaVencimiento().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("No se puede marcar como VENCIDO un préstamo que no ha vencido");
            }
        }

        prestamo.setEstado(nuevoEstadoEnum);
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

    @Override
    public Double calcularMontoRestante(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

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
        boolean interesMoratorioAplicado = prestamo.getInteresMoratorioAplicado() != null
                ? prestamo.getInteresMoratorioAplicado()
                : false; // Manejar valores nulos

        if (prestamo.getFechaVencimiento() != null
                && hoy.isAfter(prestamo.getFechaVencimiento())
                && !interesMoratorioAplicado) {
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

    @Transactional
    @Override
    public void verificarYActualizarEstado(Prestamo prestamo) {
        LocalDate hoy = LocalDate.now();

        // Verificar si el préstamo está completamente pagado
        double deudaRestante = calcularMontoRestante(prestamo.getId());
        if (deudaRestante <= 0) {
            if (!prestamo.getEstado().equals(EstadoPrestamo.PAGADO)) {
                prestamo.setEstado(EstadoPrestamo.PAGADO);
                prestamoRepository.save(prestamo);
            }
            return;
        }

        // Verificar si el préstamo está vencido
        if (prestamo.getFechaVencimiento() != null && hoy.isAfter(prestamo.getFechaVencimiento())) {
            if (!prestamo.getEstado().equals(EstadoPrestamo.VENCIDO)) {
                prestamo.setEstado(EstadoPrestamo.VENCIDO);
                prestamoRepository.save(prestamo);
            }
        } else {
            // Si no está vencido, asegúrate de que el estado no sea "VENCIDO"
            if (prestamo.getEstado().equals(EstadoPrestamo.VENCIDO)) {
                prestamo.setEstado(EstadoPrestamo.PENDIENTE); // O el estado adecuado
                prestamoRepository.save(prestamo);
            }
        }
    }

    private BigDecimal calcularTotalPagado(Prestamo prestamo) {
        if (prestamo.getPagos() == null || prestamo.getPagos().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return prestamo.getPagos().stream()
                .map(pago -> pago.getMonto() != null ? pago.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void aplicarInteresMoratorioAutomatico() {
        LocalDate hoy = LocalDate.now();

        List<Prestamo> prestamosVencidos = prestamoRepository.findPrestamosVencidosNoPagados(hoy);

        prestamosVencidos.forEach(prestamo -> {
            // Calcular días de mora desde el vencimiento
            long diasMora = ChronoUnit.DAYS.between(prestamo.getFechaVencimiento(), hoy);

            if (diasMora > 0) {
                BigDecimal saldoPendiente = prestamo.getMonto()
                        .add(prestamo.getMonto().multiply(prestamo.getInteres())
                                .divide(BigDecimal.valueOf(100),
                                4, RoundingMode.HALF_UP))
                        .subtract(calcularTotalPagado(prestamo));

                BigDecimal interesDiario = saldoPendiente
                        .multiply(prestamo.getInteresMoratorio())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(365), 4, RoundingMode.HALF_UP); // Tasa diaria

                prestamo.setSaldoMoratorio(
                        prestamo.getSaldoMoratorio().add(interesDiario.multiply(BigDecimal.valueOf(diasMora)))
                );
                prestamo.setFechaUltimaMora(hoy);
                prestamoRepository.save(prestamo);

                if (prestamo.getEstado() != EstadoPrestamo.VENCIDO) {
                    prestamo.setEstado(EstadoPrestamo.VENCIDO);
                }
            }
        });
    }

    private PrestamoModel convertirEntidadAModelo(Prestamo prestamo) {

        BigDecimal interesMoratorio = prestamo.getInteresMoratorio() != null
                ? prestamo.getInteresMoratorio()
                : BigDecimal.valueOf(10.00);

        return PrestamoModel.builder()
                .id(prestamo.getId())
                .monto(prestamo.getMonto())
                .interes(prestamo.getInteres())
                .interesMoratorio(interesMoratorio)
                /*.saldoMoratorio(prestamo.getSaldoMoratorio())*/
                .fechaCreacion(LocalDate.from(prestamo.getFechaCreacion()))
                .fechaVencimiento(prestamo.getFechaVencimiento())
                .estado(prestamo.getEstado().name())
                .clienteId(prestamo.getCliente().getId())
                .deudaRestante(calcularMontoRestante(prestamo.getId()))
                .pagos(prestamo.getPagos() != null
                        ? prestamo.getPagos().stream()
                        .map(pago -> PagoModel.builder()
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
