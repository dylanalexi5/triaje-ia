package com.campuslands.tickets.ai;

/**
 * Contrato de cualquier analizador de tickets (interfaz sugerida en el
 * enunciado, sección 7). Hoy hay dos implementaciones: una por reglas y
 * otra con un modelo de lenguaje. El resto del sistema no conoce a
 * ninguna de las dos; solo habla con ClasificadorTicket.
 */
public interface AnalizadorTicketService {

    AnalisisTicketResponse analizar(String titulo, String descripcion);
}
