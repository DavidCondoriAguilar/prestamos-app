package com.prestamosrapidos.prestamos_app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Email
    @Column(unique = true, nullable = false)
    private String correo;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Cuenta> cuentas;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Prestamo> prestamos;

    // Método para agregar una cuenta a la lista de cuentas
    public void addCuenta(Cuenta cuenta) {
        if (cuentas == null) {
            cuentas = new ArrayList<>();
        }
        cuentas.add(cuenta);
    }
}
