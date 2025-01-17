package com.prestamosrapidos.prestamos_app.validation;

import com.prestamosrapidos.prestamos_app.model.ClienteModel;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.CuentaRepository;
import org.springframework.stereotype.Component;

@Component
public class ClienteValidator {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;

    public ClienteValidator(ClienteRepository clienteRepository, CuentaRepository cuentaRepository) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
    }

    public void validateClienteModel(ClienteModel clienteModel) {
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

    }

    public void validateCuentaModel(ClienteModel clienteModel) {
        if (clienteModel.getCuenta() != null && cuentaRepository.existsByNumeroCuenta(clienteModel.getCuenta().getNumeroCuenta())) {
            throw new IllegalArgumentException("El número de cuenta ya está en uso: "
                    + clienteModel.getCuenta().getNumeroCuenta());
        }
    }
}
