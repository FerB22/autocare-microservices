package com.autocare.booking_service.repository;

import com.autocare.booking_service.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, String> {

    // ─────────────────────────────────────────
    //  CONSULTAS POR VEHÍCULO
    // ─────────────────────────────────────────

    // Todas las citas de un vehículo específico
    List<Cita> findByIdVehiculo(String idVehiculo);

    // Citas de un vehículo filtradas por estado (ej: todas las CONFIRMADAS de ese vehículo)
    List<Cita> findByIdVehiculoAndEstado(String idVehiculo, Cita.EstadoCita estado);

    // Cantidad de citas de un vehículo con un estado concreto (usado en Regla 4)
    long countByIdVehiculoAndEstado(String idVehiculo, Cita.EstadoCita estado);

    // ─────────────────────────────────────────
    //  CONSULTAS POR ESTADO
    // ─────────────────────────────────────────

    // Solo las citas con un estado específico
    List<Cita> findByEstado(Cita.EstadoCita estado);

    // ─────────────────────────────────────────
    //  CONSULTAS POR CLIENTE
    // ─────────────────────────────────────────

    // Todas las citas de un cliente específico
    List<Cita> findByIdCliente(String idCliente);

    // Citas de un cliente filtradas por estado
    List<Cita> findByIdClienteAndEstado(String idCliente, Cita.EstadoCita estado);

    // ─────────────────────────────────────────
    //  CONSULTAS POR FECHA Y HORARIO
    // ─────────────────────────────────────────

    // Verifica si existe una cita en ese horario exacto que NO esté cancelada (Regla 3)
    boolean existsByFechaHoraAndEstadoNot(LocalDateTime fechaHora, Cita.EstadoCita estado);

    // Citas dentro de un rango de fechas (útil para reportes o vistas de agenda)
    List<Cita> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);

    // Citas dentro de un rango de fechas filtradas por estado
    List<Cita> findByFechaHoraBetweenAndEstado(LocalDateTime desde, LocalDateTime hasta, Cita.EstadoCita estado);
}