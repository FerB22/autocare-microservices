package com.autocare.fleet_service.service;

import com.autocare.fleet_service.dto.ClienteDTO;
import com.autocare.fleet_service.model.Vehiculo;
import com.autocare.fleet_service.repository.VehiculoRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final WebClient webClient; // ✅ AGREGADO

    // ✅ ACTUALIZADO: ahora recibe también el WebClient.Builder
    public VehiculoService(VehiculoRepository vehiculoRepository,
                           WebClient.Builder webClientBuilder) {
        this.vehiculoRepository = vehiculoRepository;
        this.webClient = webClientBuilder
                .baseUrl("lb://customer-service")
                .build();
    }

    // ✅ NUEVO: consulta los datos del dueño del vehículo
    public ClienteDTO obtenerDuenio(String idDuenio) {
        return webClient.get()
                .uri("/clientes/{id}", idDuenio)
                .retrieve()
                .bodyToMono(ClienteDTO.class)
                .block();
    }

    // --- Lo que ya tenías, sin cambios ---

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