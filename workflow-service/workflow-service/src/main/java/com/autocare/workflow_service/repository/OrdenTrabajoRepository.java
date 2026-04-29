package com.autocare.workflow_service.repository;

import com.autocare.workflow_service.model.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, String> {

    List<OrdenTrabajo> findByIdVehiculo(String idVehiculo);

    List<OrdenTrabajo> findByEstado(OrdenTrabajo.EstadoOrden estado);

    List<OrdenTrabajo> findByIdMecanicoAsignado(String idMecanico);
}