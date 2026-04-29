package com.autocare.crm_service.controller;

import com.autocare.crm_service.model.Interaccion;
import com.autocare.crm_service.service.InteraccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/interacciones")
public class InteraccionController {

    private final InteraccionService interaccionService;

    public InteraccionController(InteraccionService interaccionService) {
        this.interaccionService = interaccionService;
    }

    @GetMapping
    public ResponseEntity<List<Interaccion>> listar() {
        return ResponseEntity.ok(interaccionService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Interaccion> resultado = interaccionService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Interacción no encontrada con ID: " + id));
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<Interaccion>> buscarPorCliente(
            @PathVariable String idCliente) {
        return ResponseEntity.ok(interaccionService.buscarPorCliente(idCliente));
    }

    // GET /interacciones/tipo/RECLAMO
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<Object> buscarPorTipo(
            @PathVariable Interaccion.TipoInteraccion tipo) {
        return ResponseEntity.ok(interaccionService.buscarPorTipo(tipo));
    }

    // GET /interacciones/seguimiento/ABIERTO
    @GetMapping("/seguimiento/{seguimiento}")
    public ResponseEntity<Object> buscarPorSeguimiento(
            @PathVariable Interaccion.SeguimientoEstado seguimiento) {
        return ResponseEntity.ok(interaccionService.buscarPorSeguimiento(seguimiento));
    }

    @PostMapping
    public ResponseEntity<Object> registrar(
            @Valid @RequestBody Interaccion interaccion) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(interaccionService.registrar(interaccion));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /interacciones/{id}/seguimiento?estado=EN_PROCESO
    @PatchMapping("/{id}/seguimiento")
    public ResponseEntity<Object> cambiarSeguimiento(
            @PathVariable String id,
            @RequestParam Interaccion.SeguimientoEstado estado) {
        try {
            return ResponseEntity.ok(interaccionService.cambiarSeguimiento(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            interaccionService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}