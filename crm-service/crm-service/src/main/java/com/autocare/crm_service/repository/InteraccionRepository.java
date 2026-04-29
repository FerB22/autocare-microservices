package com.autocare.crm_service.repository;

import com.autocare.crm_service.model.Interaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InteraccionRepository extends JpaRepository<Interaccion, String> {

    List<Interaccion> findByIdCliente(String idCliente);

    List<Interaccion> findByTipo(Interaccion.TipoInteraccion tipo);

    List<Interaccion> findBySeguimiento(Interaccion.SeguimientoEstado seguimiento);
}