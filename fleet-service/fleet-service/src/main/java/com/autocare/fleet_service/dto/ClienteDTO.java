package com.autocare.fleet_service.dto;

import lombok.Data;

/**
 * Patrón DTO (Data Transfer Object).
 * Un DTO es un objeto plano (POJO) que se utiliza exclusivamente para transportar 
 * datos entre procesos o capas de una aplicación, sin contener lógica de negocio.
 * 
 * En una arquitectura de microservicios, el "fleet-service" no tiene acceso directo 
 * a la base de datos de clientes. Por lo tanto, utiliza esta clase para mapear 
 * (deserializar) la respuesta JSON que recibe cuando hace una petición HTTP 
 * al "customer-service" a través de un RestTemplate o FeignClient.
 */
@Data 
// A diferencia de las entidades JPA (@Entity) donde @Data puede ser peligroso por las 
// relaciones bidireccionales y el Lazy Loading, en un DTO es la anotación perfecta.
// Genera de forma segura todos los getters, setters, constructores requeridos 
// genéricos, equals(), hashCode() y toString() necesarios para que librerías como 
// Jackson (el conversor JSON de Spring) puedan instanciar y poblar esta clase.
public class ClienteDTO {
    
    // NOTA DE INTEGRACIÓN:
    // Los nombres de estos atributos deben coincidir exactamente con las claves (keys) 
    // del JSON que devuelve el endpoint del customer-service. 
    // Si el customer-service enviara {"id_cliente": "..."}, Jackson no lo mapearía 
    // automáticamente a "idCliente" sin usar la anotación @JsonProperty("id_cliente").
    
    private String idCliente;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
}