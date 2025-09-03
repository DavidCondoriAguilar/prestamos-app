package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.model.FechasModel;
import com.prestamosrapidos.prestamos_app.exception.ClienteNotFoundException;
import com.prestamosrapidos.prestamos_app.model.*;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.CuentaRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.ClienteService;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import com.prestamosrapidos.prestamos_app.validation.ClienteValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio para la gestión de clientes en el sistema de préstamos.
 * Proporciona operaciones CRUD para clientes, incluyendo la gestión de sus cuentas y préstamos asociados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final PrestamoRepository prestamoRepository;

    private final PrestamoService prestamoService;
    private final ClienteValidator clienteValidator;

    /**
     * Crea un nuevo cliente en el sistema con sus datos básicos y opcionalmente una cuenta asociada.
     * Valida los datos del cliente y de la cuenta antes de guardarlos.
     *
     * @param clienteModel DTO con los datos del cliente a crear
     * @return ClienteModel El cliente creado con su ID generado
     * @throws IllegalArgumentException Si los datos del cliente o cuenta no son válidos
     * @throws RuntimeException Si ocurre un error al guardar en la base de datos
     */
    @Transactional
    @Override
    public ClienteModel crearCliente(ClienteModel clienteModel) {
        clienteValidator.validateClienteModel(clienteModel);
        
        // Ensure saldo is properly converted to BigDecimal
        if (clienteModel.getCuenta() != null && clienteModel.getCuenta().getSaldo() != null) {
            try {
                BigDecimal saldo;
                Object saldoObj = clienteModel.getCuenta().getSaldo();
                
                if (saldoObj instanceof String) {
                    saldo = new BigDecimal((String) saldoObj);
                } else if (saldoObj instanceof Number) {
                    saldo = BigDecimal.valueOf(((Number) saldoObj).doubleValue());
                } else if (saldoObj instanceof BigDecimal) {
                    saldo = (BigDecimal) saldoObj;
                } else {
                    throw new IllegalArgumentException("Formato de saldo no válido");
                }
                clienteModel.getCuenta().setSaldo(saldo);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El saldo debe ser un número válido: " + e.getMessage());
            }
        }
        
        clienteValidator.validateCuentaModel(clienteModel);

        Cliente cliente = Cliente.builder()
                .nombre(clienteModel.getNombre().trim())
                .correo(clienteModel.getCorreo().trim().toLowerCase())
                .build();

        if (clienteModel.getCuenta() != null) {
            String numeroCuenta = clienteModel.getCuenta().getNumeroCuenta();
            log.debug("Verificando número de cuenta: {}", numeroCuenta);
            
            // Verificar si el número de cuenta tiene el formato correcto
            if (numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
                log.warn("Número de cuenta inválido: {}", numeroCuenta);
                throw new IllegalArgumentException("El número de cuenta no puede ser nulo o vacío");
            }
            
            // Verificar si el número de cuenta ya existe
            boolean existeCuenta = cuentaRepository.existsByNumeroCuenta(numeroCuenta);
            log.debug("Cuenta existe en la base de datos: {}", existeCuenta);
            
            if (existeCuenta) {
                log.warn("Número de cuenta ya existe: {}", numeroCuenta);
                throw new IllegalArgumentException("El número de cuenta ya está en uso: "
                        + numeroCuenta);
            }

            // First save the cliente to get an ID
            cliente = clienteRepository.save(cliente);
            
            // Then create and save the cuenta with the persisted cliente
            Cuenta cuenta = convertirACuenta(clienteModel.getCuenta(), cliente);
            cuenta = cuentaRepository.save(cuenta);
            
            // Add the saved cuenta to the cliente's list of cuentas
            cliente.addCuenta(cuenta);
        }

        try {
            cliente = clienteRepository.save(cliente);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el cliente en la base de datos", e);
        }

        return convertirAClienteModel(cliente);
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param id ID del cliente a actualizar
     * @param clienteModel DTO con los nuevos datos del cliente
     * @return ClienteModel El cliente actualizado
     * @throws ClienteNotFoundException Si no se encuentra el cliente con el ID especificado
     */
    @Override
    public ClienteModel actualizarCliente(Long id, ClienteModel clienteModel) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException("Cliente no encontrado con ID: " + id));

        // Actualizar datos básicos del cliente
        clienteExistente.setNombre(clienteModel.getNombre());
        clienteExistente.setCorreo(clienteModel.getCorreo());

        if (clienteModel.getCuenta() != null) {
            Cuenta cuenta = convertirACuenta(clienteModel.getCuenta(), clienteExistente);

            if (!clienteExistente.getCuentas().contains(cuenta)) {
                clienteExistente.addCuenta(cuenta);
            }
        }

        // Guardar el cliente con las cuentas actualizadas
        Cliente clienteActualizado = clienteRepository.save(clienteExistente);
        return convertirAClienteModel(clienteActualizado);
    }

    /**
     * Obtiene un cliente por su ID, incluyendo sus cuentas y préstamos asociados.
     *
     * @param id ID del cliente a buscar
     * @return ClienteModel Los datos del cliente con sus relaciones
     * @throws ClienteNotFoundException Si no se encuentra el cliente con el ID especificado
     */
    @Override
    public ClienteModel obtenerClientePorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException("Cliente no encontrado con ID: " + id));
        return convertirAClienteModel(cliente);
    }

    /**
     * Obtiene una lista de todos los clientes registrados en el sistema.
     * Incluye información básica de cada cliente, sus cuentas y préstamos asociados.
     *
     * @return List<ClienteModel> Lista de todos los clientes
     */
    @Override
    public List<ClienteModel> obtenerTodosLosClientes() {
        return clienteRepository.findAll()
                .stream()
                .map(this::convertirAClienteModel)
                .collect(Collectors.toList());
    }

    /**
     * Elimina un cliente del sistema por su ID.
     * No se puede eliminar un cliente que tenga préstamos activos.
     *
     * @param id ID del cliente a eliminar
     * @throws ClienteNotFoundException Si no se encuentra el cliente con el ID especificado
     * @throws IllegalStateException Si el cliente tiene préstamos activos
     */
    @Override
    public void eliminarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException("Cliente no encontrado con ID: " + id));
        if (!cliente.getPrestamos().isEmpty()) {
            throw new IllegalStateException("No se puede eliminar un cliente con préstamos activos");
        }
        // Luego eliminar el cliente
        clienteRepository.delete(cliente);
    }


    /**
     * Convierte una entidad Cliente a su correspondiente DTO ClienteModel.
     * Incluye la conversión de cuentas y préstamos asociados.
     *
     * @param cliente Entidad Cliente a convertir
     * @return ClienteModel DTO con los datos del cliente
     */
    private ClienteModel convertirAClienteModel(Cliente cliente) {
        return ClienteModel.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .correo(cliente.getCorreo())
                .cuenta(cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()
                        ? convertirACuentaModel(cliente.getCuentas().getFirst())
                        : null)
                .prestamos(cliente.getPrestamos() != null
                        ? cliente.getPrestamos().stream()
                        .map(prestamo -> {
                            BigDecimal deudaRestante = prestamoService.calcularMontoRestante(prestamo.getId());
                            PrestamoModel prestamoModel = convertirAPrestamoModel(prestamo);
                            prestamoModel.setDeudaRestante(deudaRestante);
                            return prestamoModel;
                        })
                        .collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }


    /**
     * Convierte una entidad Cuenta a su correspondiente DTO CuentaModel.
     *
     * @param cuenta Entidad Cuenta a convertir
     * @return CuentaModel DTO con los datos de la cuenta, o null si la cuenta es nula
     */
    private CuentaModel convertirACuentaModel(Cuenta cuenta) {
        if (cuenta == null) return null;
        return CuentaModel.builder()
                .id(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .clienteId(cuenta.getId())
                .build();
    }

    /**
     * Convierte una entidad Prestamo a su correspondiente DTO PrestamoModel.
     * Incluye el cálculo de intereses, moras y desglose de pagos.
     *
     * @param prestamo Entidad Prestamo a convertir
     * @return PrestamoModel DTO con los datos del préstamo
     */
    private PrestamoModel convertirAPrestamoModel(Prestamo prestamo) {
        // Construir el objeto Fechas
        FechasModel fechas = FechasModel.builder()
                .creacion(LocalDate.from(prestamo.getFechaCreacion()))
                .vencimiento(prestamo.getFechaVencimiento())
                .diasMora(prestamo.getDiasMora())
                .build();
        
        // Calcular el pago diario (monto * (1 + interes/100) / 30)
        BigDecimal montoDiario = prestamo.getMonto()
                .multiply(BigDecimal.ONE.add(prestamo.getInteres().divide(BigDecimal.valueOf(100))))
                .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        
        // Crear el objeto PagoDiario
        PagoDiarioModel pagoDiario = PagoDiarioModel.builder()
                .moraDiaria(prestamo.getInteresMoratorio() != null ? 
                        prestamo.getMonto().multiply(prestamo.getInteresMoratorio())
                                .divide(BigDecimal.valueOf(100 * 30), 2, RoundingMode.HALF_UP) : 
                        BigDecimal.ZERO)
                .proximoVencimiento(prestamo.getFechaVencimiento() != null ? 
                        prestamo.getFechaVencimiento() : 
                        LocalDate.now().plusDays(30))
                .build();
        
        // Crear el desglose de pago
        BigDecimal interesOrdinario = prestamo.getMonto()
                .multiply(prestamo.getInteres())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                
        BigDecimal moraAcumulada = prestamo.getMoraAcumulada() != null ? 
                prestamo.getMoraAcumulada() : 
                BigDecimal.ZERO;
                
        BigDecimal total = prestamo.getMonto()
                .add(interesOrdinario)
                .add(moraAcumulada);
        
        DesglosePagoModel desglosePago = DesglosePagoModel.builder()
                .capital(prestamo.getMonto())
                .interesOrdinario(interesOrdinario)
                .moraAcumulada(moraAcumulada)
                .totalDeuda(total)
                .build();
                
        return PrestamoModel.builder()
                .id(prestamo.getId())
                .monto(prestamo.getMonto())
                .interes(prestamo.getInteres())
                .interesMoratorio(prestamo.getInteresMoratorio())
                .fechas(fechas)
                .estado(String.valueOf(prestamo.getEstado()))
                .clienteId(prestamo.getCliente().getId())
                .pagos(prestamo.getPagos() != null
                        ? prestamo.getPagos().stream().map(this::convertirPagoAModelo).collect(Collectors.toList())
                        : new ArrayList<>())
                .pagoDiario(pagoDiario)
                .desglosePago(desglosePago)
                .build();
    }

    /**
     * Convierte una entidad Pago a su correspondiente DTO PagoModel.
     *
     * @param pago Entidad Pago a convertir
     * @return PagoModel DTO con los datos del pago
     */
    private PagoModel convertirPagoAModelo(Pago pago) {
        return PagoModel.builder()
                .id(pago.getId())
                .montoPago(pago.getMonto())
                .fecha(pago.getFecha())
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }

    /**
     * Convierte un DTO CuentaModel a su correspondiente entidad Cuenta.
     * Realiza validaciones sobre los datos de la cuenta.
     *
     * @param cuentaModel DTO CuentaModel a convertir
     * @param cliente Cliente al que pertenece la cuenta
     * @return Cuenta Entidad de cuenta creada
     * @throws IllegalArgumentException Si los datos de la cuenta no son válidos
     */
    private Cuenta convertirACuenta(CuentaModel cuentaModel, Cliente cliente) {
        if (cuentaModel == null) {
            return null;
        }

        // Validar que el saldo sea positivo
        BigDecimal saldo = cuentaModel.getSaldo();
        log.debug("Saldo recibido: {}", saldo);
        
        if (saldo == null) {
            log.warn("El saldo es nulo");
            throw new IllegalArgumentException("El saldo inicial debe ser mayor a cero");
        }
        
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("El saldo es menor o igual a cero: {}", saldo);
            throw new IllegalArgumentException("El saldo inicial debe ser mayor a cero");
        }
        
        // Asegurar que el saldo tenga exactamente 2 decimales
        saldo = saldo.setScale(2, RoundingMode.HALF_UP);
        log.debug("Saldo validado: {}", saldo);
        
        // Verificar que el saldo no sea demasiado grande
        if (saldo.compareTo(new BigDecimal("999999999999.99")) > 0) {
            log.warn("El saldo excede el límite máximo permitido: {}", saldo);
            throw new IllegalArgumentException("El saldo excede el límite máximo permitido");
        }

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(cuentaModel.getNumeroCuenta())
                .saldo(cuentaModel.getSaldo())
                .cliente(cliente)
                .build();

        return cuenta;
    }
}
