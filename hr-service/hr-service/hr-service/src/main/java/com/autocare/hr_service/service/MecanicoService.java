package com.autocare.hr_service.service;

import com.autocare.hr_service.model.Mecanico;
import com.autocare.hr_service.repository.MecanicoRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MecanicoService {

    // Especialidades válidas en el taller AutoCare.
    // Centralizar esta lista aquí evita que lleguen valores arbitrarios
    // como "mecánico general" o "todo" que romperían los filtros por especialidad.
    private static final List<String> ESPECIALIDADES_VALIDAS = List.of(
        "MOTOR", "FRENOS", "ELECTRICO", "SUSPENSION", "TRANSMISION", "CARROCERIA", "GENERAL"
    );

    private final MecanicoRepository mecanicoRepository;

    public MecanicoService(MecanicoRepository mecanicoRepository) {
        this.mecanicoRepository = mecanicoRepository;
    }

    // ─────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────

    public List<Mecanico> listarTodos() {
        log.info("Listando todos los mecánicos");
        return mecanicoRepository.findAll();
    }

    public Optional<Mecanico> buscarPorId(String id) {
        log.info("Buscando mecánico con ID: {}", id);
        return mecanicoRepository.findById(id);
    }

    public List<Mecanico> buscarDisponibles() {
        log.info("Buscando mecánicos disponibles");
        return mecanicoRepository.findByEstaDisponible(true);
    }

    public List<Mecanico> buscarPorEspecialidad(String especialidad) {
        log.info("Buscando mecánicos con especialidad: {}", especialidad);

        // ── REGLA 1: La especialidad de búsqueda debe ser válida ──────────────
        // Evita consultas con valores arbitrarios que siempre retornarían vacío
        // y causarían confusión al usuario ("¿por qué no hay mecánicos?").
        String especialidadNormalizada = especialidad.toUpperCase().trim();
        if (!ESPECIALIDADES_VALIDAS.contains(especialidadNormalizada)) {
            log.warn("Especialidad de búsqueda inválida: {}", especialidad);
            throw new RuntimeException(
                "Especialidad inválida: '" + especialidad + "'. " +
                "Las especialidades válidas son: " + ESPECIALIDADES_VALIDAS
            );
        }

        return mecanicoRepository.findByEspecialidad(especialidadNormalizada);
    }

    public List<Mecanico> buscarDisponiblesPorEspecialidad(String especialidad) {
        log.info("Buscando mecánicos disponibles con especialidad: {}", especialidad);
        String especialidadNormalizada = especialidad.toUpperCase().trim();

        if (!ESPECIALIDADES_VALIDAS.contains(especialidadNormalizada)) {
            log.warn("Especialidad de búsqueda inválida: {}", especialidad);
            throw new RuntimeException(
                "Especialidad inválida: '" + especialidad + "'. " +
                "Las especialidades válidas son: " + ESPECIALIDADES_VALIDAS
            );
        }

        return mecanicoRepository.findByEspecialidadAndEstaDisponible(especialidadNormalizada, true);
    }

    // ─────────────────────────────────────────
    //  CREACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Mecanico guardar(Mecanico mecanico) {
        log.info("Guardando nuevo mecánico: {}", mecanico.getNombre());

        // ── REGLA 2: El nombre no puede ser solo espacios en blanco ──────────
        // @NotBlank en el modelo valida esto desde el Controller con @Valid.
        // Lo verificamos también aquí porque el Service puede ser llamado
        // internamente sin pasar por el Controller (ej: desde tests o seeds).
        if (mecanico.getNombre() == null || mecanico.getNombre().isBlank()) {
            log.warn("Intento de guardar mecánico sin nombre");
            throw new RuntimeException("El nombre del mecánico es obligatorio.");
        }

        // ── REGLA 3: La especialidad debe ser un valor del catálogo ───────────
        // En el taller real, los mecánicos se contratan para un área específica.
        // Aceptar "abc" o "mecánico multiuso" dificulta asignar órdenes después.
        String especialidadNormalizada = mecanico.getEspecialidad().toUpperCase().trim();
        if (!ESPECIALIDADES_VALIDAS.contains(especialidadNormalizada)) {
            log.warn("Especialidad inválida al guardar: {}", mecanico.getEspecialidad());
            throw new RuntimeException(
                "Especialidad inválida: '" + mecanico.getEspecialidad() + "'. " +
                "Las especialidades permitidas son: " + ESPECIALIDADES_VALIDAS
            );
        }

        // ── REGLA 4: No puede existir dos mecánicos con el mismo nombre ───────
        // En un taller pequeño, el nombre es el identificador humano.
        // Duplicarlo causaría confusión al asignar órdenes en el workflow-service.
        boolean nombreDuplicado = mecanicoRepository.findAll()
                .stream()
                .anyMatch(m -> m.getNombre().equalsIgnoreCase(mecanico.getNombre().trim()));

        if (nombreDuplicado) {
            log.warn("Nombre duplicado al guardar mecánico: {}", mecanico.getNombre());
            throw new RuntimeException(
                "Ya existe un mecánico registrado con el nombre: '" +
                mecanico.getNombre() + "'."
            );
        }

        // Normalizar especialidad antes de guardar para consistencia en BD
        mecanico.setEspecialidad(especialidadNormalizada);
        mecanico.setNombre(mecanico.getNombre().trim());

        log.info("Mecánico '{}' registrado con especialidad: {}",
                mecanico.getNombre(), mecanico.getEspecialidad());
        return mecanicoRepository.save(mecanico);
    }

    // ─────────────────────────────────────────
    //  DISPONIBILIDAD CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Mecanico cambiarDisponibilidad(String id, boolean disponible) {
        log.info("Cambiando disponibilidad del mecánico {} a: {}", id, disponible);

        Mecanico mecanico = mecanicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Mecánico no encontrado con ID: " + id
                ));

        // ── REGLA 5: No cambiar a disponible si ya está disponible ────────────
        // Evita llamadas redundantes y registros de log innecesarios que
        // dificultan el seguimiento real de cambios de estado.
        if (mecanico.isEstaDisponible() == disponible) {
            String estadoTexto = disponible ? "disponible" : "no disponible";
            log.warn("Mecánico {} ya está {}, no se requiere cambio", id, estadoTexto);
            throw new RuntimeException(
                "El mecánico '" + mecanico.getNombre() +
                "' ya está marcado como " + estadoTexto + "."
            );
        }

        mecanico.setEstaDisponible(disponible);
        log.info("Mecánico '{}' ahora está {}",
                mecanico.getNombre(), disponible ? "disponible" : "no disponible");
        return mecanicoRepository.save(mecanico);
    }

    // ─────────────────────────────────────────
    //  ACTUALIZACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public Mecanico actualizar(String id, Mecanico datos) {
        log.info("Actualizando mecánico con ID: {}", id);

        Mecanico existente = mecanicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Mecánico no encontrado con ID: " + id
                ));

        // ── REGLA 6: No se puede cambiar especialidad a un valor inválido ─────
        // Un mecánico puede ser reentrenado, pero solo en especialidades reales.
        if (datos.getEspecialidad() != null) {
            String especialidadNormalizada = datos.getEspecialidad().toUpperCase().trim();
            if (!ESPECIALIDADES_VALIDAS.contains(especialidadNormalizada)) {
                log.warn("Especialidad inválida al actualizar mecánico {}: {}",
                        id, datos.getEspecialidad());
                throw new RuntimeException(
                    "Especialidad inválida: '" + datos.getEspecialidad() + "'. " +
                    "Las especialidades válidas son: " + ESPECIALIDADES_VALIDAS
                );
            }
            existente.setEspecialidad(especialidadNormalizada);
        }

        // ── REGLA 7: No se puede cambiar el nombre a uno ya existente ─────────
        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            boolean nombreDuplicado = mecanicoRepository.findAll()
                    .stream()
                    .anyMatch(m -> m.getNombre().equalsIgnoreCase(datos.getNombre().trim())
                               && !m.getIdMecanico().equals(id)); // excluir al propio mecánico

            if (nombreDuplicado) {
                log.warn("Nombre duplicado al actualizar mecánico: {}", datos.getNombre());
                throw new RuntimeException(
                    "Ya existe otro mecánico con el nombre: '" + datos.getNombre() + "'."
                );
            }
            existente.setNombre(datos.getNombre().trim());
        }

        existente.setEstaDisponible(datos.isEstaDisponible());
        log.info("Mecánico '{}' actualizado correctamente", existente.getNombre());
        return mecanicoRepository.save(existente);
    }

    // ─────────────────────────────────────────
    //  ELIMINACIÓN CON REGLAS DE NEGOCIO
    // ─────────────────────────────────────────

    public void eliminar(String id) {
        log.info("Eliminando mecánico con ID: {}", id);

        Mecanico mecanico = mecanicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Mecánico no encontrado con ID: " + id
                ));

        // ── REGLA 8: No se puede eliminar un mecánico que está ocupado ────────
        // Si el mecánico está marcado como NO disponible, significa que tiene
        // una orden de trabajo asignada en el workflow-service.
        // Eliminarlo dejaría esa orden sin mecánico responsable y sin registro.
        if (!mecanico.isEstaDisponible()) {
            log.warn("Intento de eliminar mecánico ocupado: {}", id);
            throw new RuntimeException(
                "No se puede eliminar a '" + mecanico.getNombre() +
                "' porque está actualmente asignado a una orden de trabajo. " +
                "Primero libere al mecánico con cambiarDisponibilidad()."
            );
        }

        mecanicoRepository.deleteById(id);
        log.info("Mecánico '{}' eliminado del sistema", mecanico.getNombre());
    }
}