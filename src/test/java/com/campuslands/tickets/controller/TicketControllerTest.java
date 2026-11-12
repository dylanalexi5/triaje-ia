package com.campuslands.tickets.controller;

import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.dto.TicketResponse.UsuarioResumen;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.Estado;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.exception.RecursoNoEncontradoException;
import com.campuslands.tickets.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private static TicketResponse ticketDeEjemplo() {
        return new TicketResponse(25L, "Problema con internet",
                "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.",
                Categoria.CONECTIVIDAD, Prioridad.ALTA, Estado.ABIERTO,
                "Usuario sin conexión a internet antes de una reunión importante.",
                List.of("internet", "conectividad", "reunión"), OrigenAnalisis.IA,
                new UsuarioResumen(1L, "Laura Gómez"), null,
                LocalDateTime.of(2026, 7, 30, 19, 30), LocalDateTime.of(2026, 7, 30, 19, 30));
    }

    @Test
    void crear_devuelve201ConElFormatoDeLaSeccion17() throws Exception {
        when(ticketService.crear(any())).thenReturn(ticketDeEjemplo());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Problema con internet",
                                 "descripcion": "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.",
                                 "usuarioId": 1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tickets/25")))
                .andExpect(jsonPath("$.categoria").value("CONECTIVIDAD"))
                .andExpect(jsonPath("$.prioridad").value("ALTA"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.etiquetasIA[0]").value("internet"))
                .andExpect(jsonPath("$.solicitante.nombre").value("Laura Gómez"))
                // el enunciado muestra "tecnicoAsignado": null, así que tiene que estar presente
                .andExpect(jsonPath("$.tecnicoAsignado").isEmpty())
                .andExpect(jsonPath("$.fechaCreacion").value("2026-07-30T19:30:00"));
    }

    @Test
    void crear_conDatosInvalidosNoLlegaAlServicio() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Hola", "descripcion": "muy corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasItem("titulo: El título debe tener entre 5 y 100 caracteres")))
                .andExpect(jsonPath("$.detalles", hasItem("descripcion: La descripción debe tener entre 20 y 2000 caracteres")))
                .andExpect(jsonPath("$.detalles", hasItem("usuarioId: El usuario solicitante es obligatorio")));

        verifyNoInteractions(ticketService);
    }

    @Test
    void buscarPorId_inexistenteDevuelve404ConElFormatoDelEnunciado() throws Exception {
        when(ticketService.buscarPorId(15L)).thenThrow(new RecursoNoEncontradoException("ticket", 15L));

        mockMvc.perform(get("/api/tickets/15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No se encontró el ticket con id 15"))
                .andExpect(jsonPath("$.path").value("/api/tickets/15"));
    }

    @Test
    void listar_pasaLosFiltrosAlServicio() throws Exception {
        when(ticketService.listar(Estado.ABIERTO, null, Prioridad.ALTA, null)).thenReturn(List.of(ticketDeEjemplo()));

        mockMvc.perform(get("/api/tickets").param("estado", "ABIERTO").param("prioridad", "ALTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(25));

        verify(ticketService).listar(Estado.ABIERTO, null, Prioridad.ALTA, null);
    }

    @Test
    void listar_conUnEstadoQueNoExisteExplicaLosValoresPermitidos() throws Exception {
        mockMvc.perform(get("/api/tickets").param("estado", "PENDIENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        containsString("[ABIERTO, EN_PROCESO, RESUELTO, CERRADO]")));
    }
}
