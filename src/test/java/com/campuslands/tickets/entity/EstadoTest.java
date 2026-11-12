package com.campuslands.tickets.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las 16 combinaciones origen → destino, escritas a mano. Si alguien cambia
 * una transición en Estado, esta tabla obliga a cambiarla aquí también:
 * es la documentación ejecutable de DECISIONS.md, D-12.
 */
class EstadoTest {

    @ParameterizedTest(name = "{0} → {1}: {2}")
    @CsvSource({
            "ABIERTO,    ABIERTO,    false",
            "ABIERTO,    EN_PROCESO, true",
            "ABIERTO,    RESUELTO,   false",
            "ABIERTO,    CERRADO,    true",
            "EN_PROCESO, ABIERTO,    true",
            "EN_PROCESO, EN_PROCESO, false",
            "EN_PROCESO, RESUELTO,   true",
            "EN_PROCESO, CERRADO,    false",
            "RESUELTO,   ABIERTO,    false",
            "RESUELTO,   EN_PROCESO, true",
            "RESUELTO,   RESUELTO,   false",
            "RESUELTO,   CERRADO,    true",
            "CERRADO,    ABIERTO,    false",
            "CERRADO,    EN_PROCESO, false",
            "CERRADO,    RESUELTO,   false",
            "CERRADO,    CERRADO,    false",
    })
    void transicionesPermitidas(Estado origen, Estado destino, boolean permitida) {
        assertThat(origen.puedePasarA(destino)).isEqualTo(permitida);
    }
}
