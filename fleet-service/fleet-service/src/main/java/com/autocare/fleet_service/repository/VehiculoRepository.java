package com.autocare.fleet_service.repository;

import com.autocare.fleet_service.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Capa de persistencia (Repository Layer) para la gestión de vehículos en la base de datos.
 * @Repository indica a Spring que esta interfaz es un componente de acceso a datos.
 * Se encarga de capturar las excepciones específicas de la base de datos (SQL exceptions) 
 * y traducirlas a la jerarquía genérica de excepciones de Spring (DataAccessException).
 */
@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {

    /**
     * Al extender JpaRepository<Vehiculo, String>, Spring Data genera dinámicamente 
     * en tiempo de ejecución (runtime) la implementación de esta interfaz.
     * Nos provee automáticamente de métodos como save(), findById(), delete(), etc.,
     * donde 'Vehiculo' es la entidad a mapear y 'String' es el tipo de dato de su @Id.
     */

    // ── QUERY METHODS (Consultas Derivadas) ──────────────────────────────────

    // Spring genera el SQL automáticamente por el nombre del método
    /**
     * Traducido a SQL: SELECT * FROM vehiculos WHERE patente = ?
     * 
     * El uso de Optional<Vehiculo> es una gran práctica introducida en Java 8. 
     * Obliga al desarrollador que llama a este método (en el Service) a manejar 
     * explícitamente el caso en el que el vehículo no exista, reduciendo 
     * drásticamente la posibilidad de un NullPointerException.
     * 
     * @param patente La placa patente exacta a buscar.
     * @return Un contenedor Optional con el vehículo si se encuentra, o vacío si no.
     */
    Optional<Vehiculo> findByPatente(String patente);

    /**
     * Traducido a SQL: SELECT count(id_vehiculo) FROM vehiculos WHERE patente = ?
     * 
     * A nivel de base de datos, los métodos "existsBy..." son mucho más eficientes 
     * para las validaciones de negocio (ej. evitar duplicados al registrar) que 
     * usar "findBy...". En lugar de traer todos los campos de la tabla a la 
     * memoria de la aplicación, solo devuelven un valor booleano (true/false).
     * 
     * @param patente La patente a verificar.
     * @return true si ya existe en la base de datos, false de lo contrario.
     */
    boolean existsByPatente(String patente);

    // VehiculoRepository.java — agregar este método
    /**
     * Similar al método anterior, verifica la existencia de un registro basándose 
     * en el Número de Identificación Vehicular (VIN) o número de chasis.
     * Fundamental para validar que no se ingresen dos vehículos físicos distintos 
     * con el mismo número de serie de fábrica.
     * 
     * @param vinChasis El número de chasis a verificar.
     * @return true si el VIN ya está registrado, false de lo contrario.
     */
    boolean existsByVinChasis(String vinChasis);
}