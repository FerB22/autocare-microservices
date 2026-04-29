package com.autocare.crm_service.model;

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
@Table(name = "interacciones")
public class Interaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idInteraccion;

    @NotBlank(message = "El id del cliente es obligatorio")
    private String idCliente;

    // Se llena consultando al customer-service
    private String nombreCliente;

    @NotNull(message = "El tipo de interacción es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoInteraccion tipo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    // Se genera automáticamente al registrar
    private LocalDateTime fechaInteraccion;

    @Enumerated(EnumType.STRING)
    private SeguimientoEstado seguimiento = SeguimientoEstado.ABIERTO;

    public enum TipoInteraccion {
        LLAMADA, VISITA, RECLAMO, CONSULTA, SEGUIMIENTO_POSVENTA
    }

    public enum SeguimientoEstado {
        ABIERTO, EN_PROCESO, CERRADO
    }
}