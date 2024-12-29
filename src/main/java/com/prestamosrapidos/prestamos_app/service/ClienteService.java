package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.model.ClienteModel;

import java.util.List;
public interface ClienteService {
    ClienteModel crearCliente(ClienteModel clienteModel);
    ClienteModel actualizarCliente(Long id, ClienteModel clienteModel);
    ClienteModel obtenerClientePorId(Long id);
    List<ClienteModel> obtenerTodosLosClientes();
    void eliminarCliente(Long id);
}
