package com.autocare.booking_service.controller;

import com.autocare.booking_service.model.Cita;
import com.autocare.booking_service.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Citas", description = "Gestión de citas del taller")
@RestController
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @Operation(summary = "Listar todos las citas")
    @GetMapping
    public ResponseEntity<List<Cita>> listar() {
        return ResponseEntity.ok(citaService.listarTodas());
    }

    @Operation(summary = "Obtener cita por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Cita> resultado = citaService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Cita no encontrada con ID: " + id));
    }

    @GetMapping("/vehiculo/{idVehiculo}")
    public ResponseEntity<List<Cita>> buscarPorVehiculo(@PathVariable String idVehiculo) {
        return ResponseEntity.ok(citaService.buscarPorVehiculo(idVehiculo));
    }

    @Operation(summary = "Crear una nueva cita")
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Cita cita) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(citaService.guardar(cita));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(@PathVariable String id,
                                                 @RequestParam Cita.EstadoCita estado) {
        try {
            return ResponseEntity.ok(citaService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar cita")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            citaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}