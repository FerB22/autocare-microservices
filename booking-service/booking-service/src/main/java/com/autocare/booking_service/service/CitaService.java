package com.autocare.booking_service.service;

import com.autocare.booking_service.dto.ClienteDTO;
import com.autocare.booking_service.dto.VehiculoDTO;
import com.autocare.booking_service.model.Cita;
import com.autocare.booking_service.repository.CitaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;

@Service
public class CitaService {

    private static final Logger log = LoggerFactory.getLogger(CitaService.class);

    private final CitaRepository citaRepository;
    private final WebClient.Builder webClientBuilder;

    public CitaService(CitaRepository citaRepository, WebClient.Builder webClientBuilder) {
        this.citaRepository = citaRepository;
        this.webClientBuilder = webClientBuilder;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Cita> listarTodas() {
        log.info("Listando todas las citas");
        return citaRepository.findAll();
    }

    public Optional<Cita> buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("El ID de la cita no puede ser nulo o vacío.");
        }
        log.info("Buscando cita con ID: {}", id);
        return citaRepository.findById(id);
    }

    public List<Cita> buscarPorVehiculo(String idVehiculo) {
        if (idVehiculo == null || idVehiculo.isBlank()) {
            throw new RuntimeException("El ID del vehículo no puede ser nulo o vacío.");
        }
        log.info("Buscando citas para vehículo: {}", idVehiculo);
        return citaRepository.findByIdVehiculo(idVehiculo);
    }

    // ─────────────────────────────────────────
    //  CREACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Cita guardar(Cita cita) {

        // ── REGLA 1: El vehículo debe existir en el sistema ──────────────────
        VehiculoDTO vehiculo = verificarVehiculo(cita.getIdVehiculo());
        log.info("Vehículo verificado: {} {}", vehiculo.getMarca(), vehiculo.getModelo());

        // ── REGLA 2: El cliente debe existir en el sistema ───────────────────
        ClienteDTO cliente = verificarCliente(cita.getIdCliente());
        log.info("Cliente verificado: {}", cliente.getNombre());

        // ── REGLA 3: No puede haber dos citas en el mismo horario ────────────
        boolean horarioOcupado = citaRepository
                .existsByFechaHoraAndEstadoNot(cita.getFechaHora(), Cita.EstadoCita.CANCELADA);

        if (horarioOcupado) {
            log.warn("Intento de agendar en horario ya ocupado: {}", cita.getFechaHora());
            throw new RuntimeException(
                "Ya existe una cita agendada para el horario: " + cita.getFechaHora() +
                ". Por favor elige otro horario."
            );
        }

        // ── REGLA 4: Un vehículo no puede tener más de una cita CONFIRMADA activa
        long citasActivasDelVehiculo = citaRepository
                .countByIdVehiculoAndEstado(cita.getIdVehiculo(), Cita.EstadoCita.CONFIRMADA);

        if (citasActivasDelVehiculo >= 1) {
            log.warn("Vehículo {} ya tiene una cita confirmada activa", cita.getIdVehiculo());
            throw new RuntimeException(
                "El vehículo ya tiene una cita CONFIRMADA pendiente. " +
                "Cancélala antes de agendar una nueva."
            );
        }

        cita.setEstado(Cita.EstadoCita.CONFIRMADA);

        log.info("Guardando nueva cita para vehículo {} el {}",
                cita.getIdVehiculo(), cita.getFechaHora());
        return citaRepository.save(cita);
    }

    // ─────────────────────────────────────────
    //  CAMBIO DE ESTADO CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Cita cambiarEstado(String id, Cita.EstadoCita nuevoEstado) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("El ID de la cita no puede ser nulo o vacío.");
        }
        log.info("Cambiando estado de cita {} a {}", id, nuevoEstado);

        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada con ID: " + id));

        // ── REGLA 5: Una cita EJECUTADA no puede cambiar de estado ───────────
        if (cita.getEstado() == Cita.EstadoCita.EJECUTADA) {
            log.warn("Intento de modificar cita ya ejecutada: {}", id);
            throw new RuntimeException(
                "La cita ya fue EJECUTADA y no puede cambiar de estado."
            );
        }

        // ── REGLA 6: Una cita CANCELADA no puede reactivarse ─────────────────
        if (cita.getEstado() == Cita.EstadoCita.CANCELADA) {
            log.warn("Intento de reactivar cita cancelada: {}", id);
            throw new RuntimeException(
                "La cita está CANCELADA y no puede modificarse. Crea una nueva cita."
            );
        }

        cita.setEstado(nuevoEstado);
        log.info("Estado de cita {} actualizado exitosamente a {}", id, nuevoEstado);
        return citaRepository.save(cita);
    }

    // ─────────────────────────────────────────
    //  CANCELACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Cita cancelarCita(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("El ID de la cita no puede ser nulo o vacío.");
        }
        log.info("Cancelando cita con ID: {}", id);

        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada con ID: " + id));

        // ── REGLA 7: No se puede cancelar una cita ya ejecutada ──────────────
        if (cita.getEstado() == Cita.EstadoCita.EJECUTADA) {
            log.warn("Intento de cancelar cita ya ejecutada: {}", id);
            throw new RuntimeException(
                "No se puede cancelar una cita que ya fue EJECUTADA."
            );
        }

        // ── REGLA 8: No tiene sentido cancelar una cita ya cancelada ─────────
        if (cita.getEstado() == Cita.EstadoCita.CANCELADA) {
            log.warn("La cita {} ya estaba cancelada", id);
            throw new RuntimeException(
                "La cita ya se encuentra CANCELADA."
            );
        }

        cita.setEstado(Cita.EstadoCita.CANCELADA);
        log.info("Cita {} cancelada exitosamente", id);
        return citaRepository.save(cita);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("El ID de la cita no puede ser nulo o vacío.");
        }
        log.info("Eliminando cita con ID: {}", id);

        if (!citaRepository.existsById(id)) {
            throw new RuntimeException("Cita no encontrada con ID: " + id);
        }

        citaRepository.deleteById(id);
        log.info("Cita {} eliminada del sistema", id);
    }

    // ─────────────────────────────────────────
    //  COMUNICACIÓN CON OTROS SERVICIOS
    // ─────────────────────────────────────────

    public VehiculoDTO verificarVehiculo(String idVehiculo) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://fleet-service/vehiculos/" + idVehiculo)
                    .retrieve()
                    .bodyToMono(VehiculoDTO.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Vehículo con ID {} no encontrado en fleet-service", idVehiculo);
            throw new RuntimeException("El vehículo no existe en el sistema: " + idVehiculo);
        }
    }

    public ClienteDTO verificarCliente(String idCliente) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://customer-service/clientes/" + idCliente)
                    .retrieve()
                    .bodyToMono(ClienteDTO.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Cliente con ID {} no encontrado en customer-service", idCliente);
            throw new RuntimeException("El cliente no existe en el sistema: " + idCliente);
        }
    }
}