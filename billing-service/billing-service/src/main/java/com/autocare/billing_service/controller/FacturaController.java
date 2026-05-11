package com.autocare.billing_service.controller;

import com.autocare.billing_service.model.Factura;
import com.autocare.billing_service.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Facturas", description = "Gestión de facturas del taller")
@RestController
@RequestMapping("/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @Operation(summary = "Listar todas las facturas")
    @GetMapping
    public ResponseEntity<List<Factura>> listar() {
        return ResponseEntity.ok(facturaService.listarTodas());
    }

    @Operation(summary = "Obtener factura por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Factura> resultado = facturaService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Factura no encontrada con ID: " + id));
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<Factura>> buscarPorCliente(@PathVariable String idCliente) {
        return ResponseEntity.ok(facturaService.buscarPorCliente(idCliente));
    }

    // GET /facturas/estado/PENDIENTE
    @GetMapping("/estado/{estado}")
    public ResponseEntity<Object> buscarPorEstado(
            @PathVariable Factura.EstadoFactura estado) {
        return ResponseEntity.ok(facturaService.buscarPorEstado(estado));
    }

    @Operation(summary = "Crear una nueva factura")
    @PostMapping
    public ResponseEntity<Object> generar(@Valid @RequestBody Factura factura) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(facturaService.generar(factura));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /facturas/{id}/estado?estado=PAGADA
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(
            @PathVariable String id,
            @RequestParam Factura.EstadoFactura estado) {
        try {
            return ResponseEntity.ok(facturaService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar factura")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            facturaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}