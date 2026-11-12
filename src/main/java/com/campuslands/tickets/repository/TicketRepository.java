package com.campuslands.tickets.repository;

import com.campuslands.tickets.entity.Ticket;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * Los filtros de GET /api/tickets son todos opcionales y combinables.
 * Con métodos derivados saldrían 16 combinaciones (findByEstadoAndPrioridad...);
 * con Specifications cada filtro es una pieza que se suma solo si viene
 * (ver TicketSpecifications).
 *
 * El EntityGraph trae solicitante, técnico y etiquetas en la misma consulta.
 * Sin él, listar 50 tickets dispararía más de 100 consultas extra (N+1).
 */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Override
    @EntityGraph(attributePaths = {"usuarioSolicitante", "tecnicoAsignado", "etiquetasIA"})
    Optional<Ticket> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"usuarioSolicitante", "tecnicoAsignado", "etiquetasIA"})
    List<Ticket> findAll(Specification<Ticket> filtros, Sort orden);
}
