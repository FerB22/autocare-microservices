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

    // Niveles de combustible válidos (lo que puede marcar el tablero del auto).
    // Aceptar "full" o "casi lleno" haría inconsistentes los registros entre
    // recepciones y complicaría los informes de entrega.
    private static final List<String> NIVELES_COMBUSTIBLE_VALIDOS = List.of(
        "VACIO", "RESERVA", "CUARTO", "MEDIO", "TRES_CUARTOS", "LLENO"
    );

    private final RecepcionRepository recepcionRepository;
    private final RestTemplate restTemplate;

    public RecepcionService(RecepcionRepository recepcionRepository,
                            RestTemplate restTemplate) {
        this.recepcionRepository = recepcionRepository;
        this.restTemplate = restTemplate;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Recepcion> listarTodas() {
        log.info("Listando todas las recepciones");
        return recepcionRepository.findAll();
    }

    public Optional<Recepcion> buscarPorId(String id) {
        log.info("Buscando recepción con ID: {}", id);
        return recepcionRepository.findById(id);
    }

    public List<Recepcion> buscarPorVehiculo(String idVehiculo) {
        log.info("Buscando recepciones del vehículo: {}", idVehiculo);
        return recepcionRepository.findByIdVehiculo(idVehiculo);
    }

    // ─────────────────────────────────────────
    //  REGISTRO CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Recepcion registrar(Recepcion recepcion) {
        log.info("Registrando recepción para vehículo: {}", recepcion.getIdVehiculo());

        // ── REGLA 1: Un vehículo no puede entrar si ya tiene una recepción activa ──
        // "Activa" significa que tiene una orden de trabajo creada y aún no
        // entregada. Si el auto ya está en el taller, no puede hacer check-in
        // de nuevo — es físicamente imposible.
        List<Recepcion> historial = recepcionRepository
                .findByIdVehiculo(recepcion.getIdVehiculo());

        boolean tieneRecepcionActiva = historial.stream()
                .anyMatch(r -> r.getIdOrdenCreada() != null
                            && !r.getIdOrdenCreada().startsWith("PENDIENTE"));

        if (tieneRecepcionActiva) {
            log.warn("Vehículo {} ya tiene una recepción activa", recepcion.getIdVehiculo());
            throw new RuntimeException(
                "El vehículo ya se encuentra dentro del taller con una orden activa. " +
                "No puede registrarse una nueva recepción hasta que el vehículo sea entregado."
            );
        }

        // ── REGLA 2: El kilometraje no puede ser negativo ─────────────────────
        // El modelo tiene @Min(0) que actúa desde el Controller.
        // Lo validamos también aquí como segunda línea de defensa para
        // llamadas internas (tests, seeds de datos iniciales, etc.).
        if (recepcion.getKilometrajeEntrada() == null || recepcion.getKilometrajeEntrada() < 0) {
            log.warn("Kilometraje inválido recibido: {}", recepcion.getKilometrajeEntrada());
            throw new RuntimeException(
                "El kilometraje de entrada no puede ser negativo ni nulo. " +
                "Valor recibido: " + recepcion.getKilometrajeEntrada()
            );
        }

        // ── REGLA 3: El nivel de combustible debe ser un valor del catálogo ───
        // Normalizar antes de validar para aceptar "lleno" y "LLENO" igual.
        String nivelNormalizado = recepcion.getNivelCombustible().toUpperCase().trim()
                                           .replace(" ", "_"); // "tres cuartos" → "TRES_CUARTOS"

        if (!NIVELES_COMBUSTIBLE_VALIDOS.contains(nivelNormalizado)) {
            log.warn("Nivel de combustible inválido: {}", recepcion.getNivelCombustible());
            throw new RuntimeException(
                "Nivel de combustible inválido: '" + recepcion.getNivelCombustible() + "'. " +
                "Los valores permitidos son: " + NIVELES_COMBUSTIBLE_VALIDOS
            );
        }
        recepcion.setNivelCombustible(nivelNormalizado);

        // ── REGLA 4: El kilometraje debe ser razonable (no mayor a 1.000.000 km) ──
        // Un auto con más de un millón de kilómetros es prácticamente imposible
        // y casi siempre indica un error de digitación (ej: 9999999).
        if (recepcion.getKilometrajeEntrada() > 1_000_000) {
            log.warn("Kilometraje sospechosamente alto: {}", recepcion.getKilometrajeEntrada());
            throw new RuntimeException(
                "El kilometraje ingresado (" + recepcion.getKilometrajeEntrada() + " km) " +
                "parece incorrecto. El máximo aceptable es 1.000.000 km. " +
                "Verifique el valor e intente nuevamente."
            );
        }

        // ── REGLA 5: El historial de kilometraje debe ser consistente ─────────
        // Si el vehículo ya estuvo antes en el taller, su nuevo kilometraje
        // DEBE ser mayor al de la última visita. Un auto no puede "retroceder"
        // en kilómetros — eso indicaría adulteración del odómetro o error.
        if (!historial.isEmpty()) {
            int kmMaximoHistorico = historial.stream()
                    .mapToInt(Recepcion::getKilometrajeEntrada)
                    .max()
                    .orElse(0);

            if (recepcion.getKilometrajeEntrada() < kmMaximoHistorico) {
                log.warn("Kilometraje {} menor al histórico {} para vehículo {}",
                        recepcion.getKilometrajeEntrada(),
                        kmMaximoHistorico,
                        recepcion.getIdVehiculo());
                throw new RuntimeException(
                    "El kilometraje ingresado (" + recepcion.getKilometrajeEntrada() + " km) " +
                    "es menor al registrado en la última visita (" + kmMaximoHistorico + " km). " +
                    "Verifique el odómetro del vehículo."
                );
            }
        }

        // ── Comunicación con workflow-service para crear la orden ──────────────
        // Esto no es una regla de negocio, sino integración entre servicios.
        // Si falla, guardamos igual con estado PENDIENTE para no perder la recepción.
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

            Map<String, Object> cuerpo = respuesta.getBody();
            if (respuesta.getStatusCode().is2xxSuccessful() && cuerpo != null) {
                String idOrden = (String) cuerpo.get("idOrden");
                recepcion.setIdOrdenCreada(idOrden);
                log.info("Orden {} creada automáticamente para vehículo {}",
                        idOrden, recepcion.getIdVehiculo());
            }

        } catch (Exception e) {
            log.error("No se pudo crear la orden en workflow-service: {}", e.getMessage());
            recepcion.setIdOrdenCreada("PENDIENTE - " + e.getMessage());
        }

        log.info("Recepción registrada para vehículo {} con kilometraje {} km",
                recepcion.getIdVehiculo(), recepcion.getKilometrajeEntrada());
        return recepcionRepository.save(recepcion);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando recepción con ID: {}", id);

        Recepcion recepcion = recepcionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Recepción no encontrada con ID: " + id
                ));

        // ── REGLA 6: No se puede eliminar una recepción con orden activa ──────
        // La recepción es el documento legal de entrada del vehículo.
        // Si tiene una orden de trabajo vinculada y activa, eliminar la
        // recepción dejaría esa orden huérfana (sin documento de respaldo).
        if (recepcion.getIdOrdenCreada() != null
                && !recepcion.getIdOrdenCreada().startsWith("PENDIENTE")) {
            log.warn("Intento de eliminar recepción con orden activa: {}", id);
            throw new RuntimeException(
                "No se puede eliminar esta recepción porque tiene una orden de trabajo " +
                "activa vinculada (ID Orden: " + recepcion.getIdOrdenCreada() + "). " +
                "Primero complete o cierre la orden en el workflow-service."
            );
        }

        recepcionRepository.deleteById(id);
        log.info("Recepción {} eliminada correctamente", id);
    }
}