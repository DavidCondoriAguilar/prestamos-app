package com.prestamosrapidos.prestamos_app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = true)
    private LocalDate fecha;

    @ManyToOne
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    // Método para establecer la fecha automáticamente
    @PrePersist
    public void prePersist() {
        if (this.fecha == null) {
            this.fecha = LocalDate.now();
        }
    }
}
