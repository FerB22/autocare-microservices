package com.autocare.billing_service.dto;

/**
 * DTO simple que representa la información mínima que el servicio de facturación
 * necesita recibir desde el servicio de estimaciones (estimation-service).
 *
 * Notas:
 * - Es un POJO (Plain Old Java Object) sin lógica de negocio.
 * - Se usa para deserializar JSON recibido por WebClient.
 * - Mantenerlo ligero y con los nombres de campo que coincidan con el JSON esperado.
 */
public class CotizacionDTO {

    // Identificador de la cotización en el servicio de estimaciones
    private String idCotizacion;

    // Identificador de la orden a la que pertenece la cotización
    private String idOrden;

    // Estado textual de la cotización (ej: "APROBADA", "RECHAZADA", "PENDIENTE")
    private String estado;

    // Total de la línea; puede ser null si el servicio remoto no lo envía
    private Double totalLinea;

    // ---------------------------
    // Getters y setters
    // ---------------------------

    // Devuelve el id de la cotización
    public String getIdCotizacion() { return idCotizacion; }

    // Asigna el id de la cotización
    public void setIdCotizacion(String idCotizacion) { this.idCotizacion = idCotizacion; }

    // Devuelve el id de la orden asociada
    public String getIdOrden() { return idOrden; }

    // Asigna el id de la orden asociada
    public void setIdOrden(String idOrden) { this.idOrden = idOrden; }

    // Devuelve el estado de la cotización
    public String getEstado() { return estado; }

    // Asigna el estado de la cotización
    public void setEstado(String estado) { this.estado = estado; }

    // Devuelve el total de la línea (puede ser null)
    public Double getTotalLinea() { return totalLinea; }

    // Asigna el total de la línea
    public void setTotalLinea(Double totalLinea) { this.totalLinea = totalLinea; }
}
