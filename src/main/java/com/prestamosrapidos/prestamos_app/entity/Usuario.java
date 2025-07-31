package com.prestamosrapidos.prestamos_app.entity;

import com.prestamosrapidos.prestamos_app.entity.enums.Rol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
})
@Where(clause = "estado != 'ELIMINADO'")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoUsuario estado;

    @Column(name = "fecha_ultimo_acceso")
    private LocalDateTime fechaUltimoAcceso;

    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "fecha_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    @UpdateTimestamp
    private LocalDateTime fechaActualizacion;

    // Campos para verificación de correo electrónico
    @Column(name = "email_verificado")
    private boolean emailVerificado = false;

    @Column(name = "token_verificacion", length = 64)
    private String tokenVerificacion;

    // Campos para recuperación de contraseña
    @Column(name = "token_reset_password", length = 64)
    private String tokenResetPassword;

    @Column(name = "fecha_expiracion_reset")
    private LocalDateTime fechaExpiracionReset;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Set.of(new SimpleGrantedAuthority(rol.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return estado != EstadoUsuario.ELIMINADO;
    }

    @Override
    public boolean isAccountNonLocked() {
        return estado != EstadoUsuario.BLOQUEADO;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return estado == EstadoUsuario.ACTIVO || estado == EstadoUsuario.PENDIENTE_VERIFICACION;
    }

    // Métodos de ayuda
    public void incrementarIntentosFallidos() {
        this.intentosFallidos++;
        if (this.intentosFallidos >= 5) {
            this.estado = EstadoUsuario.BLOQUEADO;
            this.fechaBloqueo = LocalDateTime.now();
        }
    }

    public void resetearIntentosFallidos() {
        this.intentosFallidos = 0;
    }

    public void marcarEmailComoVerificado() {
        this.emailVerificado = true;
        this.estado = EstadoUsuario.ACTIVO;
        this.tokenVerificacion = null;
    }

    public void prepararParaEliminacion() {
        this.estado = EstadoUsuario.ELIMINADO;
        this.email = "deleted_" + this.id + "_" + this.email;
        this.username = "deleted_" + this.id + "_" + this.username;
    }



    public enum EstadoUsuario {
        ACTIVO,
        PENDIENTE_VERIFICACION,
        BLOQUEADO,
        ELIMINADO
    }
}
