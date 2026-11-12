package com.campuslands.tickets.mapper;

import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.dto.TicketResponse.UsuarioResumen;
import com.campuslands.tickets.entity.Ticket;
import com.campuslands.tickets.entity.Usuario;

public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketResponse aResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitulo(),
                ticket.getDescripcion(),
                ticket.getCategoria(),
                ticket.getPrioridad(),
                ticket.getEstado(),
                ticket.getResumenIA(),
                ticket.getEtiquetasIA(),
                ticket.getOrigenAnalisis(),
                resumen(ticket.getUsuarioSolicitante()),
                resumen(ticket.getTecnicoAsignado()),
                ticket.getFechaCreacion(),
                ticket.getFechaActualizacion()
        );
    }

    private static UsuarioResumen resumen(Usuario usuario) {
        return usuario == null ? null : new UsuarioResumen(usuario.getId(), usuario.getNombre());
    }
}
