package com.autocare.customer_service.service;

import com.autocare.customer_service.model.Cliente;
import com.autocare.customer_service.repository.ClienteRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Capa de Servicio (Service Layer).
 * Aquí reside el "corazón" de la aplicación: la lógica de negocio.
 * Actúa como intermediario entre el Controlador (que recibe la petición HTTP)
 * y el Repositorio (que habla con la base de datos).
 */
@Slf4j // Anotación de Lombok para habilitar el log.info(), log.warn(), etc., sin instanciar la clase Logger manualmente.
@Service // Registra esta clase como un Bean o componente dentro del contexto de Spring Boot.
public class ClienteService {

    // Largo mínimo de un número de teléfono válido en Chile: 9 dígitos.
    // Rechazamos valores como "123" que claramente son errores de digitación.
    // CONSTANTES: Se declaran como 'static final' para que pertenezcan a la clase 
    // y no a las instancias, ahorrando memoria y asegurando que su valor es inmutable.
    private static final int TELEFONO_LONGITUD_MINIMA = 8;
    private static final int TELEFONO_LONGITUD_MAXIMA = 15;

    // Dependencia inyectada. Se marca como 'final' para garantizar que el servicio 
    // no pueda existir ni mutar sin su capa de acceso a datos.
    private final ClienteRepository clienteRepository;

    /**
     * Inyección de dependencias por constructor.
     * Es la mejor práctica de ingeniería de software en Spring, ya que facilita 
     * crear pruebas unitarias (Unit Tests) inyectando "Mocks" (simulaciones) del repositorio.
     */
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

    /**
     * Retorna un Optional para manejar elegantemente la posibilidad de que el 
     * cliente no exista, evitando el temido NullPointerException.
     */
    public Optional<Cliente> buscarPorId(String id) {
        log.info("Buscando cliente con ID: {}", id);
        return clienteRepository.findById(id);
    }

    public Optional<Cliente> buscarPorEmail(String email) {
        log.info("Buscando cliente con email: {}", email);
        // Normalizamos el input del usuario (trim y minúsculas) antes de buscar 
        // para asegurar que la coincidencia en BD sea exacta.
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

        // Se utiliza la consulta booleana del repositorio por eficiencia de recursos.
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
        // replaceAll usa una expresión regular (Regex) "\\s+" para buscar y eliminar todos los espacios en blanco.
        String telefonoLimpio = cliente.getTelefono().trim().replaceAll("\\s+", "");
        
        // matches() evalúa el string contra otra Regex:
        // ^[+]? -> Opcionalmente empieza con un '+'
        // [0-9]{8,15}$ -> Seguido estrictamente por entre 8 y 15 números hasta el final de la cadena.
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
        if (cliente.getNombre().trim().matches("^[0-9]+$")) { // Verifica si TODA la cadena son solo números.
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

        // Patrón de actualización clásico en JPA:
        // 1. Extraemos la entidad original desde la BD.
        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Cliente no encontrado con ID: " + id
                ));

        // 2. Aplicamos las reglas de negocio y mutamos SOLO los campos necesarios 
        // de la entidad 'existente' con los valores del objeto 'datos' recibido.
        
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
        // 3. Guardamos la entidad modificada. JPA reconocerá que el ID ya existe y ejecutará un UPDATE.
        return clienteRepository.save(existente);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando cliente con ID: {}", id);

        // ── REGLA 6: Verificar que el cliente existe antes de eliminar ────────
        // En lugar de existsById + deleteById (2 consultas a la BD),
        // hacemos findById (1 consulta) y reutilizamos el objeto para el log,
        // optimizando las transacciones hacia la base de datos.
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
        
        // Separa el texto en un array de palabras usando los espacios como delimitador.
        String[] palabras = texto.trim().toLowerCase().split("\\s+");
        
        // Uso de StringBuilder: En Java, los Strings son inmutables. 
        // Si concatenamos con "+", se crea un nuevo objeto en memoria por cada iteración.
        // StringBuilder es mutable y altamente eficiente para construir cadenas dinámicas.
        StringBuilder resultado = new StringBuilder();
        
        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                // Toma el primer caracter, lo hace mayúscula, y le concatena el resto de la palabra.
                resultado.append(Character.toUpperCase(palabra.charAt(0)))
                         .append(palabra.substring(1))
                         .append(" ");
            }
        }
        return resultado.toString().trim();
    }
}