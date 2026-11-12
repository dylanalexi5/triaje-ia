package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.OrigenAnalisis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasAnalizadorTicketServiceTest {

    private final ReglasAnalizadorTicketService analizador = new ReglasAnalizadorTicketService();

    @Test
    void elEjemploDelEnunciadoSaleConectividadAlta() {
        AnalisisTicketResponse analisis = analizador.analizar(
                "Problema con internet",
                "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.");

        assertThat(analisis.categoria()).isEqualTo("CONECTIVIDAD");
        assertThat(analisis.prioridad()).isEqualTo("ALTA");
        assertThat(analisis.etiquetas()).startsWith("internet", "conexión").contains("reunión");
    }

    @Test
    void contrasenaYLoginSonAccesos() {
        AnalisisTicketResponse analisis = analizador.analizar(
                "No puedo entrar", "Olvidé mi contraseña y el login del portal me rechaza siempre.");

        assertThat(analisis.categoria()).isEqualTo("ACCESOS");
    }

    @Test
    void produccionDetenidaEsCritica() {
        AnalisisTicketResponse analisis = analizador.analizar(
                "Servidor de facturación", "Producción detenida, el sistema de facturación no responde.");

        assertThat(analisis.prioridad()).isEqualTo("CRITICA");
    }

    @Test
    void noEsUrgenteNoSeConfundeConUrgente() {
        AnalisisTicketResponse analisis = analizador.analizar(
                "Cambio de teclado", "Algunas teclas del teclado se pegan, no es urgente, cuando puedan lo revisan.");

        assertThat(analisis.categoria()).isEqualTo("HARDWARE");
        assertThat(analisis.prioridad()).isEqualTo("BAJA");
    }

    @Test
    void lasPalabrasSeComparanCompletas() {
        // "redactar" contiene "red", pero no es un problema de red.
        AnalisisTicketResponse analisis = analizador.analizar(
                "Ayuda con documento", "Necesito ayuda para redactar un documento para mañana.");

        assertThat(analisis.categoria()).isEqualTo("OTRO");
        assertThat(analisis.prioridad()).isEqualTo("MEDIA");
        assertThat(analisis.etiquetas()).containsExactly("otro");
    }

    @Test
    void elResumenEsLaPrimeraOracion() {
        AnalisisTicketResponse analisis = analizador.analizar(
                "Impresora", "La impresora del piso 3 no imprime. Ya la reinicié dos veces.");

        assertThat(analisis.resumen()).isEqualTo("La impresora del piso 3 no imprime.");
    }

    @Test
    void siempreProduceUnaClasificacionValida() {
        AnalisisTicketResponse analisis = analizador.analizar("xxxxx", "yyyyyyyyyyyyyyyyyyyyyyyy");

        // Lo que sale de las reglas tiene que pasar el mismo filtro que la IA.
        Clasificacion clasificacion = NormalizadorAnalisis.normalizar(analisis, OrigenAnalisis.REGLAS);
        assertThat(clasificacion.etiquetas()).isNotEmpty();
    }
}
