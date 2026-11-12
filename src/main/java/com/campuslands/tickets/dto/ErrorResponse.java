package com.campuslands.tickets.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato único de error de toda la API (sección 8 del enunciado).
 *
 * "detalles" solo aparece cuando hay algo que detallar, por ejemplo la
 * lista de campos que fallaron la validación.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> detalles
) {
}
