package com.autocare.booking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para transferencia de datos del vehículo entre microservicios.
 * - Usado en llamadas WebClient a fleet-service para verificar existencia del vehículo.
 * - Contiene datos identificatorios mínimos (no toda la entidad Vehiculo).
 * - Validaciones específicas para formato chileno de patentes.
 * 
 * Nota: Patente chilena formato actual: ABC1234 o ABCD12 (4 letras + 3/2 números).
 */
public class VehiculoDTO {

    /**
     * ID único del vehículo en fleet-service.
     * - Formato: UUID generado por la BD.
     * - Obligatorio para búsquedas y referencias.
     */
    @NotBlank(message = "ID del vehículo es obligatorio")
    private String id;

    /**
     * Marca del vehículo (ej: Toyota, Ford, Hyundai).
     * - Usado en logs y confirmaciones de cita.
     */
    @NotBlank(message = "Marca es obligatoria")
    @Size(max = 30, message = "Marca no puede exceder 30 caracteres")
    private String marca;

    /**
     * Modelo del vehículo (ej: Corolla, F-150, Tucson).
     * - Usado en logs detallados.
     */
    @NotBlank(message = "Modelo es obligatorio")
    @Size(max = 50, message = "Modelo no puede exceder 50 caracteres")
    private String modelo;

    /**
     * Patente chilena única del vehículo.
     * - Formato actual (desde 2008): 4 letras + 3 números (ABCD123) o 4 letras + 2 números (ABCD12).
     * - Valida regex específico para Chile.
     * - Única en fleet-service.
     */
    @NotBlank(message = "Patente es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{2,3}$", 
             message = "Patente debe tener formato chileno (ABCD123 o ABCD12)")
    private String patente;

    // Getters y setters estándar
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    /**
     * Descripción legible del vehículo para logs y confirmaciones.
     * - Ejemplo: "Toyota Corolla - ABC1234"
     */
    public String getDescripcionCompleta() {
        return String.format("%s %s - %s", marca, modelo, patente);
    }
}