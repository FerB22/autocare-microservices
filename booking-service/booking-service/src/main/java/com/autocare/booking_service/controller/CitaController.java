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

/**
 * Controller REST para gestión de citas del taller.
 * - Expone endpoints RESTful para CRUD de citas.
 * - Usa @Valid para validación de entrada (400 automático vía GlobalExceptionHandler).
 * - Maneja Optional para 404 cuando no se encuentra recurso.
 * - Try-catch mínimos; delega manejo de excepciones al GlobalExceptionHandler.
 */
@Tag(name = "Citas", description = "Gestión de citas del taller")
@RestController
@RequestMapping("/citas")
public class CitaController {

    // Servicio inyectado por constructor (mejor para testing que @Autowired en campo)
    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /**
     * GET /citas - Lista todas las citas.
     * - 200 OK con lista de citas (vacía si no hay ninguna).
     * - Nota: en producción agregar paginación (?page=0&size=20).
     */
    @Operation(summary = "Listar todas las citas")
    @GetMapping
    public ResponseEntity<List<Cita>> listar() {
        // Delega al servicio; no hay lógica de negocio en controller
        return ResponseEntity.ok(citaService.listarTodas());
    }

    /**
     * GET /citas/{id} - Obtiene una cita específica.
     * - 200 OK si existe.
     * - 404 Not Found si no existe (manejo manual de Optional).
     * - Validación de ID en servicio (evita consulta BD con ID inválido).
     */
    @Operation(summary = "Obtener cita por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Cita> resultado = citaService.buscarPorId(id);
        if (resultado.isPresent()) {
            // 200 OK con el objeto Cita
            return ResponseEntity.ok(resultado.get());
        }
        // 404 explícito con mensaje claro para el cliente
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Cita no encontrada con ID: " + id));
    }

    /**
     * GET /citas/vehiculo/{idVehiculo} - Lista citas de un vehículo.
     * - 200 OK con lista (vacía si el vehículo no tiene citas).
     * - Validación de ID en servicio.
     */
    @GetMapping("/vehiculo/{idVehiculo}")
    public ResponseEntity<List<Cita>> buscarPorVehiculo(@PathVariable String idVehiculo) {
        // Sin documentación Swagger explícita porque es consulta secundaria
        return ResponseEntity.ok(citaService.buscarPorVehiculo(idVehiculo));
    }

    /**
     * POST /citas - Crea nueva cita.
     * - @Valid activa validaciones del modelo Cita (400 vía handler si falla).
     * - 201 Created si éxito (reglas de negocio OK).
     * - Errores de negocio (horario ocupado, etc.) → 400 vía handler.
     */
    @Operation(summary = "Crear una nueva cita")
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Cita cita) {
        try {
            // Delega reglas de negocio al servicio (verificación externa, conflictos)
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(citaService.guardar(cita));
        } catch (RuntimeException e) {
            // Try-catch de respaldo; normalmente el GlobalExceptionHandler lo captura
            // (horario ocupado, vehículo ya agendado, cliente/vehículo inexistente)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /citas/{id}/estado?estado=CONFIRMADA - Cambia estado de cita.
     * - Usa PATCH para cambios parciales (solo estado).
     * - @RequestParam para el nuevo estado (más RESTful que body para enums simples).
     * - 200 OK si éxito.
     * - 404/400 vía servicio/handler si inválido.
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(@PathVariable String id,
                                                @RequestParam Cita.EstadoCita estado) {
        try {
            // Delega reglas (no modificar EJECUTADA/CANCELADA) al servicio
            return ResponseEntity.ok(citaService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            // 404 si cita no existe, 400 si estado inválido (manejado por servicio)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /citas/{id} - Elimina cita.
     * - 204 No Content si éxito (convención REST para DELETE exitoso).
     * - 404 si no existe (validación en servicio).
     */
    @Operation(summary = "Eliminar cita")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            // Delega eliminación al servicio (con validación previa)
            citaService.eliminar(id);
            // 204 sin body = operación exitosa
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // 404 si cita no existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}