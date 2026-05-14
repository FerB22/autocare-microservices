package com.autocare.booking_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una Cita en el sistema de reserva. [cite: 108]
 * - Utiliza Lombok (@Data) para reducir código boilerplate (getters/setters). [cite: 315]
 * - Define la estructura de la tabla 'citas' en la base de datos de persistencia. [cite: 36]
 */
@Data
@Entity
@Table(name = "citas")
public class Cita {

    /**
     * Identificador único de la cita.
     * Se genera automáticamente usando la estrategia UUID para mayor escalabilidad
     * en entornos distribuidos/microservicios.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idCita;

    /**
     * Referencia lógica al vehículo (ID proveniente de fleet-service). [cite: 89]
     * La validación @NotBlank asegura que no se procesen registros sin un vehículo asociado. [cite: 290]
     */
    @NotBlank(message = "El id del vehículo es obligatorio")
    private String idVehiculo;

    /**
     * Referencia lógica al cliente (ID proveniente de customer-service). [cite: 111]
     * Es fundamental para la trazabilidad y comunicación con el microservicio de clientes.
     */
    @NotBlank(message = "El id del cliente es obligatorio")
    private String idCliente;

    /**
     * Fecha y hora programada para la cita.
     * - @NotNull: Campo obligatorio en la creación. [cite: 322]
     * - @Future: Regla de negocio crítica que impide agendar citas en el pasado.
     */
    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La cita debe ser en una fecha futura")
    private LocalDateTime fechaHora;

    /**
     * Descripción corta del problema o servicio solicitado (ej: "Cambio de aceite").
     * Ayuda al mecánico a tener un contexto previo antes de la recepción.
     */
    @NotBlank(message = "El motivo es obligatorio")
    private String motivoBreve;

    /**
     * Estado actual de la cita en su ciclo de vida. [cite: 39]
     * Por defecto se crea como CONFIRMADA. Se almacena como STRING en BD para legibilidad.
     */
    @Enumerated(EnumType.STRING)
    private EstadoCita estado = EstadoCita.CONFIRMADA;

    /**
     * Define los estados posibles para una cita.
     * - CONFIRMADA: Cita vigente y activa. 
     * - CANCELADA: El cliente o el taller anularon la reserva. [cite: 40]
     * - EJECUTADA: El servicio ya fue realizado y la cita finalizó. [cite: 39]
     */
    public enum EstadoCita {
        CONFIRMADA, CANCELADA, EJECUTADA
    }
}