package com.autocare.checkin_service.repository;

import com.autocare.checkin_service.model.Recepcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecepcionRepository extends JpaRepository<Recepcion, String> {

    List<Recepcion> findByIdVehiculo(String idVehiculo);

    Optional<Recepcion> findByIdOrdenCreada(String idOrden);
}