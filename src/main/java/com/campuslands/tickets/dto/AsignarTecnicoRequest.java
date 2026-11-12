package com.campuslands.tickets.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarTecnicoRequest(

        @NotNull(message = "El técnico es obligatorio")
        @Positive(message = "El id del técnico debe ser un número positivo")
        Long tecnicoId
) {
}
