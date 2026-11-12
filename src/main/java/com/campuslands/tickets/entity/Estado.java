package com.campuslands.tickets.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de un ticket.
 *
 * Las transiciones están restringidas a propósito (ver DECISIONS.md, D-12):
 * un ticket CERRADO no se reabre, y no se puede saltar de ABIERTO a RESUELTO
 * sin que alguien lo haya tomado.
 */
public enum Estado {
    ABIERTO,
    EN_PROCESO,
    RESUELTO,
    CERRADO;

    public boolean puedePasarA(Estado destino) {
        return siguientesPermitidos().contains(destino);
    }

    private Set<Estado> siguientesPermitidos() {
        // Un switch sobre el enum en vez de un mapa estático: así el
        // compilador obliga a decidir qué pasa si algún día se agrega un estado.
        return switch (this) {
            case ABIERTO -> EnumSet.of(EN_PROCESO, CERRADO);
            case EN_PROCESO -> EnumSet.of(RESUELTO, ABIERTO);
            case RESUELTO -> EnumSet.of(CERRADO, EN_PROCESO);
            case CERRADO -> EnumSet.noneOf(Estado.class);
        };
    }
}
