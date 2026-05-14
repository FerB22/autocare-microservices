package com.autocare.fleet_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Clase de configuración central para los clientes HTTP del microservicio.
 * @Configuration indica a Spring que esta clase contiene definiciones de @Beans
 * que deben ser inicializados y gestionados por el contenedor de Inversión de Control (IoC)
 * al arrancar la aplicación.
 */
@Configuration
public class WebClientConfig {

    /**
     * Define un constructor (Builder) de WebClient como un Bean de Spring.
     * WebClient es el cliente HTTP moderno, asíncrono y reactivo de Spring WebFlux,
     * diseñado para reemplazar al antiguo RestTemplate.
     * 
     * @Bean expone este Builder para que pueda ser inyectado en cualquier capa 
     * de servicio (como en tu VehiculoService) mediante el constructor.
     * 
     * @LoadBalanced es la pieza clave en una arquitectura de microservicios.
     * Le dice a Spring Cloud que intercepte todas las peticiones hechas con este cliente
     * y las pase a través de un balanceador de carga del lado del cliente (Client-Side Load Balancer).
     * Esto permite:
     * 1. Usar nombres lógicos (Service Discovery) en lugar de URLs físicas 
     *    (ej. usar "http://customer-service" en lugar de "http://localhost:8081").
     * 2. Si hay múltiples instancias del "customer-service" ejecutándose, el balanceador 
     *    distribuirá el tráfico entre ellas automáticamente (generalmente usando Round Robin),
     *    mejorando la disponibilidad y escalabilidad del sistema.
     *
     * @return Una instancia preconfigurada de WebClient.Builder.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}