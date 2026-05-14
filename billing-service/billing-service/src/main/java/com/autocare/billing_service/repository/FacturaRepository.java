package com.autocare.billing_service.repository;

import com.autocare.billing_service.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Factura.
 * - Extiende JpaRepository para heredar operaciones CRUD y paginación.
 * - El tipo de la entidad es Factura y la PK es String (idFactura).
 * - Aquí se declaran consultas derivadas por nombre (query methods).
 */
@Repository
public interface FacturaRepository extends JpaRepository<Factura, String> {

    /**
     * Busca todas las facturas asociadas a un cliente.
     * Devuelve una lista (posible lista vacía si no hay resultados).
     *
     * @param idCliente identificador del cliente
     * @return lista de facturas del cliente
     */
    List<Factura> findByIdCliente(String idCliente);

    /**
     * Busca la factura asociada a una orden específica.
     * Se devuelve Optional porque puede no existir factura para la orden.
     *
     * @param idOrden identificador de la orden
     * @return Optional con la factura si existe
     */
    Optional<Factura> findByIdOrden(String idOrden);

    /**
     * Busca facturas por su estado (PENDIENTE, PAGADA, ANULADA).
     * El uso de Enum en el parámetro permite consultas tipadas y evita errores
     * por strings mal escritos.
     *
     * @param estado estado de la factura
     * @return lista de facturas con el estado indicado
     */
    List<Factura> findByEstado(Factura.EstadoFactura estado);
}
