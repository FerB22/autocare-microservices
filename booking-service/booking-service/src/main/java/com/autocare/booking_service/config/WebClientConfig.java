package com.autocare.booking_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración para inyectar un WebClient.Builder balanceado.
 * - @LoadBalanced permite resolver servicios por nombre (p. ej. http://fleet-service).
 * - Inyectar el Builder facilita tests y la creación de WebClient por servicio.
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        // Devuelve un builder reutilizable; cada service puede llamar .baseUrl(...) o .build()
        return WebClient.builder();
    }
}
