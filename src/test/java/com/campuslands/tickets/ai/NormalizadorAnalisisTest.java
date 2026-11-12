package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Simula respuestas "sucias" como las que de verdad devuelve un modelo de
 * lenguaje, y comprueba que solo pasa lo que se puede arreglar sin inventar.
 */
class NormalizadorAnalisisTest {

    @Test
    void arreglaMayusculasEspaciosYTildes() {
        var sucio = new AnalisisTicketResponse(" conectividad ", "Crítica", "  Sin internet.  ", List.of("Internet"));

        Clasificacion limpio = NormalizadorAnalisis.normalizar(sucio, OrigenAnalisis.IA);

        assertThat(limpio.categoria()).isEqualTo(Categoria.CONECTIVIDAD);
        assertThat(limpio.prioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(limpio.resumen()).isEqualTo("Sin internet.");
        assertThat(limpio.origen()).isEqualTo(OrigenAnalisis.IA);
    }

    @Test
    void rechazaUnaCategoriaInventadaEnVezDeRellenarla() {
        var inventado = new AnalisisTicketResponse("REDES", "ALTA", "Resumen", List.of("red"));

        assertThatThrownBy(() -> NormalizadorAnalisis.normalizar(inventado, OrigenAnalisis.IA))
                .isInstanceOf(AnalisisInvalidoException.class)
                .hasMessageContaining("categoria");
    }

    @Test
    void rechazaUnaPrioridadFaltante() {
        var incompleto = new AnalisisTicketResponse("SOFTWARE", null, "Resumen", List.of("app"));

        assertThatThrownBy(() -> NormalizadorAnalisis.normalizar(incompleto, OrigenAnalisis.IA))
                .isInstanceOf(AnalisisInvalidoException.class);
    }

    @Test
    void recortaElResumenATreintaPalabras() {
        String cuarentaPalabras = IntStream.rangeClosed(1, 40).mapToObj(i -> "palabra" + i)
                .collect(Collectors.joining(" "));
        var largo = new AnalisisTicketResponse("OTRO", "BAJA", cuarentaPalabras, List.of("x"));

        String resumen = NormalizadorAnalisis.normalizar(largo, OrigenAnalisis.IA).resumen();

        assertThat(resumen.split(" ")).hasSize(30);
        assertThat(Arrays.asList(resumen.split(" "))).last().isEqualTo("palabra30");
    }

    @Test
    void limpiaDeduplicaYLimitaLasEtiquetas() {
        var etiquetas = Arrays.asList("#Internet", "internet", " WIFI ", null, "", "red", "vpn", "router", "extra",
                "una etiqueta absurdamente larga que no cabe en la columna");

        List<String> limpias = NormalizadorAnalisis.limpiarEtiquetas(etiquetas);

        assertThat(limpias).containsExactly("internet", "wifi", "red", "vpn", "router");
    }

    @Test
    void sinNingunaEtiquetaUtilizableEsInvalido() {
        assertThatThrownBy(() -> NormalizadorAnalisis.limpiarEtiquetas(List.of("  ", "#")))
                .isInstanceOf(AnalisisInvalidoException.class);
    }
}
