package com.campuslands.tickets.repository;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.Estado;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.entity.Ticket;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Cada filtro de GET /api/tickets es una pieza independiente; solo se
 * suman las que vienen en la petición. Agregar un filtro nuevo es agregar
 * un método aquí, sin tocar el repositorio.
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> conFiltros(Estado estado, Categoria categoria,
                                                   Prioridad prioridad, Long usuarioId) {
        List<Specification<Ticket>> filtros = new ArrayList<>();

        if (estado != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado));
        }
        if (categoria != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("categoria"), categoria));
        }
        if (prioridad != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("prioridad"), prioridad));
        }
        if (usuarioId != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("usuarioSolicitante").get("id"), usuarioId));
        }

        return Specification.allOf(filtros);
    }
}
