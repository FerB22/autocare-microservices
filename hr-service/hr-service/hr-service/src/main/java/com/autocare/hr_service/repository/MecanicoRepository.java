package com.autocare.hr_service.repository;

import com.autocare.hr_service.model.Mecanico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MecanicoRepository extends JpaRepository<Mecanico, String> {

    // Buscar mecánicos por especialidad
    List<Mecanico> findByEspecialidad(String especialidad);

    // Buscar solo los disponibles
    List<Mecanico> findByEstaDisponible(boolean estaDisponible);

    // Buscar disponibles por especialidad
    List<Mecanico> findByEspecialidadAndEstaDisponible(String especialidad, boolean estaDisponible);
}