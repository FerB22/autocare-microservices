package com.autocare.spare_parts_service.controller;

import com.autocare.spare_parts_service.model.Repuesto;
import com.autocare.spare_parts_service.service.RepuestoService;
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
 * Controlador REST para el microservicio de Repuestos (spare-parts-service).
 * Actúa como el punto de entrada HTTP (API Gateway/Frontend) para gestionar el inventario.
 * @RestController indica que cada método devuelve un objeto que será serializado 
 * automáticamente a JSON en el cuerpo de la respuesta HTTP.
 */
@Tag(name = "Repuesto", description = "Gestión de repuesto") // Configuración para documentar el endpoint en Swagger UI.
@RestController
@RequestMapping("/repuestos") // Establece la ruta base para todos los métodos de esta clase.
public class RepuestoController {

    // Dependencia inyectada del servicio. Se declara como 'final' para garantizar 
    // que el controlador siempre mantenga su capa lógica y sea inmutable.
    private final RepuestoService repuestoService;

    /**
     * Inyección de dependencias por constructor.
     * Es la práctica recomendada de Spring (sobre el uso de @Autowired en el campo),
     * ya que facilita la escritura de pruebas unitarias inyectando un mock del servicio.
     */
    public RepuestoController(RepuestoService repuestoService) {
        this.repuestoService = repuestoService;
    }

    /**
     * Endpoint GET genérico para obtener todo el catálogo de repuestos.
     * @return HTTP 200 (OK) con la lista completa.
     */
    @Operation(summary = "Listar repuestos")
    @GetMapping
    public ResponseEntity<List<Repuesto>> listar() {
        return ResponseEntity.ok(repuestoService.listarTodos());
    }

    /**
     * Endpoint GET para recuperar un repuesto específico usando su identificador interno (UUID).
     * @PathVariable vincula la parte de la ruta "{id}" al parámetro del método.
     */
    @Operation(summary = "Obtener por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Repuesto> resultado = repuestoService.buscarPorId(id);
        
        // Si el Optional tiene valor, se devuelve HTTP 200 con el repuesto.
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        
        // Si no existe, se construye una respuesta HTTP 404 (Not Found)
        // devolviendo un JSON estructurado con el mensaje de error.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Repuesto no encontrado con ID: " + id));
    }

    /**
     * Endpoint GET para recuperar un repuesto usando una clave de negocio (su código de parte).
     * Ideal para búsquedas que hacen los mecánicos directamente desde el sistema o lector de código.
     */
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Object> buscarPorCodigo(@PathVariable String codigo) {
        Optional<Repuesto> resultado = repuestoService.buscarPorCodigo(codigo);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Repuesto no encontrado con código: " + codigo));
    }

    /**
     * Endpoint GET optimizado para obtener solo los repuestos que tienen disponibilidad.
     * Muy útil para el frontend al llenar selectores desplegables al momento de cotizar.
     */
    @GetMapping("/con-stock")
    public ResponseEntity<List<Repuesto>> listarConStock() {
        return ResponseEntity.ok(repuestoService.listarConStock());
    }

    /**
     * Endpoint POST para registrar un nuevo repuesto en el inventario.
     * @Valid obliga a ejecutar las reglas de validación de Jakarta Bean (@NotBlank, @Min, etc.) 
     * definidas en el modelo antes de procesar la petición.
     * @RequestBody transforma el JSON que viene en la petición a un objeto Java.
     */
    @Operation(summary = "Crear nuevo repuesto")
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Repuesto repuesto) {
        try {
            // El estándar REST especifica que una creación exitosa debe devolver HTTP 201 (Created).
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(repuestoService.guardar(repuesto));
        } catch (RuntimeException e) {
            // Si el servicio detecta un problema de negocio (ej. Código duplicado),
            // se devuelve HTTP 409 (Conflict).
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /repuestos/{id}/reservar?cantidad=3
    /**
     * Endpoint POST que ejecuta una acción de negocio concreta (RPC style sobre REST).
     * En lugar de actualizar todo el repuesto con PUT, se utiliza este método para descontar
     * stock atómicamente de forma más segura.
     * @RequestParam extrae el valor "cantidad" desde los parámetros de la URL (query string).
     */
    @PostMapping("/{id}/reservar")
    public ResponseEntity<Object> reservar(@PathVariable String id,
                                            @RequestParam int cantidad) {
        try {
            return ResponseEntity.ok(repuestoService.reservar(id, cantidad));
        } catch (RuntimeException e) {
            // Si falla la validación de stock insuficiente o cantidad inválida, 
            // devolvemos un HTTP 400 (Bad Request).
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint PUT diseñado para actualizar la información completa de un repuesto existente.
     * De acuerdo a REST, PUT reemplaza la representación del recurso actual con la del payload.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id,
                                              @Valid @RequestBody Repuesto repuesto) {
        try {
            return ResponseEntity.ok(repuestoService.actualizar(id, repuesto));
        } catch (RuntimeException e) {
            // Si se intenta actualizar un ID que no existe, responde 404 (Not Found).
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint DELETE para eliminar (dar de baja) un repuesto del sistema.
     */
    @Operation(summary = "Eliminar por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            repuestoService.eliminar(id);
            // HTTP 204 (No Content) es la respuesta correcta para un borrado exitoso, 
            // indicando que todo salió bien pero no hay un body que devolver.
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}