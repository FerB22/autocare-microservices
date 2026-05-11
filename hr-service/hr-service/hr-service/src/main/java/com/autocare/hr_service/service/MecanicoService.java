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

    private final MecanicoRepository mecanicoRepository;

    public MecanicoService(MecanicoRepository mecanicoRepository) {
        this.mecanicoRepository = mecanicoRepository;
    }

    public List<Mecanico> listarTodos() {
        return mecanicoRepository.findAll();
    }

    public Optional<Mecanico> buscarPorId(String id) {
        return mecanicoRepository.findById(id);
    }

    public List<Mecanico> buscarDisponibles() {
        return mecanicoRepository.findByEstaDisponible(true);
    }

    public List<Mecanico> buscarPorEspecialidad(String especialidad) {
        return mecanicoRepository.findByEspecialidad(especialidad);
    }

    public List<Mecanico> buscarDisponiblesPorEspecialidad(String especialidad) {
        return mecanicoRepository.findByEspecialidadAndEstaDisponible(especialidad, true);
    }

    public Mecanico guardar(Mecanico mecanico) {
        return mecanicoRepository.save(mecanico);
    }

    public Mecanico cambiarDisponibilidad(String id, boolean disponible) {
        Mecanico mecanico = mecanicoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mecánico no encontrado con ID: " + id));
        mecanico.setEstaDisponible(disponible);
        return mecanicoRepository.save(mecanico);
    }

    public Mecanico actualizar(String id, Mecanico datos) {
        Mecanico existente = mecanicoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mecánico no encontrado con ID: " + id));
        existente.setNombre(datos.getNombre());
        existente.setEspecialidad(datos.getEspecialidad());
        existente.setEstaDisponible(datos.isEstaDisponible());
        return mecanicoRepository.save(existente);
    }

    public void eliminar(String id) {
        if (!mecanicoRepository.existsById(id)) {
            throw new RuntimeException("Mecánico no encontrado con ID: " + id);
        }
        mecanicoRepository.deleteById(id);
    }
}