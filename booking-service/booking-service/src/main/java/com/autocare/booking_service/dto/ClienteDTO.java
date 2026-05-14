package com.autocare.booking_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO (Data Transfer Object) para transferencia de datos del cliente entre microservicios.
 * - Usado en llamadas WebClient a customer-service para verificar existencia del cliente.
 * - Contiene solo datos necesarios (no toda la entidad Cliente).
 * - Incluye validaciones para uso en APIs públicas.
 * 
 * Nota: En producción agregar @JsonIgnoreProperties(ignoreUnknown = true) para tolerar campos extra.
 */
public class ClienteDTO {

    /**
     * ID único del cliente en customer-service.
     * - Formato: UUID o similar.
     * - Obligatorio para búsquedas.
     */
    @NotBlank(message = "ID del cliente es obligatorio")
    private String idCliente;

    /**
     * Nombre del cliente (solo primer nombre).
     * - Usado para logs y confirmaciones.
     */
    @NotBlank(message = "Nombre es obligatorio")
    private String nombre;

    /**
     * Apellido del cliente.
     * - Usado para logs completos (nombre + apellido).
     */
    @NotBlank(message = "Apellido es obligatorio")
    private String apellido;

    /**
     * Email del cliente.
     * - Formato RFC 5322 válido.
     * - Único en customer-service.
     */
    @NotBlank(message = "Email es obligatorio")
    @Email(message = "Email debe tener formato válido")
    private String email;

    /**
     * Teléfono del cliente.
     * - Formato chileno: +56 9 XX XXX XXXX.
     * - Opcional pero recomendado.
     */
    @Pattern(regexp = "^(\\+56|0)?9\\d{8}$", 
             message = "Teléfono debe tener formato chileno válido (+569XXXXXXXX o 9XXXXXXXX)")
    private String telefono;

    // Getters y setters (generados por Lombok @Data en versión moderna)
    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    /**
     * Nombre completo para uso en logs y respuestas.
     * - Computed property (no persistida).
     */
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}