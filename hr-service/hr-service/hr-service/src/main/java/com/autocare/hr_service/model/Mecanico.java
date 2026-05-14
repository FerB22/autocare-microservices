package com.autocare.hr_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad de dominio que representa a un Mecánico en el microservicio de Recursos Humanos.
 * Mapea directamente a la tabla "mecanicos" en la base de datos relacional.
 */
// ── ANOTACIONES DE LOMBOK ──────────────────────────────────────────────────
// Se recomienda evitar @Data en entidades JPA (@Entity) porque autogenera los 
// métodos equals() y hashCode() evaluando todos los campos, lo que puede causar 
// graves problemas de rendimiento o bucles infinitos si hay relaciones (Lazy Loading).
// Al separar explícitamente @Getter, @Setter y @ToString, mantenemos los beneficios
// de reducir el código "boilerplate" sin los riesgos asociados a @Data.
@Getter
@Setter
@NoArgsConstructor  // Requerido obligatoriamente por JPA/Hibernate para instanciar la entidad internamente usando Reflection.
@AllArgsConstructor // Útil para crear instancias rápidas en pruebas unitarias o patrones Builder.
@ToString
// ── ANOTACIONES JPA ────────────────────────────────────────────────────────
@Entity // Marca la clase como una entidad persistente gestionada por el ORM.
@Table(name = "mecanicos") // Especifica el nombre explícito de la tabla en el motor de base de datos.
public class Mecanico {

    /**
     * Clave Primaria (PK) de la tabla.
     * La estrategia GenerationType.UUID delega a la aplicación/ORM la creación de 
     * un identificador universal único. Esto es altamente recomendado en arquitecturas 
     * de microservicios para evitar colisiones si en el futuro se unifican o migran 
     * bases de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idMecanico;

    /**
     * Validación de Jakarta (Nivel de Aplicación).
     * @NotBlank asegura que cuando se reciba un JSON en el Controlador, este campo
     * no sea nulo ("null"), no esté vacío ("") y no tenga solo espacios ("   ").
     */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /**
     * Validación de doble capa (Aplicación y Base de Datos).
     * @NotBlank: Detiene las peticiones inválidas antes de ejecutar lógica de negocio.
     * @Column(nullable = false): Aplica una restricción física de "NOT NULL" en el esquema 
     * DDL de la base de datos (PostgreSQL en este caso), protegiendo la integridad de los 
     * datos frente a inserciones manuales directas o fallos de la aplicación.
     */
    @NotBlank(message = "La especialidad es obligatoria")
    @Column(nullable = false)
    private String especialidad; // "Frenos", "Motor", "Eléctrico", etc.

    /**
     * Estado lógico por defecto.
     * Al registrar un mecánico, asume directamente que está disponible (true) para 
     * aceptar órdenes de trabajo. No se requiere anotación especial porque el ORM 
     * mapea el tipo boolean primitivo al tipo BOOLEAN de la base de datos de manera natural.
     */
    private boolean estaDisponible = true;
}