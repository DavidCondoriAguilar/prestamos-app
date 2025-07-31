package com.prestamosrapidos.prestamos_app.service.serviceImpl;

import com.prestamosrapidos.prestamos_app.model.AuthRequest;
import com.prestamosrapidos.prestamos_app.model.AuthResponse;
import com.prestamosrapidos.prestamos_app.model.RegisterRequest;
import com.prestamosrapidos.prestamos_app.entity.Usuario;
import com.prestamosrapidos.prestamos_app.entity.enums.Rol;
import com.prestamosrapidos.prestamos_app.repository.UsuarioRepository;
import com.prestamosrapidos.prestamos_app.security.JwtTokenProvider;
import com.prestamosrapidos.prestamos_app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        // Autenticar al usuario
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        // Establecer la autenticación en el contexto de seguridad
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generar token JWT
        String jwt = tokenProvider.generateToken(authentication);
        
        // Obtener información del usuario para incluir en la respuesta
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
        return AuthResponse.builder()
            .token(jwt)
            .username(usuario.getUsername())
            .rol(usuario.getRol().name())
            .nombre(usuario.getNombre())
            .apellidos(usuario.getApellidos())
            .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Verificar si el nombre de usuario ya existe
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }

        // Verificar si el correo electrónico ya existe
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El correo electrónico ya está en uso");
        }

        // Crear nuevo usuario
        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .telefono(request.getTelefono())
                .rol(Rol.valueOf(request.getRol()))
                .estado(Usuario.EstadoUsuario.ACTIVO) // Set default estado
                .build();

        // Guardar el usuario en la base de datos
        usuarioRepository.save(usuario);

        // Autenticar al usuario recién registrado
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        // Generar token JWT
        String jwt = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
            .token(jwt)
            .username(usuario.getUsername())
            .rol(usuario.getRol().name())
            .nombre(usuario.getNombre())
            .apellidos(usuario.getApellidos())
            .build();
    }
}
