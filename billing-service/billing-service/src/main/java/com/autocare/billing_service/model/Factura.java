package com.autocare.billing_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Entidad JPA que representa una factura del taller.
 *
 * Notas generales:
 * - Se usan anotaciones Lombok para reducir boilerplate (getters/setters, constructores, toString).
 * - Esta clase se mapea a la tabla "facturas".
 * - Actualmente los montos son Double; ver recomendaciones abajo para precisión monetaria.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "facturas")
public class Factura {

    /**
     * Identificador de la factura.
     * - @Id marca la PK.
     * - @GeneratedValue(strategy = GenerationType.UUID) genera un UUID como String.
     *   Asegúrate de que tu versión de JPA/Hibernate soporte esta estrategia o ajusta según tu proveedor.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idFactura;

    /**
     * Identificador de la orden asociada a la factura.
     * - @NotBlank evita que se persista una factura sin idOrden válido.
     * - Mensaje personalizado para feedback de validación.
     */
    @NotBlank(message = "El id de la orden es obligatorio")
    private String idOrden;

    /**
     * Identificador del cliente dueño de la factura.
     * - @NotBlank asegura que siempre haya referencia al cliente.
     */
    @NotBlank(message = "El id del cliente es obligatorio")
    private String idCliente;

    /**
     * Subtotal calculado sumando los totalLinea de las cotizaciones aprobadas.
     * - Actualmente Double; ver recomendaciones para usar BigDecimal.
     * - No tiene anotaciones de validación porque se calcula en el servicio.
     */
    private Double subtotal;

    /**
     * Impuesto aplicado (por ejemplo 19% IVA).
     * - Se guarda como Double; considerar BigDecimal y precisión/scale en la columna.
     */
    private Double impuesto; // 19% IVA

    /**
     * Total final (subtotal + impuesto).
     * - Campo calculado en el servicio antes de persistir.
     */
    private Double total;

    /**
     * Estado de la factura.
     * - Se persiste como String (EnumType.STRING) para legibilidad en la BD.
     * - Valor por defecto: PENDIENTE.
     */
    @Enumerated(EnumType.STRING)
    private EstadoFactura estado = EstadoFactura.PENDIENTE;

    /**
     * Estados posibles de la factura:
     * - PENDIENTE: creada pero no pagada.
     * - PAGADA: cobrada.
     * - ANULADA: anulada/cancelada.
     */
    public enum EstadoFactura {
        PENDIENTE, PAGADA, ANULADA
    }
}
    