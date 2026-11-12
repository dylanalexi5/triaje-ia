package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Estado;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoRequest(

        @NotNull(message = "El estado es obligatorio")
        Estado estado
) {
}
