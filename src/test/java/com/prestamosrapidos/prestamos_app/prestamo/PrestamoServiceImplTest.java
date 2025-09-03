package com.prestamosrapidos.prestamos_app.prestamo;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.exception.RecursoNoEncontradoException;
import com.prestamosrapidos.prestamos_app.exception.SaldoInsuficienteException;
import com.prestamosrapidos.prestamos_app.model.*;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.serviceImpl.PrestamoServiceImpl;
import com.prestamosrapidos.prestamos_app.validation.PrestamoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PrestamoServiceImplTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private PrestamoServiceImpl prestamoService;

    private Prestamo prestamo;
    private Cliente cliente;
    private Cuenta cuenta;
    private PrestamoModel prestamoModel;

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
        cuenta.setSaldo(new BigDecimal("10000.00"));
        cuenta.setCliente(cliente);

        cliente.setCuentas(List.of(cuenta));

        prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setMonto(new BigDecimal("5000.00"));
        prestamo.setInteres(new BigDecimal("10.00"));
        prestamo.setInteresMoratorio(new BigDecimal("5.00"));
        prestamo.setFechaCreacion(LocalDate.now().atStartOfDay());
        prestamo.setFechaVencimiento(LocalDate.now().plusDays(30));
        prestamo.setEstado(EstadoPrestamo.APROBADO);
        prestamo.setCliente(cliente);

        FechasModel fechas = new FechasModel();
        fechas.setCreacion(LocalDate.now());
        fechas.setVencimiento(LocalDate.now().plusDays(30));

        prestamoModel = PrestamoModel.builder()
                .id(1L)
                .monto(new BigDecimal("5000.00"))
                .interes(new BigDecimal("10.00"))
                .interesMoratorio(new BigDecimal("5.00"))
                .fechas(fechas)
                .estado("APROBADO")
                .clienteId(1L)
                .build();
    }

    @Test
    void crearPrestamoWithValidDataShouldCreatePrestamo() {
        // Arrange
        when(clienteRepository.findById(anyLong())).thenReturn(Optional.of(cliente));
        when(prestamoRepository.save(any(Prestamo.class))).thenReturn(prestamo);

        // Act
        PrestamoModel result = prestamoService.crearPrestamo(prestamoModel);

        // Assert
        assertNotNull(result);
        assertEquals(prestamoModel.getMonto(), result.getMonto());
        verify(clienteRepository, times(1)).findById(anyLong());
        verify(prestamoRepository, times(1)).save(any(Prestamo.class));
    }

    @Test
    void crearPrestamoWithNonExistentClienteShouldThrowException() {
        // Arrange
        when(clienteRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            prestamoService.crearPrestamo(prestamoModel);
        });
    }

    @Test
    void crearPrestamoWithInsufficientBalanceShouldThrowException() {
        // Arrange
        prestamoModel.setMonto(new BigDecimal("20000.00")); // More than account balance
        when(clienteRepository.findById(anyLong())).thenReturn(Optional.of(cliente));

        // Act & Assert
        assertThrows(SaldoInsuficienteException.class, () -> {
            prestamoService.crearPrestamo(prestamoModel);
        });
    }

    @Test
    void obtenerPrestamoPorIdWithValidIdShouldReturnPrestamo() {
        // Arrange
        when(prestamoRepository.findById(anyLong())).thenReturn(Optional.of(prestamo));

        // Act
        PrestamoModel result = prestamoService.obtenerPrestamoPorId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(prestamo.getId(), result.getId());
        verify(prestamoRepository, times(2)).findById(anyLong());
    }

    @Test
    void obtenerPrestamoPorIdWithInvalidIdShouldThrowException() {
        // Arrange
        when(prestamoRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNoEncontradoException.class, () -> {
            prestamoService.obtenerPrestamoPorId(999L);
        });
    }

    @Test
    void actualizarPrestamoWithValidDataShouldUpdatePrestamo() {
        // Arrange
        when(prestamoRepository.findById(anyLong())).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.save(any(Prestamo.class))).thenReturn(prestamo);

        // Update some values
        prestamoModel.setMonto(new BigDecimal("6000.00"));
        prestamoModel.setInteres(new BigDecimal("12.00"));

        // Act
        PrestamoModel result = prestamoService.actualizarPrestamo(1L, prestamoModel);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("6000.00"), result.getMonto());
        verify(prestamoRepository, times(1)).findById(anyLong());
        verify(prestamoRepository, times(1)).save(any(Prestamo.class));
    }

    @Test
    void eliminarPrestamoWithValidIdShouldDeletePrestamo() {
        // Arrange
        when(prestamoRepository.findById(anyLong())).thenReturn(Optional.of(prestamo));
        doNothing().when(prestamoRepository).delete(any(Prestamo.class));

        // Act
        prestamoService.eliminarPrestamo(1L);

        // Assert
        verify(prestamoRepository, times(1)).delete(any(Prestamo.class));
    }

    @Test
    void calcularInteresTotalShouldReturnCorrectValue() {
        // Arrange
        when(prestamoRepository.findById(anyLong())).thenReturn(Optional.of(prestamo));

        // Act
        BigDecimal interesTotal = prestamoService.calcularInteresTotal(1L);

        // Assert
        assertEquals(new BigDecimal("5500.00"), interesTotal); // 5000 + (5000 * 10%)
    }

    @Test
    void obtenerPrestamosPorClienteShouldReturnPrestamosList() {
        // Arrange
        when(prestamoRepository.findByClienteId(anyLong())).thenReturn(Arrays.asList(prestamo));

        // Act
        List<PrestamoModel> result = prestamoService.obtenerPrestamosPorCliente(1L);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(prestamo.getId(), result.get(0).getId());
    }

    @Test
    void verificarYActualizarEstadoWithPaidLoanShouldUpdateToPagado() {
        // Arrange
        prestamo.setEstado(EstadoPrestamo.APROBADO);
        when(prestamoRepository.findById(anyLong())).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.save(any(Prestamo.class))).thenReturn(prestamo);

        // Act
        PrestamoModel result = prestamoService.actualizarEstado(1L, new EstadoModel("PAGADO"));

        // Assert
        assertEquals("PAGADO", result.getEstado());
    }

    @Test
    void verificarYActualizarMoraWithVencidoLoanShouldUpdateMora() {
        // Arrange
        prestamo.setEstado(EstadoPrestamo.APROBADO);
        prestamo.setFechaVencimiento(LocalDate.now().minusDays(5));
        when(prestamoRepository.findById(anyLong())).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.save(any(Prestamo.class))).thenReturn(prestamo);

        // Act
        PrestamoModel result = prestamoService.obtenerPrestamoPorId(1L);

        // Assert
        assertNotNull(result);
        // Additional assertions for mora calculation can be added
    }
}