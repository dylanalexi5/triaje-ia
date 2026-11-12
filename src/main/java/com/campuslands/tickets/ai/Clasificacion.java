package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;

import java.util.List;

/**
 * Resultado ya validado y listo para guardar. Si existe una Clasificacion,
 * sus valores pasaron por la normalización: no hay forma de construir una
 * con una categoría inventada por el modelo.
 */
public record Clasificacion(
        Categoria categoria,
        Prioridad prioridad,
        String resumen,
        List<String> etiquetas,
        OrigenAnalisis origen
) {
}
