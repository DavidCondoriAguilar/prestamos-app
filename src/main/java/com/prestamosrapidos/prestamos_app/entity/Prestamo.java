package com.prestamosrapidos.prestamos_app.entity;

import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestamos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interes;

    @Column(nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPrestamo estado;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pago> pagos = new ArrayList<>();


    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDate.now();
        }
    }

    public void addPago(Pago pago) {
        if (pagos == null) {
            pagos = new ArrayList<>();
        }
        pagos.add(pago);
        pago.setPrestamo(this); // Establece la relación bidireccional
    }

}
