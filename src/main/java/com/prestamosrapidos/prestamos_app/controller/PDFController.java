package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import com.prestamosrapidos.prestamos_app.util.PDFGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PDFController {

    private final PDFGeneratorService pdfGeneratorService;
    private final ClienteRepository clienteRepository;

    @GetMapping("/cliente/{id}")
    public ResponseEntity<byte[]> generateClientPDF(@PathVariable Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        ByteArrayInputStream pdfStream = pdfGeneratorService.generateClientReport(cliente);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=cliente_" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfStream.readAllBytes());
    }
}
