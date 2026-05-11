# AutoCare — Sistema de Gestión de Taller Mecánico

## Integrantes
- Fernando Barrera
- Benjamín Montanares
- Sebastián Saavedra

## Descripción
Sistema de microservicios para la gestión integral de un taller mecánico automotriz,
desarrollado con Spring Boot 3, Eureka, API Gateway y bases de datos independientes por servicio.

## Microservicios
| Servicio           | Puerto | Descripción                        |
|--------------------|--------|------------------------------------|
| eureka-server      | 8761   | Registro y descubrimiento de servicios |
| api-gateway        | 8080   | Puerta de entrada única            |
| fleet-service      | 8081   | Gestión de vehículos               |
| customer-service   | 8082   | Gestión de clientes                |
| booking-service    | 8083   | Citas y reservas                   |
| checkin-service    | 8084   | Recepción de vehículos             |
| workflow-service   | 8085   | Órdenes de trabajo                 |
| estimation-service | 8086   | Cotizaciones de repuestos          |
| spare-parts-service| 8087   | Gestión de repuestos               |
| hr-service         | 8088   | Gestión de mecánicos               |
| billing-service    | 8089   | Facturación                        |
| crm-service        | 8090   | Interacciones con clientes         |
| notification-service| 8091  | Notificaciones                     |

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

***

## Reglas de Negocio — Módulo de Citas (`booking-service`)

Esta sección documenta las reglas de negocio implementadas en `CitaService`.

### RN-01: Existencia del vehículo y cliente
Antes de registrar una cita, el sistema verifica que el vehículo y el cliente existan
consultando `fleet-service` y `customer-service` respectivamente.
Si alguno no existe, la cita se rechaza con un error 400.

### RN-02: Duración estándar de una cita
Cada cita tiene una duración fija de **60 minutos**.
Este valor se usa para calcular el rango de tiempo ocupado al validar conflictos.

### RN-03: Sin choques de horario por vehículo
Un mismo vehículo no puede tener dos citas en estado `CONFIRMADA` o `EJECUTADA`
cuyas ventanas de tiempo se solapen.
Si se detecta un conflicto, la cita se rechaza con el mensaje:
> "El vehículo ya tiene una cita en ese horario."

**Ejemplo:**
- Cita existente: 10:00 – 11:00
- Nueva cita a las 10:30 → **rechazada** (se solapa)
- Nueva cita a las 11:00 → **aceptada** (sin solapamiento)

### RN-04: Límite diario de citas del taller
El taller acepta un máximo de **20 citas por día calendario**.
Si se alcanza el límite, nuevas solicitudes son rechazadas con:
> "Se alcanzó el máximo de citas permitidas para este día."

### RN-05: Transiciones de estado válidas
El estado de una cita solo puede cambiar según las siguientes transiciones permitidas:

| Estado actual | Estados permitidos |
|---------------|--------------------|
| `CONFIRMADA`  | `CANCELADA`, `EJECUTADA` |
| `CANCELADA`   | *(ninguno — estado final)* |
| `EJECUTADA`   | *(ninguno — estado final)* |

Cualquier intento de transición no permitida retorna error 400 con:
> "Transición de estado inválida: [estado_actual] → [estado_nuevo]"

### RN-06: Creación automática de Orden de Trabajo
Cuando una cita pasa a estado `EJECUTADA`, el sistema crea automáticamente una
Orden de Trabajo en `workflow-service` con los datos de la cita (idVehiculo, idCliente, fecha).
Si la creación falla, el cambio de estado se revierte y se registra el error en los logs.

### RN-07: La fecha de la cita debe ser futura
No se permiten citas con fecha y hora en el pasado.
Esta regla se aplica mediante la anotación `@Future` en el campo `fechaHora` de la entidad `Cita`.

***

## Escenarios de prueba — Reglas de Negocio

| Escenario | Input esperado | Respuesta esperada |
|-----------|---------------|--------------------|
| Vehículo no existe | `idVehiculo` inválido | 400 + "El vehículo no existe en el sistema" |
| Choque de horario | Cita en rango ocupado | 400 + "El vehículo ya tiene una cita en ese horario" |
| Límite diario excedido | 21ª cita en el mismo día | 400 + "Se alcanzó el máximo de citas permitidas para este día" |
| Transición inválida | `CANCELADA → CONFIRMADA` | 400 + "Transición de estado inválida" |
| Fecha en el pasado | `fechaHora` < ahora | 400 + "La cita debe ser en una fecha futura" |
| Cambio a EJECUTADA exitoso | Cita válida → EJECUTADA | 200 + orden de trabajo creada en workflow-service |
