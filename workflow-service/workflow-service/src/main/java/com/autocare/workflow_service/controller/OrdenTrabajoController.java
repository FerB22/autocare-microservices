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

/**
 * Controlador REST principal para el microservicio de Flujo de Trabajo (workflow-service).
 * Actúa como la capa de presentación que expone la API HTTP para gestionar todo
 * el ciclo de vida de una Orden de Trabajo en el taller (desde que ingresa el 
 * vehículo hasta que se entrega).
 * @RestController Combina @Controller y @ResponseBody para que todos los métodos 
 * devuelvan directamente respuestas en formato JSON.
 */
@RestController
@RequestMapping("/ordenes") // Define la ruta base unificada para este recurso.
public class OrdenTrabajoController {

    // Se inyecta la capa de servicio como inmutable (final) para asegurar que 
    // la lógica de negocio esté completamente desacoplada del enrutamiento web.
    private final OrdenTrabajoService ordenService;

    /**
     * Inyección de dependencias por constructor.
     * Esta práctica garantiza que el controlador no pueda instanciarse en un 
     * estado inválido (sin su servicio) y facilita inyectar objetos "Mock"
     * durante las pruebas unitarias.
     */
    public OrdenTrabajoController(OrdenTrabajoService ordenService) {
        this.ordenService = ordenService;
    }

    /**
     * Endpoint GET genérico para listar todas las órdenes.
     * @return HTTP 200 (OK) con la colección completa de órdenes.
     */
    @GetMapping
    public ResponseEntity<List<OrdenTrabajo>> listar() {
        return ResponseEntity.ok(ordenService.listarTodas());
    }

    /**
     * Endpoint GET para recuperar los detalles de una orden específica.
     * @PathVariable extrae el identificador (UUID) directamente desde la URL.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<OrdenTrabajo> resultado = ordenService.buscarPorId(id);
        
        // Si el contenedor Optional tiene datos, retorna 200 OK.
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        
        // Si no existe, devuelve explícitamente un código HTTP 404 (Not Found)
        // estructurado en JSON para que el frontend pueda manejar el error de forma limpia.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Orden no encontrada con ID: " + id));
    }

    /**
     * Endpoint GET que funciona como un filtro o sub-recurso.
     * Es vital para la arquitectura, ya que permite consultar rápidamente 
     * todo el historial de reparaciones y mantenimientos de un vehículo en particular.
     */
    @GetMapping("/vehiculo/{idVehiculo}")
    public ResponseEntity<List<OrdenTrabajo>> buscarPorVehiculo(
            @PathVariable String idVehiculo) {
        return ResponseEntity.ok(ordenService.buscarPorVehiculo(idVehiculo));
    }

    // GET /ordenes/estado/EN_PROCESO
    /**
     * Endpoint GET para filtrar por la máquina de estados.
     * Extremadamente útil para crear dashboards operativos (por ejemplo, 
     * una pantalla que solo muestre las órdenes "PENDIENTE_REVISION" a los mecánicos libres).
     * Spring convierte automáticamente el String de la URL al Enum EstadoOrden.
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<Object> buscarPorEstado(
            @PathVariable OrdenTrabajo.EstadoOrden estado) {
        return ResponseEntity.ok(ordenService.buscarPorEstado(estado));
    }

    /**
     * Endpoint POST para iniciar (crear) una nueva Orden de Trabajo.
     * @Valid Detiene la ejecución antes de llegar al servicio si el JSON 
     * no cumple con las anotaciones de validación (@NotBlank, @NotNull) del modelo.
     * @RequestBody Deserializa el cuerpo JSON de la petición HTTP al objeto OrdenTrabajo.
     */
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody OrdenTrabajo orden) {
        try {
            // Un POST exitoso en una API RESTful debe devolver HTTP 201 (Created).
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ordenService.crear(orden));
        } catch (RuntimeException e) {
            // Cualquier regla de negocio rota (ej. Vehículo no existe o Mecánico no disponible)
            // se atrapa aquí y se devuelve como un Bad Request (HTTP 400).
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /ordenes/{id}/estado?estado=EN_PROCESO
    /**
     * Endpoint PATCH diseñado exclusivamente para transiciones de estado.
     * A nivel de diseño REST, PATCH es la elección correcta en lugar de PUT, 
     * porque solo estamos modificando un atributo específico (el estado) 
     * y no reemplazando la orden completa.
     * @RequestParam lee el nuevo estado desde la query string (?estado=...).
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(
            @PathVariable String id,
            @RequestParam OrdenTrabajo.EstadoOrden estado) {
        try {
            return ResponseEntity.ok(ordenService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            // Si el ID de la orden no existe, responde 404 Not Found.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint DELETE para eliminar (o cancelar) una orden de trabajo.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            ordenService.eliminar(id);
            // El código HTTP 204 (No Content) es el estándar REST para indicar 
            // que un recurso fue eliminado exitosamente y no hay datos que devolver.
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}