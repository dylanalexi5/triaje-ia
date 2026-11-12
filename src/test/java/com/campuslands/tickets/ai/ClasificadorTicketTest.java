package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba de "clasificación con inteligencia artificial" (sección 12).
 * El modelo está simulado con Mockito: la prueba no depende de Groq, no
 * cuesta dinero y da siempre el mismo resultado.
 */
class ClasificadorTicketTest {

    private static final String TITULO = "Problema con internet";
    private static final String DESCRIPCION =
            "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.";

    private final AnalizadorTicketService ia = mock(AnalizadorTicketService.class);
    private final ClasificadorTicket clasificador = new ClasificadorTicket(ia, new ReglasAnalizadorTicketService());

    @Test
    void usaElResultadoDeLaIaYloNormaliza() {
        when(ia.analizar(TITULO, DESCRIPCION)).thenReturn(new AnalisisTicketResponse(
                "conectividad", "Alta", "Usuario sin conexión a internet antes de una reunión importante.",
                List.of("Internet", "conectividad", "reunión", "internet")));

        Clasificacion clasificacion = clasificador.clasificar(TITULO, DESCRIPCION);

        assertThat(clasificacion.origen()).isEqualTo(OrigenAnalisis.IA);
        assertThat(clasificacion.categoria()).isEqualTo(Categoria.CONECTIVIDAD);
        assertThat(clasificacion.prioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(clasificacion.resumen()).isEqualTo("Usuario sin conexión a internet antes de una reunión importante.");
        assertThat(clasificacion.etiquetas()).containsExactly("internet", "conectividad", "reunión");
    }

    @Test
    void siElProveedorFallaUsaLasReglas() {
        when(ia.analizar(anyString(), anyString())).thenThrow(new ProveedorIaException("timeout"));

        Clasificacion clasificacion = clasificador.clasificar(TITULO, DESCRIPCION);

        assertThat(clasificacion.origen()).isEqualTo(OrigenAnalisis.REGLAS);
        assertThat(clasificacion.categoria()).isEqualTo(Categoria.CONECTIVIDAD);
        assertThat(clasificacion.prioridad()).isEqualTo(Prioridad.ALTA);
    }

    @Test
    void siElModeloInventaUnaCategoriaNoSeGuardaSinoQueUsaLasReglas() {
        when(ia.analizar(anyString(), anyString())).thenReturn(new AnalisisTicketResponse(
                "INTERNET_LENTO", "ALTA", "Resumen.", List.of("internet")));

        Clasificacion clasificacion = clasificador.clasificar(TITULO, DESCRIPCION);

        assertThat(clasificacion.origen()).isEqualTo(OrigenAnalisis.REGLAS);
        assertThat(clasificacion.categoria()).isEqualTo(Categoria.CONECTIVIDAD);
    }

    @Test
    void unErrorInesperadoTampocoImpideClasificar() {
        when(ia.analizar(anyString(), anyString())).thenThrow(new IllegalStateException("algo raro"));

        assertThat(clasificador.clasificar(TITULO, DESCRIPCION).origen()).isEqualTo(OrigenAnalisis.REGLAS);
    }
}
