package com.autocare.customer_service.repository;

import com.autocare.customer_service.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Interfaz que actúa como la capa de acceso a datos (DAO - Data Access Object).
 * @Repository indica a Spring que esta interfaz es un componente encargado de
 * interactuar con la base de datos y que debe traducir las excepciones nativas 
 * de la base de datos (como una violación de llave única) a excepciones 
 * estándar de Spring (DataAccessException).
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, String> {

    // NOTA ARQUITECTÓNICA: Esta es una "interfaz", no una clase. No necesitamos escribir
    // la implementación (el código SQL o los métodos). Spring Data JPA generará
    // dinámicamente una clase (proxy) en tiempo de ejecución (runtime) que
    // implemente todo esto por nosotros.

    /**
     * Al extender JpaRepository<Cliente, String>, heredamos inmediatamente decenas
     * de métodos predefinidos sin escribir una sola línea de código, tales como:
     * - save(Cliente) -> INSERT o UPDATE
     * - findById(String) -> SELECT * WHERE id = ?
     * - findAll() -> SELECT *
     * - deleteById(String) -> DELETE WHERE id = ?
     * 
     * Los genéricos indican:
     * <Cliente> -> La entidad o tabla que este repositorio va a manejar.
     * <String>  -> El tipo de dato de la Clave Primaria (@Id) de esa entidad (el UUID).
     */

    /**
     * Query Method (Consulta derivada).
     * Spring analiza el nombre del método ("findBy" + "Email") y construye 
     * automáticamente la consulta SQL subyacente: 
     * SELECT * FROM clientes WHERE email = ?
     * 
     * @param email El correo exacto a buscar.
     * @return Se envuelve en un Optional<> para prevenir NullPointerException 
     *         en caso de que no exista ningún cliente con ese correo en la base de datos.
     */
    Optional<Cliente> findByEmail(String email);

    /**
     * Query Method de comprobación booleana.
     * Traduce el nombre del método a una consulta SQL de tipo COUNT:
     * SELECT count(id) FROM clientes WHERE email = ? (y devuelve true si count > 0)
     * 
     * Es mucho más eficiente para validar reglas de negocio (ej. evitar correos duplicados
     * al crear un cliente) porque solo devuelve un valor booleano en lugar de 
     * cargar toda la fila (y el objeto Cliente) en la memoria RAM del servidor.
     * 
     * @param email El correo a verificar.
     * @return true si el correo ya está registrado en la base de datos, false de lo contrario.
     */
    boolean existsByEmail(String email);
}