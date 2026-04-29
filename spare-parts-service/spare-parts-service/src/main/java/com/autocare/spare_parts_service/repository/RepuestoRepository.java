package com.autocare.spare_parts_service.repository;

import com.autocare.spare_parts_service.model.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepuestoRepository extends JpaRepository<Repuesto, String> {

    Optional<Repuesto> findByCodigoParte(String codigoParte);

    // Repuestos con stock disponible
    List<Repuesto> findByStockGreaterThan(int cantidad);

    boolean existsByCodigoParte(String codigoParte);
}