package com.campuslands.tickets.ai;

import com.campuslands.tickets.config.IaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Prueba el cliente de Groq sin red: MockRestServiceServer responde en
 * lugar del proveedor. Así se puede simular lo que en la vida real es
 * difícil de provocar a voluntad (un 503, un modelo que responde basura).
 */
class GroqAnalizadorTicketServiceTest {

    private static final String URL = "http://ia.test/chat/completions";

    private MockRestServiceServer servidor;
    private GroqAnalizadorTicketService analizador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ia.test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        analizador = new GroqAnalizadorTicketService(builder.build(),
                new IaProperties("clave-de-prueba", "modelo-de-prueba", "http://ia.test", 5),
                JsonMapper.builder().build());
    }

    private static String respuestaDelModelo(String contenido) {
        String escapado = contenido.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return """
                {"id": "chatcmpl-1", "choices": [{"index": 0, "message": {"role": "assistant", "content": "%s"}}],
                 "usage": {"total_tokens": 120}}
                """.formatted(escapado);
    }

    @Test
    void enviaUnaPeticionDeterministaYConElTicketDelimitado() {
        servidor.expect(once(), requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer clave-de-prueba"))
                .andExpect(jsonPath("$.model").value("modelo-de-prueba"))
                .andExpect(jsonPath("$.temperature").value(0.0))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content", containsString("<ticket>\nTítulo: Sin internet")))
                .andRespond(withSuccess(respuestaDelModelo("""
                        {"categoria": "CONECTIVIDAD", "prioridad": "ALTA", "resumen": "Sin internet.", "etiquetas": ["internet"]}
                        """), MediaType.APPLICATION_JSON));

        AnalisisTicketResponse analisis = analizador.analizar("Sin internet", "No tengo internet desde esta mañana.");

        assertThat(analisis.categoria()).isEqualTo("CONECTIVIDAD");
        assertThat(analisis.etiquetas()).containsExactly("internet");
        servidor.verify();
    }

    @Test
    void toleraQueElModeloEnvuelvaElJsonEnMarkdown() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(respuestaDelModelo("""
                Claro, aquí está:
                ```json
                {"categoria": "software", "prioridad": "media", "resumen": "Excel se cierra.", "etiquetas": ["excel"]}
                ```
                """), MediaType.APPLICATION_JSON));

        AnalisisTicketResponse analisis = analizador.analizar("Excel", "Excel se cierra al abrir un archivo.");

        // Aquí todavía viene en minúscula: corregirlo es trabajo del normalizador.
        assertThat(analisis.categoria()).isEqualTo("software");
    }

    @Test
    void unaRespuestaQueNoEsJsonEsInvalida() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(
                respuestaDelModelo("Lo siento, no puedo ayudar con eso."), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> analizador.analizar("Título", "Descripción del problema."))
                .isInstanceOf(AnalisisInvalidoException.class);
    }

    @Test
    void unErrorPasajeroSeReintentaUnaVez() {
        servidor.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor.expect(once(), requestTo(URL)).andRespond(withSuccess(respuestaDelModelo("""
                {"categoria": "OTRO", "prioridad": "BAJA", "resumen": "Consulta.", "etiquetas": ["consulta"]}
                """), MediaType.APPLICATION_JSON));

        assertThat(analizador.analizar("Título", "Descripción del problema.").categoria()).isEqualTo("OTRO");
        servidor.verify();
    }

    @Test
    void siElErrorPersisteSeRindeTrasDosIntentos() {
        servidor.expect(times(2), requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> analizador.analizar("Título", "Descripción del problema."))
                .isInstanceOf(ProveedorIaException.class)
                .hasMessageContaining("503");
        servidor.verify();
    }

    @Test
    void unaKeyInvalidaNoSeReintenta() {
        servidor.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> analizador.analizar("Título", "Descripción del problema."))
                .isInstanceOf(ProveedorIaException.class)
                .hasMessageContaining("401");
        servidor.verify();
    }

    @Test
    void sinApiKeyNiSiquieraLlamaAlProveedor() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer sinLlamadas = MockRestServiceServer.bindTo(builder).build();
        sinLlamadas.expect(never(), requestTo(URL));
        var sinKey = new GroqAnalizadorTicketService(builder.build(),
                new IaProperties("", "modelo", "http://ia.test", 5), JsonMapper.builder().build());

        assertThatThrownBy(() -> sinKey.analizar("Título", "Descripción del problema."))
                .isInstanceOf(ProveedorIaException.class)
                .hasMessageContaining("AI_API_KEY");
        sinLlamadas.verify();
    }
}
