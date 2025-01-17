package com.prestamosrapidos.prestamos_app.mapper;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.model.ClienteModel;
import com.prestamosrapidos.prestamos_app.model.CuentaModel;

public class ClienteMapper {

    public static ClienteModel toModel(Cliente cliente) {
        if (cliente == null) return null;

        return ClienteModel.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .correo(cliente.getCorreo())
                .cuenta(cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()
                        ? CuentaMapper.toModel(cliente.getCuentas().getFirst())
                        : null)
                .build();
    }

    public static Cliente toEntity(ClienteModel clienteModel) {
        if (clienteModel == null) return null;

        return Cliente.builder()
                .nombre(clienteModel.getNombre())
                .correo(clienteModel.getCorreo())
                .build();
    }
}
