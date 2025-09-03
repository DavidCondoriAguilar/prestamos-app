package com.prestamosrapidos.prestamos_app.cliente;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.exception.ClienteNotFoundException;
import com.prestamosrapidos.prestamos_app.model.ClienteModel;
import com.prestamosrapidos.prestamos_app.model.CuentaModel;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.CuentaRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import com.prestamosrapidos.prestamos_app.service.serviceImpl.ClienteServiceImpl;
import com.prestamosrapidos.prestamos_app.validation.ClienteValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private PrestamoService prestamoService;

    @Mock
    private ClienteValidator clienteValidator;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Cliente cliente;
    private ClienteModel clienteModel;
    private Cuenta cuenta;
    private CuentaModel cuentaModel;

    @BeforeEach
    void setUp() {
        // Setup test data
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Juan Perez");
        cliente.setCorreo("juan@example.com");

        cuenta = new Cuenta();
        cuenta.setId(1L);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setSaldo(new BigDecimal("1000.00"));
        cuenta.setCliente(cliente);

        cliente.setCuentas(Collections.singletonList(cuenta));

        cuentaModel = CuentaModel.builder()
                .id(1L)
                .numeroCuenta("1234567890")
                .saldo(new BigDecimal("1000.00"))
                .clienteId(1L)
                .build();

        clienteModel = ClienteModel.builder()
                .id(1L)
                .nombre("Juan Perez")
                .correo("juan@example.com")
                .cuenta(cuentaModel)
                .build();
    }

    @Test
    void crearClienteWithValidDataReturnsClienteModel() {
        // Arrange
        Cliente clienteGuardado = new Cliente();
        clienteGuardado.setId(1L);
        clienteGuardado.setNombre("Juan Perez");
        clienteGuardado.setCorreo("juan@example.com");
        clienteGuardado.setCuentas(new ArrayList<>());

        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(1L); // Simulamos que se asigna un ID al guardar
            return cliente;
        });

        // Act
        ClienteModel result = clienteService.crearCliente(clienteModel);

        // Assert
        assertNotNull(result);
        assertEquals(clienteModel.getNombre(), result.getNombre());
        assertEquals(clienteModel.getCorreo(), result.getCorreo());
        verify(cuentaRepository, times(1)).existsByNumeroCuenta(anyString());
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
        verify(clienteRepository, times(2)).save(any(Cliente.class)); // Se esperan 2 llamadas
    }

    @Test
    void actualizarClienteWithValidIdReturnsUpdatedClienteModel() {
        // Arrange
        Long clienteId = 1L;
        ClienteModel updatedModel = ClienteModel.builder()
                .nombre("Juan Carlos Perez")
                .correo("juan.carlos@example.com")
                .build();

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        // Act
        ClienteModel result = clienteService.actualizarCliente(clienteId, updatedModel);

        // Assert
        assertNotNull(result);
        assertEquals(updatedModel.getNombre(), result.getNombre());
        assertEquals(updatedModel.getCorreo(), result.getCorreo());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void actualizarClienteWithInvalidIdThrowsClienteNotFoundException() {
        // Arrange
        Long invalidId = 999L;
        when(clienteRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ClienteNotFoundException.class, () -> {
            clienteService.actualizarCliente(invalidId, clienteModel);
        });
    }

    @Test
    void obtenerClientePorIdWithValidIdReturnsClienteModel() {
        // Arrange
        Long clienteId = 1L;
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        // Eliminamos el mock innecesario de prestamoService.calcularMontoRestante

        // Act
        ClienteModel result = clienteService.obtenerClientePorId(clienteId);

        // Assert
        assertNotNull(result);
        assertEquals(cliente.getNombre(), result.getNombre());
        assertEquals(cliente.getCorreo(), result.getCorreo());
        verify(clienteRepository).findById(clienteId);
    }

    @Test
    void obtenerClientePorIdWithInvalidIdThrowsClienteNotFoundException() {
        // Arrange
        Long invalidId = 999L;
        when(clienteRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ClienteNotFoundException.class, () -> {
            clienteService.obtenerClientePorId(invalidId);
        });
    }

    @Test
    void obtenerTodosLosClientesReturnsListOfClientes() {
        // Arrange
        when(clienteRepository.findAll()).thenReturn(Collections.singletonList(cliente));

        // Act
        List<ClienteModel> result = clienteService.obtenerTodosLosClientes();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(cliente.getNombre(), result.get(0).getNombre());
    }

    @Test
    void eliminarClienteWithNoActiveLoansDeletesCliente() {
        // Arrange
        Long clienteId = 1L;
        cliente.setPrestamos(Collections.emptyList()); // No active loans
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

        // Act
        clienteService.eliminarCliente(clienteId);

        // Assert
        verify(clienteRepository, times(1)).delete(cliente);
    }

    @Test
    void eliminarClienteWithActiveLoansThrowsIllegalStateException() {
        // Arrange
        Long clienteId = 1L;
        Prestamo prestamoActivo = new Prestamo();
        cliente.setPrestamos(Collections.singletonList(prestamoActivo)); // Has active loan
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            clienteService.eliminarCliente(clienteId);
        });
        verify(clienteRepository, never()).delete(any());
    }
}