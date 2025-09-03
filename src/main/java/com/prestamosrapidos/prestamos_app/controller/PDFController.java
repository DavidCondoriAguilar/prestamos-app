package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.model.ErrorResponse;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.util.PDFGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", 
             allowedHeaders = "*",
             allowCredentials = "true")
public class PDFController {

    private final PDFGeneratorService pdfGeneratorService;
    private final ClienteRepository clienteRepository;

    @GetMapping("/cliente/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> generateClientPDF(@PathVariable Long id) {
        try {
            Cliente cliente = clienteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));

            ByteArrayInputStream pdfStream = pdfGeneratorService.generateClientReport(cliente);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=cliente_" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfStream.readAllBytes());
        } catch (RuntimeException ex) {
            log.error("Error al generar PDF para el cliente {}: {}", id, ex.getMessage(), ex);
            return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("Error inesperado al generar PDF para el cliente {}: {}", id, ex.getMessage(), ex);
            return buildErrorResponse("Error al procesar el documento PDF", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, HttpStatus status) {
        log.error("Error en PDFController: {}", message);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }
}
