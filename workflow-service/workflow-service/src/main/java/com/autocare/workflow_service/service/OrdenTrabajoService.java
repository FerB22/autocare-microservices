package com.autocare.workflow_service.service;

import com.autocare.workflow_service.model.OrdenTrabajo;
import com.autocare.workflow_service.repository.OrdenTrabajoRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OrdenTrabajoService {

    // El flujo válido es lineal y solo avanza. Nunca retrocede.
    // Definimos el orden aquí como referencia para todas las reglas.
    private static final List<OrdenTrabajo.EstadoOrden> FLUJO_ESTADOS = List.of(
        OrdenTrabajo.EstadoOrden.EN_ESPERA,
        OrdenTrabajo.EstadoOrden.EN_PROCESO,
        OrdenTrabajo.EstadoOrden.CONTROL_CALIDAD,
        OrdenTrabajo.EstadoOrden.LISTO,
        OrdenTrabajo.EstadoOrden.ENTREGADO
    );

    private final OrdenTrabajoRepository ordenRepository;
    private final RestTemplate restTemplate;

    public OrdenTrabajoService(OrdenTrabajoRepository ordenRepository,
                               RestTemplate restTemplate) {
        this.ordenRepository = ordenRepository;
        this.restTemplate = restTemplate;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<OrdenTrabajo> listarTodas() {
        log.info("Listando todas las órdenes de trabajo");
        return ordenRepository.findAll();
    }

    public Optional<OrdenTrabajo> buscarPorId(String id) {
        log.info("Buscando orden con ID: {}", id);
        return ordenRepository.findById(id);
    }

    public List<OrdenTrabajo> buscarPorVehiculo(String idVehiculo) {
        log.info("Buscando órdenes del vehículo: {}", idVehiculo);
        return ordenRepository.findByIdVehiculo(idVehiculo);
    }

    public List<OrdenTrabajo> buscarPorEstado(OrdenTrabajo.EstadoOrden estado) {
        log.info("Buscando órdenes con estado: {}", estado);
        return ordenRepository.findByEstado(estado);
    }

    public List<OrdenTrabajo> buscarPorMecanico(String idMecanico) {
        log.info("Buscando órdenes del mecánico: {}", idMecanico);
        return ordenRepository.findByIdMecanicoAsignado(idMecanico);
    }

    // ─────────────────────────────────────────
    //  CREACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public OrdenTrabajo crear(OrdenTrabajo orden) {
        log.info("Creando nueva orden de trabajo para vehículo: {}", orden.getIdVehiculo());

        // ── REGLA 1: Un vehículo no puede tener dos órdenes activas ──────────
        // "Activa" significa cualquier estado que no sea LISTO ni ENTREGADO.
        // Si el auto ya está en el taller, no puede entrar dos veces.
        List<OrdenTrabajo> ordenesActivas = ordenRepository
                .findByIdVehiculo(orden.getIdVehiculo())
                .stream()
                .filter(o -> o.getEstado() != OrdenTrabajo.EstadoOrden.LISTO &&
                             o.getEstado() != OrdenTrabajo.EstadoOrden.ENTREGADO)
                .toList();

        if (!ordenesActivas.isEmpty()) {
            log.warn("Vehículo {} ya tiene una orden activa", orden.getIdVehiculo());
            throw new RuntimeException(
                "El vehículo ya tiene una orden de trabajo activa. " +
                "No se puede crear una nueva hasta que la actual esté ENTREGADA."
            );
        }

        // ── REGLA 2: La prioridad debe ser un valor válido ────────────────────
        // Evitamos que lleguen valores arbitrarios como "URGENTÍSIMO" o "none".
        List<String> prioridadesValidas = List.of("ALTA", "MEDIA", "BAJA");
        if (!prioridadesValidas.contains(orden.getPrioridad().toUpperCase())) {
            log.warn("Prioridad inválida recibida: {}", orden.getPrioridad());
            throw new RuntimeException(
                "Prioridad inválida: '" + orden.getPrioridad() + "'. " +
                "Los valores permitidos son: ALTA, MEDIA o BAJA."
            );
        }

        // ── REGLA 3: Debe haber un mecánico disponible para asignar ──────────
        // Consulta al hr-service. Sin mecánico, no puede iniciarse el trabajo.
        String url = "http://hr-service/mecanicos/disponibles";
        try {
            ResponseEntity<List<Object>> respuesta = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Object>>() {}
            );

            List<Object> mecanicos = respuesta.getBody();
            if (mecanicos == null || mecanicos.isEmpty()) {
                log.warn("No hay mecánicos disponibles al crear la orden");
                throw new RuntimeException(
                    "No hay mecánicos disponibles en este momento. " +
                    "Intente más tarde o reasigne manualmente."
                );
            }

            // Toma el primer mecánico disponible
            // (en versiones futuras se puede filtrar por especialidad)
            Object primerMecanico = mecanicos.get(0);
            if (primerMecanico instanceof java.util.LinkedHashMap<?, ?> mapa) {
                String idMecanico = (String) mapa.get("idMecanico");
                orden.setIdMecanicoAsignado(idMecanico);
                log.info("Mecánico {} asignado a la orden", idMecanico);
            }

        } catch (RuntimeException e) {
            throw new RuntimeException("Error al consultar mecánicos: " + e.getMessage());
        }

        // Toda orden nueva comienza siempre en EN_ESPERA, sin excepciones
        orden.setEstado(OrdenTrabajo.EstadoOrden.EN_ESPERA);
        orden.setPrioridad(orden.getPrioridad().toUpperCase());

        log.info("Orden de trabajo creada para vehículo {} con prioridad {}",
                orden.getIdVehiculo(), orden.getPrioridad());
        return ordenRepository.save(orden);
    }

    // ─────────────────────────────────────────
    //  CAMBIO DE ESTADO CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public OrdenTrabajo cambiarEstado(String id, OrdenTrabajo.EstadoOrden nuevoEstado) {
        log.info("Cambiando estado de orden {} a {}", id, nuevoEstado);

        OrdenTrabajo orden = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Orden no encontrada con ID: " + id
                ));

        int indiceActual = FLUJO_ESTADOS.indexOf(orden.getEstado());
        int indiceNuevo  = FLUJO_ESTADOS.indexOf(nuevoEstado);

        // ── REGLA 4: Una orden ENTREGADA es estado final, no cambia ──────────
        // Una vez entregado el auto al cliente, la orden cierra definitivamente.
        if (orden.getEstado() == OrdenTrabajo.EstadoOrden.ENTREGADO) {
            log.warn("Intento de modificar orden ya ENTREGADA: {}", id);
            throw new RuntimeException(
                "La orden ya fue ENTREGADA al cliente. No puede cambiar de estado."
            );
        }

        // ── REGLA 5: El estado solo puede avanzar, nunca retroceder ──────────
        // Imagínalo como una línea de ensamblaje: el auto solo va hacia adelante.
        // No puede "volver" a EN_ESPERA si ya está EN_PROCESO.
        if (indiceNuevo <= indiceActual) {
            log.warn("Intento de retroceder estado: {} → {}", orden.getEstado(), nuevoEstado);
            throw new RuntimeException(
                "El estado no puede retroceder. " +
                "Flujo actual: " + orden.getEstado() +
                " → Intentado: " + nuevoEstado +
                ". El orden válido es: EN_ESPERA → EN_PROCESO → CONTROL_CALIDAD → LISTO → ENTREGADO."
            );
        }

        // ── REGLA 6: Solo se permite avanzar UN paso a la vez ────────────────
        // No se puede saltar de EN_ESPERA directo a LISTO sin pasar por
        // EN_PROCESO y CONTROL_CALIDAD. Cada etapa tiene su propósito físico.
        if (indiceNuevo != indiceActual + 1) {
            log.warn("Intento de saltar etapas: {} → {}", orden.getEstado(), nuevoEstado);
            throw new RuntimeException(
                "No se pueden saltar etapas del flujo. " +
                "Desde " + orden.getEstado() +
                " el siguiente estado válido es: " + FLUJO_ESTADOS.get(indiceActual + 1)
            );
        }

        orden.setEstado(nuevoEstado);
        log.info("Orden {} avanzó de {} a {} exitosamente", id, 
                FLUJO_ESTADOS.get(indiceActual), nuevoEstado);
        return ordenRepository.save(orden);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando orden con ID: {}", id);

        OrdenTrabajo orden = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Orden no encontrada con ID: " + id
                ));

        // ── REGLA 7: Solo se puede eliminar una orden en EN_ESPERA ───────────
        // Si el trabajo ya comenzó (EN_PROCESO en adelante), eliminar la orden
        // borra el historial de un trabajo real que ya se está ejecutando.
        // Una orden activa es un documento de trabajo en curso.
        if (orden.getEstado() != OrdenTrabajo.EstadoOrden.EN_ESPERA) {
            log.warn("Intento de eliminar orden en estado {}: {}", orden.getEstado(), id);
            throw new RuntimeException(
                "Solo se pueden eliminar órdenes en estado EN_ESPERA. " +
                "Esta orden está en estado: " + orden.getEstado() + "."
            );
        }

        ordenRepository.deleteById(id);
        log.info("Orden {} eliminada del sistema", id);
    }
}