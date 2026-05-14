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

/**
 * Controlador REST para la gestión de facturas.
 * Contiene endpoints para listar, obtener, crear, actualizar estado y eliminar facturas.
 * Las respuestas usan ResponseEntity para controlar códigos HTTP y cuerpos de error.
 */
@Tag(name = "Facturas", description = "Gestión de facturas del taller")
@RestController
@RequestMapping("/facturas")
public class FacturaController {

    // Servicio que contiene la lógica de negocio y acceso a datos
    private final FacturaService facturaService;

    // Inyección por constructor, facilita testing y claridad de dependencias
    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @Operation(summary = "Listar todas las facturas")
    @GetMapping
    public ResponseEntity<List<Factura>> listar() {
        // Devuelve 200 OK con la lista completa (posible lista vacía)
        return ResponseEntity.ok(facturaService.listarTodas());
    }

    @Operation(summary = "Obtener factura por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        // Busca la factura por id; usar Optional permite distinguir entre encontrado/no encontrado
        Optional<Factura> resultado = facturaService.buscarPorId(id);
        if (resultado.isPresent()) {
            // 200 OK con la factura encontrada
            return ResponseEntity.ok(resultado.get());
        }
        // 404 Not Found con un cuerpo JSON simple que describe el error
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Factura no encontrada con ID: " + id));
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<Factura>> buscarPorCliente(@PathVariable String idCliente) {
        // Devuelve 200 OK con todas las facturas asociadas al cliente
        return ResponseEntity.ok(facturaService.buscarPorCliente(idCliente));
    }

    // GET /facturas/estado/PENDIENTE
    @GetMapping("/estado/{estado}")
    public ResponseEntity<Object> buscarPorEstado(
            @PathVariable Factura.EstadoFactura estado) {
        // Convierte automáticamente el path variable al enum Factura.EstadoFactura
        // Devuelve 200 OK con la lista filtrada por estado
        return ResponseEntity.ok(facturaService.buscarPorEstado(estado));
    }

    @Operation(summary = "Crear una nueva factura")
    @PostMapping
    public ResponseEntity<Object> generar(@Valid @RequestBody Factura factura) {
        // @Valid activa validaciones de Jakarta Validation sobre el cuerpo recibido
        try {
            // Si la generación falla por reglas de negocio, el servicio lanza RuntimeException
            // Aquí se captura y se devuelve 400 Bad Request con el mensaje de error
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(facturaService.generar(factura));
        } catch (RuntimeException e) {
            // 400 Bad Request con mensaje de error en JSON
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
            // Intenta cambiar el estado; si la factura no existe o la regla falla,
            // el servicio lanza RuntimeException y se devuelve 404 Not Found
            return ResponseEntity.ok(facturaService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            // 404 Not Found con mensaje de error
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar factura")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            // Elimina la factura; si no existe, el servicio lanza RuntimeException
            facturaService.eliminar(id);
            // 204 No Content cuando la eliminación fue exitosa
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // 404 Not Found con mensaje de error
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
