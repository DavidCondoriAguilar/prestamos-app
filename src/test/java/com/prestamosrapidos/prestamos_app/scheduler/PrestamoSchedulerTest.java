package com.prestamosrapidos.prestamos_app.scheduler;

import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.PrestamoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PrestamoScheduler} class.
 * Tests cover the main functionality including mora calculation, status updates,
 * and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrestamoScheduler Unit Tests")
class PrestamoSchedulerTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private PrestamoService prestamoService;

    @InjectMocks
    private PrestamoScheduler prestamoScheduler;

    @Captor
    private ArgumentCaptor<Prestamo> prestamoCaptor;

    private static final BigDecimal DEFAULT_PORCENTAJE_MORA = new BigDecimal("0.1");
    private static final int DEFAULT_DIAS_GRACIA = 0;

    @BeforeEach
    void setUp() {
        // Initialize default values
        ReflectionTestUtils.setField(prestamoScheduler, "porcentajeMoraDiario", DEFAULT_PORCENTAJE_MORA);
        ReflectionTestUtils.setField(prestamoScheduler, "diasGracia", DEFAULT_DIAS_GRACIA);
        ReflectionTestUtils.setField(prestamoScheduler, "moraHabilitada", true);
    }

    @Test
    @DisplayName("Should not process any loans when mora calculation is disabled")
    void calcularInteresMoratorioDiarioWhenMoraDisabledShouldNotProcessAnyLoans() {
        // Arrange
        ReflectionTestUtils.setField(prestamoScheduler, "moraHabilitada", false);

        // Act
        prestamoScheduler.calcularInteresMoratorioDiario();

        // Assert
        verifyNoInteractions(prestamoRepository);
        verifyNoInteractions(prestamoService);
    }

    @Test
    @DisplayName("Should update overdue loan status to VENCIDO")
    void actualizarPrestamosVencidosWithApprovedLoanShouldUpdateToVencido() {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Prestamo prestamo = crearPrestamo(1L, EstadoPrestamo.APROBADO, yesterday, null);
        
        when(prestamoRepository.findAprobadosVencidos(any())).thenReturn(List.of(prestamo));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        prestamoScheduler.calcularInteresMoratorioDiario();

        // Assert
        verify(prestamoRepository).save(prestamoCaptor.capture());
        Prestamo updatedPrestamo = prestamoCaptor.getValue();
        
        assertEquals(EstadoPrestamo.VENCIDO, updatedPrestamo.getEstado());
        assertNotNull(updatedPrestamo.getFechaUltimoCalculoMora());
    }

    @Test
    @DisplayName("Should calculate daily mora for overdue loan")
    void calcularMoraParaPrestamoWithOverdueLoanShouldCalculateMora() throws Exception {
        // Arrange
        LocalDate fiveDaysAgo = LocalDate.now().minusDays(5);
        Prestamo prestamo = crearPrestamo(1L, EstadoPrestamo.VENCIDO, fiveDaysAgo, fiveDaysAgo);
        prestamo.setMonto(new BigDecimal("1000.00"));
        
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Use reflection to access private method
        var method = PrestamoScheduler.class.getDeclaredMethod("calcularMoraParaPrestamo", Prestamo.class, LocalDate.class);
        method.setAccessible(true);

        // Act
        method.invoke(prestamoScheduler, prestamo, LocalDate.now());

        // Assert
        verify(prestamoRepository).save(prestamoCaptor.capture());
        Prestamo updatedPrestamo = prestamoCaptor.getValue();
        
        assertEquals(5, updatedPrestamo.getDiasMora());
        assertEquals(new BigDecimal("5.00"), updatedPrestamo.getMoraAcumulada()); // 0.1% of 1000 * 5 days
        assertEquals(EstadoPrestamo.EN_MORA, updatedPrestamo.getEstado());
    }

    @Test
    @DisplayName("Should respect grace period when calculating mora")
    void calcularMoraParaPrestamoWithGracePeriodShouldRespectGracePeriod() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(prestamoScheduler, "diasGracia", 3);
        
        LocalDate fiveDaysAgo = LocalDate.now().minusDays(5);
        Prestamo prestamo = crearPrestamo(1L, EstadoPrestamo.VENCIDO, fiveDaysAgo, fiveDaysAgo);
        prestamo.setMonto(new BigDecimal("1000.00"));
        
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Use reflection to access private method
        var method = PrestamoScheduler.class.getDeclaredMethod("calcularMoraParaPrestamo", Prestamo.class, LocalDate.class);
        method.setAccessible(true);

        // Act
        method.invoke(prestamoScheduler, prestamo, LocalDate.now());

        // Assert
        verify(prestamoRepository).save(prestamoCaptor.capture());
        Prestamo updatedPrestamo = prestamoCaptor.getValue();
        
        assertEquals(2, updatedPrestamo.getDiasMora()); // 5 days - 3 days grace = 2 days
        assertEquals(new BigDecimal("2.00"), updatedPrestamo.getMoraAcumulada()); // 0.1% of 1000 * 2 days
    }

    @Test
    @DisplayName("Should update loan status to EN_MORA when mora is applied")
    void actualizarPrestamoConMoraShouldUpdateStatusToEnMora() throws Exception {
        // Arrange
        Prestamo prestamo = crearPrestamo(1L, EstadoPrestamo.VENCIDO, LocalDate.now().minusDays(5), null);
        prestamo.setMonto(new BigDecimal("1000.00"));
        
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Use reflection to access private method
        var method = PrestamoScheduler.class.getDeclaredMethod("actualizarPrestamoConMora", 
            Prestamo.class, long.class, BigDecimal.class, LocalDate.class);
        method.setAccessible(true);

        // Act
        method.invoke(prestamoScheduler, prestamo, 5, new BigDecimal("5.00"), LocalDate.now());

        // Assert
        verify(prestamoRepository).save(prestamoCaptor.capture());
        Prestamo updatedPrestamo = prestamoCaptor.getValue();
        
        assertEquals(EstadoPrestamo.EN_MORA, updatedPrestamo.getEstado());
        assertTrue((boolean) ReflectionTestUtils.getField(updatedPrestamo, "moraAplicada"));
        assertTrue((boolean) ReflectionTestUtils.getField(updatedPrestamo, "interesMoratorioAplicado"));
    }

    @Test
    @DisplayName("Should not update status if loan is not VENCIDO or EN_MORA")
    void calcularMoraParaPrestamoWithNonEligibleStatusShouldNotUpdate() throws Exception {
        // Arrange
        Prestamo prestamo = crearPrestamo(1L, EstadoPrestamo.PAGADO, LocalDate.now().minusDays(5), null);
        
        // Use reflection to access private method
        var method = PrestamoScheduler.class.getDeclaredMethod("calcularMoraParaPrestamo", Prestamo.class, LocalDate.class);
        method.setAccessible(true);
        
        // Act
        method.invoke(prestamoScheduler, prestamo, LocalDate.now());
        
        // Assert
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    @DisplayName("Should handle empty loan list gracefully")
    void calcularInteresMoratorioDiarioWithNoLoansShouldNotFail() {
        // Arrange
        when(prestamoRepository.findAprobadosVencidos(any())).thenReturn(List.of());
        when(prestamoRepository.findVencidosSinMoraActualizada(any())).thenReturn(List.of());
        
        // Act & Assert (should not throw exceptions)
        assertDoesNotThrow(() -> prestamoScheduler.calcularInteresMoratorioDiario());
    }

    @Test
    @DisplayName("Should handle multiple loans with different statuses")
    void calcularInteresMoratorioDiarioWithMultipleLoansShouldProcessAppropriately() {
        // Arrange
        LocalDate fiveDaysAgo = LocalDate.now().minusDays(5);
        Prestamo vencido = crearPrestamo(1L, EstadoPrestamo.APROBADO, fiveDaysAgo, null);
        Prestamo enMora = crearPrestamo(2L, EstadoPrestamo.EN_MORA, fiveDaysAgo, fiveDaysAgo);
        Prestamo pagado = crearPrestamo(3L, EstadoPrestamo.PAGADO, fiveDaysAgo, null);
        
        when(prestamoRepository.findAprobadosVencidos(any())).thenReturn(List.of(vencido));
        when(prestamoRepository.findVencidosSinMoraActualizada(any())).thenReturn(List.of(enMora));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        prestamoScheduler.calcularInteresMoratorioDiario();

        // Assert
        // Verify that save was called for both the vencido and enMora loans
        verify(prestamoRepository, atLeast(2)).save(any(Prestamo.class));
        // Verify the vencido loan was updated to VENCIDO state
        assertEquals(EstadoPrestamo.VENCIDO, vencido.getEstado());
    }

    @Test
    @DisplayName("Should handle loan with future due date")
    void calcularMoraParaPrestamoWithFutureDueDateShouldNotCalculateMora() throws Exception {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Prestamo prestamo = crearPrestamo(1L, EstadoPrestamo.APROBADO, tomorrow, null);
        
        // Use reflection to access private method
        var method = PrestamoScheduler.class.getDeclaredMethod("calcularMoraParaPrestamo", Prestamo.class, LocalDate.class);
        method.setAccessible(true);
        
        // Act
        method.invoke(prestamoScheduler, prestamo, LocalDate.now());
        
        // Assert
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    @DisplayName("Should handle loan with zero amount")
    void calcularMoraParaPrestamoWithZeroAmountShouldNotCalculateMora() throws Exception {
        // Arrange
        LocalDate fiveDaysAgo = LocalDate.now().minusDays(5);
        Prestamo prestamo = crearPrestamo(1L, EstadoPrestamo.VENCIDO, fiveDaysAgo, fiveDaysAgo);
        prestamo.setMonto(BigDecimal.ZERO);
        
        // Mock the repository to return the saved prestamo
        when(prestamoRepository.save(any(Prestamo.class))).thenReturn(prestamo);
        
        // Use reflection to access private method
        var method = PrestamoScheduler.class.getDeclaredMethod("calcularMoraParaPrestamo", Prestamo.class, LocalDate.class);
        method.setAccessible(true);
        
        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(prestamoScheduler, prestamo, LocalDate.now()));
        
        // Verify the repository was called to save the prestamo
        verify(prestamoRepository).save(any(Prestamo.class));
        
        // Verify the loan was updated to EN_MORA state
        assertEquals(EstadoPrestamo.EN_MORA, prestamo.getEstado());
    }

    @Test
    @DisplayName("Should handle null values in loan properties")
    void calcularMoraParaPrestamoWithNullValuesShouldNotFail() throws Exception {
        // Arrange
        Prestamo prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setEstado(EstadoPrestamo.VENCIDO);
        
        // Use reflection to access private method
        var method = PrestamoScheduler.class.getDeclaredMethod("calcularMoraParaPrestamo", Prestamo.class, LocalDate.class);
        method.setAccessible(true);
        
        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(prestamoScheduler, prestamo, LocalDate.now()));
    }

    @Test
    @DisplayName("Should handle negative mora percentage")
    void calcularMoraParaPrestamoWithNegativeMoraPercentageShouldUseAbsoluteValue() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(prestamoScheduler, "porcentajeMoraDiario", new BigDecimal("-0.1"));
        LocalDate fiveDaysAgo = LocalDate.now().minusDays(5);
        Prestamo prestamo = crearPrestamo(1L, EstadoPrestamo.VENCIDO, fiveDaysAgo, fiveDaysAgo);
        prestamo.setMonto(new BigDecimal("1000.00"));
        
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Use reflection to access private method
        var method = PrestamoScheduler.class.getDeclaredMethod("calcularMoraParaPrestamo", Prestamo.class, LocalDate.class);
        method.setAccessible(true);

        // Act
        method.invoke(prestamoScheduler, prestamo, LocalDate.now());

        // Assert
        verify(prestamoRepository).save(prestamoCaptor.capture());
        Prestamo updatedPrestamo = prestamoCaptor.getValue();
        
        // Should still calculate positive mora despite negative percentage
        assertEquals(new BigDecimal("5.00"), updatedPrestamo.getMoraAcumulada());
    }

    @Test
    @DisplayName("Should handle exception during loan processing")
    void calcularInteresMoratorioDiarioWhenExceptionThrownShouldContinueProcessing() {
        // Arrange
        LocalDate fiveDaysAgo = LocalDate.now().minusDays(5);
        Prestamo goodLoan = crearPrestamo(1L, EstadoPrestamo.APROBADO, fiveDaysAgo, null);
        Prestamo badLoan = crearPrestamo(2L, EstadoPrestamo.APROBADO, fiveDaysAgo, null);
        badLoan.setMonto(null); // This will cause NPE
        
        when(prestamoRepository.findAprobadosVencidos(any())).thenReturn(List.of(goodLoan, badLoan));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> prestamoScheduler.calcularInteresMoratorioDiario());
        
        // Should still process the good loan
        verify(prestamoRepository, atLeastOnce()).save(any(Prestamo.class));
    }

    // Helper method to create a test Prestamo
    private Prestamo crearPrestamo(Long id, EstadoPrestamo estado, LocalDate fechaVencimiento, LocalDate fechaUltimoCalculo) {
        Prestamo prestamo = new Prestamo();
        prestamo.setId(id);
        prestamo.setEstado(estado);
        prestamo.setFechaVencimiento(fechaVencimiento);
        prestamo.setFechaUltimoCalculoMora(fechaUltimoCalculo);
        prestamo.setMonto(new BigDecimal("1000.00"));
        prestamo.setInteres(new BigDecimal("10.0"));
        prestamo.setInteresMoratorio(new BigDecimal("5.0"));
        return prestamo;
    }
}
