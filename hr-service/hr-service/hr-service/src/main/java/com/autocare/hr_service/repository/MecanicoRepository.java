package com.autocare.hr_service.repository;

import com.autocare.hr_service.model.Mecanico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Capa de acceso a datos (Data Access Object - DAO) para la entidad Mecánico.
 * @Repository indica a Spring Boot que esta interfaz es un componente 
 * encargado de las operaciones con la base de datos.
 * Además, habilita la traducción de excepciones de base de datos nativas (SQL)
 * a excepciones estandarizadas de Spring (DataAccessException).
 */
@Repository
public interface MecanicoRepository extends JpaRepository<Mecanico, String> {

    /**
     * NOTA ARQUITECTÓNICA:
     * Al extender JpaRepository<Mecanico, String>, esta interfaz ya hereda
     * de forma automática todas las operaciones CRUD estándar (save, findById, 
     * findAll, delete, etc.) sin tener que escribir la implementación.
     * Spring Data JPA usa un patrón Proxy para generar la clase real en tiempo 
     * de ejecución.
     */

    // ── QUERY METHODS (Consultas Derivadas) ──────────────────────────────────
    // Spring analiza la firma del método y construye dinámicamente el SQL.

    // Buscar mecánicos por especialidad
    /**
     * Traducido a SQL: SELECT * FROM mecanicos WHERE especialidad = ?
     * * Retorna una lista de mecánicos que coincidan exactamente con la 
     * especialidad enviada. Es útil para el filtrado en el frontend o 
     * cuando se necesita un listado general de expertos en un área.
     *
     * @param especialidad El String exacto de la especialidad (ej. "MOTOR").
     * @return Lista de mecánicos correspondientes.
     */
    List<Mecanico> findByEspecialidad(String especialidad);

    // Buscar solo los disponibles
    /**
     * Traducido a SQL: SELECT * FROM mecanicos WHERE esta_disponible = ?
     * * Este método es vital para la operación del taller, ya que permite saber 
     * de un solo vistazo quiénes no están asignados actualmente a una orden 
     * de trabajo y pueden recibir nuevos clientes.
     *
     * @param estaDisponible Booleano (true para mecánicos libres).
     * @return Lista de mecánicos que cumplen la condición de disponibilidad.
     */
    List<Mecanico> findByEstaDisponible(boolean estaDisponible);

    // Buscar disponibles por especialidad
    /**
     * Traducido a SQL: SELECT * FROM mecanicos WHERE especialidad = ? AND esta_disponible = ?
     * * Este es un "Query Method" compuesto usando la palabra reservada 'And'.
     * Es la consulta más valiosa para el workflow-service o order-service, 
     * ya que permite preguntar específicamente: "¿Quién está libre AHORA y que 
     * además sepa reparar FRENOS?".
     *
     * @param especialidad El área de expertise requerida.
     * @param estaDisponible El estado actual del mecánico (true para libres).
     * @return Lista de mecánicos aptos y listos para trabajar.
     */
    List<Mecanico> findByEspecialidadAndEstaDisponible(String especialidad, boolean estaDisponible);
}