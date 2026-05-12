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

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);
    private static final double IVA = 0.19;

    private final FacturaRepository facturaRepository;
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

    // ─────────────────────────────────────────
    //  GENERACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Factura generar(Factura factura) {
        log.info("Generando factura para orden: {}", factura.getIdOrden());

        // ── REGLA 1: No puede existir ya una factura para la misma orden ─────
        // Cada orden de trabajo solo puede tener UNA factura. Si ya existe, es
        // un error: no puedes cobrar dos veces el mismo trabajo.
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

        if (cotizaciones == null || cotizaciones.isEmpty()) {
            log.error("No hay cotizaciones para la orden: {}", factura.getIdOrden());
            throw new RuntimeException(
                "No hay cotizaciones para la orden: " + factura.getIdOrden()
            );
        }

        // ── REGLA 2: Solo se suman cotizaciones APROBADAS ────────────────────
        // Si el cliente no aprobó el presupuesto, no se le cobra ese ítem.
        double subtotal = cotizaciones.stream()
                .filter(c -> "APROBADA".equals(c.getEstado()))
                .mapToDouble(c -> {
                    Double total = c.getTotalLinea();
                    return total != null ? total : 0.0;
                })
                .sum();

        // ── REGLA 3: No generar facturas con monto $0 ─────────────────────────
        // Una factura de cero no tiene sentido comercial ni legal.
        if (subtotal == 0) {
            log.error("No hay cotizaciones APROBADAS para la orden: {}", factura.getIdOrden());
            throw new RuntimeException(
                "No se puede generar una factura con monto $0. " +
                "La orden " + factura.getIdOrden() + " no tiene cotizaciones APROBADAS."
            );
        }

        double impuesto = Math.round(subtotal * IVA * 100.0) / 100.0;
        double total    = Math.round((subtotal + impuesto) * 100.0) / 100.0;

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

        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Factura no encontrada con ID: " + id
                ));

        // ── REGLA 4: No se puede pagar una factura ya pagada ─────────────────
        // Evita cobros duplicados, que serían un error grave en cualquier sistema
        // de facturación real.
        if (factura.getEstado() == Factura.EstadoFactura.PAGADA) {
            log.warn("Intento de pagar una factura ya pagada: {}", id);
            throw new RuntimeException(
                "La factura ya fue PAGADA. No se puede procesar el mismo pago dos veces."
            );
        }

        // ── REGLA 5: No se puede pagar una factura anulada ───────────────────
        // Una factura anulada está fuera del sistema. Si quieren cobrar, deben
        // generar una nueva factura para esa orden.
        if (factura.getEstado() == Factura.EstadoFactura.ANULADA) {
            log.warn("Intento de pagar una factura anulada: {}", id);
            throw new RuntimeException(
                "La factura está ANULADA y no puede ser pagada. " +
                "Genere una nueva factura si corresponde."
            );
        }

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

        // ── REGLA 6: No se puede anular una factura ya pagada ────────────────
        // Si ya se cobró, no se puede anular unilateralmente. En la vida real
        // habría que emitir una nota de crédito, pero en este sistema lo bloqueamos.
        if (factura.getEstado() == Factura.EstadoFactura.PAGADA) {
            log.warn("Intento de anular una factura ya pagada: {}", id);
            throw new RuntimeException(
                "No se puede anular una factura que ya fue PAGADA."
            );
        }

        // ── REGLA 7: No tiene sentido anular una factura ya anulada ──────────
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

        // ── REGLA 8: Una factura PAGADA o ANULADA es estado final ────────────
        // Ningún estado puede sobreescribir estos dos. Son el cierre de la factura.
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

        // ── REGLA 9: No se puede eliminar una factura pagada ─────────────────
        // Las facturas pagadas son documentos contables. Eliminarlas sería
        // una pérdida de información financiera importante.
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
}