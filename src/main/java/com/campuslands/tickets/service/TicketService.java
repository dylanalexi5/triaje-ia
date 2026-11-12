package com.campuslands.tickets.service;

import com.campuslands.tickets.ai.Clasificacion;
import com.campuslands.tickets.ai.ClasificadorTicket;
import com.campuslands.tickets.dto.TicketCreateRequest;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.Estado;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.entity.Ticket;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.RecursoNoEncontradoException;
import com.campuslands.tickets.mapper.TicketMapper;
import com.campuslands.tickets.repository.TicketRepository;
import com.campuslands.tickets.repository.TicketSpecifications;
import com.campuslands.tickets.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {

    private static final Sort MAS_RECIENTES_PRIMERO =
            Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClasificadorTicket clasificador;

    public TicketService(TicketRepository ticketRepository,
                         UsuarioRepository usuarioRepository,
                         ClasificadorTicket clasificador) {
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.clasificador = clasificador;
    }

    /**
     * Este método NO es @Transactional a propósito (DECISIONS.md, D-09).
     * Clasificar puede tardar varios segundos cuando hay un modelo de por
     * medio, y no tiene sentido tener una conexión de la base ocupada
     * mientras tanto. El orden es:
     *   1. validar que el solicitante existe (barato; si no existe, ni se clasifica)
     *   2. clasificar, sin transacción abierta
     *   3. guardar ticket y análisis juntos: save() abre su propia transacción corta
     */
    public TicketResponse crear(TicketCreateRequest request) {
        Usuario solicitante = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", request.usuarioId()));

        Clasificacion clasificacion = clasificador.clasificar(request.titulo(), request.descripcion());

        Ticket ticket = new Ticket(request.titulo(), request.descripcion(), solicitante);
        ticket.aplicarAnalisis(
                clasificacion.categoria(),
                clasificacion.prioridad(),
                clasificacion.resumen(),
                clasificacion.etiquetas(),
                clasificacion.origen()
        );

        return TicketMapper.aResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponse buscarPorId(Long id) {
        return TicketMapper.aResponse(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listar(Estado estado, Categoria categoria, Prioridad prioridad, Long usuarioId) {
        return ticketRepository
                .findAll(TicketSpecifications.conFiltros(estado, categoria, prioridad, usuarioId), MAS_RECIENTES_PRIMERO)
                .stream()
                .map(TicketMapper::aResponse)
                .toList();
    }

    private Ticket obtener(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ticket", id));
    }
}
