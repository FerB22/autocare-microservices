package com.autocare.billing_service.dto;

public class CotizacionDTO {
    private String idCotizacion;
    private String idOrden;
    private String estado;
    private Double totalLinea;

    public String getIdCotizacion() { return idCotizacion; }
    public void setIdCotizacion(String idCotizacion) { this.idCotizacion = idCotizacion; }
    public String getIdOrden() { return idOrden; }
    public void setIdOrden(String idOrden) { this.idOrden = idOrden; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getTotalLinea() { return totalLinea; }
    public void setTotalLinea(Double totalLinea) { this.totalLinea = totalLinea; }
}