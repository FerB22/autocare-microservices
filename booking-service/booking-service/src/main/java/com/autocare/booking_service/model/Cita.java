package com.autocare.booking_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idCita;

    @NotBlank(message = "El id del vehículo es obligatorio")
    private String idVehiculo;

    @NotBlank(message = "El id del cliente es obligatorio")
    private String idCliente;

    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La cita debe ser en una fecha futura")
    private LocalDateTime fechaHora;

    @NotBlank(message = "El motivo es obligatorio")
    private String motivoBreve;

    @Enumerated(EnumType.STRING)
    private EstadoCita estado = EstadoCita.CONFIRMADA;

    public enum EstadoCita {
        CONFIRMADA, CANCELADA, EJECUTADA
    }
}