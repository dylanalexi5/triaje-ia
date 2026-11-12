package com.campuslands.tickets.exception;

import com.campuslands.tickets.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Único punto donde una excepción se convierte en respuesta HTTP.
 *
 * Regla de oro: el cliente recibe un mensaje claro y accionable, nunca un
 * stack trace, un nombre de tabla o el texto crudo de una excepción de
 * Jackson o Hibernate. El detalle técnico se queda en el log del servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- 404 ---------------------------------------------------------------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> recursoNoEncontrado(RecursoNoEncontradoException ex,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", request);
    }

    // --- 409 ---------------------------------------------------------------

    /**
     * 409: la petición está bien formada, pero choca con el estado actual
     * del recurso (correo ya usado, ticket cerrado, transición no permitida).
     */
    @ExceptionHandler({
            CorreoDuplicadoException.class,
            TransicionEstadoInvalidaException.class,
            TicketCerradoException.class
    })
    public ResponseEntity<ErrorResponse> conflicto(RuntimeException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // --- 400: lo que envió el cliente no sirve ------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex,
                                                    HttpServletRequest request) {
        // Un campo vacío dispara a la vez @NotBlank y @Size, y el orden en que
        // llegan no está garantizado. Se deja un mensaje por campo y, si falta
        // el dato, gana "es obligatorio": así la respuesta es siempre la misma.
        Map<String, FieldError> unoPorCampo = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error,
                        (actual, otro) -> esObligatorio(otro) ? otro : actual,
                        TreeMap::new));

        List<String> detalles = unoPorCampo.values().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos", request, detalles);
    }

    private static boolean esObligatorio(FieldError error) {
        return "NotBlank".equals(error.getCode()) || "NotNull".equals(error.getCode());
    }

    /** El enunciado lista "técnico con rol permitido" entre las validaciones (sección 9). */
    @ExceptionHandler(TecnicoInvalidoException.class)
    public ResponseEntity<ErrorResponse> tecnicoInvalido(TecnicoInvalidoException ex,
                                                         HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * JSON mal formado o un valor que no encaja con el tipo esperado.
     * El caso más común es un enum inválido ("rol": "JEFE"); ahí vale la
     * pena decirle al cliente cuáles son los valores permitidos.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex,
                                                        HttpServletRequest request) {
        if (ex.getCause() instanceof InvalidFormatException formato && formato.getTargetType().isEnum()) {
            String campo = formato.getPath().isEmpty()
                    ? "valor"
                    : formato.getPath().getLast().getPropertyName();
            String mensaje = "Valor inválido para '%s': %s. Valores permitidos: %s"
                    .formatted(campo, formato.getValue(), valoresDe(formato.getTargetType()));
            return responder(HttpStatus.BAD_REQUEST, mensaje, request);
        }

        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido", request);
    }

    /** Parámetros de URL o query con un tipo incorrecto: ?rol=JEFE o /api/usuarios/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex,
                                                           HttpServletRequest request) {
        Class<?> tipo = ex.getRequiredType();
        String mensaje = tipo != null && tipo.isEnum()
                ? "Valor inválido para '%s': %s. Valores permitidos: %s"
                        .formatted(ex.getName(), ex.getValue(), valoresDe(tipo))
                : "Valor inválido para '%s': %s".formatted(ex.getName(), ex.getValue());

        return responder(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED,
                "El método %s no está permitido en esta ruta".formatted(ex.getMethod()), request);
    }

    // --- 500: red de seguridad ----------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest request) {
        // El stack trace completo va al log; al cliente solo un mensaje genérico.
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.", request);
    }

    // -------------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje, HttpServletRequest request) {
        return responder(status, mensaje, request, List.of());
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje,
                                                    HttpServletRequest request, List<String> detalles) {
        ErrorResponse cuerpo = new ErrorResponse(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.name(),
                mensaje,
                request.getRequestURI(),
                detalles
        );
        return ResponseEntity.status(status).body(cuerpo);
    }

    private static String valoresDe(Class<?> tipoEnum) {
        return Arrays.toString(tipoEnum.getEnumConstants());
    }
}
