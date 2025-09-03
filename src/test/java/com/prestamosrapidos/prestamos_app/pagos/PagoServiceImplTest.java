package com.prestamosrapidos.prestamos_app.pagos;

import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.model.PagoModel;
import com.prestamosrapidos.prestamos_app.repository.PagoRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.serviceImpl.PagoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoService Unit Tests")
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private PrestamoRepository prestamoRepository;

    @InjectMocks
    private PagoServiceImpl pagoService;

    @Captor
    private ArgumentCaptor<Pago> pagoCaptor;

    private Prestamo prestamo;

    @BeforeEach
    void setUp() {
        // Initialize test prestamo with all required fields
        prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setMonto(new BigDecimal("1000.00"));
        prestamo.setInteres(new BigDecimal("10.00"));  // Added interest rate
        prestamo.setInteresMoratorio(new BigDecimal("5.00"));  // Added default mora interest
        prestamo.setEstado(EstadoPrestamo.APROBADO);
        prestamo.setFechaCreacion(LocalDate.now().atStartOfDay());
        prestamo.setFechaVencimiento(LocalDate.now().plusDays(30));
        prestamo.setDeudaRestante(new BigDecimal("1000.00"));  // Added initial debt
    }

    @Test
    @DisplayName("Should register a new payment successfully")
    void registrarPagoShouldSaveNewPago() {
        // Arrange
        PagoModel pagoModel = PagoModel.builder()
                .montoPago(new BigDecimal("100.00"))
                .fecha(LocalDate.now())  // Added date
                .prestamoId(1L)
                .build();

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> {
            Pago pago = invocation.getArgument(0);
            pago.setId(1L);
            return pago;
        });

        // Act
        PagoModel result = pagoService.registrarPago(pagoModel);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("100.00"), result.getMontoPago());
        assertEquals(1L, result.getPrestamoId());

        verify(pagoRepository).save(pagoCaptor.capture());
        Pago savedPago = pagoCaptor.getValue();
        assertEquals(new BigDecimal("100.00"), savedPago.getMonto());
        assertEquals(prestamo, savedPago.getPrestamo());
        assertNotNull(savedPago.getFecha());  // Ensure date is set
    }

    @Test
    @DisplayName("Should get all payments for a loan")
    void obtenerPagosPorPrestamoShouldReturnPagos() {
        // Arrange
        Pago pago1 = new Pago();
        pago1.setId(1L);
        pago1.setMonto(new BigDecimal("100.00"));
        pago1.setPrestamo(prestamo);
        pago1.setFecha(LocalDate.now());  // Added date

        Pago pago2 = new Pago();
        pago2.setId(2L);
        pago2.setMonto(new BigDecimal("200.00"));
        pago2.setPrestamo(prestamo);
        pago2.setFecha(LocalDate.now());  // Added date

        when(pagoRepository.findByPrestamoId(1L)).thenReturn(Arrays.asList(pago1, pago2));

        // Act
        List<PagoModel> result = pagoService.obtenerPagosPorPrestamo(1L);

        // Assert
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(new BigDecimal("100.00"), result.get(0).getMontoPago());
        assertEquals(2L, result.get(1).getId());
        assertEquals(new BigDecimal("200.00"), result.get(1).getMontoPago());
        assertNotNull(result.get(0).getFecha());  // Verify date is set
    }

    @Test
    @DisplayName("Should get paginated payments")
    void obtenerTodosLosPagosPaginadosShouldReturnPagedResults() {
        // Arrange
        Pago pago1 = new Pago();
        pago1.setId(1L);
        pago1.setMonto(new BigDecimal("100.00"));
        pago1.setPrestamo(prestamo);
        pago1.setFecha(LocalDate.now());  // Added date

        Pago pago2 = new Pago();
        pago2.setId(2L);
        pago2.setMonto(new BigDecimal("200.00"));
        pago2.setPrestamo(prestamo);
        pago2.setFecha(LocalDate.now());  // Added date

        Page<Pago> pagedResponse = new PageImpl<>(Arrays.asList(pago1, pago2));
        Pageable pageable = PageRequest.of(0, 10);

        when(pagoRepository.findAll(pageable)).thenReturn(pagedResponse);

        // Act
        Page<PagoModel> result = pagoService.obtenerTodosLosPagosPaginados(pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());
        assertNotNull(result.getContent().get(0).getFecha());  // Verify date is set
    }

    @Test
    @DisplayName("Should calculate remaining amount for a loan")
    void calcularMontoRestanteShouldReturnCorrectAmount() {
        // Arrange
        prestamo.setMonto(new BigDecimal("1000.00"));
        prestamo.setInteres(new BigDecimal("10.00")); // 10% interest
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(pagoRepository.calcularTotalPagado(1L)).thenReturn(300.0);

        // Calculate expected: 1000 + (1000 * 0.10) - 300 = 1000 + 100 - 300 = 800
        BigDecimal expected = new BigDecimal("800.00");

        // Act
        BigDecimal result = pagoService.calcularMontoRestante(1L);

        // Assert
        assertEquals(0, expected.compareTo(result),
                "Expected: " + expected + " but was: " + result);
    }

    @Test
    @DisplayName("Should update loan status to PAID when full amount is paid")
    void verificarYActualizarEstadoShouldUpdateStatusToPaid() {
        // Arrange
        prestamo.setMonto(new BigDecimal("1000.00"));
        prestamo.setInteres(new BigDecimal("10.00")); // 10% interest
        // Total to pay = 1000 + (1000 * 10%) = 1100.00
        when(pagoRepository.calcularTotalPagado(1L)).thenReturn(1100.0);

        // Act
        pagoService.verificarYActualizarEstado(prestamo);

        // Assert
        assertEquals(EstadoPrestamo.PAGADO, prestamo.getEstado(), 
            "Loan status should be updated to PAGADO when full amount (including interest) is paid");
        verify(prestamoRepository).save(prestamo);
    }

    @Test
    @DisplayName("Should update loan status to IN_MORA when payment is late")
    void verificarYActualizarEstadoShouldUpdateStatusToInMora() {
        // Arrange
        prestamo.setMonto(new BigDecimal("1000.00"));
        prestamo.setFechaVencimiento(LocalDate.now().minusDays(1));
        when(pagoRepository.calcularTotalPagado(1L)).thenReturn(500.0);

        // Act
        pagoService.verificarYActualizarEstado(prestamo);

        // Assert
        assertEquals(EstadoPrestamo.EN_MORA, prestamo.getEstado());
        verify(prestamoRepository).save(prestamo);
    }

    @Test
    @DisplayName("Should delete a payment by ID")
    void eliminarPagoShouldDeletePayment() {
        // Arrange
        Pago pago = new Pago();
        pago.setId(1L);
        pago.setPrestamo(prestamo);
        pago.setFecha(LocalDate.now());  // Added date
        pago.setMonto(new BigDecimal("100.00"));  // Added amount

        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));
        doNothing().when(pagoRepository).delete(any(Pago.class));

        // Act
        pagoService.eliminarPago(1L);

        // Assert
        verify(pagoRepository).delete(pago);
    }
}