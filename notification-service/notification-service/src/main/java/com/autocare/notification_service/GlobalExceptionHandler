package com.autocare.notification_service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor global de excepciones para el microservicio de Notificaciones.
 * @RestControllerAdvice es una anotación basada en AOP (Programación Orientada a Aspectos).
 * Funciona como un "escudo" centralizado que escucha las excepciones lanzadas por 
 * cualquier @RestController dentro de este microservicio. Atrapa el error antes 
 * de que llegue al cliente (frontend o API Gateway) y lo formatea automáticamente 
 * en una respuesta JSON estructurada.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Captura errores de @Valid (campos inválidos)
    /**
     * Intercepta las excepciones de validación generadas por Jakarta Bean Validation.
     * Este método se ejecuta automáticamente cuando una petición (ej. un POST para crear 
     * una notificación) contiene datos que no cumplen con las reglas definidas en el 
     * modelo (como @NotBlank o @NotNull).
     * * @param ex La excepción que agrupa todos los campos que fallaron en la validación.
     * @return Un mapa clave-valor (que Spring serializará a JSON) donde la clave es el 
     * nombre del campo ("mensaje") y el valor es el error ("El mensaje es obligatorio").
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Se inicializa un HashMap para construir dinámicamente la lista de errores.
        Map<String, String> errores = new HashMap<>();
        
        // Iteramos sobre cada error detectado por el BindingResult de Spring.
        // Esto es ideal para el desarrollo frontend, ya que permite pintar los mensajes 
        // de error exactamente debajo del campo de texto correspondiente en la interfaz gráfica.
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        
        // Retorna HTTP 400 (Bad Request), indicando que el cliente envió datos mal formados.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }

    // Captura errores generales de lógica (RuntimeException)
    /**
     * Actúa como una red de seguridad genérica para las reglas de negocio.
     * Atrapa todas las excepciones en tiempo de ejecución (RuntimeException) lanzadas 
     * manualmente en la capa de servicio (por ejemplo, cuando se intenta buscar o 
     * marcar como leída una notificación con un ID que no existe en la base de datos).
     * * @param ex La excepción capturada.
     * @return Una respuesta JSON estructurada y predecible: {"error": "Detalle del problema"}.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException ex) {

        // Utiliza Map.of() para instanciar rápidamente un mapa inmutable.
        // Se devuelve un HTTP 400 (Bad Request) estandarizando el formato de error 
        // para que el consumidor de la API no reciba un stacktrace (traza de código) ilegible.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}