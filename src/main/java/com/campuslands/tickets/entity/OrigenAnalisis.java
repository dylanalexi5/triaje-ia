package com.campuslands.tickets.entity;

/**
 * Quién clasificó el ticket. Sirve para auditar cuántas veces se usó el
 * respaldo por reglas porque el proveedor de IA no respondió bien.
 */
public enum OrigenAnalisis {
    IA,
    REGLAS
}
