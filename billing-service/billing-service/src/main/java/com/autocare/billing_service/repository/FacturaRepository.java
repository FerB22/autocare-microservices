package com.autocare.billing_service.repository;

import com.autocare.billing_service.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, String> {

    List<Factura> findByIdCliente(String idCliente);

    Optional<Factura> findByIdOrden(String idOrden);

    List<Factura> findByEstado(Factura.EstadoFactura estado);
}