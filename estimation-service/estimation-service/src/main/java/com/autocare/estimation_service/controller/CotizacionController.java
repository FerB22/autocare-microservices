package com.autocare.estimation_service.controller;

import com.autocare.estimation_service.model.Cotizacion;
import com.autocare.estimation_service.service.CotizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/cotizaciones")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    public ResponseEntity<List<Cotizacion>> listar() {
        return ResponseEntity.ok(cotizacionService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Cotizacion> resultado = cotizacionService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Cotización no encontrada con ID: " + id));
    }

    @GetMapping("/orden/{idOrden}")
    public ResponseEntity<List<Cotizacion>> buscarPorOrden(@PathVariable String idOrden) {
        return ResponseEntity.ok(cotizacionService.buscarPorOrden(idOrden));
    }

    // GET /cotizaciones/estado/PENDIENTE
    @GetMapping("/estado/{estado}")
    public ResponseEntity<Object> buscarPorEstado(
            @PathVariable Cotizacion.EstadoCotizacion estado) {
        return ResponseEntity.ok(cotizacionService.buscarPorEstado(estado));
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Cotizacion cotizacion) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(cotizacionService.crear(cotizacion));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /cotizaciones/{id}/estado?estado=APROBADA
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(
            @PathVariable String id,
            @RequestParam Cotizacion.EstadoCotizacion estado) {
        try {
            return ResponseEntity.ok(cotizacionService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            cotizacionService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}