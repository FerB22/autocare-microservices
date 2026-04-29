# AutoCare — Sistema de Gestión de Taller Mecánico

## Integrantes
- Fernando Barrera
- Benjamín Montanares
- Sebastián Saavedra

## Descripción
Sistema de microservicios para la gestión integral de un taller mecánico automotriz,
desarrollado con Spring Boot 3, Eureka, API Gateway y bases de datos independientes por servicio.

## Microservicios
| Servicio | Puerto | Descripción |
|---|---|---|
| eureka-server | 8761 | Registro y descubrimiento de servicios |
| api-gateway | 8080 | Puerta de entrada única |
| fleet-service | 8081 | Gestión de vehículos |
| customer-service | 8082 | Gestión de clientes |
| booking-service | 8083 | Citas y reservas |
| checkin-service | 8084 | Recepción de vehículos |
| workflow-service | 8085 | Órdenes de trabajo |
| estimation-service | 8086 | Cotizaciones de repuestos |
| spare-parts-service | 8087 | Gestión de repuestos |
| hr-service | 8088 | Gestión de mecánicos |
| billing-service | 8089 | Facturación |
| crm-service | 8090 | Interacciones con clientes |
| notification-service | 8091 | Notificaciones |

## Tecnologías
- Java 21 + Spring Boot 3.5
- Spring Cloud (Eureka, API Gateway)
- JPA + Hibernate + MySQL
- WebClient para comunicación entre servicios
- Bean Validation (Jakarta)

## Pasos para ejecutar
1. Iniciar `eureka-server` (puerto 8761)
2. Iniciar los microservicios en cualquier orden
3. Iniciar `api-gateway` (puerto 8080)
4. Verificar registro en Eureka: http://localhost:8761
5. Probar endpoints a través del gateway: http://localhost:8080/api/...

## Comunicación entre microservicios
- `booking-service` → consulta `fleet-service` (verificar vehículo) y `customer-service` (verificar cliente)
- `billing-service` → consulta `estimation-service` (obtener cotizaciones aprobadas para calcular factura)