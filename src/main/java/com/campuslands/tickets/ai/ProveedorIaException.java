package com.campuslands.tickets.ai;

/**
 * El proveedor de IA no respondió: timeout, error HTTP, sin API key, etc.
 * Igual que AnalisisInvalidoException, no llega al cliente: ClasificadorTicket
 * la atrapa y usa el respaldo por reglas.
 */
public class ProveedorIaException extends RuntimeException {

    public ProveedorIaException(String mensaje) {
        super(mensaje);
    }

    public ProveedorIaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
