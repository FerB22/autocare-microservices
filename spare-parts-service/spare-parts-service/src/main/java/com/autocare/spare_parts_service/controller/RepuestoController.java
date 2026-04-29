package com.autocare.spare_parts_service.controller;

import com.autocare.spare_parts_service.model.Repuesto;
import com.autocare.spare_parts_service.service.RepuestoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/repuestos")
public class RepuestoController {

    private final RepuestoService repuestoService;

    public RepuestoController(RepuestoService repuestoService) {
        this.repuestoService = repuestoService;
    }

    @GetMapping
    public ResponseEntity<List<Repuesto>> listar() {
        return ResponseEntity.ok(repuestoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Repuesto> resultado = repuestoService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Repuesto no encontrado con ID: " + id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Object> buscarPorCodigo(@PathVariable String codigo) {
        Optional<Repuesto> resultado = repuestoService.buscarPorCodigo(codigo);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Repuesto no encontrado con código: " + codigo));
    }

    @GetMapping("/con-stock")
    public ResponseEntity<List<Repuesto>> listarConStock() {
        return ResponseEntity.ok(repuestoService.listarConStock());
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Repuesto repuesto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(repuestoService.guardar(repuesto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /repuestos/{id}/reservar?cantidad=3
    @PostMapping("/{id}/reservar")
    public ResponseEntity<Object> reservar(@PathVariable String id,
                                            @RequestParam int cantidad) {
        try {
            return ResponseEntity.ok(repuestoService.reservar(id, cantidad));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id,
                                              @Valid @RequestBody Repuesto repuesto) {
        try {
            return ResponseEntity.ok(repuestoService.actualizar(id, repuesto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            repuestoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}