package com.autocare.billing_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idFactura;

    @NotBlank(message = "El id de la orden es obligatorio")
    private String idOrden;

    @NotBlank(message = "El id del cliente es obligatorio")
    private String idCliente;

    // Se calcula sumando los totalLinea de las cotizaciones aprobadas
    private Double subtotal;

    private Double impuesto; // 19% IVA

    private Double total;

    @Enumerated(EnumType.STRING)
    private EstadoFactura estado = EstadoFactura.PENDIENTE;

    public enum EstadoFactura {
        PENDIENTE, PAGADA, ANULADA
    }
}