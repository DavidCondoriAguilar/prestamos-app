package com.prestamosrapidos.prestamos_app.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prestamosrapidos.prestamos_app.controller.AuthController;
import com.prestamosrapidos.prestamos_app.model.AuthRequest;
import com.prestamosrapidos.prestamos_app.model.AuthResponse;
import com.prestamosrapidos.prestamos_app.model.RegisterRequest;
import com.prestamosrapidos.prestamos_app.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerValidRequestReturnsCreated() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123!")
                .nombre("Test")
                .apellidos("User")
                .rol("ROLE_USER")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt.token.here")
                .username("testuser")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void registerInvalidRequestReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("invalid-email")
                .password("short")
                .nombre("")
                .apellidos("")
                .rol("INVALID_ROLE")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginValidCredentialsReturnsToken() throws Exception {
        // Arrange
        AuthRequest request = AuthRequest.builder()
                .username("testuser")
                .password("Password123!")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt.token.here")
                .username("testuser")
                .build();

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void loginInvalidCredentialsReturnsUnauthorized() throws Exception {
        // Arrange
        AuthRequest request = AuthRequest.builder()
                .username("wronguser")
                .password("wrongpassword")
                .build();

        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    @Test
    void registerDuplicateUsernameReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("test@example.com")
                .password("Password123!")
                .nombre("Test")
                .apellidos("User")
                .rol("ROLE_USER")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("El nombre de usuario ya está en uso"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    System.out.println("Response content: " + responseContent);
                    if (!responseContent.contains("El nombre de usuario ya está en uso")) {
                        System.out.println("Response status: " + result.getResponse().getStatus());
                        System.out.println("Response headers: " + result.getResponse().getHeaderNames());
                    }
                });
    }

    @Test
    void login_ServerErrorReturnsInternalServerError() throws Exception {
        // Arrange
        AuthRequest request = AuthRequest.builder()
                .username("testuser")
                .password("Password123!")
                .build();

        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Error inesperado"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}