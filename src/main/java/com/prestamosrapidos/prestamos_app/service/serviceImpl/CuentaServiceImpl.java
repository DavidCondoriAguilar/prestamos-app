package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.exception.ClienteNotFoundException;
import com.prestamosrapidos.prestamos_app.exception.CuentaNotFoundException;
import com.prestamosrapidos.prestamos_app.model.CuentaModel;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.CuentaRepository;
import com.prestamosrapidos.prestamos_app.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;

    @Transactional
    @Override
    public CuentaModel crearCuenta(CuentaModel cuentaModel) {

        if (cuentaRepository.existsByClienteId(cuentaModel.getClienteId())) {
            throw new IllegalArgumentException("El cliente con ID " + cuentaModel.getClienteId() + " ya tiene una cuenta asociada.");
        }

        if (cuentaRepository.existsByNumeroCuenta(cuentaModel.getNumeroCuenta())) {
            throw new CuentaNotFoundException("El número de cuenta ya está en uso: " + cuentaModel.getNumeroCuenta());
        }

        if (cuentaModel.getSaldo().compareTo(new BigDecimal("1.00")) < 0) {
            throw new IllegalArgumentException("El saldo debe ser mayor o igual a 1.00");
        }

        if (!cuentaModel.getNumeroCuenta().matches("\\d{10}")) {
            throw new IllegalArgumentException("El número de cuenta debe tener 10 dígitos numéricos");
        }

        Cliente cliente = clienteRepository.findById(cuentaModel.getClienteId())
                .orElseThrow(() -> new ClienteNotFoundException("Cliente no encontrado con ID: " + cuentaModel.getClienteId()));

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(cuentaModel.getNumeroCuenta())
                .saldo(cuentaModel.getSaldo())
                .cliente(cliente)
                .build();

        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        // Si queremos que el cliente tenga una referencia a la cuenta, agrega la cuenta a la lista de cuentas del cliente
        cliente.addCuenta(cuenta);
        clienteRepository.save(cliente);

        return convertirACuentaModel(cuentaGuardada);
    }


    @Override
    @Transactional(readOnly = true)
    public CuentaModel obtenerCuentaPorClienteId(Long clienteId) {
        // Buscar la cuenta del cliente
        Cuenta cuenta = cuentaRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new CuentaNotFoundException("Cuenta no encontrada para el cliente con ID: " + clienteId));

        return convertirACuentaModel(cuenta);
    }

    @Override
    @Transactional
    public void eliminarCuenta(Long id) {
        // Verificar si la cuenta existe antes de eliminar
        if (!cuentaRepository.existsById(id)) {
            throw new CuentaNotFoundException("No se encontró la cuenta con ID: " + id);
        }

        // Eliminar la cuenta
        cuentaRepository.deleteById(id);
    }

    // Método para convertir una entidad Cuenta a un modelo CuentaModel
    private CuentaModel convertirACuentaModel(Cuenta cuenta) {
        return CuentaModel.builder()
                .id(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .clienteId(cuenta.getCliente() !=  null ? cuenta.getCliente().getId() : null)
                .build();
    }
}
