package com.autocare.notification_service.service;

import com.autocare.notification_service.model.Notificacion;
import com.autocare.notification_service.repository.NotificacionRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Capa de Servicio (Service Layer) para la gestión de Notificaciones.
 * Actúa como el intermediario entre el Controlador (API) y la Base de Datos (Repositorio),
 * centralizando la lógica de negocio y asegurando la consistencia de los datos.
 */
@Slf4j // Anotación de Lombok para inyectar automáticamente un logger estático y registrar eventos.
@Service // Registra la clase como un componente (Bean) dentro del contenedor de Spring.
public class NotificacionService {

    // Dependencia al repositorio declarada como 'final' para garantizar su inmutabilidad.
    private final NotificacionRepository notificacionRepository;

    /**
     * Inyección de dependencias por constructor.
     * Garantiza que el servicio no pueda ser instanciado sin su repositorio,
     * facilitando además las pruebas unitarias (testing) al permitir inyectar mocks.
     */
    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Notificacion> listarTodas() {
        return notificacionRepository.findAll();
    }

    public Optional<Notificacion> buscarPorId(String id) {
        // El uso de Optional previene NullPointerExceptions y obliga a la capa
        // superior (Controller) a manejar explícitamente el escenario donde no exista.
        return notificacionRepository.findById(id);
    }

    public List<Notificacion> buscarPorDestinatario(String idDestinatario) {
        return notificacionRepository.findByIdDestinatario(idDestinatario);
    }

    public List<Notificacion> buscarNoLeidasPorDestinatario(String idDestinatario) {
        // Delega en el Repositorio la búsqueda filtrando de una vez por el estado NO_LEIDA.
        // A nivel de base de datos esto es mucho más eficiente y consume menos memoria 
        // que traer todas las notificaciones y filtrarlas mediante Streams de Java.
        return notificacionRepository.findByIdDestinatarioAndEstado(
            idDestinatario,
            Notificacion.EstadoNotificacion.NO_LEIDA
        );
    }

    public List<Notificacion> buscarPorTipo(Notificacion.TipoNotificacion tipo) {
        return notificacionRepository.findByTipo(tipo);
    }

    // ─────────────────────────────────────────
    //  ESCRITURA Y REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Notificacion enviar(Notificacion notificacion) {
        // Regla de Negocio y Seguridad (Programación Defensiva):
        // La fecha de envío y el estado inicial NO deben depender de lo que envíe el cliente
        // en la petición HTTP. El backend siempre debe sobreescribir estos valores 
        // para garantizar la auditoría y evitar manipulaciones o inconsistencias.
        notificacion.setFechaEnvio(LocalDateTime.now());
        notificacion.setEstado(Notificacion.EstadoNotificacion.NO_LEIDA);
        
        return notificacionRepository.save(notificacion);
    }

    public Notificacion marcarComoLeida(String id) {
        // Patrón clásico de actualización en JPA:
        // 1. Buscar la entidad existente en la base de datos. Si no existe, lanza excepción inmediatamente.
        Notificacion notificacion = notificacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Notificación no encontrada con ID: " + id));
        
        // 2. Modificar el estado del objeto gestionado por el ORM.
        notificacion.setEstado(Notificacion.EstadoNotificacion.LEIDA);
        
        // 3. Guardar. JPA detectará que la entidad ya tiene un ID asignado y 
        // ejecutará automáticamente un UPDATE en vez de un INSERT.
        return notificacionRepository.save(notificacion);
    }

    public void eliminar(String id) {
        // Verificación defensiva antes de intentar borrar.
        // Esto permite lanzar una excepción controlada (que el GlobalExceptionHandler 
        // convertirá en un error 404) en lugar de que Spring Data arroje una 
        // EmptyResultDataAccessException difícil de leer.
        if (!notificacionRepository.existsById(id)) {
            throw new RuntimeException("Notificación no encontrada con ID: " + id);
        }
        notificacionRepository.deleteById(id);
    }
}