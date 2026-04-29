package com.autocare.booking_service.service;


import com.autocare.booking_service.dto.VehiculoDTO;
import com.autocare.booking_service.dto.ClienteDTO;
import com.autocare.booking_service.model.Cita;
import com.autocare.booking_service.repository.CitaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;


import java.util.List;
import java.util.Optional;


@Service
public class CitaService {


    private static final Logger log = LoggerFactory.getLogger(CitaService.class);


    private final CitaRepository citaRepository;
    private final WebClient.Builder webClientBuilder;


    public CitaService(CitaRepository citaRepository, WebClient.Builder webClientBuilder) {
        this.citaRepository = citaRepository;
        this.webClientBuilder = webClientBuilder;
    }


    public List<Cita> listarTodas() {
        log.info("Listando todas las citas");
        return citaRepository.findAll();
    }


    public Optional<Cita> buscarPorId(String id) {
        log.info("Buscando cita con ID: {}", id);
        return citaRepository.findById(id);
    }


    public List<Cita> buscarPorVehiculo(String idVehiculo) {
        log.info("Buscando citas para vehículo: {}", idVehiculo);
        return citaRepository.findByIdVehiculo(idVehiculo);
    }


    public Cita guardar(Cita cita) {
        // Verificar vehículo en fleet-service
        VehiculoDTO vehiculo = verificarVehiculo(cita.getIdVehiculo());
        log.info("Vehículo verificado: {} {}", vehiculo.getMarca(), vehiculo.getModelo());


        // Verificar cliente en customer-service
        ClienteDTO cliente = verificarCliente(cita.getIdCliente());
        log.info("Cliente verificado: {} {}", cliente.getNombre(), cliente.getApellido());


        log.info("Guardando cita para cliente {} y vehículo {}", 
                cita.getIdCliente(), cita.getIdVehiculo());
        return citaRepository.save(cita);
    }


    public VehiculoDTO verificarVehiculo(String idVehiculo) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://fleet-service/vehiculos/" + idVehiculo)
                    .retrieve()
                    .bodyToMono(VehiculoDTO.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Vehículo con ID {} no encontrado en fleet-service", idVehiculo);
            throw new RuntimeException("El vehículo no existe en el sistema: " + idVehiculo);
        }
    }


    public ClienteDTO verificarCliente(String idCliente) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://customer-service/clientes/" + idCliente)
                    .retrieve()
                    .bodyToMono(ClienteDTO.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Cliente con ID {} no encontrado en customer-service", idCliente);
            throw new RuntimeException("El cliente no existe en el sistema: " + idCliente);
        }
    }


    public Cita cambiarEstado(String id, Cita.EstadoCita nuevoEstado) {
        log.info("Cambiando estado de cita {} a {}", id, nuevoEstado);
        Cita cita = citaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cita no encontrada con ID: " + id));
        cita.setEstado(nuevoEstado);
        return citaRepository.save(cita);
    }


    public void eliminar(String id) {
        log.info("Eliminando cita con ID: {}", id);
        if (!citaRepository.existsById(id)) {
            throw new RuntimeException("Cita no encontrada con ID: " + id);
        }
        citaRepository.deleteById(id);
    }
}