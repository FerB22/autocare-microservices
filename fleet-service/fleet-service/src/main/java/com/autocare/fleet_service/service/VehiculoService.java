package com.autocare.fleet_service.service;

import com.autocare.fleet_service.model.Vehiculo;
import com.autocare.fleet_service.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;

    // Inyección de dependencias por constructor (buena práctica)
    public VehiculoService(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    public List<Vehiculo> listarTodos() {
        return vehiculoRepository.findAll();
    }

    public Optional<Vehiculo> buscarPorPatente(String patente) {
        return vehiculoRepository.findByPatente(patente);
    }

    public Optional<Vehiculo> buscarPorId(String id) {
        return vehiculoRepository.findById(id);
    }

    public Vehiculo guardar(Vehiculo vehiculo) {
        if (vehiculoRepository.existsByPatente(vehiculo.getPatente())) {
            throw new RuntimeException("Ya existe un vehículo con la patente: " + vehiculo.getPatente());
        }
        return vehiculoRepository.save(vehiculo);
    }

    public Vehiculo actualizar(String id, Vehiculo datos) {
        Vehiculo existente = vehiculoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con ID: " + id));

        existente.setMarca(datos.getMarca());
        existente.setModelo(datos.getModelo());
        existente.setAnio(datos.getAnio());
        existente.setVinChasis(datos.getVinChasis());
        existente.setIdDuenio(datos.getIdDuenio());

        return vehiculoRepository.save(existente);
    }

    public void eliminar(String id) {
        if (!vehiculoRepository.existsById(id)) {
            throw new RuntimeException("Vehículo no encontrado con ID: " + id);
        }
        vehiculoRepository.deleteById(id);
    }
}