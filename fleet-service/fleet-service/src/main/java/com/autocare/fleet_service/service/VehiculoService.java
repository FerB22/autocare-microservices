package com.autocare.fleet_service.service;

import com.autocare.fleet_service.dto.ClienteDTO;
import com.autocare.fleet_service.model.Vehiculo;
import com.autocare.fleet_service.repository.VehiculoRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class VehiculoService {

    private static final int ANIO_MINIMO = 1950;

    private static final String PATRON_PATENTE_CHILENA = "^[A-Z]{2}[0-9]{4}$|^[A-Z]{4}[0-9]{2}$";

    private final VehiculoRepository vehiculoRepository;
    private final WebClient webClient;

    public VehiculoService(VehiculoRepository vehiculoRepository,
                           WebClient.Builder webClientBuilder) {
        this.vehiculoRepository = vehiculoRepository;
        this.webClient = webClientBuilder
                .baseUrl("lb://customer-service")
                .build();
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Vehiculo> listarTodos() {
        log.info("Listando todos los vehículos");
        return vehiculoRepository.findAll();
    }

    public Optional<Vehiculo> buscarPorId(String id) {
        log.info("Buscando vehículo con ID: {}", id);
        return vehiculoRepository.findById(id);
    }

    public Optional<Vehiculo> buscarPorPatente(String patente) {
        log.info("Buscando vehículo con patente: {}", patente);
        return vehiculoRepository.findByPatente(patente.toUpperCase().trim());
    }

    public ClienteDTO obtenerDuenio(String idDuenio) {
        log.info("Consultando dueño con ID: {}", idDuenio);
        return webClient.get()
                .uri("/clientes/{id}", idDuenio)
                .retrieve()
                .bodyToMono(ClienteDTO.class)
                .block();
    }

    public List<Vehiculo> buscarPorDuenio(String idDuenio) {
        log.info("Buscando vehículos del dueño: {}", idDuenio);
        return vehiculoRepository.findAll()
                .stream()
                .filter(v -> idDuenio.equals(v.getIdDuenio()))
                .toList();
    }

    // ─────────────────────────────────────────
    //  CREACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Vehiculo guardar(Vehiculo vehiculo) {
        log.info("Registrando nuevo vehículo con patente: {}", vehiculo.getPatente());

        // ── REGLA 1: La patente debe ser normalizada a mayúsculas ────────────
        String patenteNormalizada = vehiculo.getPatente().toUpperCase().trim()
                                            .replaceAll("\\s+|-", "");
        vehiculo.setPatente(patenteNormalizada);

        // ── REGLA 2: La patente debe tener formato válido chileno ─────────────
        if (!patenteNormalizada.matches(PATRON_PATENTE_CHILENA)) {
            log.warn("Patente con formato inválido: {}", patenteNormalizada);
            throw new RuntimeException(
                "La patente '" + patenteNormalizada + "' no tiene un formato válido. " +
                "Formatos aceptados: AB1234 (antiguo) o ABCD12 (nuevo/Mercosur)."
            );
        }

        // ── REGLA 3: La patente no puede estar ya registrada ─────────────────
        if (vehiculoRepository.existsByPatente(patenteNormalizada)) {
            log.warn("Patente duplicada al registrar: {}", patenteNormalizada);
            throw new RuntimeException(
                "Ya existe un vehículo registrado con la patente: '" +
                patenteNormalizada + "'."
            );
        }

        // ── REGLA 4: El año no puede ser anterior al mínimo ni futuro ─────────
        int anioActual = Year.now().getValue();
        if (vehiculo.getAnio() < ANIO_MINIMO || vehiculo.getAnio() > anioActual + 1) {
            log.warn("Año de vehículo fuera de rango: {}", vehiculo.getAnio());
            throw new RuntimeException(
                "El año del vehículo (" + vehiculo.getAnio() + ") no es válido. " +
                "Debe estar entre " + ANIO_MINIMO + " y " + (anioActual + 1) + "."
            );
        }

        // ── REGLA 5: Si tiene VIN/Chasis, debe ser único ─────────────────────
        if (vehiculo.getVinChasis() != null && !vehiculo.getVinChasis().isBlank()) {
            String vinNormalizado = vehiculo.getVinChasis().toUpperCase().trim();
            vehiculo.setVinChasis(vinNormalizado);

            // ✅ Una sola consulta SQL eficiente — reemplaza findAll() + stream()
            if (vehiculoRepository.existsByVinChasis(vinNormalizado)) {
                log.warn("VIN duplicado al registrar vehículo: {}", vinNormalizado);
                throw new RuntimeException(
                    "Ya existe un vehículo registrado con el VIN/Chasis: '" +
                    vinNormalizado + "'. Cada vehículo tiene un número de chasis único."
                );
            }
        }

        // ── REGLA 6: Si tiene dueño asignado, debe existir en customer-service ──
        if (vehiculo.getIdDuenio() != null && !vehiculo.getIdDuenio().isBlank()) {
            try {
                ClienteDTO duenio = obtenerDuenio(vehiculo.getIdDuenio());
                if (duenio == null) {
                    throw new RuntimeException(
                        "El cliente con ID '" + vehiculo.getIdDuenio() +
                        "' no existe. Registre primero al cliente en customer-service."
                    );
                }
                log.info("Dueño verificado: {} {}", duenio.getNombre(), duenio.getApellido());
            } catch (WebClientResponseException.NotFound e) {
                log.warn("Cliente no encontrado al registrar vehículo: {}", vehiculo.getIdDuenio());
                throw new RuntimeException(
                    "El cliente con ID '" + vehiculo.getIdDuenio() + "' no existe en el sistema."
                );
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error al verificar cliente en customer-service: {}", e.getMessage());
                throw new RuntimeException(
                    "No se pudo verificar el cliente: " + e.getMessage()
                );
            }
        }

        vehiculo.setMarca(capitalizar(vehiculo.getMarca()));
        vehiculo.setModelo(capitalizar(vehiculo.getModelo()));

        log.info("Vehículo {} {} {} (patente: {}) registrado correctamente",
                vehiculo.getAnio(), vehiculo.getMarca(),
                vehiculo.getModelo(), patenteNormalizada);
        return vehiculoRepository.save(vehiculo);
    }

    // ─────────────────────────────────────────
    //  ACTUALIZACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Vehiculo actualizar(String id, Vehiculo datos) {
        log.info("Actualizando vehículo con ID: {}", id);

        Vehiculo existente = vehiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Vehículo no encontrado con ID: " + id
                ));

        // ── REGLA 7: No se puede cambiar la patente por una ya registrada ─────
        if (datos.getPatente() != null && !datos.getPatente().isBlank()) {
            String nuevaPatente = datos.getPatente().toUpperCase().trim()
                                       .replaceAll("\\s+|-", "");

            if (!nuevaPatente.matches(PATRON_PATENTE_CHILENA)) {
                throw new RuntimeException(
                    "La nueva patente '" + nuevaPatente + "' no tiene formato válido. " +
                    "Formatos aceptados: AB1234 o ABCD12."
                );
            }

            if (!nuevaPatente.equals(existente.getPatente())
                    && vehiculoRepository.existsByPatente(nuevaPatente)) {
                log.warn("Patente duplicada al actualizar vehículo {}: {}", id, nuevaPatente);
                throw new RuntimeException(
                    "La patente '" + nuevaPatente + "' ya pertenece a otro vehículo registrado."
                );
            }
            existente.setPatente(nuevaPatente);
        }

        // ── REGLA 8: El año actualizado también debe ser válido ───────────────
        if (datos.getAnio() != null) {
            int anioActual = Year.now().getValue();
            if (datos.getAnio() < ANIO_MINIMO || datos.getAnio() > anioActual + 1) {
                throw new RuntimeException(
                    "El año (" + datos.getAnio() + ") no es válido. " +
                    "Debe estar entre " + ANIO_MINIMO + " y " + (anioActual + 1) + "."
                );
            }
            existente.setAnio(datos.getAnio());
        }

        if (datos.getMarca() != null && !datos.getMarca().isBlank()) {
            existente.setMarca(capitalizar(datos.getMarca()));
        }
        if (datos.getModelo() != null && !datos.getModelo().isBlank()) {
            existente.setModelo(capitalizar(datos.getModelo()));
        }
        if (datos.getVinChasis() != null && !datos.getVinChasis().isBlank()) {
            existente.setVinChasis(datos.getVinChasis().toUpperCase().trim());
        }
        if (datos.getIdDuenio() != null && !datos.getIdDuenio().isBlank()) {
            existente.setIdDuenio(datos.getIdDuenio());
        }

        log.info("Vehículo {} actualizado correctamente", id);
        return vehiculoRepository.save(existente);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando vehículo con ID: {}", id);

        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Vehículo no encontrado con ID: " + id
                ));

        vehiculoRepository.deleteById(id);
        log.info("Vehículo patente {} (ID: {}) eliminado del sistema",
                vehiculo.getPatente(), id);
    }

    // ─────────────────────────────────────────
    //  MÉTODO AUXILIAR PRIVADO
    // ─────────────────────────────────────────

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        String[] palabras = texto.trim().toLowerCase().split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                resultado.append(Character.toUpperCase(palabra.charAt(0)))
                         .append(palabra.substring(1))
                         .append(" ");
            }
        }
        return resultado.toString().trim();
    }
}