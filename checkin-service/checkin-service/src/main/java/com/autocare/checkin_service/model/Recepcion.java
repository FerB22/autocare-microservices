package com.autocare.checkin_service.model;

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
@Table(name = "recepciones")
public class Recepcion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idRecepcion;

    @NotBlank(message = "El id del vehículo es obligatorio")
    private String idVehiculo;

    @NotNull(message = "El kilometraje es obligatorio")
    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    private Integer kilometrajeEntrada;

    @NotBlank(message = "El nivel de combustible es obligatorio")
    private String nivelCombustible;

    private String danosVisuales;

    private String objetosDejados;

    private String idOrdenCreada;
}