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
import com.prestamosrapidos.prestamos_app.repository.PagoRepository;
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

/**
 * Implementación del servicio para la gestión de préstamos en el sistema.
 * Proporciona operaciones CRUD para préstamos, cálculo de intereses, moras y gestión de pagos.
 * También incluye tareas programadas para el cálculo automático de intereses moratorios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrestamoServiceImpl implements PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final ClienteRepository clienteRepository;
    private final PagoRepository pagoRepository;

    /**
     * Crea un nuevo préstamo en el sistema con los datos proporcionados.
     * Valida los datos del préstamo, verifica el saldo del cliente y actualiza su cuenta.
     *
     * @param prestamoModel DTO con los datos del préstamo a crear
     * @return PrestamoModel El préstamo creado con su ID generado
     * @throws NoSuchElementException Si no se encuentra el cliente especificado
     * @throws IllegalArgumentException Si los datos del préstamo son inválidos
     * @throws SaldoInsuficienteException Si el cliente no tiene saldo suficiente
     */
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

    /**
     * Actualiza los datos de un préstamo existente.
     * Permite modificar el monto, intereses, fechas y estado del préstamo.
     *
     * @param id ID del préstamo a actualizar
     * @param prestamoModel DTO con los nuevos datos del préstamo
     * @return PrestamoModel El préstamo actualizado
     * @throws RecursoNoEncontradoException Si no se encuentra el préstamo con el ID especificado
     * @throws IllegalArgumentException Si los datos proporcionados son inválidos
     */
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

    /**
     * Actualiza el estado de un préstamo existente.
     * Realiza validaciones específicas según el nuevo estado a asignar.
     *
     * @param id ID del préstamo a actualizar
     * @param nuevoEstado Nuevo estado a asignar al préstamo
     * @return PrestamoModel El préstamo con el estado actualizado
     * @throws RecursoNoEncontradoException Si no se encuentra el préstamo
     * @throws IllegalArgumentException Si el cambio de estado no es válido
     */
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

    /**
     * Obtiene un préstamo por su ID, incluyendo información actualizada de mora.
     * Actualiza automáticamente el cálculo de mora antes de devolver el préstamo.
     *
     * @param id ID del préstamo a buscar
     * @return PrestamoModel El préstamo encontrado con información actualizada
     * @throws RecursoNoEncontradoException Si no se encuentra el préstamo
     */
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
    
    /**
     * Obtiene una lista de todos los préstamos en el sistema.
     * Incluye la actualización de moras para cada préstamo.
     *
     * @return List<PrestamoModel> Lista de todos los préstamos
     */
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

    /**
     * Obtiene todos los préstamos asociados a un cliente específico.
     * Actualiza el cálculo de moras para cada préstamo del cliente.
     *
     * @param clienteId ID del cliente del cual se desean obtener los préstamos
     * @return List<PrestamoModel> Lista de préstamos del cliente
     */
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

    /**
     * Obtiene los préstamos que coinciden con un estado específico.
     * Actualiza el cálculo de moras para cada préstamo devuelto.
     *
     * @param estado Estado por el cual filtrar los préstamos
     * @return List<PrestamoModel> Lista de préstamos que coinciden con el estado
     */
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

    /**
     * Elimina un préstamo del sistema por su ID.
     * No realiza validaciones adicionales sobre el estado del préstamo.
     *
     * @param id ID del préstamo a eliminar
     * @throws RecursoNoEncontradoException Si no se encuentra el préstamo
     */
    @Override
    public void eliminarPrestamo(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
        prestamoRepository.delete(prestamo);
    }

    /**
     * Calcula el interés total a pagar por un préstamo.
     * Incluye el monto del préstamo más los intereses calculados.
     *
     * @param prestamoId ID del préstamo para el cálculo
     * @return BigDecimal Monto total a pagar (capital + intereses)
     * @throws RecursoNoEncontradoException Si no se encuentra el préstamo
     */
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

    /**
     * Calcula el monto restante por pagar de un préstamo.
     * Incluye capital, intereses y moras, restando los pagos realizados.
     * Aplica automáticamente intereses moratorios si el préstamo está vencido.
     *
     * @param prestamoId ID del préstamo para el cálculo
     * @return BigDecimal Monto restante por pagar
     * @throws RecursoNoEncontradoException Si no se encuentra el préstamo
     */
    @Override
    public BigDecimal calcularMontoRestante(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));

        // Calculate total amount (principal + interest)
        BigDecimal montoTotal = prestamo.getMonto()
                .add(prestamo.getMonto().multiply(prestamo.getInteres())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

        // Get total paid amount from repository
        Double totalPagado = pagoRepository.calcularTotalPagado(prestamoId);
        BigDecimal montoPagado = totalPagado != null ? BigDecimal.valueOf(totalPagado) : BigDecimal.ZERO;

        // Calculate remaining amount
        BigDecimal saldoPendiente = montoTotal.subtract(montoPagado);

        // Apply late interest if the loan is overdue
        LocalDate hoy = LocalDate.now();
        if (prestamo.getFechaVencimiento() != null
                && hoy.isAfter(prestamo.getFechaVencimiento())
                && !prestamo.getEstado().equals(EstadoPrestamo.PAGADO)) {
            // Calculate days late
            long diasMora = ChronoUnit.DAYS.between(prestamo.getFechaVencimiento(), hoy);
            if (diasMora > 0) {
                // Calculate daily interest (0.1% = 0.001)
                BigDecimal interesDiario = saldoPendiente.multiply(BigDecimal.valueOf(0.001));
                BigDecimal interesMoratorio = interesDiario.multiply(BigDecimal.valueOf(diasMora));
                saldoPendiente = saldoPendiente.add(interesMoratorio);
            }
        }

        return saldoPendiente.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Verifica y actualiza el estado de un préstamo según su fecha de vencimiento y pagos.
     * Puede cambiar el estado a PAGADO, VENCIDO o PENDIENTE según corresponda.
     *
     * @param prestamo Préstamo a verificar y actualizar
     */
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

    /**
     * Verifica y actualiza la mora de un préstamo según su estado y fechas de vencimiento.
     * Calcula intereses moratorios diarios y actualiza el saldo pendiente.
     *
     * @param prestamo Préstamo a verificar y actualizar
     */
    @Transactional
    public void verificarYActualizarMora(Prestamo prestamo) {
        if (prestamo.getEstado() == EstadoPrestamo.PAGADO ||
                prestamo.getFechaVencimiento() == null) {
            return; // No hay nada que hacer
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaReferencia = prestamo.getFechaUltimoCalculoMora() != null ?
                prestamo.getFechaUltimoCalculoMora() : prestamo.getFechaVencimiento();

        // Si la fecha actual es posterior a la fecha de referencia
        if (hoy.isAfter(fechaReferencia)) {
            // Calcular días de mora desde la fecha de referencia
            long diasMora = ChronoUnit.DAYS.between(fechaReferencia, hoy);

            if (diasMora > 0) {
                // Calcular capital pendiente
                BigDecimal capitalPendiente = prestamo.getMonto();
                if (prestamo.getPagos() != null && !prestamo.getPagos().isEmpty()) {
                    BigDecimal totalPagado = prestamo.getPagos().stream()
                            .map(Pago::getMonto)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    capitalPendiente = capitalPendiente.subtract(totalPagado).max(BigDecimal.ZERO);
                }

                // Calcular mora diaria (0.1% del capital pendiente)
                BigDecimal moraDiaria = capitalPendiente
                        .multiply(new BigDecimal("0.001"))
                        .setScale(2, RoundingMode.HALF_UP);

                // Calcular mora total para los días transcurridos
                BigDecimal moraNueva = moraDiaria.multiply(BigDecimal.valueOf(diasMora))
                        .setScale(2, RoundingMode.HALF_UP);

                // Actualizar el préstamo
                int totalDiasMora = prestamo.getDiasMora() + (int) diasMora;
                BigDecimal moraTotal = prestamo.getMoraAcumulada() != null ?
                        prestamo.getMoraAcumulada().add(moraNueva) : moraNueva;

                prestamo.setDiasMora(totalDiasMora);
                prestamo.setMoraAcumulada(moraTotal);
                prestamo.setEstado(EstadoPrestamo.EN_MORA);
                prestamo.setFechaUltimoCalculoMora(hoy);

                // Guardar los cambios
                prestamoRepository.save(prestamo);
            }
        } else if (hoy.isBefore(prestamo.getFechaVencimiento()) &&
                prestamo.getEstado() == EstadoPrestamo.EN_MORA) {
            // Si la fecha actual es anterior al vencimiento y el estado es EN_MORA, cambiar a APROBADO
            prestamo.setEstado(EstadoPrestamo.APROBADO);
            prestamoRepository.save(prestamo);
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

    /**
     * Tarea programada que se ejecuta diariamente a medianoche.
     * Aplica intereses moratorios a los préstamos vencidos no pagados.
     * Actualiza el estado de los préstamos según corresponda.
     */
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

    /**
     * Valida que el cliente tenga saldo suficiente para realizar una operación.
     *
     * @param cliente Cliente a validar
     * @param montoPrestamo Monto a verificar
     * @throws IllegalStateException Si el cliente no tiene cuentas asociadas
     * @throws SaldoInsuficienteException Si el saldo es insuficiente
     */
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
    
    /**
     * Actualiza el saldo de la cuenta principal de un cliente.
     * Registra la operación en el log de la aplicación.
     *
     * @param cliente Cliente cuya cuenta se actualizará
     * @param monto Monto a sumar (puede ser negativo para restar)
     */
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
    
    /**
     * Convierte una entidad Prestamo a su correspondiente DTO PrestamoModel.
     * Incluye el cálculo de intereses, moras y desglose de pagos.
     *
     * @param prestamo Entidad Prestamo a convertir
     * @return PrestamoModel DTO con los datos del préstamo
     */
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
