package com.campuslands.tickets.ai;

import java.util.List;

/**
 * Salida "en bruto" de un analizador, tal como la define el enunciado.
 *
 * Todo es String a propósito: un modelo de lenguaje puede devolver
 * cualquier cosa, y es NormalizadorAnalisis quien decide si sirve o no.
 */
public record AnalisisTicketResponse(
        String categoria,
        String prioridad,
        String resumen,
        List<String> etiquetas
) {
}
