package com.campuslands.tickets.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class IaConfig {

    /**
     * Cliente HTTP exclusivo para el proveedor de IA.
     *
     * - Timeout de conexión y de respuesta explícitos: si el proveedor se
     *   cuelga, la creación del ticket no se queda esperando para siempre.
     * - ProxySelector.getDefault() respeta https.proxyHost si la máquina
     *   sale a internet por un proxy; si no hay proxy, conecta directo.
     * - User-Agent propio: Cloudflare (delante de Groq) rechaza con 403 los
     *   User-Agent genéricos de algunas librerías HTTP. Se comprobó probando
     *   el prompt antes de escribir este código.
     *
     * Se usa RestClient.builder() de Spring Framework y no el builder que
     * autoconfigura Boot: en Boot 4 ese vive en otro módulo, y este cliente
     * no necesita nada de él porque define todo lo suyo aquí.
     */
    @Bean
    public RestClient clienteIa(IaProperties propiedades) {
        Duration timeout = Duration.ofSeconds(propiedades.timeoutSegundos());

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .proxy(ProxySelector.getDefault())
                .build();

        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(timeout);

        return RestClient.builder()
                .requestFactory(fabrica)
                .baseUrl(propiedades.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, "triaje-ia/0.1")
                .build();
    }
}
