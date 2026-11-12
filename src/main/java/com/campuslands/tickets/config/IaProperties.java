package com.campuslands.tickets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del proveedor de IA, leída de variables de entorno
 * (AI_API_KEY, AI_MODEL, AI_BASE_URL, AI_TIMEOUT_SECONDS).
 *
 * La API key nunca tiene valor por defecto: si falta, la app arranca igual
 * y clasifica con reglas.
 */
@ConfigurationProperties(prefix = "ia")
public record IaProperties(
        String apiKey,
        String modelo,
        String baseUrl,
        int timeoutSegundos
) {

    public boolean tieneApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Para que la key no termine en un log si alguien imprime la configuración. */
    @Override
    public String toString() {
        return "IaProperties[modelo=%s, baseUrl=%s, timeoutSegundos=%d, apiKey=%s]"
                .formatted(modelo, baseUrl, timeoutSegundos, tieneApiKey() ? "****" : "(vacía)");
    }
}
