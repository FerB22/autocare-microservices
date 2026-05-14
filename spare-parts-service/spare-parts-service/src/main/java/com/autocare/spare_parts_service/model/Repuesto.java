package com.autocare.spare_parts_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.NonNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

// ✅ Después
/**
 * Entidad de dominio que representa un Repuesto o pieza en el inventario.
 * Mapea directamente a la tabla "repuestos" en la base de datos relacional.
 */
// ── ANOTACIONES DE LOMBOK ──────────────────────────────────────────────────
// Se utilizan anotaciones específicas (@Getter, @Setter, @ToString) en lugar 
// del atajo @Data. Esto es una buena práctica en entidades JPA para evitar 
// que se autogeneren métodos equals() y hashCode() que puedan causar 
// problemas de rendimiento o recursión infinita al evaluar relaciones (Lazy Loading).
@Getter
@Setter
@NoArgsConstructor  // Constructor sin argumentos, requerido por el framework de persistencia (Hibernate/JPA).
@AllArgsConstructor // Constructor con todos los argumentos, útil para crear instancias rápidamente (ej. en Tests).
@ToString
// ── ANOTACIONES JPA ────────────────────────────────────────────────────────
@Entity // Le indica a Spring/Hibernate que esta clase debe ser persistida en la base de datos.
@Table(name = "repuestos") // Especifica explícitamente el nombre de la tabla para mantener convenciones SQL.
public class Repuesto {

    /**
     * Clave Primaria (PK).
     * Utiliza un UUID (Identificador Único Universal) autogenerado. 
     * Es ideal en arquitecturas de microservicios ya que previene colisiones 
     * de IDs si se necesita escalar o fusionar bases de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idRepuesto;

    /**
     * Validación a nivel de aplicación (Jakarta Validation).
     * @NotBlank asegura que el cliente no envíe un JSON con un nombre vacío,
     * nulo o compuesto únicamente por espacios en blanco.
     * @NonNull (Lombok) lanza un NullPointerException rápido si se intenta 
     * instanciar el objeto internamente sin este valor.
     */
    @NotBlank(message = "El nombre es obligatorio")
    @NonNull
    private String nombre;

    /**
     * Clave de negocio (Business Key).
     * Se aplican validaciones en dos capas de seguridad:
     * 1. @NotBlank: Bloquea las peticiones web mal formadas (Application Layer).
     * 2. @Column: Asegura la integridad a nivel de esquema (Database Layer),
     * garantizando que el motor SQL rechace nulos y creando un índice UNIQUE
     * para que no existan dos piezas con el mismo código de fabricante.
     */
    @NotBlank(message = "El código de parte es obligatorio")
    @NonNull
    @Column(unique = true, nullable = false)
    private String codigoParte;

    /**
     * Gestión de Inventario.
     * @NotNull exige que el campo venga en el payload JSON.
     * @Min(0) es una regla de negocio declarativa que impide al controlador 
     * aceptar cantidades negativas antes de que lleguen a la capa de servicio.
     */
    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    /**
     * Valores monetarios.
     * @DecimalMin funciona de manera similar a @Min, pero es específico para 
     * tipos de datos con decimales, evitando precios ilógicos en el sistema.
     */
    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private Double precioUnitario;

    /**
     * Campo opcional. 
     * No posee anotaciones de validación porque puede que un repuesto recién 
     * ingresado aún no tenga un pasillo o estante físico asignado en la bodega.
     */
    private String ubicacionBodega;
}