package com.autocare.workflow_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Entidad de dominio que representa una Orden de Trabajo en el taller AutoCare.
 * Centraliza el flujo de estados desde que un vehículo ingresa hasta su entrega.
 */
@Getter
@Setter
@NoArgsConstructor  // Constructor vacío exigido por la especificación JPA/Hibernate.
@AllArgsConstructor // Genera un constructor con todos los atributos, útil para pruebas unitarias.
@ToString           // Facilita la depuración al volcar el estado del objeto en los logs.
@Entity             // Marca la clase como una entidad persistente gestionada por el ORM.
@Table(name = "ordenes_trabajo") // Mapea explícitamente a la tabla correspondiente en la BD.
public class OrdenTrabajo {

    /**
     * Identificador único de la orden.
     * Se utiliza la estrategia UUID (Universally Unique Identifier) para garantizar 
     * la unicidad global, facilitando la integración y escalabilidad entre microservicios.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idOrden;

    /**
     * Referencia lógica (Soft Foreign Key) al vehículo.
     * Dado que el vehículo pertenece al 'fleet-service', solo almacenamos su ID como String
     * para mantener el desacoplamiento entre las bases de datos de cada servicio.
     */
    @NotBlank(message = "El id del vehículo es obligatorio")
    private String idVehiculo;

    /**
     * Referencia lógica al mecánico responsable.
     * Al igual que el vehículo, la información detallada reside en otro dominio (hr-service).
     * Se asigna dinámicamente durante el ciclo de vida de la orden.
     */
    // Se llena después de consultar al hr-service
    private String idMecanicoAsignado;

    /**
     * Representación del estado actual en el flujo de trabajo.
     * @Enumerated(EnumType.STRING) es fundamental para guardar el nombre literal 
     * ("EN_ESPERA") en la base de datos, evitando inconsistencias si se altera 
     * el orden de las constantes en el código Java.
     */
    @Enumerated(EnumType.STRING)
    private EstadoOrden estado = EstadoOrden.EN_ESPERA;

    /**
     * Indica la urgencia de la atención.
     * Validado en la capa de entrada para asegurar que no se creen órdenes sin prioridad.
     */
    @NotBlank(message = "La prioridad es obligatoria")
    private String prioridad; // "ALTA", "MEDIA", "BAJA"

    /**
     * Definición de la máquina de estados que rige el flujo operativo del taller.
     */
    public enum EstadoOrden {
        EN_ESPERA, EN_PROCESO, CONTROL_CALIDAD, LISTO, ENTREGADO
    }
}