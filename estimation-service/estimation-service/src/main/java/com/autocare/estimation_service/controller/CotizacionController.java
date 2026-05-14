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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST que expone la API para la gestión de cotizaciones.
 * @RestController Combina @Controller y @ResponseBody, garantizando que el retorno
 * de los métodos se serialice automáticamente a JSON en el cuerpo de la respuesta HTTP.
 * @RequestMapping("/cotizaciones") Establece la ruta base para todos los endpoints de la clase.
 */
@Tag(name = "Cotizaciones", description = "Gestión de cotizaciones del taller") // Documentación para la interfaz de Swagger UI
@RestController
@RequestMapping("/cotizaciones")
public class CotizacionController {

    // Dependencia inyectada del servicio. Declarada como final para asegurar 
    // su inmutabilidad y que el controlador no pierda su capa lógica.
    private final CotizacionService cotizacionService;

    /**
     * Inyección de dependencias mediante constructor.
     * Es el patrón recomendado por Spring frente a @Autowired en la propiedad, 
     * ya que facilita la creación de pruebas unitarias y asegura la integridad del componente.
     */
    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    /**
     * Endpoint GET genérico para recuperar todas las cotizaciones.
     * Retorna un código HTTP 200 (OK) por defecto al usar ResponseEntity.ok().
     */
    @Operation(summary = "Listar todas las cotizaciones")
    @GetMapping
    public ResponseEntity<List<Cotizacion>> listar() {
        return ResponseEntity.ok(cotizacionService.listarTodas());
    }

    /**
     * Endpoint GET para recuperar un recurso específico por su identificador.
     * @PathVariable vincula la variable de la URI "{id}" con el parámetro del método.
     */
    @Operation(summary = "Obtener por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Cotizacion> resultado = cotizacionService.buscarPorId(id);
        
        // Patrón estándar de resolución: si existe, devuelve 200 OK con los datos.
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        
        // Si no existe, devuelve explícitamente 404 Not Found con un JSON descriptivo.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Cotización no encontrada con ID: " + id));
    }

    /**
     * Endpoint GET que filtra los recursos basándose en la relación con otra entidad (Orden).
     * Representa una ruta de sub-recurso lógica en el diseño REST.
     */
    @GetMapping("/orden/{idOrden}")
    public ResponseEntity<List<Cotizacion>> buscarPorOrden(@PathVariable String idOrden) {
        return ResponseEntity.ok(cotizacionService.buscarPorOrden(idOrden));
    }

    // GET /cotizaciones/estado/PENDIENTE
    /**
     * Endpoint GET para filtrar por estado.
     * Spring Boot tiene la capacidad de mapear y convertir automáticamente la cadena
     * de texto enviada en la URL a la constante del Enum (Cotizacion.EstadoCotizacion).
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<Object> buscarPorEstado(
            @PathVariable Cotizacion.EstadoCotizacion estado) {
        return ResponseEntity.ok(cotizacionService.buscarPorEstado(estado));
    }

    /**
     * Endpoint POST para la creación de una nueva cotización.
     * @Valid Detona las validaciones de Jakarta Bean Validation configuradas en la entidad/DTO.
     * @RequestBody Deserializa el JSON del cliente y lo convierte en un objeto Java.
     */
    @Operation(summary = "Crear nueva cotización")
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Cotizacion cotizacion) {
        try {
            // Cumpliendo con REST, el éxito en la creación de un recurso devuelve HTTP 201 (Created).
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(cotizacionService.crear(cotizacion));
        } catch (RuntimeException e) {
            // Si el servicio detecta una violación de reglas de negocio, se captura la excepción
            // y se traduce a un error HTTP 400 (Bad Request).
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /cotizaciones/{id}/estado?estado=APROBADA
    /**
     * Endpoint PATCH destinado a actualizaciones parciales (en este caso, solo el estado).
     * @RequestParam extrae valores de los parámetros de consulta de la URL (?estado=...).
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(
            @PathVariable String id,
            @RequestParam Cotizacion.EstadoCotizacion estado) {
        try {
            return ResponseEntity.ok(cotizacionService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            // Se asume que si falla el cambio de estado, o el ID no existe o 
            // la transición de estado es inválida. En este bloque devuelve 404.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint DELETE para eliminar cotizaciones del sistema.
     */
    @Operation(summary = "Eliminar por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            cotizacionService.eliminar(id);
            // HTTP 204 (No Content) es el estándar REST para indicar que la acción
            // tuvo éxito y ya no hay nada más que decir (sin cuerpo de respuesta).
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}