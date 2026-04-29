package com.autocare.checkin_service.controller;

import com.autocare.checkin_service.model.Recepcion;
import com.autocare.checkin_service.service.RecepcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/recepciones")
public class RecepcionController {

    private final RecepcionService recepcionService;

    public RecepcionController(RecepcionService recepcionService) {
        this.recepcionService = recepcionService;
    }

    @GetMapping
    public ResponseEntity<List<Recepcion>> listar() {
        return ResponseEntity.ok(recepcionService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Recepcion> resultado = recepcionService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Recepción no encontrada con ID: " + id));
    }

    @GetMapping("/vehiculo/{idVehiculo}")
    public ResponseEntity<List<Recepcion>> buscarPorVehiculo(
            @PathVariable String idVehiculo) {
        return ResponseEntity.ok(recepcionService.buscarPorVehiculo(idVehiculo));
    }

    @PostMapping
    public ResponseEntity<Object> registrar(@Valid @RequestBody Recepcion recepcion) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(recepcionService.registrar(recepcion));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            recepcionService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}