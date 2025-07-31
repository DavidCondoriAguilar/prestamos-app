package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.ClienteModel;
import com.prestamosrapidos.prestamos_app.service.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", 
             allowedHeaders = "*",
             allowCredentials = "true")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteModel> crearCliente(@RequestBody ClienteModel clienteModel) {
        log.debug("Recibida solicitud de creación de cliente: {}", clienteModel);
        
        try {
            ClienteModel nuevoCliente = clienteService.crearCliente(clienteModel);
            log.debug("Cliente creado exitosamente: {}", nuevoCliente);
            return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Error al crear cliente: {}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error inesperado al crear cliente: {}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<ClienteModel>> obtenerTodosLosClientes() {
        List<ClienteModel> clientes = clienteService.obtenerTodosLosClientes();
        return new ResponseEntity<>(clientes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteModel> obtenerClientePorId(@PathVariable Long id) {
        try {
            ClienteModel cliente = clienteService.obtenerClientePorId(id);
            return new ResponseEntity<>(cliente, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteModel> actualizarCliente(
            @PathVariable Long id, @RequestBody ClienteModel clienteModel) {
        try {
            ClienteModel clienteActualizado = clienteService.actualizarCliente(id, clienteModel);
            return new ResponseEntity<>(clienteActualizado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        try {
            clienteService.eliminarCliente(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
