package com.campuslands.tickets.controller;

import com.campuslands.tickets.dto.TicketCreateRequest;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.Estado;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody TicketCreateRequest request) {
        TicketResponse creado = ticketService.crear(request);

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/{id}")
    public TicketResponse buscarPorId(@PathVariable Long id) {
        return ticketService.buscarPorId(id);
    }

    /** Ejemplo: GET /api/tickets?estado=ABIERTO&prioridad=ALTA. Todos los filtros son opcionales. */
    @GetMapping
    public List<TicketResponse> listar(@RequestParam(required = false) Estado estado,
                                       @RequestParam(required = false) Categoria categoria,
                                       @RequestParam(required = false) Prioridad prioridad,
                                       @RequestParam(required = false) Long usuarioId) {
        return ticketService.listar(estado, categoria, prioridad, usuarioId);
    }
}
