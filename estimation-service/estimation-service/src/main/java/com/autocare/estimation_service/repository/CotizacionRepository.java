package com.autocare.estimation_service.repository;

import com.autocare.estimation_service.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, String> {

    List<Cotizacion> findByIdOrden(String idOrden);

    List<Cotizacion> findByEstado(Cotizacion.EstadoCotizacion estado);
}