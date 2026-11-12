package com.campuslands.tickets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarTecnicoRequest(

        @Schema(example = "2", description = "Id de un usuario con rol SOPORTE")
        @NotNull(message = "El técnico es obligatorio")
        @Positive(message = "El id del técnico debe ser un número positivo")
        Long tecnicoId
) {
}
