package com.autocare.crm_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa una interacción con el cliente (Llamadas, Reclamos, etc.).
 * - Usa Lombok para limpieza de código (@Getter, @Setter, @NoArgsConstructor, etc.).
 * - Mapea a la tabla 'interacciones' para persistencia en base de datos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "interacciones")
public class Interaccion {

    /**
     * Identificador único generado como UUID para evitar colisiones en sistemas distribuidos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idInteraccion;

    /**
     * ID del cliente proveniente del customer-service. 
     * @NotBlank asegura que la relación lógica siempre exista.
     */
    @NotBlank(message = "El id del cliente es obligatorio")
    private String idCliente;

    /**
     * Campo informativo que se llena dinámicamente consultando el microservicio de clientes.
     */
    private String nombreCliente;

    /**
     * Define la naturaleza de la interacción. Se guarda como STRING en BD para facilitar auditorías manuales.
     */
    @NotNull(message = "El tipo de interacción es obligatorio")
    @Enumerated(EnumType.STRING)
    private TipoInteraccion tipo;

    /**
     * Detalle de lo conversado o reportado por el cliente.
     */
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    /**
     * Marca temporal asignada automáticamente por el servidor al momento del registro.
     */
    private LocalDateTime fechaInteraccion;

    /**
     * Estado del ciclo de vida de la interacción. Inicia siempre como ABIERTO.
     */
    @Enumerated(EnumType.STRING)
    private SeguimientoEstado seguimiento = SeguimientoEstado.ABIERTO;

    /**
     * Tipos de contacto soportados por el CRM.
     */
    public enum TipoInteraccion {
        LLAMADA, VISITA, RECLAMO, CONSULTA, SEGUIMIENTO_POSVENTA
    }

    /**
     * Estados permitidos para el flujo de trabajo del agente.
     */
    public enum SeguimientoEstado {
        ABIERTO, EN_PROCESO, CERRADO
    }
}