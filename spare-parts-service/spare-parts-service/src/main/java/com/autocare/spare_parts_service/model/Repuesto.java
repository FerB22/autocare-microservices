package com.autocare.spare_parts_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.NonNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

// ✅ Después
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "repuestos")
public class Repuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idRepuesto;

    @NotBlank(message = "El nombre es obligatorio")
    @NonNull
    private String nombre;

    @NotBlank(message = "El código de parte es obligatorio")
    @NonNull
    @Column(unique = true, nullable = false)
    private String codigoParte;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private Double precioUnitario;

    private String ubicacionBodega;
}
