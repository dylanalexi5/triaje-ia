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
import com.campuslands.tickets.exception.TecnicoInvalidoException;
import com.campuslands.tickets.exception.TicketCerradoException;
import com.campuslands.tickets.exception.TransicionEstadoInvalidaException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    // --- Actualización de estado (sección 12) ---------------------------

    private Ticket ticketAbierto() {
        return new Ticket("Problema con internet", "No tengo internet desde esta mañana.", laura);
    }

    @Test
    void actualizarEstado_conUnaTransicionPermitidaCambiaElEstado() {
        Ticket ticket = ticketAbierto();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse respuesta = ticketService.actualizarEstado(1L, Estado.EN_PROCESO);

        assertThat(ticket.getEstado()).isEqualTo(Estado.EN_PROCESO);
        assertThat(respuesta.estado()).isEqualTo(Estado.EN_PROCESO);
        verify(ticketRepository).flush();
    }

    @Test
    void actualizarEstado_conUnaTransicionNoPermitidaFallaYNoCambiaNada() {
        Ticket ticket = ticketAbierto();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.actualizarEstado(1L, Estado.RESUELTO))
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessage("No se puede pasar un ticket de ABIERTO a RESUELTO");
        assertThat(ticket.getEstado()).isEqualTo(Estado.ABIERTO);
    }

    // --- Asignación de técnico (sección 12) -----------------------------

    @Test
    void asignarTecnico_conRolSoporteLoAsigna() {
        Ticket ticket = ticketAbierto();
        Usuario andres = new Usuario("Andrés Pérez", "andres@empresa.com", "hash", Rol.SOPORTE);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(andres));

        TicketResponse respuesta = ticketService.asignarTecnico(1L, 5L);

        assertThat(ticket.getTecnicoAsignado()).isSameAs(andres);
        assertThat(respuesta.tecnicoAsignado().nombre()).isEqualTo("Andrés Pérez");
    }

    @Test
    void asignarTecnico_sinRolSoporteSeRechazaYElTicketQuedaSinTecnico() {
        Ticket ticket = ticketAbierto();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(laura));

        assertThatThrownBy(() -> ticketService.asignarTecnico(1L, 2L))
                .isInstanceOf(TecnicoInvalidoException.class)
                .hasMessageContaining("rol USUARIO")
                .hasMessageContaining("solo un usuario con rol SOPORTE");
        assertThat(ticket.getTecnicoAsignado()).isNull();
        verify(ticketRepository, never()).flush();
    }

    @Test
    void asignarTecnico_inexistenteDevuelveNoEncontrado() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticketAbierto()));
        when(usuarioRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.asignarTecnico(1L, 77L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el usuario con id 77");
    }

    @Test
    void asignarTecnico_aUnTicketCerradoNoSePermite() {
        Ticket ticket = ticketAbierto();
        ticket.cambiarEstado(Estado.CERRADO);
        Usuario andres = new Usuario("Andrés Pérez", "andres@empresa.com", "hash", Rol.SOPORTE);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(andres));

        assertThatThrownBy(() -> ticketService.asignarTecnico(1L, 5L))
                .isInstanceOf(TicketCerradoException.class);
    }

    // --- Eliminación ----------------------------------------------------

    @Test
    void eliminar_esLogicaNoBorraLaFila() {
        Ticket ticket = ticketAbierto();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        ticketService.eliminar(1L);

        assertThat(ticket.estaEliminado()).isTrue();
        // Ningún delete de ningún tipo: solo se buscó el ticket y se marcó.
        verify(ticketRepository).findById(1L);
        verifyNoMoreInteractions(ticketRepository);
    }
}
