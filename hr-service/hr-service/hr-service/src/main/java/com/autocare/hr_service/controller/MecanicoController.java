package com.autocare.hr_service.controller;

import com.autocare.hr_service.model.Mecanico;
import com.autocare.hr_service.service.MecanicoService;
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
 * Controlador REST para el microservicio de Recursos Humanos (hr-service).
 * Expone los endpoints HTTP para administrar el ciclo de vida y disponibilidad de los mecánicos.
 * @RestController Asegura que todos los métodos devuelvan los datos serializados en el body (JSON).
 */
@Tag(name = "Mecánicos", description = "Gestión de mecánicos del taller") // Anotación para documentar el controlador en Swagger UI.
@RestController
@RequestMapping("/mecanicos")
public class MecanicoController {

    // Dependencia del servicio inyectada. Al ser 'final', garantizamos que 
    // su estado es inmutable una vez que el bean es inicializado por Spring.
    private final MecanicoService mecanicoService;

    /**
     * Inyección de dependencias por constructor.
     * Es la forma recomendada en Spring Boot, ya que facilita la creación de 
     * pruebas unitarias inyectando un mock del servicio sin necesidad de usar Reflection.
     */
    public MecanicoController(MecanicoService mecanicoService) {
        this.mecanicoService = mecanicoService;
    }

    /**
     * Endpoint GET genérico para listar la colección completa.
     * Retorna HTTP 200 (OK) por defecto mediante ResponseEntity.ok().
     */
    @Operation(summary = "Listar todos los mecánicos")
    @GetMapping
    public ResponseEntity<List<Mecanico>> listar() {
        return ResponseEntity.ok(mecanicoService.listarTodos());
    }

    /**
     * Endpoint GET para recuperar un mecánico específico por su clave primaria.
     * @PathVariable extrae la variable "{id}" de la URI y la mapea al parámetro.
     */
    @Operation(summary = "Obtener por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Mecanico> resultado = mecanicoService.buscarPorId(id);
        
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        
        // Si no se encuentra, es imperativo devolver HTTP 404 (Not Found) estructurando 
        // la respuesta en un JSON mediante Map.of() para facilitar la lectura en el frontend.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Mecánico no encontrado con ID: " + id));
    }

    // GET /mecanicos/disponibles → lista los libres
    /**
     * Endpoint GET de filtrado rápido.
     * Muy útil en la arquitectura para que el 'order-service' consulte 
     * a quién puede asignarle un nuevo trabajo de forma inmediata.
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Mecanico>> listarDisponibles() {
        return ResponseEntity.ok(mecanicoService.buscarDisponibles());
    }

    // GET /mecanicos/especialidad/Motor → filtra por especialidad
    /**
     * Endpoint GET que filtra la colección basándose en un atributo específico.
     */
    @GetMapping("/especialidad/{especialidad}")
    public ResponseEntity<List<Mecanico>> buscarPorEspecialidad(
            @PathVariable String especialidad) {
        return ResponseEntity.ok(mecanicoService.buscarPorEspecialidad(especialidad));
    }

    // GET /mecanicos/disponibles/Motor → libres Y con esa especialidad
    /**
     * Endpoint GET con filtrado combinado.
     * Agrupa criterios (disponibilidad + especialidad) en una sola ruta para 
     * optimizar las consultas y reducir las peticiones desde el cliente.
     */
    @GetMapping("/disponibles/{especialidad}")
    public ResponseEntity<List<Mecanico>> disponiblesPorEspecialidad(
            @PathVariable String especialidad) {
        return ResponseEntity.ok(mecanicoService.buscarDisponiblesPorEspecialidad(especialidad));
    }

    /**
     * Endpoint POST para registrar un nuevo recurso.
     * @Valid Activa la validación de Jakarta Bean Validation sobre el DTO/Modelo de entrada.
     * @RequestBody Mapea el cuerpo JSON entrante hacia el objeto Java 'Mecanico'.
     */
    @Operation(summary = "Crear nuevo mecánico")
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Mecanico mecanico) {
        try {
            // Un POST exitoso debe devolver HTTP 201 (Created) según las convenciones REST.
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(mecanicoService.guardar(mecanico));
        } catch (RuntimeException e) {
            // Si se rompe una regla de negocio en el servicio, atrapamos la excepción 
            // y devolvemos un HTTP 400 (Bad Request).
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /mecanicos/{id}/disponibilidad?disponible=false
    /**
     * Endpoint PATCH para actualizaciones parciales.
     * El estándar REST dicta que si solo mutamos un atributo (la disponibilidad), 
     * se debe usar PATCH y no PUT.
     * @RequestParam extrae el valor booleano desde la cadena de consulta (query string).
     */
    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<Object> cambiarDisponibilidad(
            @PathVariable String id,
            @RequestParam boolean disponible) {
        try {
            return ResponseEntity.ok(mecanicoService.cambiarDisponibilidad(id, disponible));
        } catch (RuntimeException e) {
            // Si el mecánico no existe, devuelve HTTP 404.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint PUT para la actualización completa (reemplazo) del recurso.
     * Exige que se envíe la representación completa del objeto Mecanico.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id,
                                              @Valid @RequestBody Mecanico mecanico) {
        try {
            return ResponseEntity.ok(mecanicoService.actualizar(id, mecanico));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint DELETE para la eliminación de registros.
     */
    @Operation(summary = "Eliminar por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            mecanicoService.eliminar(id);
            // Retorna HTTP 204 (No Content), indicando que la operación de borrado 
            // fue exitosa y no se requiere enviar cuerpo de respuesta.
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}