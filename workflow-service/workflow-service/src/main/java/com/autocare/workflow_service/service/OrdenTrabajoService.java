package com.autocare.workflow_service.service;

import com.autocare.workflow_service.model.OrdenTrabajo;
import com.autocare.workflow_service.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Optional;

@Service
public class OrdenTrabajoService {

    private final OrdenTrabajoRepository ordenRepository;
    private final RestTemplate restTemplate;

    public OrdenTrabajoService(OrdenTrabajoRepository ordenRepository,
                                RestTemplate restTemplate) {
        this.ordenRepository = ordenRepository;
        this.restTemplate = restTemplate;
    }

    public List<OrdenTrabajo> listarTodas() {
        return ordenRepository.findAll();
    }

    public Optional<OrdenTrabajo> buscarPorId(String id) {
        return ordenRepository.findById(id);
    }

    public List<OrdenTrabajo> buscarPorVehiculo(String idVehiculo) {
        return ordenRepository.findByIdVehiculo(idVehiculo);
    }

    public List<OrdenTrabajo> buscarPorEstado(OrdenTrabajo.EstadoOrden estado) {
        return ordenRepository.findByEstado(estado);
    }

    public OrdenTrabajo crear(OrdenTrabajo orden) {
        // 🔑 Llama al hr-service para buscar un mecánico disponible
        String url = "http://hr-service/mecanicos/disponibles";
        try {
            ResponseEntity<List<Object>> respuesta = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Object>>() {}
            );

            List<Object> mecanicos = respuesta.getBody();
            if (mecanicos == null || mecanicos.isEmpty()) {
                throw new RuntimeException("No hay mecánicos disponibles en este momento");
            }

            // Toma el primer mecánico disponible
            // (en versiones futuras podrías filtrar por especialidad)
            Object primerMecanico = mecanicos.get(0);
            if (primerMecanico instanceof java.util.LinkedHashMap<?, ?> mapa) {
                String idMecanico = (String) mapa.get("idMecanico");
                orden.setIdMecanicoAsignado(idMecanico);
            }

        } catch (RuntimeException e) {
            throw new RuntimeException("Error al consultar mecánicos: " + e.getMessage());
        }

        orden.setEstado(OrdenTrabajo.EstadoOrden.EN_ESPERA);
        return ordenRepository.save(orden);
    }

    public OrdenTrabajo cambiarEstado(String id, OrdenTrabajo.EstadoOrden nuevoEstado) {
        OrdenTrabajo orden = ordenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + id));
        orden.setEstado(nuevoEstado);
        return ordenRepository.save(orden);
    }

    public void eliminar(String id) {
        if (!ordenRepository.existsById(id)) {
            throw new RuntimeException("Orden no encontrada con ID: " + id);
        }
        ordenRepository.deleteById(id);
    }
}