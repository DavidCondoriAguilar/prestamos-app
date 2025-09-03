package com.prestamosrapidos.prestamos_app.service;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.repository.PrestamoRepository;
import com.prestamosrapidos.prestamos_app.service.serviceImpl.PrestamoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceImplTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private PrestamoServiceImpl prestamoService;

    private Prestamo prestamo;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void setUp() {
        // Configuración común para las pruebas
        prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setMonto(new BigDecimal("1000.00"));
        prestamo.setInteres(new BigDecimal("10.00")); // 10% de interés
        prestamo.setInteresMoratorio(new BigDecimal("10.00")); // 10% de interés moratorio anual
        prestamo.setFechaCreacion(LocalDateTime.now().minusDays(2));
        prestamo.setFechaVencimiento(hoy.minusDays(1)); // Vencido ayer
        prestamo.setEstado(EstadoPrestamo.VENCIDO);
        prestamo.setDiasMora(0);
        prestamo.setMoraAcumulada(BigDecimal.ZERO);
        prestamo.setInteresMoratorioAplicado(false);
    }

    @Test
    void verificarYActualizarMora_CuandoPrestamoEstaVencido_DebeCalcularMoraCorrectamente() {
        // Arrange
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        prestamoService.verificarYActualizarMora(prestamo);

        // Assert
        assertEquals(1, prestamo.getDiasMora());
        assertEquals(new BigDecimal("1.00"), prestamo.getMoraAcumulada()); // 0.1% de 1000 = 1.00
        assertEquals(EstadoPrestamo.EN_MORA, prestamo.getEstado());
        assertNotNull(prestamo.getFechaUltimoCalculoMora());
    }

    @Test
    void verificarYActualizarMora_CuandoPrestamoTienePagos_DebeCalcularSobreCapitalPendiente() {
        // Arrange
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("500.00"));
        List<Pago> pagos = new ArrayList<>();
        pagos.add(pago);
        prestamo.setPagos(pagos);
        
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        prestamoService.verificarYActualizarMora(prestamo);

        // Assert - Capital pendiente: 1000 - 500 = 500, mora: 0.1% de 500 = 0.50
        assertEquals(new BigDecimal("0.50"), prestamo.getMoraAcumulada());
    }

    @Test
    void verificarYActualizarMora_CuandoPrestamoNoEstaVencido_NoDebeCalcularMora() {
        // Arrange
        prestamo.setFechaVencimiento(hoy.plusDays(1)); // Aún no vence
        prestamo.setEstado(EstadoPrestamo.APROBADO);

        // Act
        prestamoService.verificarYActualizarMora(prestamo);

        // Assert
        assertEquals(0, prestamo.getDiasMora());
        assertEquals(BigDecimal.ZERO, prestamo.getMoraAcumulada());
        assertEquals(EstadoPrestamo.APROBADO, prestamo.getEstado());
    }

    @Test
    void verificarYActualizarMora_CuandoEsSegundoDiaDeMora_DebeAcumularMora() {
        // Arrange
        prestamo.setDiasMora(1);
        prestamo.setMoraAcumulada(new BigDecimal("1.00")); // Mora del primer día
        prestamo.setFechaUltimoCalculoMora(hoy.minusDays(1));
        
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        prestamoService.verificarYActualizarMora(prestamo);

        // Assert - Debe sumar 1.00 de mora adicional (total 2.00)
        assertEquals(2, prestamo.getDiasMora());
        assertEquals(new BigDecimal("2.00"), prestamo.getMoraAcumulada());
    }

    @Test
    void verificarYActualizarMora_CuandoPrestamoEstaPagado_NoDebeHacerNada() {
        // Arrange
        prestamo.setEstado(EstadoPrestamo.PAGADO);
        prestamo.setDeudaRestante(BigDecimal.ZERO);

        // Act
        prestamoService.verificarYActualizarMora(prestamo);

        // Assert
        assertEquals(0, prestamo.getDiasMora());
        assertEquals(BigDecimal.ZERO, prestamo.getMoraAcumulada());
    }
}
