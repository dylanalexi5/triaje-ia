package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.Estado;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Forma del ticket hacia afuera, basada en el ejemplo de la sección 17.
 *
 * Solicitante y técnico van como {id, nombre}: suficiente para mostrarlos
 * sin exponer correo ni rol en cada ticket. origenAnalisis no está en el
 * ejemplo del enunciado; se agrega para que se vea si clasificó la IA o
 * las reglas.
 */
public record TicketResponse(
        Long id,
        String titulo,
        String descripcion,
        Categoria categoria,
        Prioridad prioridad,
        Estado estado,
        String resumenIA,
        List<String> etiquetasIA,
        OrigenAnalisis origenAnalisis,
        UsuarioResumen solicitante,
        UsuarioResumen tecnicoAsignado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {

    public record UsuarioResumen(Long id, String nombre) {
    }
}
