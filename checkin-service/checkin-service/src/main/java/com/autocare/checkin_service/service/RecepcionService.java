package com.autocare.checkin_service.service;

import com.autocare.checkin_service.model.Recepcion;
import com.autocare.checkin_service.repository.RecepcionRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class RecepcionService {

    private final RecepcionRepository recepcionRepository;
    private final RestTemplate restTemplate;

    public RecepcionService(RecepcionRepository recepcionRepository,
                             RestTemplate restTemplate) {
        this.recepcionRepository = recepcionRepository;
        this.restTemplate = restTemplate;
    }

    public List<Recepcion> listarTodas() {
        return recepcionRepository.findAll();
    }

    public Optional<Recepcion> buscarPorId(String id) {
        return recepcionRepository.findById(id);
    }

    public List<Recepcion> buscarPorVehiculo(String idVehiculo) {
        return recepcionRepository.findByIdVehiculo(idVehiculo);
    }

    @SuppressWarnings("unchecked")
    public Recepcion registrar(Recepcion recepcion) {
        String url = "http://workflow-service/ordenes";

        Map<String, String> body = new HashMap<>();
        body.put("idVehiculo", recepcion.getIdVehiculo());
        body.put("prioridad", "MEDIA");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> respuesta = restTemplate.postForEntity(
                url, request, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            // ✅ Se guarda el body en variable para evitar doble llamada a getBody()
            Map<String, Object> cuerpo = respuesta.getBody();
            if (respuesta.getStatusCode().is2xxSuccessful() && cuerpo != null) {
                String idOrden = (String) cuerpo.get("idOrden");
                recepcion.setIdOrdenCreada(idOrden);
            }

        } catch (Exception e) {
            recepcion.setIdOrdenCreada("PENDIENTE - " + e.getMessage());
        }

        return recepcionRepository.save(recepcion);
    }

    public void eliminar(String id) {
        if (!recepcionRepository.existsById(id)) {
            throw new RuntimeException("Recepción no encontrada con ID: " + id);
        }
        recepcionRepository.deleteById(id);
    }
}