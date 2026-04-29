package com.autocare.fleet_service.model;

public class VehiculoDTO {
    private String idVehiculo;
    private String patente;
    private String marca;
    private String modelo;
    private Integer anio;

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
}
