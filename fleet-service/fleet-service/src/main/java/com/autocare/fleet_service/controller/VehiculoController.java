package com.autocare.fleet_service.controller;

import com.autocare.fleet_service.model.Vehiculo;
import com.autocare.fleet_service.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    // GET /vehiculos → lista todos
    @GetMapping
    public ResponseEntity<List<Vehiculo>> listar() {
        return ResponseEntity.ok(vehiculoService.listarTodos());
    }

    // GET /vehiculos/patente/ABC123
    @GetMapping("/patente/{patente}")
    public ResponseEntity<Object> buscarPorPatente(@PathVariable String patente) {
        Optional<Vehiculo> resultado = vehiculoService.buscarPorPatente(patente);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Vehículo no encontrado con patente: " + patente));
    }

    // GET /vehiculos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Vehiculo> resultado = vehiculoService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Vehículo no encontrado con ID: " + id));
    }

    // POST /vehiculos → crea nuevo
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Vehiculo vehiculo) {
        try {
            Vehiculo nuevo = vehiculoService.guardar(vehiculo);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /vehiculos/{id} → actualiza
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id,
                                              @Valid @RequestBody Vehiculo vehiculo) {
        try {
            return ResponseEntity.ok(vehiculoService.actualizar(id, vehiculo));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /vehiculos/{id} → elimina
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            vehiculoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}