package com.autocare.customer_service.controller;

import com.autocare.customer_service.model.Cliente;
import com.autocare.customer_service.service.ClienteService;
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
 * Controlador REST que expone los endpoints para la gestión de clientes.
 * @RestController Indica a Spring que esta clase procesará peticiones web y
 * serializará automáticamente las respuestas en el cuerpo (body) en formato JSON.
 * @RequestMapping("/clientes") Define la ruta base para todos los métodos HTTP de esta clase.
 */
@Tag(name = "Clientes", description = "Gestión de clientes del taller") // Documentación interactiva de Swagger/OpenAPI
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    // Dependencia del servicio para manejar la lógica de negocio de los clientes.
    // Declarada como 'final' para garantizar su inmutabilidad una vez inyectada.
    private final ClienteService clienteService;

    /**
     * Inyección de dependencias a través del constructor.
     * Esta es la práctica recomendada en Spring Boot en lugar de usar @Autowired,
     * ya que facilita el testing y asegura que la dependencia no sea nula.
     */
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * Endpoint GET para listar todos los clientes registrados.
     * @return Respuesta HTTP 200 (OK) con la lista completa.
     */
    @Operation(summary = "Listar todos los clientes")
    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        // ResponseEntity.ok() es un atajo (builder) para retornar status 200
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    /**
     * Endpoint GET para buscar un cliente específico por su identificador único.
     * @PathVariable vincula el valor "{id}" de la URL al parámetro del método.
     */
    @Operation(summary = "Obtener cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Cliente> resultado = clienteService.buscarPorId(id);
        
        // Se evalúa si el Optional contiene datos
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get()); // HTTP 200 OK
        }
        
        // Si no se encuentra, se arma manualmente una respuesta HTTP 404 (Not Found)
        // usando un Map para estructurar el error en formato JSON: {"error": "..."}
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Cliente no encontrado con ID: " + id));
    }

    /**
     * Endpoint GET adicional para buscar a un cliente mediante su correo electrónico.
     * Útil para validaciones o búsquedas donde no se conoce el ID interno.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<Object> buscarPorEmail(@PathVariable String email) {
        Optional<Cliente> resultado = clienteService.buscarPorEmail(email);
        
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        
        // Retorna HTTP 404 si el email no existe en la base de datos
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Cliente no encontrado con email: " + email));
    }

    /**
     * Endpoint POST para registrar un nuevo cliente en el sistema.
     * @Valid Ejecuta las validaciones de Jakarta (ej. @NotBlank, @Email) sobre el DTO/Modelo.
     * @RequestBody Convierte el JSON entrante de la petición HTTP al objeto Java 'Cliente'.
     */
    @Operation(summary = "Crear un nuevo cliente")
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Cliente cliente) {
        try {
            // Si la creación es exitosa, se retorna HTTP 201 (Created), que es el
            // estándar REST correcto cuando se inserta un nuevo recurso.
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.guardar(cliente));
        } catch (RuntimeException e) {
            // Se asume que el servicio lanza una excepción si se viola una regla (ej. email duplicado).
            // Se responde con HTTP 409 (Conflict) indicando que hay un conflicto con el estado actual del servidor.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint PUT para actualizar la información de un cliente existente.
     * La convención REST del método PUT implica que se actualiza (o reemplaza)
     * el recurso completo en base al ID proporcionado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id,
                                              @Valid @RequestBody Cliente cliente) {
        try {
            // Retorna HTTP 200 (OK) junto con el objeto cliente modificado
            return ResponseEntity.ok(clienteService.actualizar(id, cliente));
        } catch (RuntimeException e) {
            // Si el cliente con ese ID no existe, el servicio lanza una excepción 
            // que se captura aquí para devolver HTTP 404 (Not Found).
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint DELETE para eliminar (física o lógicamente) a un cliente de la base de datos.
     */
    @Operation(summary = "Eliminar cliente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            clienteService.eliminar(id);
            // HTTP 204 (No Content) indica que la operación fue un éxito, 
            // pero que el servidor no devolverá ningún contenido en el body.
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // Retorna HTTP 404 si se intenta eliminar un ID inexistente
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}