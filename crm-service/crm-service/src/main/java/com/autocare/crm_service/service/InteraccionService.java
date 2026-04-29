package com.autocare.crm_service.service;

import com.autocare.crm_service.model.Interaccion;
import com.autocare.crm_service.repository.InteraccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InteraccionService {

    private final InteraccionRepository interaccionRepository;
    private final RestTemplate restTemplate;

    public InteraccionService(InteraccionRepository interaccionRepository,
                               RestTemplate restTemplate) {
        this.interaccionRepository = interaccionRepository;
        this.restTemplate = restTemplate;
    }

    public List<Interaccion> listarTodas() {
        return interaccionRepository.findAll();
    }

    public Optional<Interaccion> buscarPorId(String id) {
        return interaccionRepository.findById(id);
    }

    public List<Interaccion> buscarPorCliente(String idCliente) {
        return interaccionRepository.findByIdCliente(idCliente);
    }

    public List<Interaccion> buscarPorTipo(Interaccion.TipoInteraccion tipo) {
        return interaccionRepository.findByTipo(tipo);
    }

    public List<Interaccion> buscarPorSeguimiento(Interaccion.SeguimientoEstado seguimiento) {
        return interaccionRepository.findBySeguimiento(seguimiento);
    }

    @SuppressWarnings("unchecked")
    public Interaccion registrar(Interaccion interaccion) {
        // 🔑 Consulta el nombre del cliente al customer-service
        String url = "http://customer-service/clientes/" + interaccion.getIdCliente();

        try {
            ResponseEntity<Map<String, Object>> respuesta = restTemplate.getForEntity(
                url, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> cliente = respuesta.getBody();
            if (cliente != null) {
                String nombre = (String) cliente.get("nombre");
                String apellido = (String) cliente.get("apellido");
                interaccion.setNombreCliente(nombre + " " + apellido);
            }
        } catch (Exception e) {
            interaccion.setNombreCliente("Cliente no encontrado");
        }

        // Registra la fecha y hora automáticamente
        interaccion.setFechaInteraccion(LocalDateTime.now());
        interaccion.setSeguimiento(Interaccion.SeguimientoEstado.ABIERTO);

        return interaccionRepository.save(interaccion);
    }

    public Interaccion cambiarSeguimiento(String id,
                                           Interaccion.SeguimientoEstado nuevoEstado) {
        Interaccion interaccion = interaccionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Interacción no encontrada con ID: " + id));
        interaccion.setSeguimiento(nuevoEstado);
        return interaccionRepository.save(interaccion);
    }

    public void eliminar(String id) {
        if (!interaccionRepository.existsById(id)) {
            throw new RuntimeException("Interacción no encontrada con ID: " + id);
        }
        interaccionRepository.deleteById(id);
    }
}