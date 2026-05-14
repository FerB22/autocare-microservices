package com.autocare.crm_service.repository;

import com.autocare.crm_service.model.Interaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Interfaz de acceso a datos para Interaccion.
 * Extiende JpaRepository para obtener operaciones CRUD básicas de forma automática.
 */
@Repository
public interface InteraccionRepository extends JpaRepository<Interaccion, String> {

    /**
     * Busca el historial completo de interacciones de un cliente específico.
     */
    List<Interaccion> findByIdCliente(String idCliente);

    /**
     * Filtra interacciones por su tipo (ej: buscar todos los RECLAMOS).
     */
    List<Interaccion> findByTipo(Interaccion.TipoInteraccion tipo);

    /**
     * Filtra interacciones por su estado de seguimiento.
     */
    List<Interaccion> findBySeguimiento(Interaccion.SeguimientoEstado seguimiento);
}