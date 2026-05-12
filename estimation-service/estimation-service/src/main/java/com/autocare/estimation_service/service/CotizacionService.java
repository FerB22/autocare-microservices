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

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Cotizacion> listarTodas() {
        log.info("Listando todas las cotizaciones");
        return cotizacionRepository.findAll();
    }

    public Optional<Cotizacion> buscarPorId(String id) {
        log.info("Buscando cotización con ID: {}", id);
        return cotizacionRepository.findById(id);
    }

    public List<Cotizacion> buscarPorOrden(String idOrden) {
        log.info("Buscando cotizaciones de la orden: {}", idOrden);
        return cotizacionRepository.findByIdOrden(idOrden);
    }

    public List<Cotizacion> buscarPorEstado(Cotizacion.EstadoCotizacion estado) {
        log.info("Buscando cotizaciones con estado: {}", estado);
        return cotizacionRepository.findByEstado(estado);
    }

    // ─────────────────────────────────────────
    //  CREACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Cotizacion crear(Cotizacion cotizacion) {
        log.info("Creando cotización para orden {} con repuesto {}",
                cotizacion.getIdOrden(), cotizacion.getIdRepuesto());

        // ── REGLA 1: La cantidad debe ser mayor a cero ────────────────────────
        // El modelo tiene @Min(1) desde el Controller. Lo verificamos aquí
        // también como segunda defensa para llamadas internas.
        if (cotizacion.getCantidad() == null || cotizacion.getCantidad() < 1) {
            log.warn("Cantidad inválida: {}", cotizacion.getCantidad());
            throw new RuntimeException(
                "La cantidad debe ser al menos 1. Valor recibido: " + cotizacion.getCantidad()
            );
        }

        // ── REGLA 2: No se puede cotizar el mismo repuesto dos veces en la misma orden ──
        // Si ya existe una cotización PENDIENTE o APROBADA para ese repuesto
        // en esa orden, no tiene sentido crear una duplicada. Probablemente
        // fue un doble clic o un error del usuario.
        List<Cotizacion> cotizacionesExistentes = cotizacionRepository
                .findByIdOrden(cotizacion.getIdOrden());

        boolean repuestoDuplicado = cotizacionesExistentes.stream()
                .anyMatch(c -> c.getIdRepuesto().equals(cotizacion.getIdRepuesto())
                            && c.getEstado() != Cotizacion.EstadoCotizacion.RECHAZADA);

        if (repuestoDuplicado) {
            log.warn("Repuesto {} ya cotizado en la orden {}",
                    cotizacion.getIdRepuesto(), cotizacion.getIdOrden());
            throw new RuntimeException(
                "El repuesto '" + cotizacion.getIdRepuesto() + "' ya tiene una cotización " +
                "activa (PENDIENTE o APROBADA) en esta orden. " +
                "Si necesita más unidades, rechace la cotización existente y cree una nueva " +
                "con la cantidad correcta."
            );
        }

        // ── Consulta al spare-parts-service para obtener precio y nombre ──────
        // Si el repuesto no existe o no tiene precio, no podemos cotizar.
        String url = "http://spare-parts-service/repuestos/" + cotizacion.getIdRepuesto();

        try {
            ResponseEntity<Map<String, Object>> respuesta = restTemplate.getForEntity(
                url, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> repuesto = respuesta.getBody();
            if (repuesto != null) {
                cotizacion.setNombreRepuesto((String) repuesto.get("nombre"));

                // El precio puede venir como Double o Integer según Jackson
                Object precio = repuesto.get("precioUnitario");
                if (precio instanceof Number number) {
                    cotizacion.setPrecioUnitario(number.doubleValue());
                }

                // ── REGLA 3: El repuesto debe tener precio válido ─────────────
                // Sin precio no podemos calcular el total. Un precio de $0 haría
                // que la cotización se apruebe sin costo, lo que es un error contable.
                if (cotizacion.getPrecioUnitario() == null || cotizacion.getPrecioUnitario() <= 0) {
                    log.warn("Repuesto {} tiene precio inválido: {}",
                            cotizacion.getIdRepuesto(), cotizacion.getPrecioUnitario());
                    throw new RuntimeException(
                        "El repuesto '" + cotizacion.getNombreRepuesto() + "' " +
                        "no tiene un precio válido registrado en el sistema. " +
                        "Actualice el precio en el spare-parts-service antes de cotizar."
                    );
                }

                // ── REGLA 4: Verificar stock disponible antes de cotizar ──────
                // No tiene sentido aprobar una cotización de un repuesto que no
                // existe físicamente. Mejor alertar al crear que al aprobar.
                Object stockObj = repuesto.get("stock");
                if (stockObj instanceof Number stockNum && stockNum.intValue() < cotizacion.getCantidad()) {
                    int stockDisponible = stockNum.intValue();
                    log.warn("Stock insuficiente para repuesto {}. Disponible: {}, solicitado: {}",
                            cotizacion.getIdRepuesto(), stockDisponible, cotizacion.getCantidad());
                    throw new RuntimeException(
                        "Stock insuficiente para '" + cotizacion.getNombreRepuesto() + "'. " +
                        "Disponible: " + stockDisponible + " unidades, " +
                        "solicitado: " + cotizacion.getCantidad() + " unidades. " +
                        "Ajuste la cantidad o espere reposición de stock."
                    );
                }
            }

        } catch (RuntimeException e) {
            // Re-lanzamos las RuntimeException propias (las reglas de arriba)
            throw e;
        } catch (Exception e) {
            log.error("Error al consultar spare-parts-service: {}", e.getMessage());
            throw new RuntimeException(
                "No se pudo obtener información del repuesto desde spare-parts-service: "
                + e.getMessage()
            );
        }

        // ── Calcula el total de la línea automáticamente ──────────────────────
        double totalLinea = cotizacion.getPrecioUnitario() * cotizacion.getCantidad();
        cotizacion.setTotalLinea(totalLinea);

        cotizacion.setEstado(Cotizacion.EstadoCotizacion.PENDIENTE);

        log.info("Cotización creada: {} x {} = ${} para orden {}",
                cotizacion.getNombreRepuesto(), cotizacion.getCantidad(),
                totalLinea, cotizacion.getIdOrden());
        return cotizacionRepository.save(cotizacion);
    }

    // ─────────────────────────────────────────
    //  CAMBIO DE ESTADO CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Cotizacion cambiarEstado(String id, Cotizacion.EstadoCotizacion nuevoEstado) {
        log.info("Cambiando estado de cotización {} a {}", id, nuevoEstado);

        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Cotización no encontrada con ID: " + id
                ));

        // ── REGLA 5: Una cotización APROBADA no puede volver a PENDIENTE ──────
        // Una vez que el cliente aprobó la cotización, el trabajo ya tiene luz
        // verde. Volver a PENDIENTE después de APROBADA crea ambigüedad sobre
        // si el trabajo debe ejecutarse o no.
        if (cotizacion.getEstado() == Cotizacion.EstadoCotizacion.APROBADA
                && nuevoEstado == Cotizacion.EstadoCotizacion.PENDIENTE) {
            log.warn("Intento de regresar cotización APROBADA a PENDIENTE: {}", id);
            throw new RuntimeException(
                "Una cotización APROBADA no puede regresar a PENDIENTE. " +
                "Si desea cancelarla, use el estado RECHAZADA."
            );
        }

        // ── REGLA 6: Una cotización RECHAZADA es estado final ─────────────────
        // Si el cliente rechazó la cotización, está cerrada. Para volver a
        // cotizar el mismo repuesto, se debe crear una nueva cotización.
        // Esto mantiene el historial de aprobaciones y rechazos limpio.
        if (cotizacion.getEstado() == Cotizacion.EstadoCotizacion.RECHAZADA) {
            log.warn("Intento de cambiar estado de cotización ya RECHAZADA: {}", id);
            throw new RuntimeException(
                "La cotización ya fue RECHAZADA y no puede cambiar de estado. " +
                "Cree una nueva cotización si desea volver a presupuestar este repuesto."
            );
        }

        // ── REGLA 7: El total debe ser positivo al aprobar ────────────────────
        // No se puede aprobar una cotización sin total calculado o con total $0.
        // Esto podría ocurrir si el precio del repuesto no se obtuvo correctamente.
        if (nuevoEstado == Cotizacion.EstadoCotizacion.APROBADA) {
            if (cotizacion.getTotalLinea() == null || cotizacion.getTotalLinea() <= 0) {
                log.warn("Intento de aprobar cotización con total inválido: {}", cotizacion.getTotalLinea());
                throw new RuntimeException(
                    "No se puede aprobar una cotización con total de $0 o sin total calculado. " +
                    "Verifique el precio unitario y la cantidad."
                );
            }
            log.info("Cotización {} APROBADA por ${}", id, cotizacion.getTotalLinea());
        }

        cotizacion.setEstado(nuevoEstado);
        log.info("Cotización {} cambió de estado a {}", id, nuevoEstado);
        return cotizacionRepository.save(cotizacion);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando cotización con ID: {}", id);

        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Cotización no encontrada con ID: " + id
                ));

        // ── REGLA 8: No se puede eliminar una cotización APROBADA ────────────
        // Una cotización aprobada es un compromiso de trabajo con el cliente.
        // Eliminarla borra el registro de ese compromiso y desconecta la cadena
        // estimation-service → billing-service que depende de ella.
        if (cotizacion.getEstado() == Cotizacion.EstadoCotizacion.APROBADA) {
            log.warn("Intento de eliminar cotización APROBADA: {}", id);
            throw new RuntimeException(
                "No se puede eliminar la cotización porque está APROBADA. " +
                "Las cotizaciones aprobadas son parte del historial contable. " +
                "Si necesita cancelarla, cambie su estado a RECHAZADA primero."
            );
        }

        cotizacionRepository.deleteById(id);
        log.info("Cotización {} eliminada correctamente", id);
    }
}