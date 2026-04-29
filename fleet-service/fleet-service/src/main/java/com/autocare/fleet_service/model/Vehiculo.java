package com.autocare.fleet_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idVehiculo;

    @NotBlank(message = "La patente es obligatoria")
    @Column(unique = true, nullable = false)
    private String patente;

    @NotBlank(message = "La marca es obligatoria")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    private String modelo;

    @NotNull(message = "El año es obligatorio")
    private Integer anio;

    private String vinChasis;
    private String idDuenio;

    public String getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(String idVehiculo) { this.idVehiculo = idVehiculo; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getVinChasis() { return vinChasis; }
    public void setVinChasis(String vinChasis) { this.vinChasis = vinChasis; }

    public String getIdDuenio() { return idDuenio; }
    public void setIdDuenio(String idDuenio) { this.idDuenio = idDuenio; }
}
