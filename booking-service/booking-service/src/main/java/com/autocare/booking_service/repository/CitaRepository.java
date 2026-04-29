package com.autocare.booking_service.repository;

import com.autocare.booking_service.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, String> {

    // Todas las citas de un vehículo específico
    List<Cita> findByIdVehiculo(String idVehiculo);

    // Solo las citas con un estado específico
    List<Cita> findByEstado(Cita.EstadoCita estado);
}