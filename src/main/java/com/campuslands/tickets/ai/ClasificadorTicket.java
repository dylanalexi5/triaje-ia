package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.OrigenAnalisis;
import org.springframework.stereotype.Component;

/**
 * Punto único de entrada a la clasificación. TicketService habla con esta
 * clase y no sabe qué analizador hay detrás (DECISIONS.md, D-17).
 *
 * Por ahora solo usa el analizador por reglas. Cuando entre el modelo de
 * lenguaje, esta clase lo intentará primero y caerá a las reglas si falla;
 * el resto del sistema no tendrá que cambiar.
 */
@Component
public class ClasificadorTicket {

    private final ReglasAnalizadorTicketService analizadorReglas;

    public ClasificadorTicket(ReglasAnalizadorTicketService analizadorReglas) {
        this.analizadorReglas = analizadorReglas;
    }

    public Clasificacion clasificar(String titulo, String descripcion) {
        AnalisisTicketResponse analisis = analizadorReglas.analizar(titulo, descripcion);
        return NormalizadorAnalisis.normalizar(analisis, OrigenAnalisis.REGLAS);
    }
}
