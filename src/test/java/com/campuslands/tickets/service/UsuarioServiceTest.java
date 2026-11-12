package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.UsuarioCreateRequest;
import com.campuslands.tickets.dto.UsuarioResponse;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.CorreoDuplicadoException;
import com.campuslands.tickets.exception.RecursoNoEncontradoException;
import com.campuslands.tickets.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void registrar_guardaElHashYNuncaLaContrasenaEnTextoPlano() {
        var request = new UsuarioCreateRequest("  Laura Gómez ", " Laura@Empresa.com ", "secreta123", Rol.SOPORTE);
        when(usuarioRepository.existsByCorreoIgnoreCase("laura@empresa.com")).thenReturn(false);
        when(passwordEncoder.encode("secreta123")).thenReturn("$2a$10$hash");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse respuesta = usuarioService.registrar(request);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(guardado.getValue().getNombre()).isEqualTo("Laura Gómez");
        assertThat(respuesta.correo()).isEqualTo("laura@empresa.com");
        assertThat(respuesta.rol()).isEqualTo(Rol.SOPORTE);
    }

    @Test
    void registrar_sinRolAsignaUsuarioPorDefecto() {
        var request = new UsuarioCreateRequest("Pedro", "pedro@empresa.com", "secreta123", null);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(usuarioService.registrar(request).rol()).isEqualTo(Rol.USUARIO);
    }

    @Test
    void registrar_conCorreoExistenteFallaSinGuardar() {
        var request = new UsuarioCreateRequest("Laura", "laura@empresa.com", "secreta123", null);
        when(usuarioRepository.existsByCorreoIgnoreCase("laura@empresa.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(CorreoDuplicadoException.class)
                .hasMessageContaining("laura@empresa.com");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_siDosPeticionesCompitenElIndiceUnicoTambienTerminaEnCorreoDuplicado() {
        var request = new UsuarioCreateRequest("Laura", "laura@empresa.com", "secreta123", null);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("ux_usuarios_correo"));

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(CorreoDuplicadoException.class);
    }

    @Test
    void buscarPorId_inexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el usuario con id 99");
    }
}
