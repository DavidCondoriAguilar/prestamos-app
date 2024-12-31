package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.ClienteModel;
import com.prestamosrapidos.prestamos_app.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // Endpoint para crear un nuevo cliente
    @PostMapping
    public ResponseEntity<ClienteModel> crearCliente(@RequestBody ClienteModel clienteModel) {
        try {
            ClienteModel nuevoCliente = clienteService.crearCliente(clienteModel);
            return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Endpoint para obtener todos los clientes
    @GetMapping
    public ResponseEntity<List<ClienteModel>> obtenerTodosLosClientes() {
        List<ClienteModel> clientes = clienteService.obtenerTodosLosClientes();
        return new ResponseEntity<>(clientes, HttpStatus.OK);
    }

    // Endpoint para obtener un cliente por ID
    @GetMapping("/{id}")
    public ResponseEntity<ClienteModel> obtenerClientePorId(@PathVariable Long id) {
        try {
            ClienteModel cliente = clienteService.obtenerClientePorId(id);
            return new ResponseEntity<>(cliente, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    // Endpoint para actualizar un cliente
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

    // Endpoint para eliminar un cliente
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
