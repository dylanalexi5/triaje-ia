package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.OrigenAnalisis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Punto único de entrada a la clasificación. TicketService habla con esta
 * clase y no sabe qué analizador hay detrás (DECISIONS.md, D-17).
 *
 * Primero intenta con el modelo de lenguaje. Si el proveedor no responde,
 * tarda más del límite o devuelve algo que no pasa la validación, usa el
 * analizador por reglas. Crear un ticket nunca falla por culpa de la IA.
 */
@Component
public class ClasificadorTicket {

    private static final Logger log = LoggerFactory.getLogger(ClasificadorTicket.class);

    private final AnalizadorTicketService analizadorIa;
    private final AnalizadorTicketService analizadorReglas;

    public ClasificadorTicket(@Qualifier("analizadorIa") AnalizadorTicketService analizadorIa,
                              ReglasAnalizadorTicketService analizadorReglas) {
        this.analizadorIa = analizadorIa;
        this.analizadorReglas = analizadorReglas;
    }

    public Clasificacion clasificar(String titulo, String descripcion) {
        try {
            AnalisisTicketResponse analisis = analizadorIa.analizar(titulo, descripcion);
            return NormalizadorAnalisis.normalizar(analisis, OrigenAnalisis.IA);

        } catch (RuntimeException e) {
            // Se atrapa cualquier RuntimeException, no solo las nuestras: un
            // error inesperado en el camino de la IA tampoco debe impedir que
            // el empleado registre su problema. Al log va el motivo, nunca el
            // texto del ticket (puede traer datos personales).
            log.warn("Clasificación con IA no disponible, se usa el respaldo por reglas. Motivo: {}",
                    e.getMessage());
            AnalisisTicketResponse analisis = analizadorReglas.analizar(titulo, descripcion);
            return NormalizadorAnalisis.normalizar(analisis, OrigenAnalisis.REGLAS);
        }
    }
}
