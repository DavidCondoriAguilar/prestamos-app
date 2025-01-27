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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final PrestamoRepository prestamoRepository;

    private final PrestamoService prestamoService;
    private final ClienteValidator clienteValidator;

    @Override
    public ClienteModel crearCliente(ClienteModel clienteModel) {
        clienteValidator.validateClienteModel(clienteModel);
        clienteValidator.validateCuentaModel(clienteModel);

        Cliente cliente = Cliente.builder()
                .nombre(clienteModel.getNombre().trim())
                .correo(clienteModel.getCorreo().trim().toLowerCase())
                .build();

        if (clienteModel.getCuenta() != null) {
            if (cuentaRepository.existsByNumeroCuenta(clienteModel.getCuenta().getNumeroCuenta())) {
                throw new IllegalArgumentException("El número de cuenta ya está en uso: "
                        + clienteModel.getCuenta().getNumeroCuenta());
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

            // Si el cliente ya tiene cuentas, no duplicar
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
/*

        for (Cuenta cuenta : cliente.getCuentas()) {
            cuentaRepository.delete(cuenta);
        }

        for (Prestamo prestamo : cliente.getPrestamos()) {
            prestamoRepository.delete(prestamo);
        }
*/

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

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(cuentaModel.getNumeroCuenta())
                .saldo(cuentaModel.getSaldo())
                .cliente(cliente)
                .build();

        return cuenta;
    }
}
