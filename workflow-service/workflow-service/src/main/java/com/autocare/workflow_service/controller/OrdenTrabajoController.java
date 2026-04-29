package com.autocare.workflow_service.controller;

import com.autocare.workflow_service.model.OrdenTrabajo;
import com.autocare.workflow_service.service.OrdenTrabajoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ordenes")
public class OrdenTrabajoController {

    private final OrdenTrabajoService ordenService;

    public OrdenTrabajoController(OrdenTrabajoService ordenService) {
        this.ordenService = ordenService;
    }

    @GetMapping
    public ResponseEntity<List<OrdenTrabajo>> listar() {
        return ResponseEntity.ok(ordenService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<OrdenTrabajo> resultado = ordenService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Orden no encontrada con ID: " + id));
    }

    @GetMapping("/vehiculo/{idVehiculo}")
    public ResponseEntity<List<OrdenTrabajo>> buscarPorVehiculo(
            @PathVariable String idVehiculo) {
        return ResponseEntity.ok(ordenService.buscarPorVehiculo(idVehiculo));
    }

    // GET /ordenes/estado/EN_PROCESO
    @GetMapping("/estado/{estado}")
    public ResponseEntity<Object> buscarPorEstado(
            @PathVariable OrdenTrabajo.EstadoOrden estado) {
        return ResponseEntity.ok(ordenService.buscarPorEstado(estado));
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody OrdenTrabajo orden) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ordenService.crear(orden));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /ordenes/{id}/estado?estado=EN_PROCESO
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(
            @PathVariable String id,
            @RequestParam OrdenTrabajo.EstadoOrden estado) {
        try {
            return ResponseEntity.ok(ordenService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            ordenService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}