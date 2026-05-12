package com.autocare.spare_parts_service.service;

import com.autocare.spare_parts_service.model.Repuesto;
import com.autocare.spare_parts_service.repository.RepuestoRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RepuestoService {

    private final RepuestoRepository repuestoRepository;

    public RepuestoService(RepuestoRepository repuestoRepository) {
        this.repuestoRepository = repuestoRepository;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Repuesto> listarTodos() {
        log.info("Listando todos los repuestos");
        return repuestoRepository.findAll();
    }

    public Optional<Repuesto> buscarPorId(String id) {
        log.info("Buscando repuesto con ID: {}", id);
        return repuestoRepository.findById(id);
    }

    public Optional<Repuesto> buscarPorCodigo(String codigo) {
        log.info("Buscando repuesto con código: {}", codigo);
        return repuestoRepository.findByCodigoParte(codigo);
    }

    public List<Repuesto> listarConStock() {
        log.info("Listando repuestos con stock disponible");
        return repuestoRepository.findByStockGreaterThan(0);
    }

    // ─────────────────────────────────────────
    //  CREACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Repuesto guardar(Repuesto repuesto) {
        log.info("Guardando nuevo repuesto con código: {}", repuesto.getCodigoParte());

        // ── REGLA 1: No puede existir dos repuestos con el mismo código ───────
        // El codigoParte es el identificador real del mundo físico (ej: "FIL-4521").
        // Duplicarlo causaría confusión en bodega y errores de cotización.
        if (repuestoRepository.existsByCodigoParte(repuesto.getCodigoParte())) {
            log.warn("Código de parte duplicado: {}", repuesto.getCodigoParte());
            throw new RuntimeException(
                "Ya existe un repuesto con el código: " + repuesto.getCodigoParte() +
                ". Use el método de actualización si quiere modificarlo."
            );
        }

        // ── REGLA 2: El stock inicial no puede ser negativo ───────────────────
        // Aunque el modelo tiene @Min(0), verificamos aquí también para dar
        // un mensaje de error más claro antes de llegar a la BD.
        if (repuesto.getStock() < 0) {
            log.warn("Intento de crear repuesto con stock negativo: {}", repuesto.getStock());
            throw new RuntimeException(
                "El stock inicial no puede ser negativo. Valor recibido: " + repuesto.getStock()
            );
        }

        // ── REGLA 3: El precio unitario no puede ser cero ni negativo ─────────
        // Un repuesto sin precio no puede usarse en cotizaciones del estimation-service.
        if (repuesto.getPrecioUnitario() <= 0) {
            log.warn("Intento de crear repuesto con precio inválido: {}", repuesto.getPrecioUnitario());
            throw new RuntimeException(
                "El precio unitario debe ser mayor a $0. " +
                "Valor recibido: " + repuesto.getPrecioUnitario()
            );
        }

        log.info("Repuesto '{}' guardado con stock inicial: {}", 
                repuesto.getNombre(), repuesto.getStock());
        return repuestoRepository.save(repuesto);
    }

    // ─────────────────────────────────────────
    //  RESERVA CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Repuesto reservar(String id, int cantidad) {
        log.info("Reservando {} unidades del repuesto ID: {}", cantidad, id);

        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Repuesto no encontrado con ID: " + id
                ));

        // ── REGLA 4: La cantidad a reservar debe ser mayor a cero ────────────
        // Reservar 0 o un número negativo no tiene sentido lógico.
        if (cantidad <= 0) {
            log.warn("Cantidad inválida para reserva: {}", cantidad);
            throw new RuntimeException(
                "La cantidad a reservar debe ser mayor a 0. Valor recibido: " + cantidad
            );
        }

        // ── REGLA 5: No se puede reservar si no hay stock suficiente ──────────
        // Esta es la regla principal del servicio. Si reservamos más de lo que
        // hay, el mecánico llega a buscar la pieza y no existe físicamente.
        if (repuesto.getStock() < cantidad) {
            log.warn("Stock insuficiente para repuesto {}. Disponible: {}, solicitado: {}",
                    id, repuesto.getStock(), cantidad);
            throw new RuntimeException(
                "Stock insuficiente para '" + repuesto.getNombre() + "'. " +
                "Disponible: " + repuesto.getStock() + " unidades, " +
                "solicitado: " + cantidad + " unidades."
            );
        }

        repuesto.setStock(repuesto.getStock() - cantidad);
        log.info("Reserva exitosa. Stock restante de '{}': {}", 
                repuesto.getNombre(), repuesto.getStock());
        return repuestoRepository.save(repuesto);
    }

    // ─────────────────────────────────────────
    //  REPOSICIÓN DE STOCK (método nuevo)
    // ─────────────────────────────────────────

    public Repuesto reponerStock(String id, int cantidad) {
        log.info("Reponiendo {} unidades al repuesto ID: {}", cantidad, id);

        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Repuesto no encontrado con ID: " + id
                ));

        // ── REGLA 6: La cantidad a reponer debe ser positiva ─────────────────
        // Solo tiene sentido agregar stock, no restarlo por este método.
        if (cantidad <= 0) {
            log.warn("Cantidad inválida para reposición: {}", cantidad);
            throw new RuntimeException(
                "La cantidad a reponer debe ser mayor a 0. Valor recibido: " + cantidad
            );
        }

        repuesto.setStock(repuesto.getStock() + cantidad);
        log.info("Stock de '{}' repuesto a: {} unidades", 
                repuesto.getNombre(), repuesto.getStock());
        return repuestoRepository.save(repuesto);
    }

    // ─────────────────────────────────────────
    //  ACTUALIZACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Repuesto actualizar(String id, Repuesto datos) {
        log.info("Actualizando repuesto con ID: {}", id);

        Repuesto existente = repuestoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Repuesto no encontrado con ID: " + id
                ));

        // ── REGLA 7: No se permite actualizar el stock a negativo ─────────────
        // La actualización general no debería ser el camino para manejar stock.
        // Para eso están reservar() y reponerStock(). Pero si alguien intenta
        // poner un valor negativo directo, lo bloqueamos.
        if (datos.getStock() < 0) {
            log.warn("Intento de actualizar stock a valor negativo: {}", datos.getStock());
            throw new RuntimeException(
                "El stock no puede ser negativo. " +
                "Use reponerStock() para agregar o reservar() para descontar."
            );
        }

        // ── REGLA 8: No se permite actualizar el precio a cero o negativo ─────
        if (datos.getPrecioUnitario() <= 0) {
            log.warn("Intento de actualizar precio a valor inválido: {}", datos.getPrecioUnitario());
            throw new RuntimeException(
                "El precio unitario debe ser mayor a $0. " +
                "Valor recibido: " + datos.getPrecioUnitario()
            );
        }

        existente.setNombre(datos.getNombre());
        existente.setStock(datos.getStock());
        existente.setPrecioUnitario(datos.getPrecioUnitario());
        existente.setUbicacionBodega(datos.getUbicacionBodega());

        log.info("Repuesto '{}' actualizado exitosamente", existente.getNombre());
        return repuestoRepository.save(existente);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando repuesto con ID: {}", id);

        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Repuesto no encontrado con ID: " + id
                ));

        // ── REGLA 9: No se puede eliminar un repuesto con stock activo ────────
        // Si hay unidades en bodega, eliminar el registro causaría un descuadre
        // de inventario. Primero hay que agotar o reubicar el stock.
        if (repuesto.getStock() > 0) {
            log.warn("Intento de eliminar repuesto con stock activo: {} unidades", 
                    repuesto.getStock());
            throw new RuntimeException(
                "No se puede eliminar '" + repuesto.getNombre() + "' porque tiene " +
                repuesto.getStock() + " unidades en stock. " +
                "Primero agote o reubique el inventario."
            );
        }

        repuestoRepository.deleteById(id);
        log.info("Repuesto '{}' eliminado del sistema", repuesto.getNombre());
    }
}