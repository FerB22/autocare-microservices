package com.autocare.notification_service.controller;

import com.autocare.notification_service.model.Notificacion;
import com.autocare.notification_service.service.NotificacionService;
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
 * Controlador REST para el microservicio de Notificaciones.
 * Actúa como la capa de presentación HTTP, recibiendo las peticiones de otros 
 * microservicios (o del frontend) y delegando la lógica de negocio al NotificacionService.
 * @RestController Combina @Controller y @ResponseBody para devolver directamente JSON.
 */
@Tag(name = "Notificaciones", description = "Gestión de notificaciones del taller") // Documentación para Swagger/OpenAPI
@RestController
@RequestMapping("/notificaciones") // Define la ruta base para todos los endpoints de esta clase
public class NotificacionController {

    // Dependencia del servicio declarada como final para garantizar su inmutabilidad.
    private final NotificacionService notificacionService;

    /**
     * Inyección de dependencias por constructor.
     * Patrón recomendado en Spring Boot para asegurar que el controlador nunca se 
     * instancie sin su capa de servicio, facilitando además el testing con Mocks.
     */
    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /**
     * Endpoint GET para recuperar el listado completo de notificaciones.
     * Retorna HTTP 200 (OK) implícito a través del método de conveniencia ok().
     */
    @GetMapping
    public ResponseEntity<List<Notificacion>> listar() {
        return ResponseEntity.ok(notificacionService.listarTodas());
    }

    /**
     * Endpoint GET para recuperar una notificación específica por su ID.
     * (Nota: El summary de la anotación @Operation parece haber sido copiado por error 
     * en el código original, pero funcionalmente este endpoint busca por ID).
     */
    @Operation(summary = "Listar todas las notificaciones") 
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Notificacion> resultado = notificacionService.buscarPorId(id);
        
        // Si el Optional contiene datos, devuelve 200 OK con la entidad.
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        
        // Si no existe, arma manualmente un HTTP 404 (Not Found) con un JSON explicativo.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Notificación no encontrada con ID: " + id));
    }

    /**
     * Endpoint GET que implementa un patrón de sub-recurso lógico.
     * Permite consultar todas las notificaciones que le pertenecen a un destinatario 
     * específico (ej. un cliente o un mecánico).
     */
    @Operation(summary = "Obtener por destinatario")
    @GetMapping("/destinatario/{idDestinatario}")
    public ResponseEntity<List<Notificacion>> buscarPorDestinatario(
            @PathVariable String idDestinatario) {
        return ResponseEntity.ok(
            notificacionService.buscarPorDestinatario(idDestinatario));
    }

    // GET /notificaciones/destinatario/{id}/no-leidas
    /**
     * Endpoint GET altamente específico y optimizado para la interfaz de usuario (UI).
     * Ideal para mostrar el "contador de campanita" (badge) en el frontend trayendo 
     * exclusivamente las notificaciones pendientes de lectura de un usuario.
     */
    @GetMapping("/destinatario/{idDestinatario}/no-leidas")
    public ResponseEntity<List<Notificacion>> noLeidasPorDestinatario(
            @PathVariable String idDestinatario) {
        return ResponseEntity.ok(
            notificacionService.buscarNoLeidasPorDestinatario(idDestinatario));
    }

    @Operation(summary = "Obtener por tipo")
    // GET /notificaciones/tipo/ORDEN_CREADA
    /**
     * Endpoint GET para filtrar notificaciones por su categoría (Enum).
     * Spring Boot se encarga de convertir el String de la URL directamente 
     * al tipo TipoNotificacion del Enum, arrojando error automático si no coincide.
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<Object> buscarPorTipo(
            @PathVariable Notificacion.TipoNotificacion tipo) {
        return ResponseEntity.ok(notificacionService.buscarPorTipo(tipo));
    }

    @Operation(summary = "Eviar notificación")
    /**
     * Endpoint POST para "crear/enviar" una nueva notificación.
     * @Valid exige que el JSON entrante cumpla con las restricciones del modelo (@NotBlank, @NotNull).
     * @RequestBody transforma el payload de la petición HTTP en el objeto Java Notificacion.
     */
    @PostMapping
    public ResponseEntity<Object> enviar(
            @Valid @RequestBody Notificacion notificacion) {
        try {
            // Cumpliendo con REST, la creación exitosa de un recurso debe retornar HTTP 201 (Created).
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificacionService.enviar(notificacion));
        } catch (RuntimeException e) {
            // Manejo de errores de reglas de negocio capturadas en el Service.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /notificaciones/{id}/leer
    /**
     * Endpoint PATCH destinado a actualizaciones parciales.
     * Como solo se modifica el estado de lectura y no el recurso completo,
     * el estándar REST recomienda usar PATCH en lugar de PUT.
     */
    @PatchMapping("/{id}/leer")
    public ResponseEntity<Object> marcarComoLeida(@PathVariable String id) {
        try {
            return ResponseEntity.ok(notificacionService.marcarComoLeida(id));
        } catch (RuntimeException e) {
            // Retorna HTTP 404 si el ID provisto no existe en la base de datos.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar por ID")
    /**
     * Endpoint DELETE para el borrado (físico o lógico) del recurso.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            notificacionService.eliminar(id);
            // Retorna HTTP 204 (No Content) estándar para indicar que la eliminación
            // tuvo éxito y que el servidor no tiene información adicional para devolver en el body.
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}