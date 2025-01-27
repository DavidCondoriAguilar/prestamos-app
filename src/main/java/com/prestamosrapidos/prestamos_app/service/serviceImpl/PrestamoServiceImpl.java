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
import com.prestamosrapidos.prestamos_app.validation.PrestamoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
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
                .build();

        // Guardar el préstamo en la base de datos
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

        // Guardar los cambios
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
        // Buscar el préstamo
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        // Validar los datos básicos del préstamo
        if (prestamo.getMonto() == null || prestamo.getInteres() == null) {
            throw new RuntimeException("El préstamo tiene datos incompletos");
        }

        // Calcular el monto inicial con interés normal
        BigDecimal montoBase = prestamo.getMonto();
        BigDecimal interesNormal = montoBase.multiply(prestamo.getInteres())
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        BigDecimal montoTotal = montoBase.add(interesNormal);

        // Verificar y aplicar interés moratorio si corresponde
        if (prestamo.getInteresMoratorioAplicado() != null && prestamo.getInteresMoratorioAplicado()) {
            if (prestamo.getInteresMoratorio() == null) {
                throw new RuntimeException("El préstamo no tiene definido un interés moratorio");
            }

            BigDecimal interesMoratorio = montoTotal.multiply(prestamo.getInteresMoratorio())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            montoTotal = montoTotal.add(interesMoratorio);
        }

        // Sumar pagos realizados
        List<Pago> pagos = prestamo.getPagos();
        BigDecimal montoPagado = BigDecimal.ZERO;

        if (pagos != null && !pagos.isEmpty()) {
            montoPagado = pagos.stream()
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Calcular el monto restante
        BigDecimal montoRestante = montoTotal.subtract(montoPagado);

        // Asegurarse de que no haya valores negativos
        if (montoRestante.compareTo(BigDecimal.ZERO) < 0) {
            montoRestante = BigDecimal.ZERO;
        }

        return montoRestante.doubleValue();
    }


    @Scheduled(cron = "0 0 0 * * ?") // Ejecutar diariamente a medianoche
    @Transactional
    public void aplicarInteresMoratorioAutomatico() {
        LocalDate hoy = LocalDate.now();

        // Buscar préstamos vencidos sin mora aplicada
        List<Prestamo> prestamosVencidos = prestamoRepository
                .findByFechaVencimientoBeforeAndInteresMoratorioAplicadoFalse(hoy);

        for (Prestamo prestamo : prestamosVencidos) {
            // Calcular deuda restante
            BigDecimal saldoPendiente = BigDecimal.valueOf(calcularMontoRestante(prestamo.getId()));

            // Calcular interés moratorio
            BigDecimal interesMoratorio = saldoPendiente.multiply(prestamo.getInteresMoratorio())
                    .divide(BigDecimal.valueOf(100));

            // Actualizar el monto del préstamo (opcional, según tu lógica)
            prestamo.setMonto(prestamo.getMonto().add(interesMoratorio));

            // Marcar como mora aplicada
            prestamo.setInteresMoratorioAplicado(true);
            prestamo.setFechaUltimoInteres(hoy);

            prestamoRepository.save(prestamo);

            System.out.println("Interés moratorio aplicado al préstamo ID: " + prestamo.getId());
        }
    }

    private PrestamoModel convertirEntidadAModelo(Prestamo prestamo) {
        // Obtener el interés moratorio o usar 10.00 como valor por defecto si es null
        BigDecimal interesMoratorio = prestamo.getInteresMoratorio() != null
                ? prestamo.getInteresMoratorio()
                : BigDecimal.valueOf(10.00);

        return PrestamoModel.builder()
                .id(prestamo.getId())
                .monto(prestamo.getMonto())
                .interes(prestamo.getInteres())
                .interesMoratorio(interesMoratorio) // Usar el valor seguro
                .fechaCreacion(LocalDate.from(prestamo.getFechaCreacion()))
                .fechaVencimiento(prestamo.getFechaVencimiento())
                .estado(prestamo.getEstado().getDescripcion())
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
