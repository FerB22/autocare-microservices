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

/**
 * Servicio que contiene la lógica de negocio para Citas.
 * Todas las reglas de negocio deben implementarse aquí (no en controllers ni repositories).
 * Comentarios añadidos para explicar cada bloque y las decisiones clave.
 */
@Service
public class CitaService {

    // Logger para trazabilidad y debugging
    private static final Logger log = LoggerFactory.getLogger(CitaService.class);

    // Repositorio JPA para persistencia de Cita
    private final CitaRepository citaRepository;
    // WebClient.Builder inyectado para llamadas a otros microservicios (fleet, customer)
    private final WebClient.Builder webClientBuilder;

    // Inyección por constructor (facilita testing y claridad de dependencias)
    public CitaService(CitaRepository citaRepository, WebClient.Builder webClientBuilder) {
        this.citaRepository = citaRepository;
        this.webClientBuilder = webClientBuilder;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    /**
     * Lista todas las citas.
     * Nota: en entornos con muchos registros considerar paginación.
     */
    public List<Cita> listarTodas() {
        log.info("Listando todas las citas");
        return citaRepository.findAll();
    }

    /**
     * Busca una cita por su ID.
     * - Valida que el id no sea nulo o vacío antes de consultar.
     * - Devuelve Optional para que el controlador decida entre 200 y 404.
     */
    public Optional<Cita> buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            // Validación temprana para evitar consultas inválidas al repositorio
            throw new RuntimeException("El ID de la cita no puede ser nulo o vacío.");
        }
        log.info("Buscando cita con ID: {}", id);
        return citaRepository.findById(id);
    }

    /**
     * Busca citas asociadas a un vehículo.
     * - Valida que el idVehiculo no sea nulo o vacío.
     * - Devuelve lista (posible lista vacía si no hay resultados).
     */
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

    /**
     * Guarda una nueva cita aplicando las reglas de negocio:
     * 1) Verificar que el vehículo exista en fleet-service.
     * 2) Verificar que el cliente exista en customer-service.
     * 3) Evitar duplicidad de horario (ignorando citas CANCELADAS).
     * 4) Evitar que un vehículo tenga más de una cita CONFIRMADA activa.
     *
     * Observaciones:
     * - Las llamadas a otros servicios usan WebClient.block() (síncronas).
     *   En producción considerar timeouts, retries y circuit-breaker.
     * - Para evitar condiciones de carrera al crear citas simultáneas,
     *   considerar @Transactional + constraint único en BD o locking.
     */
    public Cita guardar(Cita cita) {

        // ── REGLA 1: El vehículo debe existir en el sistema ──────────────────
        VehiculoDTO vehiculo = verificarVehiculo(cita.getIdVehiculo());
        log.info("Vehículo verificado: {} {}", vehiculo.getMarca(), vehiculo.getModelo());

        // ── REGLA 2: El cliente debe existir en el sistema ───────────────────
        ClienteDTO cliente = verificarCliente(cita.getIdCliente());
        log.info("Cliente verificado: {} {}", cliente.getNombre(), cliente.getApellido());

        // ── REGLA 3: No puede haber dos citas en el mismo horario ────────────
        // Se ignoran las citas con estado CANCELADA porque no "ocupan" el horario.
        boolean horarioOcupado = citaRepository
                .existsByFechaHoraAndEstadoNot(cita.getFechaHora(), Cita.EstadoCita.CANCELADA);

        if (horarioOcupado) {
            // Registro y excepción clara para que el controlador devuelva el código HTTP apropiado
            log.warn("Intento de agendar en horario ya ocupado: {}", cita.getFechaHora());
            throw new RuntimeException(
                "Ya existe una cita agendada para el horario: " + cita.getFechaHora() +
                ". Por favor elige otro horario."
            );
        }

        // ── REGLA 4: Un vehículo no puede tener más de una cita CONFIRMADA activa
        // Se cuenta cuántas citas CONFIRMADA tiene el vehículo; si >=1, se bloquea.
        long citasActivasDelVehiculo = citaRepository
                .countByIdVehiculoAndEstado(cita.getIdVehiculo(), Cita.EstadoCita.CONFIRMADA);

        if (citasActivasDelVehiculo >= 1) {
            log.warn("Vehículo {} ya tiene una cita confirmada activa", cita.getIdVehiculo());
            throw new RuntimeException(
                "El vehículo ya tiene una cita CONFIRMADA pendiente. " +
                "Cancélala antes de agendar una nueva."
            );
        }

        // Estado inicial al crear la cita: CONFIRMADA
        cita.setEstado(Cita.EstadoCita.CONFIRMADA);

        log.info("Guardando nueva cita para vehículo {} el {}",
                cita.getIdVehiculo(), cita.getFechaHora());
        return citaRepository.save(cita);
    }

    // ─────────────────────────────────────────
    //  CAMBIO DE ESTADO CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    /**
     * Cambia el estado de una cita aplicando reglas:
     * - Una cita EJECUTADA es histórica y no puede modificarse.
     * - Una cita CANCELADA no puede reactivarse; se debe crear una nueva.
     */
    public Cita cambiarEstado(String id, Cita.EstadoCita nuevoEstado) {
        if (id == null || id.isBlank()) {
            // Validación temprana para evitar operaciones inválidas
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

        // Aplicar el nuevo estado y persistir
        cita.setEstado(nuevoEstado);
        log.info("Estado de cita {} actualizado exitosamente a {}", id, nuevoEstado);
        return citaRepository.save(cita);
    }

    // ─────────────────────────────────────────
    //  CANCELACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    /**
     * Cancela una cita aplicando reglas:
     * - No se puede cancelar una cita ya ejecutada.
     * - No se puede cancelar una cita ya cancelada.
     */
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

        // Marcar como CANCELADA y persistir
        cita.setEstado(Cita.EstadoCita.CANCELADA);
        log.info("Cita {} cancelada exitosamente", id);
        return citaRepository.save(cita);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN
    // ─────────────────────────────────────────

    /**
     * Elimina una cita si existe.
     * - Valida id no nulo/vacío.
     * - Lanza excepción si la cita no existe.
     * - En producción considerar reglas adicionales (p. ej. solo usuarios con permisos).
     */
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

    /**
     * Verifica la existencia del vehículo consultando fleet-service.
     * - Usa WebClient y bloquea la llamada (síncrona).
     * - Si el servicio remoto responde con error, se lanza RuntimeException.
     * - En producción considerar timeouts, retries y circuit-breaker (Resilience4j).
     */
    public VehiculoDTO verificarVehiculo(String idVehiculo) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://fleet-service/vehiculos/" + idVehiculo)
                    .retrieve()
                    .bodyToMono(VehiculoDTO.class)
                    .block();
        } catch (WebClientResponseException e) {
            // Log con contexto y re-lanzar excepción de negocio para que el controlador la maneje
            log.error("Vehículo con ID {} no encontrado en fleet-service", idVehiculo);
            throw new RuntimeException("El vehículo no existe en el sistema: " + idVehiculo);
        }
    }

    /**
     * Verifica la existencia del cliente consultando customer-service.
     * - Misma consideración de resiliencia que en verificarVehiculo.
     */
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
