package com.autocare.customer_service.controller;

import com.autocare.customer_service.model.Cliente;
import com.autocare.customer_service.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Cliente> resultado = clienteService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Cliente no encontrado con ID: " + id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Object> buscarPorEmail(@PathVariable String email) {
        Optional<Cliente> resultado = clienteService.buscarPorEmail(email);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Cliente no encontrado con email: " + email));
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Cliente cliente) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.guardar(cliente));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id,
                                              @Valid @RequestBody Cliente cliente) {
        try {
            return ResponseEntity.ok(clienteService.actualizar(id, cliente));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            clienteService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}