package com.autocare.notification_service.repository;

import com.autocare.notification_service.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, String> {

    List<Notificacion> findByIdDestinatario(String idDestinatario);

    List<Notificacion> findByEstado(Notificacion.EstadoNotificacion estado);

    List<Notificacion> findByIdDestinatarioAndEstado(
        String idDestinatario,
        Notificacion.EstadoNotificacion estado
    );

    List<Notificacion> findByTipo(Notificacion.TipoNotificacion tipo);
}