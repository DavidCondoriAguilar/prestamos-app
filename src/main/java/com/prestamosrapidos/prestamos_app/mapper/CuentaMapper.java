package com.prestamosrapidos.prestamos_app.mapper;

import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.model.CuentaModel;

public class CuentaMapper {

    public static CuentaModel toModel(Cuenta cuenta) {
        if (cuenta == null) return null;

        return CuentaModel.builder()
                .id(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .build();
    }

    public static Cuenta toEntity(CuentaModel cuentaModel) {
        if (cuentaModel == null) return null;

        return Cuenta.builder()
                .numeroCuenta(cuentaModel.getNumeroCuenta())
                .saldo(cuentaModel.getSaldo())
                .build();
    }
}
