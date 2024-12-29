package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.model.CuentaModel;

public interface CuentaService {
    CuentaModel crearCuenta(CuentaModel cuentaModel);
    CuentaModel obtenerCuentaPorClienteId(Long clienteId);
    void eliminarCuenta(Long id);
}
