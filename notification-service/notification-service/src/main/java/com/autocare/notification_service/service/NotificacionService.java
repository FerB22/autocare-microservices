package com.autocare.notification_service.service;

import com.autocare.notification_service.model.Notificacion;
import com.autocare.notification_service.repository.NotificacionRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public List<Notificacion> listarTodas() {
        return notificacionRepository.findAll();
    }

    public Optional<Notificacion> buscarPorId(String id) {
        return notificacionRepository.findById(id);
    }

    public List<Notificacion> buscarPorDestinatario(String idDestinatario) {
        return notificacionRepository.findByIdDestinatario(idDestinatario);
    }

    public List<Notificacion> buscarNoLeidasPorDestinatario(String idDestinatario) {
        return notificacionRepository.findByIdDestinatarioAndEstado(
            idDestinatario,
            Notificacion.EstadoNotificacion.NO_LEIDA
        );
    }

    public List<Notificacion> buscarPorTipo(Notificacion.TipoNotificacion tipo) {
        return notificacionRepository.findByTipo(tipo);
    }

    public Notificacion enviar(Notificacion notificacion) {
        notificacion.setFechaEnvio(LocalDateTime.now());
        notificacion.setEstado(Notificacion.EstadoNotificacion.NO_LEIDA);
        return notificacionRepository.save(notificacion);
    }

    public Notificacion marcarComoLeida(String id) {
        Notificacion notificacion = notificacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Notificación no encontrada con ID: " + id));
        notificacion.setEstado(Notificacion.EstadoNotificacion.LEIDA);
        return notificacionRepository.save(notificacion);
    }

    public void eliminar(String id) {
        if (!notificacionRepository.existsById(id)) {
            throw new RuntimeException("Notificación no encontrada con ID: " + id);
        }
        notificacionRepository.deleteById(id);
    }
}