package com.autocare.notification_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idNotificacion;

    @NotBlank(message = "El destinatario es obligatorio")
    private String idDestinatario;

    @NotNull(message = "El tipo es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoNotificacion tipo;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    // Referencia opcional al recurso que originó la notificación
    private String idReferencia; // ej: idOrden, idFactura, idCotizacion

    private LocalDateTime fechaEnvio;

    @Enumerated(EnumType.STRING)
    private EstadoNotificacion estado = EstadoNotificacion.NO_LEIDA;

    public enum TipoNotificacion {
        ORDEN_CREADA,
        COTIZACION_APROBADA,
        COTIZACION_RECHAZADA,
        FACTURA_EMITIDA,
        FACTURA_PAGADA,
        SERVICIO_COMPLETADO,
        ALERTA_STOCK
    }

    public enum EstadoNotificacion {
        NO_LEIDA, LEIDA
    }
}