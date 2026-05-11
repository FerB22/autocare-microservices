package com.autocare.spare_parts_service.service;

import com.autocare.spare_parts_service.model.Repuesto;
import com.autocare.spare_parts_service.repository.RepuestoRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RepuestoService {

    private final RepuestoRepository repuestoRepository;

    public RepuestoService(RepuestoRepository repuestoRepository) {
        this.repuestoRepository = repuestoRepository;
    }

    public List<Repuesto> listarTodos() {
        return repuestoRepository.findAll();
    }

    public Optional<Repuesto> buscarPorId(String id) {
        return repuestoRepository.findById(id);
    }

    public Optional<Repuesto> buscarPorCodigo(String codigo) {
        return repuestoRepository.findByCodigoParte(codigo);
    }

    public List<Repuesto> listarConStock() {
        return repuestoRepository.findByStockGreaterThan(0);
    }

    public Repuesto guardar(Repuesto repuesto) {
        if (repuestoRepository.existsByCodigoParte(repuesto.getCodigoParte())) {
            throw new RuntimeException("Ya existe un repuesto con el código: "
                + repuesto.getCodigoParte());
        }
        return repuestoRepository.save(repuesto);
    }

    // 🔑 Aparta unidades del stock sin eliminar el repuesto
    public Repuesto reservar(String id, int cantidad) {
        Repuesto repuesto = repuestoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Repuesto no encontrado con ID: " + id));

        if (repuesto.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente. Disponible: "
                + repuesto.getStock() + ", solicitado: " + cantidad);
        }

        repuesto.setStock(repuesto.getStock() - cantidad);
        return repuestoRepository.save(repuesto);
    }

    public Repuesto actualizar(String id, Repuesto datos) {
        Repuesto existente = repuestoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Repuesto no encontrado con ID: " + id));
        existente.setNombre(datos.getNombre());
        existente.setStock(datos.getStock());
        existente.setPrecioUnitario(datos.getPrecioUnitario());
        existente.setUbicacionBodega(datos.getUbicacionBodega());
        return repuestoRepository.save(existente);
    }

    public void eliminar(String id) {
        if (!repuestoRepository.existsById(id)) {
            throw new RuntimeException("Repuesto no encontrado con ID: " + id);
        }
        repuestoRepository.deleteById(id);
    }
}