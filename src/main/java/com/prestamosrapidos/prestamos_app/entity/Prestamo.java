package com.prestamosrapidos.prestamos_app.entity;

import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private BigDecimal monto; // Cambio a BigDecimal

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interes; // Cambio a BigDecimal

    @Column(nullable = false)
    private LocalDate fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPrestamo estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<Pago> pagos;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDate.now();
        }
    }
}
