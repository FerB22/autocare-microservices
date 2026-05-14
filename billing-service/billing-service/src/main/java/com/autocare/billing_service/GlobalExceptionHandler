package com.autocare.billing_service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Maneja excepciones lanzadas por los controllers y las transforma
 * en respuestas HTTP con un cuerpo JSON simple.
 *
 * Nota: actualmente maneja RecursoNoEncontradoException e IllegalArgumentException.
 * Se recomienda ampliar el mapeo para validaciones y excepciones de negocio.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Mapea RecursoNoEncontradoException a 404 Not Found.
     * Devuelve un JSON simple con la clave "error" y el mensaje de la excepción.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNoEncontrado(
            RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Mapea IllegalArgumentException a 400 Bad Request.
     * Útil para argumentos inválidos en la lógica de negocio.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleArgumentoInvalido(
            IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
