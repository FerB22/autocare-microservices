package com.autocare.fleet_service.repository;

import com.autocare.fleet_service.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {

    // Spring genera el SQL automáticamente por el nombre del método
    Optional<Vehiculo> findByPatente(String patente);

    boolean existsByPatente(String patente);
}