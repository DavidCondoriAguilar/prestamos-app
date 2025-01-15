package com.prestamosrapidos.prestamos_app.repository;

import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    boolean existsByNumeroCuenta(String numeroCuenta);
    Optional<Cuenta> findByClienteId(Long clienteId);
    boolean existsByClienteId(Long clienteId);

}
