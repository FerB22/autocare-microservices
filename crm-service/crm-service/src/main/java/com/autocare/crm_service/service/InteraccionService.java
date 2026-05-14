package com.autocare.crm_service.service;

import com.autocare.crm_service.model.Interaccion;
import com.autocare.crm_service.repository.InteraccionRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Clase de servicio que encapsula la lógica de negocio para la gestión de interacciones.
 * @Service indica a Spring que esta clase es un componente de servicio y debe ser
 * gestionada por el contenedor de inversión de control (IoC).
 */
@Slf4j // Anotación de Lombok que inyecta automáticamente una instancia de Logger (log) en la clase.
@Service
public class InteraccionService {

    // Largo mínimo exigido para la descripción de una interacción.
    // "ok", "nada", "x" no aportan información útil para el seguimiento al cliente.
    private static final int DESCRIPCION_LARGO_MINIMO = 10;

    // Se recomienda usar 'final' para las dependencias inyectadas para asegurar su inmutabilidad.
    private final InteraccionRepository interaccionRepository;
    private final RestTemplate restTemplate;

    /**
     * Inyección de dependencias por constructor (Recomendado sobre @Autowired en campos).
     * Esto facilita el testing (permite instanciar la clase pasando mocks sin usar reflection)
     * y asegura que el servicio no pueda instanciarse sin sus dependencias requeridas.
     */
    public InteraccionService(InteraccionRepository interaccionRepository,
                              RestTemplate restTemplate) {
        this.interaccionRepository = interaccionRepository;
        this.restTemplate = restTemplate;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    /**
     * Recupera todas las interacciones de la base de datos.
     * @return Lista completa de interacciones.
     */
    public List<Interaccion> listarTodas() {
        log.info("Listando todas las interacciones");
        return interaccionRepository.findAll();
    }

    /**
     * Busca una interacción por su ID.
     * @param id Identificador único de la interacción.
     * @return Un Optional que contiene la interacción si existe, o vacío si no se encuentra.
     */
    public Optional<Interaccion> buscarPorId(String id) {
        log.info("Buscando interacción con ID: {}", id);
        return interaccionRepository.findById(id);
    }

    /**
     * Busca todas las interacciones asociadas a un cliente específico.
     * @param idCliente Identificador del cliente.
     * @return Lista de interacciones del cliente.
     */
    public List<Interaccion> buscarPorCliente(String idCliente) {
        log.info("Buscando interacciones del cliente: {}", idCliente);
        return interaccionRepository.findByIdCliente(idCliente);
    }

    /**
     * Busca interacciones filtrando por su tipo (Ej. RECLAMO, CONSULTA).
     * @param tipo El tipo de interacción a buscar.
     * @return Lista de interacciones que coinciden con el tipo.
     */
    public List<Interaccion> buscarPorTipo(Interaccion.TipoInteraccion tipo) {
        log.info("Buscando interacciones de tipo: {}", tipo);
        return interaccionRepository.findByTipo(tipo);
    }

    /**
     * Busca interacciones filtrando por su estado de seguimiento (ABIERTO, EN_PROCESO, CERRADO).
     * @param seguimiento Estado del seguimiento.
     * @return Lista de interacciones en el estado indicado.
     */
    public List<Interaccion> buscarPorSeguimiento(Interaccion.SeguimientoEstado seguimiento) {
        log.info("Buscando interacciones con seguimiento: {}", seguimiento);
        return interaccionRepository.findBySeguimiento(seguimiento);
    }

    // ─────────────────────────────────────────
    //  REGISTRO CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    /**
     * Registra una nueva interacción en el sistema aplicando las reglas de negocio,
     * validando datos locales y comunicándose de forma síncrona con otro microservicio.
     * 
     * @param interaccion El objeto Interaccion a guardar.
     * @return La interacción guardada con los datos autogenerados.
     */
    @SuppressWarnings("unchecked") // Suprime la advertencia del compilador sobre el casting a Map genérico
    public Interaccion registrar(Interaccion interaccion) {
        log.info("Registrando interacción de tipo {} para cliente {}",
                interaccion.getTipo(), interaccion.getIdCliente());

        // ── REGLA 1: La descripción debe tener un largo mínimo ───────────────
        // Una descripción de 1 o 2 caracteres ("ok", "-") no permite hacer
        // seguimiento real al cliente. Exigimos al menos 10 caracteres para
        // garantizar que se registra información útil.
        // Nota técnica: Se valida nulidad primero para evitar NullPointerException.
        if (interaccion.getDescripcion() == null
                || interaccion.getDescripcion().trim().length() < DESCRIPCION_LARGO_MINIMO) {
            log.warn("Descripción demasiado corta: '{}'", interaccion.getDescripcion());
            throw new RuntimeException(
                "La descripción es demasiado corta. " +
                "Debe tener al menos " + DESCRIPCION_LARGO_MINIMO + " caracteres " +
                "para documentar correctamente la interacción con el cliente."
            );
        }

        // ── REGLA 2: No se puede registrar más de 1 RECLAMO abierto por cliente ──
        // Si el cliente ya tiene un reclamo en estado ABIERTO o EN_PROCESO,
        // abrir otro genera confusión sobre cuál está siendo atendido.
        // El agente debe cerrar el reclamo anterior antes de abrir uno nuevo.
        if (interaccion.getTipo() == Interaccion.TipoInteraccion.RECLAMO) {
            // Uso de la API Stream de Java 8 para evaluar de forma declarativa si existe 
            // alguna interacción que incumpla la regla de negocio.
            boolean tieneReclamoActivo = interaccionRepository
                    .findByIdCliente(interaccion.getIdCliente())
                    .stream()
                    .anyMatch(i -> i.getTipo() == Interaccion.TipoInteraccion.RECLAMO
                               && i.getSeguimiento() != Interaccion.SeguimientoEstado.CERRADO);

            if (tieneReclamoActivo) {
                log.warn("Cliente {} ya tiene un reclamo activo", interaccion.getIdCliente());
                throw new RuntimeException(
                    "El cliente ya tiene un RECLAMO activo (ABIERTO o EN_PROCESO). " +
                    "Cierre el reclamo existente antes de registrar uno nuevo."
                );
            }
        }

        // ── Consulta el nombre del cliente al customer-service ────────────────
        // Si el cliente no existe, bloqueamos el registro — no tiene sentido
        // crear una interacción para un cliente que no está en el sistema.
        // Comunicación sincrónica entre microservicios (Asumiendo que usa un Service Discovery como Eureka o URL directa).
        String url = "http://customer-service/clientes/" + interaccion.getIdCliente();

        try {
            // Se realiza un GET request y se mapea la respuesta JSON a un Map de Java.
            ResponseEntity<Map<String, Object>> respuesta = restTemplate.getForEntity(
                url, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> cliente = respuesta.getBody();

            // ── REGLA 3: El cliente debe existir en customer-service ──────────
            // A diferencia del código original que guardaba "Cliente no encontrado",
            // aquí bloqueamos el registro. Una interacción sin cliente real
            // no tiene valor para el CRM.
            if (cliente == null) {
                log.warn("Cliente {} no encontrado en customer-service",
                        interaccion.getIdCliente());
                throw new RuntimeException(
                    "El cliente con ID '" + interaccion.getIdCliente() +
                    "' no existe en el sistema. " +
                    "Registre primero al cliente en customer-service."
                );
            }

            // Extracción segura de los campos del Map devuelto por el servicio externo.
            String nombre   = (String) cliente.get("nombre");
            String apellido = (String) cliente.get("apellido");
            interaccion.setNombreCliente(nombre + " " + apellido);
            log.info("Cliente verificado: {}", interaccion.getNombreCliente());

        } catch (RuntimeException e) {
            // Re-lanzamos las excepciones propias del negocio sin envolverlas
            // para que los ExceptionHandlers globales puedan capturarlas limpiamente.
            throw e;
        } catch (Exception e) {
            log.error("Error al consultar customer-service: {}", e.getMessage());
            throw new RuntimeException(
                "No se pudo verificar el cliente en customer-service: " + e.getMessage()
            );
        }

        // La fecha y el estado inicial los asigna el sistema, nunca el usuario
        // Garantiza que los metadatos de auditoría son inmutables desde el cliente.
        interaccion.setFechaInteraccion(LocalDateTime.now());
        interaccion.setSeguimiento(Interaccion.SeguimientoEstado.ABIERTO);
        interaccion.setDescripcion(interaccion.getDescripcion().trim());

        log.info("Interacción {} registrada para cliente '{}' el {}",
                interaccion.getTipo(),
                interaccion.getNombreCliente(),
                interaccion.getFechaInteraccion());
        
        // El repositorio guarda la entidad e inyecta el ID generado si es base de datos (SQL/NoSQL)
        return interaccionRepository.save(interaccion);
    }

    // ─────────────────────────────────────────
    //  CAMBIO DE SEGUIMIENTO CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    /**
     * Actualiza el estado de una interacción validando un flujo lógico de estados.
     * @param id ID de la interacción a modificar.
     * @param nuevoEstado El estado al que se desea transicionar.
     * @return La interacción actualizada.
     */
    public Interaccion cambiarSeguimiento(String id,
                                          Interaccion.SeguimientoEstado nuevoEstado) {
        log.info("Cambiando seguimiento de interacción {} a {}", id, nuevoEstado);

        // orElseThrow extrae el objeto del Optional o lanza la excepción,
        // eliminando la necesidad de bloques if (isPresent()).
        Interaccion interaccion = interaccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Interacción no encontrada con ID: " + id
                ));

        // ── REGLA 4: Una interacción CERRADA no puede cambiar de estado ───────
        // CERRADO significa que el asunto fue resuelto y documentado.
        // Reabrirlo automáticamente borraría el historial de resolución.
        // Si hay un problema nuevo, se debe crear una nueva interacción.
        if (interaccion.getSeguimiento() == Interaccion.SeguimientoEstado.CERRADO) {
            log.warn("Intento de modificar interacción ya CERRADA: {}", id);
            throw new RuntimeException(
                "Esta interacción ya fue CERRADA y no puede cambiar de estado. " +
                "Si el cliente tiene un nuevo requerimiento, registre una nueva interacción."
            );
        }

        // ── REGLA 5: El seguimiento solo puede avanzar, nunca retroceder ──────
        // El flujo es: ABIERTO → EN_PROCESO → CERRADO.
        // No tiene sentido volver de EN_PROCESO a ABIERTO — eso indicaría
        // que el agente "olvidó" que ya empezó a trabajar en el caso.
        // Se usa List.of (desde Java 9) para crear una lista inmutable que define el orden.
        List<Interaccion.SeguimientoEstado> flujo = List.of(
            Interaccion.SeguimientoEstado.ABIERTO,
            Interaccion.SeguimientoEstado.EN_PROCESO,
            Interaccion.SeguimientoEstado.CERRADO
        );

        // indexOf compara las posiciones para garantizar que el estado progrese cronológicamente.
        int indiceActual = flujo.indexOf(interaccion.getSeguimiento());
        int indiceNuevo  = flujo.indexOf(nuevoEstado);

        if (indiceNuevo < indiceActual) {
            log.warn("Intento de retroceder seguimiento: {} → {} en interacción {}",
                    interaccion.getSeguimiento(), nuevoEstado, id);
            throw new RuntimeException(
                "El seguimiento no puede retroceder. " +
                "Estado actual: " + interaccion.getSeguimiento() +
                " → Intentado: " + nuevoEstado + ". " +
                "El flujo válido es: ABIERTO → EN_PROCESO → CERRADO."
            );
        }

        // ── REGLA 6: No se puede pasar de ABIERTO directo a CERRADO ──────────
        // Cerrar sin haber pasado por EN_PROCESO indica que el caso no fue
        // atendido realmente. Todos los reclamos y consultas deben tener
        // al menos un registro de que fueron procesados.
        if (interaccion.getSeguimiento() == Interaccion.SeguimientoEstado.ABIERTO
                && nuevoEstado == Interaccion.SeguimientoEstado.CERRADO) {
            log.warn("Intento de cerrar interacción sin procesar: {}", id);
            throw new RuntimeException(
                "No se puede cerrar una interacción que nunca fue procesada. " +
                "Primero cambie el estado a EN_PROCESO, luego a CERRADO."
            );
        }

        interaccion.setSeguimiento(nuevoEstado);
        log.info("Interacción {} cambió de {} a {}",
                id, flujo.get(indiceActual), nuevoEstado);
        return interaccionRepository.save(interaccion);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    /**
     * Elimina una interacción validando que se cumplan las restricciones de negocio.
     * @param id ID de la interacción a eliminar.
     */
    public void eliminar(String id) {
        log.info("Eliminando interacción con ID: {}", id);

        Interaccion interaccion = interaccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Interacción no encontrada con ID: " + id
                ));

        // ── REGLA 7: No se puede eliminar una interacción que no está CERRADA ─
        // Las interacciones ABIERTAS o EN_PROCESO son casos activos.
        // Eliminar un caso activo haría desaparecer un problema real del cliente
        // sin que haya sido resuelto. Solo los casos cerrados pueden borrarse
        // si se requiere limpiar historial.
        if (interaccion.getSeguimiento() != Interaccion.SeguimientoEstado.CERRADO) {
            log.warn("Intento de eliminar interacción activa ({}) con ID: {}",
                    interaccion.getSeguimiento(), id);
            throw new RuntimeException(
                "No se puede eliminar una interacción que aún está " +
                interaccion.getSeguimiento() + ". " +
                "Solo se pueden eliminar interacciones con estado CERRADO."
            );
        }

        // Ejecuta el borrado lógico o físico dependiendo de cómo esté configurado el Repositorio JPA/Mongo.
        interaccionRepository.deleteById(id);
        log.info("Interacción {} del cliente '{}' eliminada correctamente",
                id, interaccion.getNombreCliente());
    }
}