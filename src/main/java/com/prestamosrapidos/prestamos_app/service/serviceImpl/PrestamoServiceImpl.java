package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.exception.RecursoNoEncontradoException;
import com.prestamosrapidos.prestamos_app.exception.SaldoInsuficienteException;
import com.prestamosrapidos.prestamos_app.model.*;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import com.prestamosrapidos.prestamos_app.validation.PrestamoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrestamoServiceImpl implements PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final ClienteRepository clienteRepository;

    @Override
    @Transactional
    public PrestamoModel crearPrestamo(PrestamoModel prestamoModel) {
        // Validar el modelo
        PrestamoValidator.validarPrestamoModel(prestamoModel);
        
        // Verificar si el cliente existe
        Cliente cliente = clienteRepository.findById(prestamoModel.getClienteId())
                .orElseThrow(() -> new NoSuchElementException("Cliente no encontrado con ID: " + prestamoModel.getClienteId()));
        
        // Obtener fechas del modelo o usar valores por defecto
        LocalDate fechaCreacion = prestamoModel.getFechas() != null && prestamoModel.getFechas().getCreacion() != null
                ? prestamoModel.getFechas().getCreacion()
                : LocalDate.now();
                
        LocalDate fechaVencimiento = null;
        if (prestamoModel.getFechas() != null && prestamoModel.getFechas().getVencimiento() != null) {
            fechaVencimiento = prestamoModel.getFechas().getVencimiento();
        } else {
            // Si no se proporciona fecha de vencimiento, se establece por defecto 30 días después
            fechaVencimiento = fechaCreacion.plusDays(30);
        }
        
        // Crear el objeto de fechas para el préstamo
        FechasModel fechasPrestamo = new FechasModel();
        fechasPrestamo.setCreacion(fechaCreacion);
        fechasPrestamo.setVencimiento(fechaVencimiento);
        prestamoModel.setFechas(fechasPrestamo);
        
        // Crear y configurar el préstamo
        Prestamo prestamo = new Prestamo();
        prestamo.setMonto(prestamoModel.getMonto());
        prestamo.setInteres(prestamoModel.getInteres() != null ? prestamoModel.getInteres() : BigDecimal.ZERO);
        prestamo.setInteresMoratorio(prestamoModel.getInteresMoratorio() != null
                ? prestamoModel.getInteresMoratorio()
                : BigDecimal.valueOf(10.00));
        prestamo.setFechaCreacion(fechaCreacion.atStartOfDay());
        prestamo.setFechaVencimiento(fechaVencimiento);
        
        // Establecer el estado inicial del préstamo
        if (prestamoModel.getEstado() != null && !prestamoModel.getEstado().isEmpty()) {
            try {
                prestamo.setEstado(EstadoPrestamo.valueOf(prestamoModel.getEstado()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado de préstamo no válido: " + prestamoModel.getEstado());
            }
        } else {
            // Por defecto, el préstamo se crea como APROBADO
            prestamo.setEstado(EstadoPrestamo.APROBADO);
        }
        
        prestamo.setCliente(cliente);
        prestamo.setDeudaRestante(prestamoModel.getMonto());
        prestamo.setInteresMoratorioAplicado(false);
        
        // Validar fechas
        if (fechaVencimiento.isBefore(fechaCreacion)) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de creación");
        }
        
        // Validar saldo suficiente
        validarSaldoSuficiente(cliente, prestamoModel.getMonto());
        
        // Actualizar saldo de la cuenta
        actualizarSaldoCuenta(cliente, prestamoModel.getMonto().negate());
        
        // Guardar el préstamo
        Prestamo prestamoGuardado = prestamoRepository.save(prestamo);
        
        // Convertir la entidad guardada de vuelta a modelo
        return convertirEntidadAModelo(prestamoGuardado);
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

        if (prestamoModel.getFechas() == null) {
            throw new IllegalArgumentException("El objeto de fechas no puede ser nulo");
        }
        
        LocalDate nuevaFechaVencimiento = prestamoModel.getFechas().getVencimiento();
        if (nuevaFechaVencimiento != null) {
            if (nuevaFechaVencimiento.isBefore(ChronoLocalDate.from(prestamo.getFechaCreacion()))) {
                throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de creación");
            }
            prestamo.setFechaVencimiento(nuevaFechaVencimiento);
        }

        EstadoPrestamo estado = EstadoPrestamo.fromString(prestamoModel.getEstado());
        prestamo.setEstado(estado);

        Prestamo updatedPrestamo = prestamoRepository.save(prestamo);
        return convertirEntidadAModelo(updatedPrestamo);
    }

    @Override
    @Transactional
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
        prestamo = prestamoRepository.save(prestamo);
        return convertirEntidadAModelo(prestamo);
    }

    @Override
    @Transactional
    public PrestamoModel obtenerPrestamoPorId(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado con ID: " + id));
        
        // Forzar la actualización de la mora
        verificarYActualizarMora(prestamo);
        
        // Recargar el préstamo para obtener los cambios más recientes
        prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Error al actualizar el préstamo con ID: " + id));
        
        return convertirEntidadAModelo(prestamo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PrestamoModel> obtenerTodosLosPrestamos() {
        List<Prestamo> prestamos = prestamoRepository.findAll();
        // Verificar y actualizar mora para cada préstamo
        prestamos.forEach(this::verificarYActualizarMora);
        return prestamos.stream()
                .map(this::convertirEntidadAModelo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<PrestamoModel> obtenerPrestamosPorCliente(Long clienteId) {
        List<Prestamo> prestamos = prestamoRepository.findByClienteId(clienteId);
        
        // Actualizar mora para cada préstamo y guardar los cambios
        prestamos.forEach(prestamo -> {
            verificarYActualizarMora(prestamo);
            prestamoRepository.save(prestamo);
        });
        
        // Volver a cargar los préstamos para obtener los cambios más recientes
        List<Prestamo> prestamosActualizados = prestamoRepository.findByClienteId(clienteId);
        
        return prestamosActualizados.stream()
                .map(this::convertirEntidadAModelo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrestamoModel> obtenerPrestamosPorEstado(String estado) {
        EstadoPrestamo estadoEnum = EstadoPrestamo.fromString(estado);
        List<Prestamo> prestamos = prestamoRepository.findByEstado(String.valueOf(estadoEnum));
        // Verificar y actualizar mora para cada préstamo
        prestamos.forEach(this::verificarYActualizarMora);
        return prestamos.stream()
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
    public BigDecimal calcularInteresTotal(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

        BigDecimal monto = prestamo.getMonto();
        BigDecimal interes = prestamo.getInteres();

        // Calcular el interés total (monto * interés%)
        BigDecimal interesTotal = monto.multiply(interes)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                
        // Sumar el monto original + intereses
        return monto.add(interesTotal).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calcularMontoRestante(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

        // Calcular monto total inicial (monto + interés inicial)
        BigDecimal montoTotal = prestamo.getMonto()
                .add(prestamo.getMonto().multiply(prestamo.getInteres())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                        
        // Sumar pagos realizados
        BigDecimal montoPagado = prestamo.getPagos().stream()
                .map(Pago::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular saldo pendiente
        BigDecimal saldoPendiente = montoTotal.subtract(montoPagado);

        // Verificar si se debe aplicar interés moratorio
        LocalDate hoy = LocalDate.now();
        boolean interesMoratorioAplicado = prestamo.getInteresMoratorioAplicado() != null
                ? prestamo.getInteresMoratorioAplicado()
                : false;

        if (prestamo.getFechaVencimiento() != null
                && hoy.isAfter(prestamo.getFechaVencimiento())
                && !interesMoratorioAplicado) {
            // Calcular interés moratorio como 0.1% del monto original por día de mora
            long diasMora = ChronoUnit.DAYS.between(prestamo.getFechaVencimiento(), hoy);
            BigDecimal interesDiario = prestamo.getMonto()
                    .multiply(BigDecimal.valueOf(0.001)); // 0.1% = 0.001
            BigDecimal interesMoratorio = interesDiario.multiply(BigDecimal.valueOf(diasMora));
                    
            saldoPendiente = saldoPendiente.add(interesMoratorio);

            // Actualizar estado del préstamo
            prestamo.setInteresMoratorioAplicado(true);
            prestamo.setFechaUltimoInteres(hoy);
            prestamoRepository.save(prestamo);
        }
        return saldoPendiente.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
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

    @Transactional
    public void verificarYActualizarMora(Prestamo prestamo) {
        if (prestamo.getEstado() == EstadoPrestamo.PAGADO || 
            prestamo.getFechaVencimiento() == null) {
            return; // No hay nada que hacer si ya está pagado o no tiene fecha de vencimiento
        }
        
        LocalDate hoy = LocalDate.now();
        
        // Si la fecha actual es posterior a la de vencimiento
        if (hoy.isAfter(prestamo.getFechaVencimiento())) {
            // Si el préstamo está aprobado o vencido, actualizar a EN_MORA
            if (prestamo.getEstado() == EstadoPrestamo.APROBADO || 
                prestamo.getEstado() == EstadoPrestamo.VENCIDO) {
                prestamo.setEstado(EstadoPrestamo.EN_MORA);
                // Si es la primera vez que entra en mora, establecer la fecha de inicio de mora como la fecha de vencimiento
                if (prestamo.getFechaUltimoCalculoMora() == null) {
                    prestamo.setFechaUltimoCalculoMora(prestamo.getFechaVencimiento());
                    // Calcular la mora acumulada hasta hoy
                    long diasMora = Math.max(0, ChronoUnit.DAYS.between(
                        prestamo.getFechaVencimiento(), 
                        hoy
                    ));
                    
                    if (diasMora > 0) {
                        // Calcular la mora acumulada
                        BigDecimal tasaDiaria = prestamo.getInteresMoratorio()
                                .divide(BigDecimal.valueOf(365 * 100), 10, RoundingMode.HALF_UP);
                        BigDecimal moraAcumulada = prestamo.getMonto()
                                .multiply(tasaDiaria)
                                .multiply(BigDecimal.valueOf(diasMora))
                                .setScale(2, RoundingMode.HALF_UP);
                                
                        prestamo.setMoraAcumulada(moraAcumulada);
                        prestamo.setDiasMora((int) diasMora);
                    }
                    
                    // Actualizar la fecha del último cálculo
                    prestamo.setFechaUltimoCalculoMora(hoy);
                }
            }
            
            // Calcular la mora solo si el préstamo está EN_MORA
            if (prestamo.getEstado() == EstadoPrestamo.EN_MORA) {
                LocalDate fechaReferencia = prestamo.getFechaUltimoCalculoMora() != null ?
                    prestamo.getFechaUltimoCalculoMora() : prestamo.getFechaVencimiento();
                    
                // Calcular días de mora desde la fecha de referencia
                long diasMora = Math.max(0, ChronoUnit.DAYS.between(fechaReferencia, hoy));
                
                if (diasMora > 0) {
                    // Calcular la mora diaria (10% anual = 0.0274% diario)
                    BigDecimal tasaDiaria = prestamo.getInteresMoratorio()
                            .divide(BigDecimal.valueOf(365 * 100), 10, RoundingMode.HALF_UP);
                            
                    // Calcular mora para el período actual
                    BigDecimal capitalPendiente = prestamo.getMonto();
                    if (prestamo.getPagos() != null && !prestamo.getPagos().isEmpty()) {
                        BigDecimal totalPagado = calcularTotalPagado(prestamo);
                        capitalPendiente = capitalPendiente.subtract(totalPagado).max(BigDecimal.ZERO);
                    }
                    
                    // Calcular mora diaria y multiplicar por días de mora
                    BigDecimal moraDiaria = calcularMoraDiaria(capitalPendiente, prestamo.getInteresMoratorio());
                    BigDecimal moraNueva = moraDiaria.multiply(BigDecimal.valueOf(diasMora))
                                                   .setScale(2, RoundingMode.HALF_UP);
                    
                    // Actualizar la mora acumulada
                    BigDecimal moraAcumulada = prestamo.getMoraAcumulada() != null ?
                            prestamo.getMoraAcumulada().add(moraNueva) : moraNueva;
                            
                    // Actualizar valores del préstamo
                    prestamo.setDiasMora((int) ChronoUnit.DAYS.between(prestamo.getFechaVencimiento(), hoy));
                    prestamo.setMoraAcumulada(moraAcumulada);
                    
                    // Calcular deuda total (capital + intereses + mora - pagos)
                    BigDecimal intereses = prestamo.getMonto()
                            .multiply(prestamo.getInteres().divide(BigDecimal.valueOf(100)))
                            .setScale(2, RoundingMode.HALF_UP);
                    
                    BigDecimal totalPagos = calcularTotalPagado(prestamo);
                    BigDecimal deudaTotal = prestamo.getMonto()
                            .add(intereses)
                            .add(moraAcumulada)
                            .subtract(totalPagos);
                            
                    prestamo.setDeudaRestante(deudaTotal.max(BigDecimal.ZERO));
                    prestamo.setFechaUltimoCalculoMora(hoy);
                    
                    // Guardar los cambios
                    prestamoRepository.save(prestamo);
                }
            }
        } else {
            // Si la fecha actual es anterior al vencimiento y el estado es EN_MORA, cambiar a APROBADO
            if (prestamo.getEstado() == EstadoPrestamo.EN_MORA) {
                prestamo.setEstado(EstadoPrestamo.APROBADO);
                prestamoRepository.save(prestamo);
            }
        }
    }
    
    /**
     * Calcula la mora diaria sobre el capital pendiente
     * @param capitalPendiente Monto sobre el que se calcula la mora
     * @param porcentajeMora Porcentaje de mora anual
     * @return Monto de mora diaria
     */
    private BigDecimal calcularMoraDiaria(BigDecimal capitalPendiente, BigDecimal porcentajeMora) {
        if (capitalPendiente.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // Tasa diaria = (tasa anual / 365 días)
        BigDecimal tasaDiaria = porcentajeMora
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                
        // Moratoria diaria = capital pendiente * tasa diaria
        return capitalPendiente.multiply(tasaDiaria)
                             .setScale(2, RoundingMode.HALF_UP);
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

    private void validarSaldoSuficiente(Cliente cliente, BigDecimal montoPrestamo) {
        if (cliente.getCuentas() == null || cliente.getCuentas().isEmpty()) {
            throw new IllegalStateException("El cliente no tiene cuentas asociadas");
        }
        
        // Tomamos la primera cuenta del cliente
        Cuenta cuenta = cliente.getCuentas().get(0);
        BigDecimal saldoActual = cuenta.getSaldo() != null ? cuenta.getSaldo() : BigDecimal.ZERO;
        
        if (saldoActual.compareTo(montoPrestamo) < 0) {
            throw new SaldoInsuficienteException(
                String.format("Saldo insuficiente. Saldo actual: %s, Monto solicitado: %s", 
                    saldoActual, montoPrestamo)
            );
        }
    }
    
    private void actualizarSaldoCuenta(Cliente cliente, BigDecimal monto) {
        if (cliente.getCuentas() == null || cliente.getCuentas().isEmpty()) {
            log.warn("No se pudo actualizar el saldo: el cliente no tiene cuentas asociadas");
            return;
        }
        
        // Tomamos la primera cuenta del cliente
        Cuenta cuenta = cliente.getCuentas().get(0);
        BigDecimal saldoActual = cuenta.getSaldo() != null ? cuenta.getSaldo() : BigDecimal.ZERO;
        BigDecimal nuevoSaldo = saldoActual.add(monto);
        
        cuenta.setSaldo(nuevoSaldo);
        log.info("Actualizado saldo de la cuenta {}: {}", cuenta.getNumeroCuenta(), nuevoSaldo);
    }
    
    private PrestamoModel convertirEntidadAModelo(Prestamo prestamo) {
        // Asegurarse de que los valores no sean nulos
        BigDecimal interesMoratorio = prestamo.getInteresMoratorio() != null
                ? prestamo.getInteresMoratorio()
                : BigDecimal.valueOf(10.00);
                
        BigDecimal moraAcumulada = prestamo.getMoraAcumulada() != null
                ? prestamo.getMoraAcumulada()
                : BigDecimal.ZERO;
                
        // Calcular intereses ordinarios
        BigDecimal interesesOrdinarios = prestamo.getMonto()
                .multiply(prestamo.getInteres())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                
        // Calcular total de pagos
        BigDecimal totalPagos = prestamo.getPagos() != null
                ? prestamo.getPagos().stream()
                        .map(Pago::getMonto)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
                
        // Calcular deuda total (capital + intereses + mora - pagos)
        BigDecimal totalDeuda = prestamo.getMonto()
                .add(interesesOrdinarios)
                .add(moraAcumulada)
                .subtract(totalPagos)
                .setScale(2, RoundingMode.HALF_UP);
                
        // Calcular mora diaria (mora diaria basada en la tasa anual)
        BigDecimal moraDiaria = BigDecimal.ZERO;
        if (prestamo.getEstado() == EstadoPrestamo.EN_MORA && 
            prestamo.getFechaVencimiento() != null) {
            
            // Calcular la tasa de interés moratorio diaria (10% anual = 0.0274% diario)
            BigDecimal tasaDiaria = prestamo.getInteresMoratorio()
                    .divide(BigDecimal.valueOf(365 * 100), 10, RoundingMode.HALF_UP);
            
            // Calcular la mora diaria
            moraDiaria = prestamo.getMonto()
                    .multiply(tasaDiaria)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Construir el objeto de fechas
        FechasModel fechas = FechasModel.builder()
                .creacion(LocalDate.from(prestamo.getFechaCreacion()))
                .vencimiento(prestamo.getFechaVencimiento())
                .diasMora(prestamo.getDiasMora())
                .build();

        // Construir el desglose de pago
        DesglosePagoModel desglosePago = DesglosePagoModel.builder()
                .capital(prestamo.getMonto())
                .interesOrdinario(interesesOrdinarios)
                .moraAcumulada(moraAcumulada)
                .totalDeuda(totalDeuda)
                .build();

        // Construir la información de pago diario
        LocalDate proximoVencimiento = prestamo.getFechaVencimiento() != null ?
                prestamo.getFechaVencimiento().plusMonths(1) : null;
                
        PagoDiarioModel pagoDiario = PagoDiarioModel.builder()
                .moraDiaria(moraDiaria)
                .proximoVencimiento(proximoVencimiento)
                .build();

        // Construir y retornar el modelo
        return PrestamoModel.builder()
                .id(prestamo.getId())
                .monto(prestamo.getMonto())
                .interes(prestamo.getInteres())
                .interesMoratorio(interesMoratorio)
                .deudaRestante(totalDeuda)  // Set deudaRestante to match totalDeuda
                .fechas(fechas)
                .estado(String.valueOf(prestamo.getEstado()))
                .clienteId(prestamo.getCliente().getId())
                .desglosePago(desglosePago)
                .pagoDiario(pagoDiario)
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
