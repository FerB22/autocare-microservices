package com.autocare.notification_service.repository;

import com.autocare.notification_service.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Capa de acceso a datos (Data Access Object - DAO) para la entidad Notificacion.
 * @Repository es un estereotipo de Spring que marca esta interfaz como un bean 
 * encargado de interactuar con la base de datos, habilitando además la traducción 
 * automática de excepciones SQL a excepciones nativas de Spring (DataAccessException).
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, String> {

    /**
     * NOTA ARQUITECTÓNICA:
     * Al extender JpaRepository<Notificacion, String>, esta interfaz hereda 
     * automáticamente toda la funcionalidad CRUD estándar (save, findById, delete, etc.).
     * Spring Data JPA utiliza un proxy dinámico en tiempo de ejecución para generar 
     * el código de implementación sin que nosotros tengamos que escribir SQL manualmente.
     */

    // ── QUERY METHODS (Consultas Derivadas) ──────────────────────────────────
    // Spring analiza sintácticamente el nombre del método para construir la consulta.

    /**
     * Traducido a SQL: SELECT * FROM notificaciones WHERE id_destinatario = ?
     * * Útil para obtener el historial completo de notificaciones de un usuario en particular 
     * (sea cliente, administrador o mecánico), utilizando su identificador único (Soft Foreign Key).
     * * @param idDestinatario El UUID del usuario dueño de las notificaciones.
     * @return Una lista con todas las notificaciones de ese usuario.
     */
    List<Notificacion> findByIdDestinatario(String idDestinatario);

    /**
     * Traducido a SQL: SELECT * FROM notificaciones WHERE estado = ?
     * * Filtra las notificaciones globales del sistema según su estado actual.
     * Por ejemplo, podría usarse para un proceso batch nocturno que limpie o archive
     * todas las notificaciones que ya estén en estado 'LEIDA'.
     * * @param estado El estado a buscar (NO_LEIDA o LEIDA).
     * @return Lista de notificaciones que coincidan con el estado.
     */
    List<Notificacion> findByEstado(Notificacion.EstadoNotificacion estado);

    /**
     * Traducido a SQL: SELECT * FROM notificaciones WHERE id_destinatario = ? AND estado = ?
     * * Consulta compuesta utilizando el operador 'And'.
     * Es probablemente el método más utilizado por el frontend, ya que permite consultar 
     * específicamente las notificaciones "NO_LEIDA" de un usuario puntual para 
     * renderizar el contador visual (badge) de alertas no revisadas en la interfaz (UI).
     * * @param idDestinatario El UUID del usuario.
     * @param estado El estado requerido (usualmente NO_LEIDA).
     * @return Lista filtrada con las notificaciones pendientes de ese usuario.
     */
    List<Notificacion> findByIdDestinatarioAndEstado(
        String idDestinatario,
        Notificacion.EstadoNotificacion estado
    );

    /**
     * Traducido a SQL: SELECT * FROM notificaciones WHERE tipo = ?
     * * Permite agrupar o filtrar notificaciones por su categoría lógica.
     * Es muy útil para métricas de auditoría o para tableros administrativos 
     * (ej. ver todas las alertas de tipo 'ALERTA_STOCK' generadas en el taller).
     * * @param tipo El tipo exacto definido en el Enum TipoNotificacion.
     * @return Lista de notificaciones correspondientes a esa categoría.
     */
    List<Notificacion> findByTipo(Notificacion.TipoNotificacion tipo);
}