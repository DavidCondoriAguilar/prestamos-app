package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.entity.Usuario;
import com.prestamosrapidos.prestamos_app.entity.enums.Rol;
import com.prestamosrapidos.prestamos_app.model.AuthRequest;
import com.prestamosrapidos.prestamos_app.model.AuthResponse;
import com.prestamosrapidos.prestamos_app.model.RegisterRequest;
import com.prestamosrapidos.prestamos_app.repository.UsuarioRepository;
import com.prestamosrapidos.prestamos_app.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private com.prestamosrapidos.prestamos_app.service.serviceImpl.AuthServiceImpl authService;

    private final String testToken = "test.jwt.token";
    private final String testUsername = "testuser";
    private final String testPassword = "Test123!";
    private final String testEmail = "test@example.com";
    private final String testName = "Test";
    private final String testLastName = "User";

    @BeforeEach
    void setUp() {
        // Common test data setup
    }

    @Test
    void authenticateValidCredentialsReturnsAuthResponse() {
        // Arrange
        AuthRequest request = new AuthRequest(testUsername, testPassword);
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = User.builder()
                .username(testUsername)
                .password(testPassword)
                .authorities("ROLE_USER")
                .build();

        Usuario usuario = Usuario.builder()
                .username(testUsername)
                .email(testEmail)
                .nombre(testName)
                .apellidos(testLastName)
                .rol(Rol.ROLE_USER)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(usuarioRepository.findByUsername(testUsername)).thenReturn(Optional.of(usuario));
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);

        // Act
        AuthResponse response = authService.authenticate(request);

        // Assert
        assertNotNull(response);
        assertEquals(testToken, response.getToken());
        assertEquals(testUsername, response.getUsername());
        assertEquals("ROLE_USER", response.getRol());
        assertEquals(testName, response.getNombre());
        assertEquals(testLastName, response.getApellidos());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(usuarioRepository).findByUsername(testUsername);
        verify(tokenProvider).generateToken(authentication);
    }

    @Test
    void authenticateInvalidCredentialsThrowsBadCredentials() {
        // Arrange
        AuthRequest request = new AuthRequest("wronguser", "wrongpass");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(request));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void registerNewUserReturnsAuthResponse() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username(testUsername)
                .email(testEmail)
                .password(testPassword)
                .nombre(testName)
                .apellidos(testLastName)
                .rol("ROLE_USER")
                .build();

        when(usuarioRepository.existsByUsername(testUsername)).thenReturn(false);
        when(usuarioRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals(testToken, response.getToken());
        assertEquals(testUsername, response.getUsername());
        assertEquals("ROLE_USER", response.getRol());
        assertEquals(testName, response.getNombre());
        assertEquals(testLastName, response.getApellidos());

        verify(usuarioRepository).existsByUsername(testUsername);
        verify(usuarioRepository).existsByEmail(testEmail);
        verify(passwordEncoder).encode(testPassword);
        verify(usuarioRepository).save(any(Usuario.class));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(authentication);
    }

    @Test
    void registerUsernameAlreadyExistsThrowsRuntimeException() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username(testUsername)
                .email(testEmail)
                .password(testPassword)
                .nombre(testName)
                .apellidos(testLastName)
                .rol("ROLE_USER")
                .build();

        when(usuarioRepository.existsByUsername(testUsername)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.register(request));
        
        assertEquals("El nombre de usuario ya está en uso", exception.getMessage());
        verify(usuarioRepository).existsByUsername(testUsername);
        verifyNoMoreInteractions(usuarioRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void registerEmailAlreadyExistsThrowsRuntimeException() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username(testUsername)
                .email(testEmail)
                .password(testPassword)
                .nombre(testName)
                .apellidos(testLastName)
                .rol("ROLE_USER")
                .build();

        when(usuarioRepository.existsByUsername(testUsername)).thenReturn(false);
        when(usuarioRepository.existsByEmail(testEmail)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.register(request));
        
        assertEquals("El correo electrónico ya está en uso", exception.getMessage());
        verify(usuarioRepository).existsByUsername(testUsername);
        verify(usuarioRepository).existsByEmail(testEmail);
        verifyNoMoreInteractions(usuarioRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(tokenProvider);
    }

    }
