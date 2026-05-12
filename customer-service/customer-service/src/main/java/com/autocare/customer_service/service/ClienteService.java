package com.autocare.customer_service.service;

import com.autocare.customer_service.model.Cliente;
import com.autocare.customer_service.repository.ClienteRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ClienteService {

    // Largo mínimo de un número de teléfono válido en Chile: 9 dígitos.
    // Rechazamos valores como "123" que claramente son errores de digitación.
    private static final int TELEFONO_LONGITUD_MINIMA = 8;
    private static final int TELEFONO_LONGITUD_MAXIMA = 15;

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Cliente> listarTodos() {
        log.info("Listando todos los clientes");
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscarPorId(String id) {
        log.info("Buscando cliente con ID: {}", id);
        return clienteRepository.findById(id);
    }

    public Optional<Cliente> buscarPorEmail(String email) {
        log.info("Buscando cliente con email: {}", email);
        return clienteRepository.findByEmail(email.toLowerCase().trim());
    }

    // ─────────────────────────────────────────
    //  CREACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Cliente guardar(Cliente cliente) {
        log.info("Registrando nuevo cliente: {} {}",
                cliente.getNombre(), cliente.getApellido());

        // ── REGLA 1: El email debe ser único (normalizado a minúsculas) ───────
        // "Juan@Gmail.COM" y "juan@gmail.com" son el mismo email.
        // Normalizar antes de comparar evita duplicados invisibles.
        String emailNormalizado = cliente.getEmail().toLowerCase().trim();
        cliente.setEmail(emailNormalizado);

        if (clienteRepository.existsByEmail(emailNormalizado)) {
            log.warn("Email duplicado al registrar cliente: {}", emailNormalizado);
            throw new RuntimeException(
                "Ya existe un cliente registrado con el email: '" + emailNormalizado + "'. " +
                "Si olvidó sus datos, use la opción de recuperación."
            );
        }

        // ── REGLA 2: El teléfono solo puede contener dígitos y el signo '+' ──
        // Evita valores como "fono: 912345678" o "no tengo" que romperían
        // los intentos de contacto y el crm-service de notificaciones.
        String telefonoLimpio = cliente.getTelefono().trim().replaceAll("\\s+", "");
        if (!telefonoLimpio.matches("^[+]?[0-9]{" + TELEFONO_LONGITUD_MINIMA
                + "," + TELEFONO_LONGITUD_MAXIMA + "}$")) {
            log.warn("Teléfono inválido al registrar cliente: {}", cliente.getTelefono());
            throw new RuntimeException(
                "El teléfono '" + cliente.getTelefono() + "' no es válido. " +
                "Debe contener entre " + TELEFONO_LONGITUD_MINIMA +
                " y " + TELEFONO_LONGITUD_MAXIMA + " dígitos, " +
                "opcionalmente comenzando con '+'."
            );
        }
        cliente.setTelefono(telefonoLimpio);

        // ── REGLA 3: Nombre y apellido no pueden ser solo números ────────────
        // Evita registros como nombre="12345" que son claramente errores.
        // El @NotBlank del modelo solo verifica que no esté vacío.
        if (cliente.getNombre().trim().matches("^[0-9]+$")) {
            log.warn("Nombre inválido (solo números): {}", cliente.getNombre());
            throw new RuntimeException(
                "El nombre '" + cliente.getNombre() + "' no es válido. " +
                "No puede contener solo números."
            );
        }
        if (cliente.getApellido().trim().matches("^[0-9]+$")) {
            log.warn("Apellido inválido (solo números): {}", cliente.getApellido());
            throw new RuntimeException(
                "El apellido '" + cliente.getApellido() + "' no es válido. " +
                "No puede contener solo números."
            );
        }

        // Normalizar nombre y apellido (trim + capitalizar primera letra)
        cliente.setNombre(capitalizar(cliente.getNombre()));
        cliente.setApellido(capitalizar(cliente.getApellido()));

        log.info("Cliente '{}  {}' registrado con email: {}",
                cliente.getNombre(), cliente.getApellido(), emailNormalizado);
        return clienteRepository.save(cliente);
    }

    // ─────────────────────────────────────────
    //  ACTUALIZACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Cliente actualizar(String id, Cliente datos) {
        log.info("Actualizando cliente con ID: {}", id);

        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Cliente no encontrado con ID: " + id
                ));

        // ── REGLA 4: No se puede cambiar el email a uno ya registrado ────────
        // Verificamos solo si el nuevo email es distinto al actual para
        // no bloquear al cliente que actualiza otros datos sin cambiar email.
        if (datos.getEmail() != null && !datos.getEmail().isBlank()) {
            String nuevoEmail = datos.getEmail().toLowerCase().trim();

            if (!nuevoEmail.equals(existente.getEmail())
                    && clienteRepository.existsByEmail(nuevoEmail)) {
                log.warn("Email duplicado al actualizar cliente {}: {}", id, nuevoEmail);
                throw new RuntimeException(
                    "El email '" + nuevoEmail + "' ya está registrado por otro cliente."
                );
            }
            existente.setEmail(nuevoEmail);
        }

        // ── REGLA 5: El teléfono actualizado también debe ser válido ─────────
        if (datos.getTelefono() != null && !datos.getTelefono().isBlank()) {
            String telefonoLimpio = datos.getTelefono().trim().replaceAll("\\s+", "");
            if (!telefonoLimpio.matches("^[+]?[0-9]{" + TELEFONO_LONGITUD_MINIMA
                    + "," + TELEFONO_LONGITUD_MAXIMA + "}$")) {
                log.warn("Teléfono inválido al actualizar cliente {}: {}",
                        id, datos.getTelefono());
                throw new RuntimeException(
                    "El teléfono '" + datos.getTelefono() + "' no es válido. " +
                    "Debe contener entre " + TELEFONO_LONGITUD_MINIMA +
                    " y " + TELEFONO_LONGITUD_MAXIMA + " dígitos."
                );
            }
            existente.setTelefono(telefonoLimpio);
        }

        // Actualizar nombre y apellido con normalización
        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            existente.setNombre(capitalizar(datos.getNombre()));
        }
        if (datos.getApellido() != null && !datos.getApellido().isBlank()) {
            existente.setApellido(capitalizar(datos.getApellido()));
        }
        if (datos.getDireccion() != null) {
            existente.setDireccion(datos.getDireccion().trim());
        }

        log.info("Cliente {} actualizado correctamente", id);
        return clienteRepository.save(existente);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando cliente con ID: {}", id);

        // ── REGLA 6: Verificar que el cliente existe antes de eliminar ────────
        // En lugar de existsById + deleteById (2 consultas),
        // hacemos findById (1 consulta) y reutilizamos el objeto para el log.
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Cliente no encontrado con ID: " + id
                ));

        clienteRepository.deleteById(id);
        log.info("Cliente '{}  {}' (ID: {}) eliminado del sistema",
                cliente.getNombre(), cliente.getApellido(), id);
    }

    // ─────────────────────────────────────────
    //  MÉTODO AUXILIAR PRIVADO
    // ─────────────────────────────────────────

    // Capitaliza la primera letra de cada palabra: "juan pablo" → "Juan Pablo"
    // Esto mantiene consistencia visual en los reportes y notificaciones del crm-service.
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