package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.exception.ClienteNotFoundException;
import com.prestamosrapidos.prestamos_app.model.ClienteModel;
import com.prestamosrapidos.prestamos_app.model.CuentaModel;
import com.prestamosrapidos.prestamos_app.model.PrestamoModel;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.CuentaRepository;
import com.prestamosrapidos.prestamos_app.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    @Autowired
    private CuentaRepository cuentaRepository;

    @Override
    public ClienteModel crearCliente(ClienteModel clienteModel) {
        if (clienteModel == null) {
            throw new IllegalArgumentException("El modelo de cliente no puede ser nulo");
        }
        if (clienteModel.getNombre() == null || clienteModel.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente no puede ser nulo o vacío");
        }
        if (clienteModel.getCorreo() == null || clienteModel.getCorreo().isBlank()) {
            throw new IllegalArgumentException("El correo del cliente no puede ser nulo o vacío");
        }
        if (clienteRepository.existsByCorreo(clienteModel.getCorreo())) {
            throw new IllegalArgumentException("El correo ya está en uso: " + clienteModel.getCorreo());
        }

        // Crear entidad Cliente a partir del modelo
        Cliente cliente = Cliente.builder()
                .nombre(clienteModel.getNombre().trim())
                .correo(clienteModel.getCorreo().trim().toLowerCase())
                .build();

        // Validar si el número de cuenta ya existe
        if (clienteModel.getCuenta() != null) {
            if (cuentaRepository.existsByNumeroCuenta(clienteModel.getCuenta().getNumeroCuenta())) {
                throw new IllegalArgumentException("El número de cuenta ya está en uso: "
                            + clienteModel.getCuenta().getNumeroCuenta());
            }

            // Convertir la cuenta y asociarla al cliente
            Cuenta cuenta = convertirACuenta(clienteModel.getCuenta(), cliente);

            // Primero, guardar la cuenta si no ha sido guardada
            cuenta = cuentaRepository.save(cuenta);

            // Ahora asociar la cuenta al cliente
            cliente.addCuenta(cuenta);  // Usamos el método addCuenta para añadir la cuenta
        }

        try {
            // Guardar cliente en la base de datos
            cliente = clienteRepository.save(cliente);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el cliente en la base de datos", e);
        }

        // Convertir la entidad Cliente a modelo y devolverla
        return convertirAClienteModel(cliente);
    }


    @Override
    public ClienteModel actualizarCliente(Long id, ClienteModel clienteModel) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException("Cliente no encontrado con ID: " + id));

        clienteExistente.setNombre(clienteModel.getNombre());
        clienteExistente.setCorreo(clienteModel.getCorreo());

        if (clienteModel.getCuenta() != null) {
            // Convertir CuentaModel a Cuenta y asociarla al cliente
            Cuenta cuenta = convertirACuenta(clienteModel.getCuenta(), clienteExistente);

            // Si el cliente ya tiene cuentas, se puede agregar la nueva cuenta
            clienteExistente.addCuenta(cuenta);
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
        if (!clienteRepository.existsById(id)) {
            throw new ClienteNotFoundException("Cliente no encontrado con ID: " + id);
        }
        clienteRepository.deleteById(id);
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
                        ? cliente.getPrestamos().stream().map(this::convertirAPrestamoModel).collect(Collectors.toList())
                        : null)
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
                .fechaCreacion(prestamo.getFechaCreacion())
                .estado(String.valueOf(prestamo.getEstado()))
                .clienteId(prestamo.getCliente() != null ? prestamo.getCliente().getId() : null)
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
