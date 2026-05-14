package com.autocare.fleet_service.controller;

import com.autocare.fleet_service.dto.ClienteDTO;
import com.autocare.fleet_service.model.Vehiculo;
import com.autocare.fleet_service.service.VehiculoService;
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
 * Controlador REST para el microservicio de flota (fleet-service).
 * Gestiona los puntos de entrada HTTP para la administración de vehículos.
 * @RestController asegura que los retornos se serialicen como JSON.
 */
@Tag(name = "Vehículos", description = "Gestión de vehículos del taller") // Documentación OpenAPI/Swagger
@RestController
@RequestMapping("/vehiculos")
public class VehiculoController {

    // Se inyecta la capa de servicio, manteniendo la lógica de negocio 
    // separada de la lógica de enrutamiento web.
    private final VehiculoService vehiculoService;

    /**
     * Inyección por constructor.
     * Facilita el testing unitario y asegura que el controlador no se inicie 
     * en un estado inválido (sin su servicio).
     */
    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    // GET /vehiculos → lista todos
    /**
     * Endpoint GET para recuperar la colección completa de vehículos.
     * Retorna HTTP 200 (OK).
     */
    @Operation(summary = "Listar todos los vehículos")
    @GetMapping
    public ResponseEntity<List<Vehiculo>> listar() {
        return ResponseEntity.ok(vehiculoService.listarTodos());
    }

    // GET /vehiculos/patente/ABC123
    /**
     * Endpoint GET para buscar por una clave de negocio única (la patente).
     * @PathVariable extrae la patente directamente de la URI.
     */
    @GetMapping("/patente/{patente}")
    public ResponseEntity<Object> buscarPorPatente(@PathVariable String patente) {
        Optional<Vehiculo> resultado = vehiculoService.buscarPorPatente(patente);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        // Devuelve HTTP 404 estructurado en JSON si la patente no existe.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Vehículo no encontrado con patente: " + patente));
    }

    // GET /vehiculos/{id}
    /**
     * Endpoint GET estándar para obtener un recurso por su Clave Primaria (UUID).
     */
    @Operation(summary = "Obtener vehículo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        Optional<Vehiculo> resultado = vehiculoService.buscarPorId(id);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Vehículo no encontrado con ID: " + id));
    }

    // ✅ NUEVO: GET /vehiculos/{id}/duenio → consulta el dueño en customer-service
    /**
     * Patrón de Sub-recurso y Composición de API en Microservicios.
     * Este endpoint permite a los clientes consultar la información del dueño 
     * navegando a través de la relación del vehículo.
     * 
     * Funciona como un orquestador ligero: primero valida el recurso local, 
     * y luego delega la consulta remota al customer-service.
     */
    @Operation(summary = "Obtener el dueño de un vehículo")
    @GetMapping("/{id}/duenio")
    public ResponseEntity<Object> obtenerDuenio(@PathVariable String id) {
        // Primero buscamos el vehículo para obtener el idDuenio localmente.
        Optional<Vehiculo> resultado = vehiculoService.buscarPorId(id);
        if (resultado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Vehículo no encontrado con ID: " + id));
        }

        try {
            // Se extrae la referencia externa (foreign key lógica).
            String idDuenio = resultado.get().getIdDuenio();
            // El servicio realiza una llamada HTTP síncrona y mapea el JSON a un DTO.
            ClienteDTO cliente = vehiculoService.obtenerDuenio(idDuenio);
            return ResponseEntity.ok(cliente); // HTTP 200 OK
        } catch (Exception e) {
            // Resiliencia: Si el customer-service está caído o responde con error,
            // capturamos la falla y devolvemos un HTTP 503 (Service Unavailable)
            // en lugar de un error genérico 500. Esto informa al cliente frontend
            // que el problema es de red/disponibilidad y podría reintentar más tarde.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "No se pudo consultar el customer-service: " + e.getMessage()));
        }
    }

    // POST /vehiculos → crea nuevo
    /**
     * Endpoint POST para insertar un nuevo vehículo.
     * @Valid obliga a pasar por las restricciones de Jakarta (@NotBlank, Regex, etc.).
     */
    @Operation(summary = "Crear un nuevo vehículo")
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody Vehiculo vehiculo) {
        try {
            Vehiculo nuevo = vehiculoService.guardar(vehiculo);
            // Si se inserta correctamente, retorna HTTP 201 (Created).
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            // Si falla una regla de negocio (ej. Patente ya registrada),
            // se devuelve HTTP 409 (Conflict).
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /vehiculos/{id} → actualiza
    /**
     * Endpoint PUT para la actualización o reemplazo del recurso.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id,
                                              @Valid @RequestBody Vehiculo vehiculo) {
        try {
            return ResponseEntity.ok(vehiculoService.actualizar(id, vehiculo));
        } catch (RuntimeException e) {
            // Si el ID a actualizar no existe, responde con HTTP 404 (Not Found).
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /vehiculos/{id} → elimina
    /**
     * Endpoint DELETE para el borrado del recurso.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            vehiculoService.eliminar(id);
            // Retorna HTTP 204 (No Content) estándar para eliminaciones exitosas.
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}