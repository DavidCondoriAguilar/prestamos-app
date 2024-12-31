package com.prestamosrapidos.prestamos_app.entity;

import jakarta.persistence.*;
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

    @Column(unique = true, nullable = false)
    private String correo;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cuenta> cuentas;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prestamo> prestamos;  // Un cliente puede tener varios préstamos

    // Método para agregar una cuenta a la lista de cuentas
    public void addCuenta(Cuenta cuenta) {
        if (cuentas == null) {
            cuentas = new ArrayList<>();
        }
        cuentas.add(cuenta);
    }
}
