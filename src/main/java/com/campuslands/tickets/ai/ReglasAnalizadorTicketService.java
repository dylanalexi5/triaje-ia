package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.Prioridad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Clasificador local por palabras clave (opción B del enunciado).
 *
 * No necesita red ni API key y siempre responde, por eso es el respaldo
 * cuando el modelo de lenguaje falla. Es deliberadamente simple: no
 * entiende el lenguaje, solo busca palabras. Contempla frases explícitas
 * como "no es urgente", pero no negaciones en general ("no creo que sea
 * urgente"); eso queda en las limitaciones del README.
 */
@Component
public class ReglasAnalizadorTicketService implements AnalizadorTicketService {

    /**
     * Cada regla tiene un patrón (sobre texto en minúscula y sin tildes) y la
     * etiqueta "bonita" que se muestra si coincide. Los patrones van con \b
     * a ambos lados: "red" no debe coincidir con "redactar".
     */
    private record Regla(Pattern patron, String etiqueta) {

        static Regla de(String regex, String etiqueta) {
            return new Regla(Pattern.compile("\\b(" + regex + ")\\b"), etiqueta);
        }

        boolean coincideCon(String texto) {
            return patron.matcher(texto).find();
        }
    }

    private static final Map<Categoria, List<Regla>> REGLAS_CATEGORIA = new EnumMap<>(Map.of(
            Categoria.CONECTIVIDAD, List.of(
                    Regla.de("internet", "internet"),
                    Regla.de("wifi|wi-fi", "wifi"),
                    Regla.de("red|redes", "red"),
                    Regla.de("vpn", "vpn"),
                    Regla.de("conect\\w*|conexion", "conexión"),
                    Regla.de("router|cable de red", "router")),
            Categoria.ACCESOS, List.of(
                    Regla.de("contrasena|clave", "contraseña"),
                    Regla.de("login|iniciar sesion|inicio de sesion", "inicio de sesión"),
                    Regla.de("permiso|permisos", "permisos"),
                    Regla.de("usuario bloqueado|cuenta bloqueada|acceso denegado", "acceso bloqueado"),
                    Regla.de("acceso|accesos", "acceso")),
            Categoria.HARDWARE, List.of(
                    Regla.de("pantalla|monitor", "pantalla"),
                    Regla.de("teclado", "teclado"),
                    Regla.de("mouse|raton", "mouse"),
                    Regla.de("computador|computadora|portatil|equipo", "computador"),
                    Regla.de("impresora", "impresora"),
                    Regla.de("bateria|cargador", "batería")),
            Categoria.SOFTWARE, List.of(
                    Regla.de("programa|software", "software"),
                    Regla.de("aplicacion|app", "aplicación"),
                    Regla.de("error|falla|se cierra|no abre", "error"),
                    Regla.de("instalar|instalacion|actualizacion|actualizar", "instalación"),
                    Regla.de("excel|word|outlook|office", "office"))
    ));

    // Se revisan en este orden y la primera que coincide gana. BAJA va antes
    // que ALTA porque "no es urgente" contiene la palabra "urgente".
    private static final Map<Prioridad, List<Regla>> REGLAS_PRIORIDAD = new EnumMap<>(Map.of(
            Prioridad.CRITICA, List.of(
                    Regla.de("produccion detenida|produccion caida", "producción detenida"),
                    Regla.de("todos los usuarios|toda la empresa|toda la oficina", "impacto general"),
                    Regla.de("servidor caido|sistema caido", "caída del sistema")),
            Prioridad.ALTA, List.of(
                    Regla.de("urgente|urgencia|lo antes posible", "urgente"),
                    Regla.de("no puedo trabajar|bloqueado|bloqueada", "bloqueo"),
                    Regla.de("reunion", "reunión"),
                    Regla.de("en \\d+ minutos", "tiempo limitado"),
                    Regla.de("cliente|clientes", "cliente")),
            Prioridad.BAJA, List.of(
                    Regla.de("cuando puedan|no es urgente|sugerencia|cuando tengan tiempo", "sin urgencia"))
    ));

    private static final List<Prioridad> ORDEN_PRIORIDAD =
            List.of(Prioridad.CRITICA, Prioridad.BAJA, Prioridad.ALTA);

    @Override
    public AnalisisTicketResponse analizar(String titulo, String descripcion) {
        String texto = NormalizadorAnalisis.sinTildes(titulo + " " + descripcion).toLowerCase(Locale.ROOT);

        Map<Categoria, List<String>> coincidencias = buscarPorCategoria(texto);
        Categoria categoria = elegirCategoria(coincidencias);

        // Primero las etiquetas de la categoría ganadora, luego las de
        // prioridad y al final el resto: el normalizador corta en 5.
        Set<String> etiquetas = new LinkedHashSet<>(coincidencias.getOrDefault(categoria, List.of()));
        Prioridad prioridad = elegirPrioridad(texto, etiquetas);
        coincidencias.values().forEach(etiquetas::addAll);

        if (etiquetas.isEmpty()) {
            etiquetas.add(categoria.name().toLowerCase(Locale.ROOT));
        }

        return new AnalisisTicketResponse(
                categoria.name(),
                prioridad.name(),
                primeraOracion(descripcion),
                new ArrayList<>(etiquetas)
        );
    }

    private Map<Categoria, List<String>> buscarPorCategoria(String texto) {
        Map<Categoria, List<String>> coincidencias = new EnumMap<>(Categoria.class);
        REGLAS_CATEGORIA.forEach((categoria, reglas) -> {
            List<String> encontradas = reglas.stream()
                    .filter(regla -> regla.coincideCon(texto))
                    .map(Regla::etiqueta)
                    .toList();
            if (!encontradas.isEmpty()) {
                coincidencias.put(categoria, encontradas);
            }
        });
        return coincidencias;
    }

    /**
     * La categoría con más palabras clave encontradas; OTRO si no hay
     * ninguna. En empate gana la que va primero en el enum (EnumMap
     * recorre en ese orden).
     */
    private Categoria elegirCategoria(Map<Categoria, List<String>> coincidencias) {
        Categoria mejor = Categoria.OTRO;
        int mejorPuntaje = 0;
        for (Map.Entry<Categoria, List<String>> entrada : coincidencias.entrySet()) {
            if (entrada.getValue().size() > mejorPuntaje) {
                mejor = entrada.getKey();
                mejorPuntaje = entrada.getValue().size();
            }
        }
        return mejor;
    }

    private Prioridad elegirPrioridad(String texto, Set<String> etiquetas) {
        for (Prioridad prioridad : ORDEN_PRIORIDAD) {
            List<String> encontradas = REGLAS_PRIORIDAD.get(prioridad).stream()
                    .filter(regla -> regla.coincideCon(texto))
                    .map(Regla::etiqueta)
                    .toList();

            if (!encontradas.isEmpty()) {
                etiquetas.addAll(encontradas);
                return prioridad;
            }
        }
        return Prioridad.MEDIA;
    }

    /**
     * Las reglas no "entienden" el texto, así que el resumen es la primera
     * oración de la descripción. El normalizador se encarga del límite de
     * 30 palabras.
     */
    private String primeraOracion(String descripcion) {
        return descripcion.strip().split("(?<=[.!?])\\s+", 2)[0];
    }
}
