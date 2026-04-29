package com.autocare.estimation_service.model;

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
@Table(name = "cotizaciones")
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idCotizacion;

    @NotBlank(message = "El id de la orden es obligatorio")
    private String idOrden;

    @NotBlank(message = "El id del repuesto es obligatorio")
    private String idRepuesto;

    // Se llena consultando al spare-parts-service
    private String nombreRepuesto;
    private Double precioUnitario;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidad;

    // Se calcula automáticamente: precioUnitario * cantidad
    private Double totalLinea;

    @Enumerated(EnumType.STRING)
    private EstadoCotizacion estado = EstadoCotizacion.PENDIENTE;

    public enum EstadoCotizacion {
        PENDIENTE, APROBADA, RECHAZADA
    }
}