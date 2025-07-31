package com.prestamosrapidos.prestamos_app.entity;

import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestamos",
        indexes = {
                @Index(name = "idx_prestamo_cliente_id", columnList = "cliente_id"),
                @Index(name = "idx_prestamo_estado", columnList = "estado")
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deuda_restante", nullable = false)
    private BigDecimal deudaRestante = BigDecimal.ZERO;

    @NotNull
    @Digits(integer = 19, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @NotNull
    @NotNull
    @Digits(integer = 5, fraction = 2)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interes;

    @Builder.Default
    @Digits(integer = 5, fraction = 2)
    @Column(name = "interes_moratorio", nullable = false)
    private BigDecimal interesMoratorio = BigDecimal.valueOf(10.00); // 10% por defecto

    @Column(name = "interes_moratorio_aplicado", nullable = false)
    private Boolean interesMoratorioAplicado = false;

    @NotNull
    @PastOrPresent
    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private EstadoPrestamo estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_prestamo_cliente"))
    @NotNull
    private Cliente cliente;

    @PastOrPresent
    private LocalDate fechaUltimoInteres;

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Pago> pagos = new ArrayList<>();

    @Column(precision = 15, scale = 2)
    private BigDecimal saldoMoratorio = BigDecimal.ZERO;

    @Column(name = "mora_aplicada", nullable = false)
    private boolean moraAplicada = false;

    @Column
    private LocalDate fechaUltimaMora;
    
    @Column(name = "dias_mora", nullable = false)
    private Integer diasMora = 0;
    
    protected void validarFechasYCalcularMora() {
        if (fechaCreacion == null || fechaVencimiento == null) {
            return; // No se puede validar si falta alguna fecha
        }

        // Convertir ambas fechas a LocalDate para comparación consistente
        LocalDate fechaCreacionLocalDate = fechaCreacion.toLocalDate();
        LocalDate hoy = LocalDate.now();
        
        // Si la fecha de vencimiento es hoy o en el futuro, no hay mora
        if (!hoy.isAfter(fechaVencimiento)) {
            this.diasMora = 0;
            if (this.estado == EstadoPrestamo.EN_MORA) {
                this.estado = EstadoPrestamo.APROBADO;
            }
            return;
        }
        
        // Si llegamos aquí, el préstamo está vencido
        this.diasMora = (int) java.time.temporal.ChronoUnit.DAYS.between(fechaVencimiento, hoy);
        this.estado = EstadoPrestamo.EN_MORA;
    }
    
    @Column(name = "mora_acumulada", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal moraAcumulada = BigDecimal.ZERO;
    
    @Column(name = "fecha_ultimo_calculo_mora")
    private LocalDate fechaUltimoCalculoMora;

    @CreatedDate
    @Column(name = "fecha_creacion_auditoria", nullable = false, updatable = false)
    private LocalDateTime fechaCreacionAuditoria;

    @LastModifiedDate
    @Column(name = "fecha_modificacion_auditoria", nullable = false)
    private LocalDateTime fechaModificacionAuditoria;
    
    @CreatedBy
    @Column(name = "creado_por", updatable = false, nullable = false)
    private String creadoPor;
    
    @LastModifiedBy
    @Column(name = "modificado_por", nullable = false)
    private String modificadoPor;

   /* @LastModifiedDate
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;*/

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        validarFechasYCalcularMora();
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaUltimoInteres = LocalDate.now();
        validarFechasYCalcularMora();
    }

    public void addPago(Pago pago) {
        if (pagos == null) {
            pagos = new ArrayList<>();
        }
        pagos.add(pago);
        pago.setPrestamo(this);
    }
}
