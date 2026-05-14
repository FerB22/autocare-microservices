package com.autocare.billing_service.service;

import com.autocare.billing_service.dto.CotizacionDTO;
import com.autocare.billing_service.exception.RecursoNoEncontradoException;
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

    // Logger para trazabilidad y debugging
    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);

    // IVA usado en los cálculos
    private static final double IVA = 0.19;

    // Repositorio JPA para persistencia
    private final FacturaRepository facturaRepository;

    // WebClient.Builder para llamadas a otros microservicios
    private final WebClient.Builder webClientBuilder;

    public FacturaService(FacturaRepository facturaRepository,
                          WebClient.Builder webClientBuilder) {
        this.facturaRepository = facturaRepository;
        this.webClientBuilder = webClientBuilder;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Factura> listarTodas() {
        log.info("Listando todas las facturas");
        // Devuelve todas las facturas; en producción considerar paginación
        return facturaRepository.findAll();
    }

    public Optional<Factura> buscarPorId(String id) {
        log.info("Buscando factura con ID: {}", id);
        // Optional permite al controlador decidir entre 200 y 404
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

    // ─────────────────────────────────────────
    //  GENERACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Factura generar(Factura factura) {
        log.info("Generando factura para orden: {}", factura.getIdOrden());

        // REGLA 1: No puede existir ya una factura para la misma orden
        // Evita facturar dos veces la misma orden. Se usa findByIdOrden() y isPresent().
        boolean yaFacturada = facturaRepository
                .findByIdOrden(factura.getIdOrden())
                .isPresent();

        if (yaFacturada) {
            log.warn("La orden {} ya tiene una factura generada", factura.getIdOrden());
            throw new RuntimeException(
                "Ya existe una factura para la orden: " + factura.getIdOrden() +
                ". No se puede facturar dos veces el mismo trabajo."
            );
        }

        // Consultar cotizaciones aprobadas desde estimation-service
        List<CotizacionDTO> cotizaciones = obtenerCotizaciones(factura.getIdOrden());

        // Si no hay cotizaciones, no se puede generar factura
        if (cotizaciones == null || cotizaciones.isEmpty()) {
            log.error("No hay cotizaciones para la orden: {}", factura.getIdOrden());
            throw new RuntimeException(
                "No hay cotizaciones para la orden: " + factura.getIdOrden()
            );
        }

        // REGLA 2: Solo se suman cotizaciones APROBADAS
        // Normaliza totalLinea nulo a 0.0 para evitar NPE en el stream
        double subtotal = cotizaciones.stream()
                .filter(c -> "APROBADA".equals(c.getEstado()))
                .mapToDouble(c -> {
                    Double total = c.getTotalLinea();
                    return total != null ? total : 0.0;
                })
                .sum();

        // REGLA 3: No generar facturas con monto $0
        // Evita facturas sin valor económico
        if (subtotal == 0) {
            log.error("No hay cotizaciones APROBADAS para la orden: {}", factura.getIdOrden());
            throw new RuntimeException(
                "No se puede generar una factura con monto $0. " +
                "La orden " + factura.getIdOrden() + " no tiene cotizaciones APROBADAS."
            );
        }

        // Cálculo de impuesto y total con redondeo a 2 decimales
        double impuesto = Math.round(subtotal * IVA * 100.0) / 100.0;
        double total    = Math.round((subtotal + impuesto) * 100.0) / 100.0;

        // Asignar valores calculados a la entidad antes de persistir
        factura.setSubtotal(subtotal);
        factura.setImpuesto(impuesto);
        factura.setTotal(total);
        factura.setEstado(Factura.EstadoFactura.PENDIENTE);

        log.info("Factura calculada — subtotal: {}, IVA: {}, total: {}", subtotal, impuesto, total);
        return facturaRepository.save(factura);
    }

    // ─────────────────────────────────────────
    //  PAGO — MÉTODO DEDICADO CON REGLAS
    // ─────────────────────────────────────────

    public Factura pagarFactura(String id) {
        log.info("Procesando pago de factura con ID: {}", id);

        // Buscar factura o lanzar excepción específica si no existe
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Factura no encontrada con ID: " + id
                ));

        // REGLA 4: No se puede pagar una factura ya pagada
        if (factura.getEstado() == Factura.EstadoFactura.PAGADA) {
            log.warn("Intento de pagar una factura ya pagada: {}", id);
            throw new RuntimeException(
                "La factura ya fue PAGADA. No se puede procesar el mismo pago dos veces."
            );
        }

        // REGLA 5: No se puede pagar una factura anulada
        if (factura.getEstado() == Factura.EstadoFactura.ANULADA) {
            log.warn("Intento de pagar una factura anulada: {}", id);
            throw new RuntimeException(
                "La factura está ANULADA y no puede ser pagada. " +
                "Genere una nueva factura si corresponde."
            );
        }

        // Marcar como pagada y persistir
        factura.setEstado(Factura.EstadoFactura.PAGADA);
        log.info("Factura {} marcada como PAGADA exitosamente", id);
        return facturaRepository.save(factura);
    }

    // ─────────────────────────────────────────
    //  ANULACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Factura anularFactura(String id) {
        log.info("Anulando factura con ID: {}", id);

        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Factura no encontrada con ID: " + id
                ));

        // REGLA 6: No se puede anular una factura ya pagada
        if (factura.getEstado() == Factura.EstadoFactura.PAGADA) {
            log.warn("Intento de anular una factura ya pagada: {}", id);
            throw new RuntimeException(
                "No se puede anular una factura que ya fue PAGADA."
            );
        }

        // REGLA 7: No tiene sentido anular una factura ya anulada
        if (factura.getEstado() == Factura.EstadoFactura.ANULADA) {
            log.warn("La factura {} ya estaba anulada", id);
            throw new RuntimeException(
                "La factura ya se encuentra ANULADA."
            );
        }

        factura.setEstado(Factura.EstadoFactura.ANULADA);
        log.info("Factura {} anulada exitosamente", id);
        return facturaRepository.save(factura);
    }

    // ─────────────────────────────────────────
    //  CAMBIO DE ESTADO GENÉRICO (uso interno)
    // ─────────────────────────────────────────

    public Factura cambiarEstado(String id, Factura.EstadoFactura nuevoEstado) {
        log.info("Cambiando estado de factura {} a {}", id, nuevoEstado);

        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Factura no encontrada con ID: " + id
                ));

        // REGLA 8: Una factura PAGADA o ANULADA es estado final
        if (factura.getEstado() == Factura.EstadoFactura.PAGADA ||
            factura.getEstado() == Factura.EstadoFactura.ANULADA) {
            log.warn("Intento de cambiar estado de factura en estado final: {}", factura.getEstado());
            throw new RuntimeException(
                "No se puede cambiar el estado de una factura que ya está " +
                factura.getEstado() + ". Es un estado final."
            );
        }

        factura.setEstado(nuevoEstado);
        log.info("Estado de factura {} actualizado a {}", id, nuevoEstado);
        return facturaRepository.save(factura);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando factura con ID: {}", id);

        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Factura no encontrada con ID: " + id
                ));

        // REGLA 9: No se puede eliminar una factura pagada
        if (factura.getEstado() == Factura.EstadoFactura.PAGADA) {
            log.warn("Intento de eliminar factura pagada: {}", id);
            throw new RuntimeException(
                "No se puede eliminar una factura PAGADA. " +
                "Es un documento contable. Si hay un error, anúlela."
            );
        }

        facturaRepository.deleteById(id);
        log.info("Factura {} eliminada del sistema", id);
    }

    // ─────────────────────────────────────────
    //  COMUNICACIÓN CON OTROS SERVICIOS
    // ─────────────────────────────────────────

    private List<CotizacionDTO> obtenerCotizaciones(String idOrden) {
        try {
            // Llamada síncrona a estimation-service; bloquea hasta recibir la lista
            return webClientBuilder.build()
                    .get()
                    .uri("http://estimation-service/cotizaciones/orden/" + idOrden)
                    .retrieve()
                    .bodyToFlux(CotizacionDTO.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            // Log con el mensaje del error remoto y re-lanzar excepción de negocio
            log.error("Error al consultar cotizaciones para orden {}: {}", idOrden, e.getMessage());
            throw new RuntimeException("Error al consultar cotizaciones: " + e.getMessage());
        }
    }
}
