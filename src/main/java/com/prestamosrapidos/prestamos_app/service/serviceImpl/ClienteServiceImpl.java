package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.exception.ClienteNotFoundException;
import com.prestamosrapidos.prestamos_app.model.ClienteModel;
import com.prestamosrapidos.prestamos_app.model.CuentaModel;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final PrestamoRepository prestamoRepository;

    private final PrestamoService prestamoService;
    private final ClienteValidator clienteValidator;

    @Transactional
    @Override
    public ClienteModel crearCliente(ClienteModel clienteModel) {
        clienteValidator.validateClienteModel(clienteModel);
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

            // Convertir la cuenta y asociarla al cliente
            Cuenta cuenta = convertirACuenta(clienteModel.getCuenta(), cliente);

            cuenta = cuentaRepository.save(cuenta);

            cliente.addCuenta(cuenta);
        }

        try {
            cliente = clienteRepository.save(cliente);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el cliente en la base de datos", e);
        }

        return convertirAClienteModel(cliente);
    }

    @Override
    public ClienteModel actualizarCliente(Long id, ClienteModel clienteModel) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException("Cliente no encontrado con ID: " + id));

        // Actualizar datos básicos del cliente
        clienteExistente.setNombre(clienteModel.getNombre());
        clienteExistente.setCorreo(clienteModel.getCorreo());

        if (clienteModel.getCuenta() != null) {
            Cuenta cuenta = convertirACuenta(clienteModel.getCuenta(), clienteExistente);

            // Verifica si la lista de cuentas del cliente no contiene la cuenta específica
            if (!clienteExistente.getCuentas().contains(cuenta)) {
                clienteExistente.addCuenta(cuenta);
            }
        }

        // Guardar el cliente con las cuentas actualizadas
        Cliente clienteActualizado = clienteRepository.save(clienteExistente);
        return convertirAClienteModel(clienteActualizado);
    }

    @Override
    public ClienteModel obtenerClientePorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException("Cliente no encontrado con ID: " + id));
        return convertirAClienteModel(cliente);
    }

    @Override
    public List<ClienteModel> obtenerTodosLosClientes() {
        return clienteRepository.findAll()
                .stream()
                .map(this::convertirAClienteModel)
                .collect(Collectors.toList());
    }

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
                            double deudaRestante = prestamoService.calcularMontoRestante(prestamo.getId());
                            PrestamoModel prestamoModel = convertirAPrestamoModel(prestamo);
                            prestamoModel.setDeudaRestante(deudaRestante);
                            return prestamoModel;
                        })
                        .collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }


    private CuentaModel convertirACuentaModel(Cuenta cuenta) {
        if (cuenta == null) return null;
        return CuentaModel.builder()
                .id(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .clienteId(cuenta.getId())
                .build();
    }

    private PrestamoModel convertirAPrestamoModel(Prestamo prestamo) {
        return PrestamoModel.builder()
                .id(prestamo.getId())
                .monto(prestamo.getMonto())
                .interes(prestamo.getInteres())
                .interesMoratorio(prestamo.getInteresMoratorio())
                .fechaCreacion(LocalDate.from(prestamo.getFechaCreacion()))
                .fechaVencimiento(prestamo.getFechaVencimiento())
                .estado(String.valueOf(prestamo.getEstado()))
                .clienteId(prestamo.getCliente().getId())
                .pagos(prestamo.getPagos() != null
                        ? prestamo.getPagos().stream().map(this::convertirPagoAModelo).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }

    private PagoModel convertirPagoAModelo(Pago pago) {
        return PagoModel.builder()
                .id(pago.getId())
                .montoPago(pago.getMonto())
                .fecha(pago.getFecha())
                .prestamoId(pago.getPrestamo().getId())
                .build();
    }

    private Cuenta convertirACuenta(CuentaModel cuentaModel, Cliente cliente) {
        if (cuentaModel == null) {
            return null;
        }

        // Validar que el saldo sea positivo
        Double saldoDouble = cuentaModel.getSaldo();
        log.debug("Saldo recibido: {}", saldoDouble);
        
        if (saldoDouble == null) {
            log.warn("El saldo es nulo");
            throw new IllegalArgumentException("El saldo inicial debe ser mayor a cero");
        }
        
        if (saldoDouble <= 0) {
            log.warn("El saldo es menor o igual a cero: {}", saldoDouble);
            throw new IllegalArgumentException("El saldo inicial debe ser mayor a cero");
        }
        
        // Convertir a BigDecimal con 2 decimales
        BigDecimal saldo = BigDecimal.valueOf(saldoDouble).setScale(2, RoundingMode.HALF_UP);
        log.debug("Saldo convertido a BigDecimal: {}", saldo);
        
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
