// dto/VehiculoDTO.java
package com.autocare.booking_service.dto;

public class VehiculoDTO {
    private String id;
    private String marca;
    private String modelo;
    private String patente;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }
}