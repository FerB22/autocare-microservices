package com.autocare.estimation_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Entidad que representa una Cotización dentro del dominio de negocio.
 * Mapea directamente a una tabla en la base de datos relacional.
 */
// ── ANOTACIONES DE LOMBOK ──────────────────────────────────────────────────
// En entidades JPA, a menudo se evita usar @Data porque sobrescribe los métodos 
// equals() y hashCode() de formas que pueden causar problemas de rendimiento o 
// bucles infinitos con relaciones bidireccionales (Lazy Loading).
// Por eso, es una buena práctica declarar explícitamente qué métodos autogenerar:
@Getter
@Setter
@NoArgsConstructor  // JPA requiere obligatoriamente un constructor vacío para instanciar la entidad mediante Reflection.
@AllArgsConstructor // Genera un constructor con todos los argumentos (útil para pruebas unitarias o builders).
@ToString
// ── ANOTACIONES JPA ────────────────────────────────────────────────────────
@Entity
@Table(name = "cotizaciones")
public class Cotizacion {

    /**
     * Clave primaria generada automáticamente como UUID (Universally Unique Identifier).
     * Ideal en microservicios, ya que previene colisiones de IDs entre diferentes
     * bases de datos y dificulta que un usuario adivine secuencias de cotizaciones.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idCotizacion;

    /**
     * Referencia "suave" (Soft Reference) a otro microservicio.
     * En una arquitectura de microservicios, no usamos @ManyToOne para enlazar 
     * entidades de dominios distintos (Orden y Repuesto) porque viven en bases de 
     * datos separadas. En su lugar, guardamos solo el ID como una cadena de texto.
     */
    @NotBlank(message = "El id de la orden es obligatorio")
    private String idOrden;

    @NotBlank(message = "El id del repuesto es obligatorio")
    private String idRepuesto;

    // ── PATRÓN DE DESNORMALIZACIÓN / CACHÉ LOCAL ──────────────────────────
    // Se llena consultando al spare-parts-service.
    // Guardamos una copia del nombre y precio en el momento exacto de la cotización.
    // Esto es vital: si el precio del repuesto cambia en el futuro en el microservicio
    // de inventario, el histórico de esta cotización NO debe verse alterado.
    private String nombreRepuesto;
    private Double precioUnitario;

    /**
     * Validaciones de Jakarta.
     * @NotNull asegura que el valor numérico no sea nulo.
     * @Min restringe el valor inferior a nivel de aplicación (antes de tocar la BD).
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidad;

    // Se calcula automáticamente: precioUnitario * cantidad
    // Almacenarlo físicamente en la BD (en lugar de calcularlo "al vuelo" con un getter)
    // optimiza futuras consultas y reportes financieros (ej. SUM(total_linea)).
    private Double totalLinea;

    /**
     * @Enumerated(EnumType.STRING) le indica a Hibernate que guarde el nombre 
     * exacto del enum ("PENDIENTE", "APROBADA") en la base de datos (tipo VARCHAR).
     * Si se usara EnumType.ORDINAL (el valor por defecto), guardaría un número (0, 1, 2), 
     * lo cual es peligroso porque si agregas un nuevo estado en medio del Enum 
     * en el futuro, todos los registros antiguos en la BD se corromperían lógicamente.
     */
    @Enumerated(EnumType.STRING)
    private EstadoCotizacion estado = EstadoCotizacion.PENDIENTE;

    /**
     * Máquina de estados finitos sencilla para la cotización.
     * Define los únicos ciclos de vida válidos para este registro.
     */
    public enum EstadoCotizacion {
        PENDIENTE, APROBADA, RECHAZADA
    }
}