package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.util.EXCELGeneratorService;
import com.prestamosrapidos.prestamos_app.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@RestController
@RequestMapping("/excel")
public class EXCELController {

    @Autowired
    private EXCELGeneratorService excelGeneratorService;

    @Autowired
    private ClienteRepository clienteRepository;

    @PostMapping("/cliente/{clienteId}/reporte")
    public ResponseEntity<byte[]> generateClientExcelReport(@PathVariable Long clienteId) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);

        if (clienteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Cliente cliente = clienteOpt.get();

        // Generar el reporte en Excel
        InputStream report = excelGeneratorService.generateClientExcelReport(cliente);

        if (report == null) {
            return ResponseEntity.internalServerError().build();
        }

        byte[] reportBytes;
        try {
            reportBytes = report.readAllBytes(); // Lee el flujo de entrada de bytes del archivo Excel
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reporte_cliente_" + clienteId + ".xlsx") // Nombre del archivo descargado
                .header("Content-Type", "application/vnd.ms-excel") // Tipo de contenido correcto para archivos Excel
                .body(reportBytes); // Cuerpo de la respuesta con el archivo Excel
    }
}
