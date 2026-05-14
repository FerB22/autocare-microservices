package com.autocare.spare_parts_service.repository;

import com.autocare.spare_parts_service.model.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Capa de persistencia (Data Access Object - DAO) para la entidad Repuesto.
 * @Repository indica a Spring Boot que esta interfaz es un componente 
 * encargado de las operaciones con la base de datos, habilitando además la 
 * traducción automática de excepciones SQL nativas a la jerarquía de 
 * excepciones de Spring (DataAccessException).
 */
@Repository
public interface RepuestoRepository extends JpaRepository<Repuesto, String> {

    /**
     * NOTA ARQUITECTÓNICA:
     * Al extender JpaRepository<Repuesto, String>, Spring Data genera dinámicamente 
     * en tiempo de ejecución (runtime) una implementación (Proxy) de esta interfaz.
     * Nos proporciona automáticamente operaciones CRUD completas (save, findById, 
     * delete, findAll) y soporte para paginación sin necesidad de escribir sentencias SQL.
     */

    // ── QUERY METHODS (Consultas Derivadas) ──────────────────────────────────
    // Spring analiza la firma del método y construye dinámicamente el SQL subyacente.

    /**
     * Traducido a SQL: SELECT * FROM repuestos WHERE codigo_parte = ?
     * * Búsqueda por la clave de negocio (Business Key).
     * El uso de Optional<Repuesto> es una excelente práctica (Java 8+). Obliga a 
     * la capa de servicio superior a manejar explícitamente el escenario en el que 
     * el código escaneado/ingresado no exista en el inventario, reduciendo así la 
     * aparición de errores del tipo NullPointerException.
     * * @param codigoParte El código alfanumérico único del fabricante de la pieza.
     * @return Un contenedor Optional con el repuesto si se encuentra.
     */
    Optional<Repuesto> findByCodigoParte(String codigoParte);

    // Repuestos con stock disponible
    /**
     * Traducido a SQL: SELECT * FROM repuestos WHERE stock > ?
     * * Utiliza la palabra reservada "GreaterThan" (mayor que) que Spring Data JPA 
     * reconoce automáticamente. Es extremadamente útil para el frontend, permitiendo 
     * llenar catálogos y selectores desplegables mostrando únicamente aquellas 
     * piezas físicas que pueden ser asignadas y descontadas para una reparación.
     * * @param cantidad El valor base a superar (en la capa de servicio usualmente se pasa 0).
     * @return Lista de repuestos cuyo stock supera la cantidad especificada.
     */
    List<Repuesto> findByStockGreaterThan(int cantidad);

    /**
     * Traducido a SQL: SELECT count(id_repuesto) FROM repuestos WHERE codigo_parte = ?
     * * En lugar de usar findByCodigoParte y evaluar si es null para validar duplicados, 
     * los métodos "existsBy..." son mucho más ligeros y eficientes. La base de datos 
     * no retorna todos los campos de la tabla hacia la memoria de la aplicación, 
     * sino que ejecuta un COUNT simple y devuelve un único valor booleano (true/false).
     * * @param codigoParte El código de parte a validar.
     * @return true si ya existe un repuesto registrado con ese código, false si está libre.
     */
    boolean existsByCodigoParte(String codigoParte);
}