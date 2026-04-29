package com.autocare.workflow_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "ordenes_trabajo")
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idOrden;

    @NotBlank(message = "El id del vehículo es obligatorio")
    private String idVehiculo;

    // Se llena después de consultar al hr-service
    private String idMecanicoAsignado;

    @Enumerated(EnumType.STRING)
    private EstadoOrden estado = EstadoOrden.EN_ESPERA;

    @NotBlank(message = "La prioridad es obligatoria")
    private String prioridad; // "ALTA", "MEDIA", "BAJA"

    public enum EstadoOrden {
        EN_ESPERA, EN_PROCESO, CONTROL_CALIDAD, LISTO, ENTREGADO
    }
}