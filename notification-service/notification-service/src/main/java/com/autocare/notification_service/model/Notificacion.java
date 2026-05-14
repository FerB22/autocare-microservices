package com.autocare.notification_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa una Notificación en el sistema.
 * Mapea directamente a la tabla "notificaciones" en la base de datos relacional.
 */
// ── ANOTACIONES DE LOMBOK ──────────────────────────────────────────────────
// Se utilizan anotaciones separadas (@Getter, @Setter, etc.) en lugar de @Data
// para evitar problemas conocidos de rendimiento en entidades JPA relacionados 
// con la generación automática de los métodos equals() y hashCode() sobre 
// atributos que podrían tener Lazy Loading.
@Getter
@Setter
@NoArgsConstructor  // Constructor vacío requerido obligatoriamente por JPA/Hibernate.
@AllArgsConstructor // Constructor con todos los argumentos, útil para testing.
@ToString
// ── ANOTACIONES JPA ────────────────────────────────────────────────────────
@Entity // Indica que esta clase será gestionada por el ORM (Hibernate).
@Table(name = "notificaciones") // Nombra explícitamente la tabla en la base de datos.
public class Notificacion {

    /**
     * Clave primaria autogenerada usando UUID.
     * En sistemas distribuidos, usar UUID es el estándar porque garantiza 
     * unicidad global sin depender de un autoincremental de la base de datos, 
     * lo cual facilita la escalabilidad y migraciones.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idNotificacion;

    /**
     * Relación "Suave" (Soft Foreign Key).
     * Como el destinatario (cliente, mecánico o administrador) pertenece a 
     * otro microservicio, aquí guardamos solo su UUID como un String plano. 
     * @NotBlank asegura que la API no acepte notificaciones sin un dueño asignado.
     */
    @NotBlank(message = "El destinatario es obligatorio")
    private String idDestinatario;

    /**
     * @NotNull obliga a que el valor esté presente en el JSON de la petición.
     * @Enumerated(EnumType.STRING) es crítico aquí: le dice a Hibernate que 
     * guarde el nombre literal del Enum ("ORDEN_CREADA") en la base de datos
     * y no su posición numérica (0, 1, 2). Esto evita que la base de datos se 
     * corrompa si en el futuro cambias el orden de las constantes del Enum.
     */
    @NotNull(message = "El tipo es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoNotificacion tipo;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    // Referencia opcional al recurso que originó la notificación
    // ej: idOrden, idFactura, idCotizacion
    // Es otra "Soft Reference" que permite trazabilidad. Por ejemplo, al hacer
    // clic en la notificación en el frontend, la app sabrá a qué ID de orden navegar.
    private String idReferencia; 

    // Campo de auditoría. Se usa LocalDateTime (Java 8+) que es inmutable 
    // y más seguro que la antigua clase Date.
    private LocalDateTime fechaEnvio;

    // El estado inicia por defecto en NO_LEIDA.
    // También guardado como STRING por seguridad estructural.
    @Enumerated(EnumType.STRING)
    private EstadoNotificacion estado = EstadoNotificacion.NO_LEIDA;

    /**
     * Catálogo cerrado de eventos que el sistema es capaz de notificar.
     * Usar un Enum en lugar de un String libre asegura la consistencia de los 
     * datos e impide que se inserten categorías con errores tipográficos.
     */
    public enum TipoNotificacion {
        ORDEN_CREADA,
        COTIZACION_APROBADA,
        COTIZACION_RECHAZADA,
        FACTURA_EMITIDA,
        FACTURA_PAGADA,
        SERVICIO_COMPLETADO,
        ALERTA_STOCK
    }

    /**
     * Máquina de estado sencilla para el ciclo de vida de la notificación.
     */
    public enum EstadoNotificacion {
        NO_LEIDA, LEIDA
    }
}