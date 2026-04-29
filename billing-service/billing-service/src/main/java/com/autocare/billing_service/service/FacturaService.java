package com.autocare.billing_service.service;

import com.autocare.billing_service.dto.CotizacionDTO;
import com.autocare.billing_service.model.Factura;
import com.autocare.billing_service.repository.FacturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;

@Service
public class FacturaService {

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);
    private static final double IVA = 0.19;

    private final FacturaRepository facturaRepository;
    private final WebClient.Builder webClientBuilder;

    public FacturaService(FacturaRepository facturaRepository,
                          WebClient.Builder webClientBuilder) {
        this.facturaRepository = facturaRepository;
        this.webClientBuilder = webClientBuilder;
    }

    public List<Factura> listarTodas() {
        log.info("Listando todas las facturas");
        return facturaRepository.findAll();
    }

    public Optional<Factura> buscarPorId(String id) {
        log.info("Buscando factura con ID: {}", id);
        return facturaRepository.findById(id);
    }

    public List<Factura> buscarPorCliente(String idCliente) {
        log.info("Buscando facturas del cliente: {}", idCliente);
        return facturaRepository.findByIdCliente(idCliente);
    }

    public List<Factura> buscarPorEstado(Factura.EstadoFactura estado) {
        log.info("Buscando facturas con estado: {}", estado);
        return facturaRepository.findByEstado(estado);
    }

    public Factura generar(Factura factura) {
        log.info("Generando factura para orden: {}", factura.getIdOrden());

        // Consultar cotizaciones aprobadas desde estimation-service
        List<CotizacionDTO> cotizaciones = obtenerCotizaciones(factura.getIdOrden());

        if (cotizaciones == null || cotizaciones.isEmpty()) {
            log.error("No hay cotizaciones para la orden: {}", factura.getIdOrden());
            throw new RuntimeException("No hay cotizaciones para la orden: " + factura.getIdOrden());
        }

        // Sumar solo las cotizaciones APROBADAS
        double subtotal = cotizaciones.stream()
            .filter(c -> "APROBADA".equals(c.getEstado()))
            .mapToDouble(c -> c.getTotalLinea() != null ? c.getTotalLinea() : 0.0)
            .sum();

        if (subtotal == 0) {
            log.error("No hay cotizaciones APROBADAS para la orden: {}", factura.getIdOrden());
            throw new RuntimeException("No hay cotizaciones APROBADAS para la orden: " + factura.getIdOrden());
        }

        double impuesto = Math.round(subtotal * IVA * 100.0) / 100.0;
        double total = Math.round((subtotal + impuesto) * 100.0) / 100.0;

        factura.setSubtotal(subtotal);
        factura.setImpuesto(impuesto);
        factura.setTotal(total);
        factura.setEstado(Factura.EstadoFactura.PENDIENTE);

        log.info("Factura calculada — subtotal: {}, IVA: {}, total: {}", subtotal, impuesto, total);
        return facturaRepository.save(factura);
    }

    private List<CotizacionDTO> obtenerCotizaciones(String idOrden) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://estimation-service/cotizaciones/orden/" + idOrden)
                    .retrieve()
                    .bodyToFlux(CotizacionDTO.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error al consultar cotizaciones para orden {}: {}", idOrden, e.getMessage());
            throw new RuntimeException("Error al consultar cotizaciones: " + e.getMessage());
        }
    }

    public Factura cambiarEstado(String id, Factura.EstadoFactura nuevoEstado) {
        log.info("Cambiando estado de factura {} a {}", id, nuevoEstado);
        Factura factura = facturaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + id));
        factura.setEstado(nuevoEstado);
        return facturaRepository.save(factura);
    }

    public void eliminar(String id) {
        log.info("Eliminando factura con ID: {}", id);
        if (!facturaRepository.existsById(id)) {
            throw new RuntimeException("Factura no encontrada con ID: " + id);
        }
        facturaRepository.deleteById(id);
    }
}