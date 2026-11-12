package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Convierte la salida en bruto de un analizador en una Clasificacion confiable.
 *
 * La regla es simple: lo que se puede arreglar sin inventar, se arregla
 * ("conectividad " → CONECTIVIDAD, "Crítica" → CRITICA, etiquetas repetidas);
 * lo que no, se rechaza con AnalisisInvalidoException. Nunca se rellena un
 * valor por defecto, porque eso escondería que el modelo falló
 * (DECISIONS.md, D-11).
 */
public final class NormalizadorAnalisis {

    static final int MAX_PALABRAS_RESUMEN = 30;
    static final int MAX_CARACTERES_RESUMEN = 300;
    static final int MAX_ETIQUETAS = 5;
    static final int MAX_CARACTERES_ETIQUETA = 40;

    private NormalizadorAnalisis() {
    }

    public static Clasificacion normalizar(AnalisisTicketResponse analisis, OrigenAnalisis origen) {
        if (analisis == null) {
            throw new AnalisisInvalidoException("El analizador no devolvió ningún resultado");
        }

        return new Clasificacion(
                aEnum(Categoria.class, analisis.categoria(), "categoria"),
                aEnum(Prioridad.class, analisis.prioridad(), "prioridad"),
                limpiarResumen(analisis.resumen()),
                limpiarEtiquetas(analisis.etiquetas()),
                origen
        );
    }

    static <E extends Enum<E>> E aEnum(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new AnalisisInvalidoException("Falta el campo '%s'".formatted(campo));
        }

        String limpio = sinTildes(valor.strip())
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]+", "_");

        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equals(limpio))
                .findFirst()
                .orElseThrow(() -> new AnalisisInvalidoException(
                        "Valor no permitido para '%s': %s".formatted(campo, valor)));
    }

    static String limpiarResumen(String resumen) {
        if (resumen == null || resumen.isBlank()) {
            throw new AnalisisInvalidoException("El resumen viene vacío");
        }

        String[] palabras = resumen.strip().split("\\s+");
        String recortado = palabras.length > MAX_PALABRAS_RESUMEN
                ? String.join(" ", Arrays.copyOf(palabras, MAX_PALABRAS_RESUMEN))
                : String.join(" ", palabras);

        // 30 palabras casi nunca pasan de 300 caracteres, pero la columna es
        // de 300 y no vale la pena que una palabra absurda tumbe el INSERT.
        return recortado.length() > MAX_CARACTERES_RESUMEN
                ? recortado.substring(0, MAX_CARACTERES_RESUMEN)
                : recortado;
    }

    static List<String> limpiarEtiquetas(List<String> etiquetas) {
        if (etiquetas == null) {
            throw new AnalisisInvalidoException("No vinieron etiquetas");
        }

        Set<String> unicas = new LinkedHashSet<>();
        for (String etiqueta : etiquetas) {
            if (etiqueta == null) {
                continue;
            }
            String limpia = etiqueta.strip()
                    .replaceFirst("^#+", "")
                    .replaceAll("\\s+", " ")
                    .toLowerCase(Locale.ROOT);

            if (!limpia.isEmpty() && limpia.length() <= MAX_CARACTERES_ETIQUETA) {
                unicas.add(limpia);
            }
            if (unicas.size() == MAX_ETIQUETAS) {
                break;
            }
        }

        if (unicas.isEmpty()) {
            throw new AnalisisInvalidoException("Ninguna etiqueta utilizable");
        }
        return List.copyOf(unicas);
    }

    static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
