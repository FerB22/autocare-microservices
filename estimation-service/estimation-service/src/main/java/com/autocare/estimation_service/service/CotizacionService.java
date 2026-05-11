package com.autocare.estimation_service.service;

import com.autocare.estimation_service.model.Cotizacion;
import com.autocare.estimation_service.repository.CotizacionRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final RestTemplate restTemplate;

    public CotizacionService(CotizacionRepository cotizacionRepository,
                              RestTemplate restTemplate) {
        this.cotizacionRepository = cotizacionRepository;
        this.restTemplate = restTemplate;
    }

    public List<Cotizacion> listarTodas() {
        return cotizacionRepository.findAll();
    }

    public Optional<Cotizacion> buscarPorId(String id) {
        return cotizacionRepository.findById(id);
    }

    public List<Cotizacion> buscarPorOrden(String idOrden) {
        return cotizacionRepository.findByIdOrden(idOrden);
    }

    public List<Cotizacion> buscarPorEstado(Cotizacion.EstadoCotizacion estado) {
        return cotizacionRepository.findByEstado(estado);
    }

    @SuppressWarnings("unchecked")
    public Cotizacion crear(Cotizacion cotizacion) {
        // 🔑 Consulta el precio del repuesto al spare-parts-service
        String url = "http://spare-parts-service/repuestos/" + cotizacion.getIdRepuesto();

        try {
            ResponseEntity<Map<String, Object>> respuesta = restTemplate.getForEntity(
                url, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> repuesto = respuesta.getBody();
            if (repuesto != null) {
                cotizacion.setNombreRepuesto((String) repuesto.get("nombre"));

                // El precio puede venir como Double o Integer según Jackson
                Object precio = repuesto.get("precio");
                if (precio instanceof Number number) {
                    cotizacion.setPrecioUnitario(number.doubleValue());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo obtener el repuesto: " + e.getMessage());
        }

        // Calcula el total de la línea automáticamente
        if (cotizacion.getPrecioUnitario() != null && cotizacion.getCantidad() != null) {
            cotizacion.setTotalLinea(cotizacion.getPrecioUnitario() * cotizacion.getCantidad());
        }

        cotizacion.setEstado(Cotizacion.EstadoCotizacion.PENDIENTE);
        return cotizacionRepository.save(cotizacion);
    }

    public Cotizacion cambiarEstado(String id, Cotizacion.EstadoCotizacion nuevoEstado) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cotización no encontrada con ID: " + id));
        cotizacion.setEstado(nuevoEstado);
        return cotizacionRepository.save(cotizacion);
    }

    public void eliminar(String id) {
        if (!cotizacionRepository.existsById(id)) {
            throw new RuntimeException("Cotización no encontrada con ID: " + id);
        }
        cotizacionRepository.deleteById(id);
    }
}