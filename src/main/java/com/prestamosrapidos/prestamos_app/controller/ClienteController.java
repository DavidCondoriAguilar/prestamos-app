package com.prestamosrapidos.prestamos_app.controller;

import com.prestamosrapidos.prestamos_app.model.ClienteModel;
import com.prestamosrapidos.prestamos_app.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteModel> crearCliente(@RequestBody @Valid ClienteModel clienteModel,
                                                     UriComponentsBuilder uriComponentsBuilder) {
        ClienteModel clienteCreado = clienteService.crearCliente(clienteModel);

        URI location = uriComponentsBuilder
                .path("/clientes/{id}")
                .buildAndExpand(clienteCreado.getId())
                .toUri();

        return ResponseEntity.created(location).body(clienteCreado);
    }


    @PutMapping("/{id}")
    public ClienteModel actualizarCliente(@PathVariable Long id, @RequestBody ClienteModel clienteModel) {
        return clienteService.actualizarCliente(id, clienteModel);
    }

    @GetMapping("/{id}")
    public ClienteModel obtenerClientePorId(@PathVariable Long id) {
        return clienteService.obtenerClientePorId(id);
    }

    @GetMapping
    public List<ClienteModel> obtenerTodosLosClientes() {
        return clienteService.obtenerTodosLosClientes();
    }

    @DeleteMapping("/{id}")
    public void eliminarCliente(@PathVariable Long id) {
        clienteService.eliminarCliente(id);
    }
}
