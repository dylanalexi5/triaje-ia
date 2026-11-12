package com.campuslands.tickets.exception;

import com.campuslands.tickets.entity.Estado;

public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(Estado actual, Estado destino) {
        super(mensaje(actual, destino));
    }

    /**
     * Pedir el mismo estado en el que ya está el ticket casi siempre significa
     * que el cliente tiene datos viejos; el mensaje se lo dice tal cual en vez
     * del confuso "no se puede pasar de ABIERTO a ABIERTO".
     */
    private static String mensaje(Estado actual, Estado destino) {
        return actual == destino
                ? "El ticket ya está en estado %s".formatted(actual)
                : "No se puede pasar un ticket de %s a %s".formatted(actual, destino);
    }
}
