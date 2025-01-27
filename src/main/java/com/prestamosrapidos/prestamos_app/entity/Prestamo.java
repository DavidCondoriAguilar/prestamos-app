package com.prestamosrapidos.prestamos_app.entity;

import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "prestamos",
        indexes = {
                @Index(name = "idx_cliente_id", columnList = "cliente_id"),
                @Index(name = "idx_estado", columnList = "estado")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Prestamo {

    @Id
    @SequenceGenerator(name = "prestamo_seq", sequenceName = "prestamo_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prestamo_seq")
    private Long id;

    @NotNull
    @Digits(integer = 19, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @NotNull
    @Digits(integer = 5, fraction = 2)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interes;

    @NotNull
    @PastOrPresent
    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Future
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private EstadoPrestamo estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @NotNull
    private Cliente cliente;

    @Builder.Default
    @Digits(integer = 5, fraction = 2)
    @Column(name = "interes_moratorio", nullable = false)
    private BigDecimal interesMoratorio = BigDecimal.valueOf(10.00); // 10% por defecto

    @PastOrPresent
    private LocalDate fechaUltimoInteres;

    @Column(name = "interes_moratorio_aplicado", nullable = false)
    private Boolean interesMoratorioAplicado = false;

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Pago> pagos = new ArrayList<>();

   /* @LastModifiedDate
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;*/

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaUltimoInteres = LocalDate.now();
    }

    public void addPago(Pago pago) {
        if (pagos == null) {
            pagos = new ArrayList<>();
        }
        pagos.add(pago);
        pago.setPrestamo(this);
    }
}
