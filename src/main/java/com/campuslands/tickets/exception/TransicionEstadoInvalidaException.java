package com.campuslands.tickets.exception;

import com.campuslands.tickets.entity.Estado;

public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(Estado actual, Estado destino) {
        super("No se puede pasar un ticket de %s a %s".formatted(actual, destino));
    }
}
