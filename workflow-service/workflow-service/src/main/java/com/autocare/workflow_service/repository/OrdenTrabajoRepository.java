package com.autocare.workflow_service.repository;

import com.autocare.workflow_service.model.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Capa de persistencia (Data Access Object - DAO) para la entidad OrdenTrabajo.
 * @Repository indica a Spring Boot que esta interfaz es un componente 
 * encargado de interactuar con la base de datos, habilitando la traducción 
 * automática de excepciones SQL a la jerarquía de excepciones de Spring.
 */
@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, String> {

    /**
     * Al extender JpaRepository<OrdenTrabajo, String>, Spring Data genera 
     * automáticamente en tiempo de ejecución un proxy con todas las operaciones 
     * CRUD estándar (save, findById, findAll, deleteById, etc.).
     */

    // ── QUERY METHODS (Consultas Derivadas) ──────────────────────────────────
    // Spring analiza la firma del método para construir dinámicamente el SQL.

    /**
     * Traducido a SQL: SELECT * FROM ordenes_trabajo WHERE id_vehiculo = ?
     * * Esta consulta es fundamental para construir el "Historial del Vehículo".
     * Permite al fleet-service o al frontend consultar todas las reparaciones 
     * y mantenimientos previos que se le han realizado a un auto específico.
     *
     * @param idVehiculo El UUID del vehículo (Soft Foreign Key hacia fleet-service).
     * @return Una lista con todas las órdenes asociadas a ese vehículo.
     */
    List<OrdenTrabajo> findByIdVehiculo(String idVehiculo);

    /**
     * Traducido a SQL: SELECT * FROM ordenes_trabajo WHERE estado = ?
     * * Ideal para construir tableros de control (Dashboards) Kanban.
     * Permite, por ejemplo, que el jefe de taller consulte rápidamente todas 
     * las órdenes en estado 'CONTROL_CALIDAD' para ir a revisarlas.
     *
     * @param estado El estado específico del Enum a buscar.
     * @return Lista de órdenes que se encuentran actualmente en dicho estado.
     */
    List<OrdenTrabajo> findByEstado(OrdenTrabajo.EstadoOrden estado);

    /**
     * Traducido a SQL: SELECT * FROM ordenes_trabajo WHERE id_mecanico_asignado = ?
     * * Útil para la vista personal del trabajador.
     * Permite que la interfaz de usuario de un mecánico específico le muestre 
     * únicamente los trabajos que tiene asignados (su carga de trabajo actual o histórica).
     *
     * @param idMecanico El UUID del mecánico (Soft Foreign Key hacia hr-service).
     * @return Lista de órdenes asignadas a ese mecánico.
     */
    List<OrdenTrabajo> findByIdMecanicoAsignado(String idMecanico);
}