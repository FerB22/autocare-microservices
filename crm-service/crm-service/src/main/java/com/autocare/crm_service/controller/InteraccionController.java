package com.autocare.crm_service.controller;

import com.autocare.crm_service.model.Interaccion;
import com.autocare.crm_service.service.InteraccionService;
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
 * Controlador REST que expone los endpoints para la gestión de interacciones.
 * @RestController Combina @Controller y @ResponseBody, indicando que los métodos 
 * devuelven datos (generalmente JSON) y no vistas web.
 * @RequestMapping Define la ruta base para todos los endpoints de esta clase.
 */
@Tag(name = "Interacción", description = "Gestión de interacción del taller") // Anotación de Swagger/OpenAPI para documentar el controlador en la interfaz gráfica.
@RestController
@RequestMapping("/interacciones")
public class InteraccionController {

    // Dependencia del servicio inyectada para separar la capa de presentación de la lógica de negocio.
    private final InteraccionService interaccionService;

    /**
     * Inyección de dependencias por constructor.
     * Es la forma recomendada por Spring, ya que asegura que el controlador 
     * siempre se instancie con sus dependencias requeridas (evitando NullPointerException).
     */
    public InteraccionController(InteraccionService interaccionService) {
        this.interaccionService = interaccionService;
    }

    /**
     * Endpoint GET para obtener todas las interacciones.
     * ResponseEntity.ok() genera una respuesta HTTP 200 (OK) con el cuerpo de la lista.
     */
    @Operation(summary = "Listar todas las interacciones") // Documentación de Swagger para este endpoint específico.
    @GetMapping
    public ResponseEntity<List<Interaccion>> listar() {
        return ResponseEntity.ok(interaccionService.listarTodas());
    }

    /**
     * Endpoint GET para buscar por identificador único.
     * @PathVariable extrae el valor "{id}" de la URL y lo inyecta en el parámetro.
     */
    @Operation(summary = "Obtener por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Interaccion> resultado = interaccionService.buscarPorId(id);
        
        // Verifica si el Optional contiene un valor. Si es así, retorna HTTP 200.
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        
        // Si no existe, retorna HTTP 404 (Not Found) estructurando la respuesta 
        // como un Map para que se serialice como un objeto JSON {"error": "..."}.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Interacción no encontrada con ID: " + id));
    }

    /**
     * Endpoint GET filtrado por un sub-recurso (cliente).
     */
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<Interaccion>> buscarPorCliente(
            @PathVariable String idCliente) {
        return ResponseEntity.ok(interaccionService.buscarPorCliente(idCliente));
    }

    // GET /interacciones/tipo/RECLAMO
    /**
     * Endpoint GET que filtra por tipo de interacción.
     * Spring Boot es capaz de convertir automáticamente el String de la URL 
     * ("RECLAMO", "CONSULTA", etc.) al Enum correspondiente (Interaccion.TipoInteraccion).
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<Object> buscarPorTipo(
            @PathVariable Interaccion.TipoInteraccion tipo) {
        return ResponseEntity.ok(interaccionService.buscarPorTipo(tipo));
    }

    // GET /interacciones/seguimiento/ABIERTO
    /**
     * Endpoint GET que filtra por estado de seguimiento, 
     * con conversión automática de String a Enum.
     */
    @GetMapping("/seguimiento/{seguimiento}")
    public ResponseEntity<Object> buscarPorSeguimiento(
            @PathVariable Interaccion.SeguimientoEstado seguimiento) {
        return ResponseEntity.ok(interaccionService.buscarPorSeguimiento(seguimiento));
    }

    /**
     * Endpoint POST para crear un nuevo recurso.
     * @Valid Activa la validación de Jakarta Bean Validation (ej. @NotNull, @Size) en el DTO/Entidad.
     * @RequestBody Mapea el cuerpo de la petición HTTP (JSON) al objeto Java 'Interaccion'.
     */
    @Operation(summary = "Crear nueva interacción")
    @PostMapping
    public ResponseEntity<Object> registrar(
            @Valid @RequestBody Interaccion interaccion) {
        try {
            // Retorna HTTP 201 (Created) estándar REST cuando se crea un recurso exitosamente.
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(interaccionService.registrar(interaccion));
        } catch (RuntimeException e) {
            // Captura las excepciones de negocio lanzadas por el servicio (ej. reglas incumplidas)
            // y devuelve un HTTP 400 (Bad Request) con el mensaje de error.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /interacciones/{id}/seguimiento?estado=EN_PROCESO
    /**
     * Endpoint PATCH para actualizaciones parciales.
     * A diferencia de PUT (que reemplaza el recurso entero), PATCH se usa para 
     * modificar un solo campo o estado (en este caso, el seguimiento).
     * @RequestParam extrae el parámetro 'estado' de la query string (la parte después del '?').
     */
    @PatchMapping("/{id}/seguimiento")
    public ResponseEntity<Object> cambiarSeguimiento(
            @PathVariable String id,
            @RequestParam Interaccion.SeguimientoEstado estado) {
        try {
            return ResponseEntity.ok(interaccionService.cambiarSeguimiento(id, estado));
        } catch (RuntimeException e) {
            // Manejo de errores: si la interacción no existe o incumple una regla de transición de estado.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint DELETE para eliminar recursos.
     */
    @Operation(summary = "Eliminar por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            interaccionService.eliminar(id);
            // Retorna HTTP 204 (No Content) estándar REST para operaciones de borrado exitosas 
            // indicando que todo salió bien pero no hay cuerpo de respuesta que enviar.
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}