package com.campuslands.tickets.service;

import com.campuslands.tickets.ai.Clasificacion;
import com.campuslands.tickets.ai.ClasificadorTicket;
import com.campuslands.tickets.dto.TicketCreateRequest;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.Estado;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.entity.Ticket;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.RecursoNoEncontradoException;
import com.campuslands.tickets.repository.TicketRepository;
import com.campuslands.tickets.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClasificadorTicket clasificador;

    @InjectMocks
    private TicketService ticketService;

    private final Usuario laura = new Usuario("Laura Gómez", "laura@empresa.com", "hash", Rol.USUARIO);

    @Test
    void crear_guardaElTicketConElResultadoDelAnalisis() {
        var request = new TicketCreateRequest("Problema con internet",
                "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.", 1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(laura));
        when(clasificador.clasificar(request.titulo(), request.descripcion())).thenReturn(new Clasificacion(
                Categoria.CONECTIVIDAD, Prioridad.ALTA,
                "Usuario sin conexión a internet antes de una reunión importante.",
                List.of("internet", "conectividad", "reunión"), OrigenAnalisis.IA));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse respuesta = ticketService.crear(request);

        ArgumentCaptor<Ticket> guardado = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(guardado.capture());
        Ticket ticket = guardado.getValue();
        assertThat(ticket.getEstado()).isEqualTo(Estado.ABIERTO);
        assertThat(ticket.getCategoria()).isEqualTo(Categoria.CONECTIVIDAD);
        assertThat(ticket.getPrioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(ticket.getEtiquetasIA()).containsExactly("internet", "conectividad", "reunión");
        assertThat(ticket.getUsuarioSolicitante()).isSameAs(laura);

        assertThat(respuesta.solicitante().nombre()).isEqualTo("Laura Gómez");
        assertThat(respuesta.tecnicoAsignado()).isNull();
        assertThat(respuesta.origenAnalisis()).isEqualTo(OrigenAnalisis.IA);
    }

    @Test
    void crear_conSolicitanteInexistenteFallaSinGastarUnaClasificacion() {
        var request = new TicketCreateRequest("Problema con internet", "Una descripción con suficiente largo.", 99L);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el usuario con id 99");
        verifyNoInteractions(clasificador, ticketRepository);
    }

    @Test
    void buscarPorId_inexistenteLanzaNoEncontrado() {
        when(ticketRepository.findById(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.buscarPorId(15L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ticket con id 15");
    }
}
