package com.autocare.fleet_service.dto;

import lombok.Data;

@Data
public class ClienteDTO {
    private String idCliente;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
}