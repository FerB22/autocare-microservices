package com.autocare.customer_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Modelo de dominio que representa a un Cliente.
 * Al formar parte de la capa de datos de la aplicación, esta clase define 
 * tanto la estructura en la base de datos relacional como las reglas de 
 * validación de los datos entrantes.
 */
@Data // Anotación de Lombok que inyecta automáticamente getters, setters, toString(), equals() y hashCode() en tiempo de compilación, manteniendo el código limpio de "boilerplate".
@Entity // Indica a JPA (Jakarta Persistence API) que esta clase es una entidad persistente que debe ser gestionada por el ORM (como Hibernate).
@Table(name = "clientes") // Especifica explícitamente el nombre de la tabla en la base de datos. Si se omite, el ORM usaría el nombre de la clase ("cliente").
public class Cliente {

    /**
     * Identificador único de la entidad.
     * El uso de UUIDs (Universally Unique Identifier) es una excelente práctica 
     * en arquitecturas distribuidas (como microservicios) porque asegura que 
     * el ID será único globalmente, evitando colisiones si se fusionan bases de datos.
     */
    @Id // Marca este campo como la clave primaria (Primary Key) de la tabla.
    @GeneratedValue(strategy = GenerationType.UUID) // Instruye al proveedor JPA para que genere automáticamente un UUID de 36 caracteres al insertar un nuevo registro.
    private String idCliente;

    // @NotBlank es parte de Jakarta Validation (validación a nivel de aplicación).
    // A diferencia de @NotNull, @NotBlank asegura que el string no sea null, 
    // no esté vacío ("") y no contenga únicamente espacios en blanco ("   ").
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    /**
     * Este campo demuestra una validación en dos capas distintas:
     * 1. Capa de Aplicación (@NotBlank, @Email): Se valida en el Controlador antes de ejecutar lógica de negocio.
     * 2. Capa de Base de Datos (@Column): Se definen las restricciones físicas (DDL) en el motor de base de datos.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato válido") // Aplica una expresión regular interna para asegurar que la cadena cumple con el formato estándar de correo.
    @Column(unique = true, nullable = false) // unique=true crea un índice único en la BD para evitar registros duplicados. nullable=false impone una restricción NOT NULL a nivel de tabla.
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    // Campo sin restricciones explícitas.
    // Al no tener anotaciones, el ORM lo mapeará por defecto a una columna 
    // tipo VARCHAR estándar en la base de datos que sí permite valores nulos (NULL).
    private String direccion;
}