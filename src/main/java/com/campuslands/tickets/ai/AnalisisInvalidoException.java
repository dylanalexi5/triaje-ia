package com.campuslands.tickets.ai;

/**
 * El analizador respondió algo que no se puede usar: una categoría que no
 * existe, un resumen vacío, cero etiquetas... No llega al cliente; quien la
 * atrapa decide qué hacer (con la IA: usar el respaldo por reglas).
 */
public class AnalisisInvalidoException extends RuntimeException {

    public AnalisisInvalidoException(String mensaje) {
        super(mensaje);
    }
}
