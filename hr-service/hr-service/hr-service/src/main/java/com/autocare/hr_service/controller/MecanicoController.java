package com.autocare.hr_service.controller;

import com.autocare.hr_service.model.Mecanico;
import com.autocare.hr_service.service.MecanicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Mecánicos", description = "Gestión de mecánicos del taller")
@RestController
@RequestMapping("/mecanicos")
public class MecanicoController {

    private final MecanicoService mecanicoService;

    public MecanicoController(MecanicoService mecanicoService) {
        this.mecanicoService = mecanicoService;
    }

    @Operation(summary = "Listar todos los mecánicos")
    @GetMapping
    public ResponseEntity<List<Mecanico>> listar() {
        return ResponseEntity.ok(mecanicoService.listarTodos());
    }

    @Operation(summary = "Obtener por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Mecanico> resultado = mecanicoService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Mecánico no encontrado con ID: " + id));
    }

    // GET /mecanicos/disponibles → lista los libres
    @GetMapping("/disponibles")
    public ResponseEntity<List<Mecanico>> listarDisponibles() {
        return ResponseEntity.ok(mecanicoService.buscarDisponibles());
    }

    // GET /mecanicos/especialidad/Motor → filtra por especialidad
    @GetMapping("/especialidad/{especialidad}")
    public ResponseEntity<List<Mecanico>> buscarPorEspecialidad(
            @PathVariable String especialidad) {
        return ResponseEntity.ok(mecanicoService.buscarPorEspecialidad(especialidad));
    }

    // GET /mecanicos/disponibles/Motor → libres Y con esa especialidad
    @GetMapping("/disponibles/{especialidad}")
    public ResponseEntity<List<Mecanico>> disponiblesPorEspecialidad(
            @PathVariable String especialidad) {
        return ResponseEntity.ok(mecanicoService.buscarDisponiblesPorEspecialidad(especialidad));
    }

    @Operation(summary = "Crear nuevo mecánico")
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Mecanico mecanico) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(mecanicoService.guardar(mecanico));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /mecanicos/{id}/disponibilidad?disponible=false
    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<Object> cambiarDisponibilidad(
            @PathVariable String id,
            @RequestParam boolean disponible) {
        try {
            return ResponseEntity.ok(mecanicoService.cambiarDisponibilidad(id, disponible));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id,
                                              @Valid @RequestBody Mecanico mecanico) {
        try {
            return ResponseEntity.ok(mecanicoService.actualizar(id, mecanico));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            mecanicoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}