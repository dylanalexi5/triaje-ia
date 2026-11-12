package com.campuslands.tickets.controller;

import com.campuslands.tickets.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.campuslands.tickets.dto.ActualizarEstadoRequest;
import com.campuslands.tickets.dto.AsignarTecnicoRequest;
import com.campuslands.tickets.dto.TicketCreateRequest;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.Estado;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Tickets", description = "Registro, clasificación con IA y ciclo de vida de los tickets")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Crear un ticket y clasificarlo con IA",
            description = "Si el proveedor de IA falla, el ticket igual se crea con el clasificador por reglas (origenAnalisis = REGLAS).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket creado y clasificado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "El usuario solicitante no existe", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody TicketCreateRequest request) {
        TicketResponse creado = ticketService.crear(request);

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Consultar un ticket, con su análisis de IA")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket encontrado"),
            @ApiResponse(responseCode = "404", description = "El ticket no existe o fue eliminado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public TicketResponse buscarPorId(@PathVariable Long id) {
        return ticketService.buscarPorId(id);
    }

    @Operation(summary = "Cambiar el estado de un ticket",
            description = "Transiciones permitidas: ABIERTO → EN_PROCESO | CERRADO; EN_PROCESO → RESUELTO | ABIERTO; RESUELTO → CERRADO | EN_PROCESO. CERRADO es final.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado vacío o inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "El ticket no existe", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Transición no permitida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/estado")
    public TicketResponse actualizarEstado(@PathVariable Long id,
                                           @Valid @RequestBody ActualizarEstadoRequest request) {
        return ticketService.actualizarEstado(id, request.estado());
    }

    @Operation(summary = "Asignar un técnico (solo rol SOPORTE)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Técnico asignado"),
            @ApiResponse(responseCode = "400", description = "El usuario no tiene rol SOPORTE", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "El ticket o el técnico no existen", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El ticket está CERRADO", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/asignar")
    public TicketResponse asignarTecnico(@PathVariable Long id,
                                         @Valid @RequestBody AsignarTecnicoRequest request) {
        return ticketService.asignarTecnico(id, request.tecnicoId());
    }

    @Operation(summary = "Eliminar un ticket (eliminación lógica)",
            description = "El registro se conserva en la base para el historial, pero deja de aparecer en la API.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ticket eliminado"),
            @ApiResponse(responseCode = "404", description = "El ticket no existe o ya fue eliminado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ticketService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Ejemplo: GET /api/tickets?estado=ABIERTO&prioridad=ALTA. Todos los filtros son opcionales. */
    @Operation(summary = "Listar tickets con filtros opcionales y combinables")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tickets, los más recientes primero"),
            @ApiResponse(responseCode = "400", description = "Algún filtro tiene un valor inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<TicketResponse> listar(@RequestParam(required = false) Estado estado,
                                       @RequestParam(required = false) Categoria categoria,
                                       @RequestParam(required = false) Prioridad prioridad,
                                       @RequestParam(required = false) Long usuarioId) {
        return ticketService.listar(estado, categoria, prioridad, usuarioId);
    }
}
