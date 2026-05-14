package com.autocare.estimation_service.repository;

import com.autocare.estimation_service.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Capa de acceso a datos (Data Access Object - DAO) para las cotizaciones.
 * @Repository es un estereotipo de Spring que marca esta interfaz como un 
 * componente gestionado por el contenedor de inversión de control (IoC).
 * Además, habilita la traducción automática de excepciones nativas de la 
 * base de datos a excepciones estándar de Spring (DataAccessException).
 */
@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, String> {

    /**
     * Al extender JpaRepository, Spring Data crea en tiempo de ejecución 
     * (runtime) una implementación concreta de esta interfaz.
     * Automáticamente heredamos métodos básicos como save(), findById(), 
     * findAll() y deleteById() sin necesidad de escribir código SQL.
     * 
     * Genéricos:
     * <Cotizacion> -> Especifica la entidad que este repositorio va a gestionar.
     * <String>     -> Especifica el tipo de dato de la clave primaria (@Id) de esa entidad.
     */

    // ── QUERY METHODS (Consultas Derivadas) ──────────────────────────────────
    // Spring Data JPA analiza gramaticalmente el nombre del método y construye 
    // dinámicamente la consulta SQL subyacente.

    /**
     * Traducido a SQL: SELECT * FROM cotizaciones WHERE id_orden = ?
     * 
     * Es ideal en una arquitectura de microservicios, ya que permite obtener 
     * fácilmente todas las piezas o servicios cotizados que pertenecen a 
     * una Orden de Trabajo específica, usando solo su ID externo.
     * 
     * @param idOrden El identificador de la orden proveniente del order-service.
     * @return Una lista con todas las cotizaciones asociadas a esa orden.
     */
    List<Cotizacion> findByIdOrden(String idOrden);

    /**
     * Traducido a SQL: SELECT * FROM cotizaciones WHERE estado = ?
     * 
     * Útil para crear reportes o dashboards. Por ejemplo, permite al taller 
     * buscar rápidamente todas las cotizaciones en estado 'PENDIENTE' 
     * para hacerles seguimiento con el cliente.
     * 
     * @param estado El estado específico del Enum (PENDIENTE, APROBADA, RECHAZADA).
     * @return Una lista de cotizaciones que coinciden con dicho estado.
     */
    List<Cotizacion> findByEstado(Cotizacion.EstadoCotizacion estado);
}