package com.autocare.billing_service.exception;

/**
 * Excepción personalizada que representa el caso en que un recurso
 * (por ejemplo, una factura) no fue encontrado en el sistema.
 *
 * - Extiende RuntimeException para que sea una excepción no verificada.
 * - Se usa desde la capa Service para señalizar "404 Not Found" semánticamente.
 * - Idealmente se captura y transforma en una respuesta HTTP 404 por un
 *   @ControllerAdvice centralizado.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    // Opcional: serialVersionUID para compatibilidad de serialización
    private static final long serialVersionUID = 1L;

    /**
     * Constructor principal que recibe el mensaje de error.
     *
     * @param mensaje Mensaje descriptivo que explica qué recurso no se encontró.
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    /**
     * Constructor adicional que permite encadenar la causa original.
     * Útil cuando la excepción se origina por otra excepción (p. ej. DB).
     *
     * @param mensaje Mensaje descriptivo.
     * @param causa   Excepción original que provocó este error.
     */
    public RecursoNoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /**
     * Constructor por defecto sin mensaje (poco usado, pero disponible).
     */
    public RecursoNoEncontradoException() {
        super();
    }
}
