package com.campuslands.tickets.exception;

public class TicketCerradoException extends RuntimeException {

    public TicketCerradoException(Long ticketId) {
        super("El ticket con id %d está CERRADO y ya no admite cambios".formatted(ticketId));
    }
}
