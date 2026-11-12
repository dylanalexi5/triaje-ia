package com.campuslands.tickets.repository;

import com.campuslands.tickets.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Los filtros de GET /api/tickets son todos opcionales y combinables.
 * Con métodos derivados saldrían 16 combinaciones (findByEstadoAndPrioridad...);
 * con Specifications cada filtro es una pieza que se suma solo si viene.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
}
