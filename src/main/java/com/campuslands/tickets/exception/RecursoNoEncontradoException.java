package com.campuslands.tickets.exception;

/**
 * Una sola excepción para "no existe" en vez de una por entidad: el
 * mensaje ya dice qué faltó, y el handler la traduce siempre a 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super("No se encontró el %s con id %d".formatted(recurso, id));
    }
}
