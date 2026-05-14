package com.autocare.fleet_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entidad JPA que mapea la clase Vehiculo a la tabla "vehiculos" en la base de datos relacional.
 * Al no usar Lombok (@Data, @Getter, @Setter) en esta clase, se opta por el enfoque
 * tradicional de Java (POJO estándar), lo cual da un control absoluto sobre el código 
 * generado y evita posibles dependencias ocultas o problemas de rendimiento con proxies de Hibernate.
 */
@Entity // Indica que esta clase es gestionada por el ORM (Hibernate) como una entidad persistente.
@Table(name = "vehiculos") // Especifica el nombre exacto de la tabla en el motor de base de datos.
public class Vehiculo {

    /**
     * Clave Primaria (PK) de la entidad.
     * El uso de UUID (Universally Unique Identifier) previene conflictos al fusionar bases
     * de datos y es el estándar de facto en arquitecturas de microservicios, ya que los IDs 
     * no son secuenciales ni predecibles.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idVehiculo;

    /**
     * La patente es un campo crítico que requiere validación en dos capas:
     * 1. @NotBlank: Intercepta peticiones mal formadas en el Controlador antes de llegar a la lógica de negocio.
     * 2. @Column: Asegura la integridad estructural a nivel de esquema de BD (DDL), creando
     *    un índice único (UNIQUE) y prohibiendo valores nulos (NOT NULL) nativamente.
     */
    @NotBlank(message = "La patente es obligatoria")
    @Column(unique = true, nullable = false)
    private String patente;

    @NotBlank(message = "La marca es obligatoria")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    private String modelo;

    // @NotNull se usa en lugar de @NotBlank porque "anio" es un objeto Integer, 
    // y @NotBlank solo aplica para secuencias de caracteres (Strings).
    @NotNull(message = "El año es obligatorio")
    private Integer anio;

    // Campos opcionales (sin restricciones explícitas de Jakarta o Columnas nulas por defecto).
    private String vinChasis;
    
    /**
     * Relación "Suave" (Soft Foreign Key) en Microservicios.
     * Nota arquitectónica clave: Aquí NO se usa @ManyToOne hacia una clase Cliente.
     * Dado que el Cliente vive en otro microservicio (customer-service) con su propia base de datos, 
     * no podemos tener una restricción de integridad referencial dura (Foreign Key física).
     * En su lugar, almacenamos solo el UUID del cliente como un String y resolvemos 
     * la información mediante llamadas a la API (RestTemplate/OpenFeign) cuando se requiera.
     */
    private String idDuenio;

    // ─────────────────────────────────────────
    //  GETTERS Y SETTERS TRADICIONALES
    // ─────────────────────────────────────────
    // Fomentan el Principio de Encapsulamiento de la Programación Orientada a Objetos.
    // Exponen de manera segura el estado interno de la clase para ser leído (get) o mutado (set).

    public String getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(String idVehiculo) { this.idVehiculo = idVehiculo; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getVinChasis() { return vinChasis; }
    public void setVinChasis(String vinChasis) { this.vinChasis = vinChasis; }

    public String getIdDuenio() { return idDuenio; }
    public void setIdDuenio(String idDuenio) { this.idDuenio = idDuenio; }
}