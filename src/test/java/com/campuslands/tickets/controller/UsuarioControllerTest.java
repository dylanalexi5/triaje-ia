package com.campuslands.tickets.controller;

import com.campuslands.tickets.dto.UsuarioResponse;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.exception.CorreoDuplicadoException;
import com.campuslands.tickets.exception.RecursoNoEncontradoException;
import com.campuslands.tickets.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la capa HTTP: códigos de estado, validaciones y formato de error.
 * El servicio está simulado; su lógica se prueba en UsuarioServiceTest.
 */
@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void registrar_devuelve201ConLocationYSinPassword() throws Exception {
        when(usuarioService.registrar(any())).thenReturn(
                new UsuarioResponse(7L, "Laura Gómez", "laura@empresa.com", Rol.USUARIO, LocalDateTime.now()));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Laura Gómez", "correo": "laura@empresa.com", "password": "secreta123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/usuarios/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrar_conDatosInvalidosDevuelve400ConCadaCampoQueFallo() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "", "correo": "esto-no-es-correo", "password": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/usuarios"))
                .andExpect(jsonPath("$.detalles", hasItem("correo: El correo no tiene un formato válido")))
                .andExpect(jsonPath("$.detalles", hasItem("password: La contraseña debe tener entre 8 y 72 caracteres")))
                // un solo mensaje por campo, aunque "" incumpla @NotBlank y @Size a la vez
                .andExpect(jsonPath("$.detalles", hasSize(3)))
                .andExpect(jsonPath("$.detalles", hasItem("nombre: El nombre es obligatorio")));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void registrar_aceptaUnCorreoConEspaciosAlrededor() throws Exception {
        when(usuarioService.registrar(any())).thenReturn(
                new UsuarioResponse(8L, "Laura", "laura@empresa.com", Rol.USUARIO, LocalDateTime.now()));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": " Laura ", "correo": "  Laura@Empresa.com ", "password": "secreta123"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void registrar_conRolInexistenteExplicaLosValoresPermitidos() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Laura", "correo": "laura@empresa.com", "password": "secreta123", "rol": "JEFE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("[ADMIN, SOPORTE, USUARIO]")));
    }

    @Test
    void registrar_conCorreoDuplicadoDevuelve409() throws Exception {
        when(usuarioService.registrar(any())).thenThrow(new CorreoDuplicadoException("laura@empresa.com"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Laura", "correo": "laura@empresa.com", "password": "secreta123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void buscarPorId_inexistenteDevuelve404ConElFormatoDelEnunciado() throws Exception {
        when(usuarioService.buscarPorId(15L)).thenThrow(new RecursoNoEncontradoException("usuario", 15L));

        mockMvc.perform(get("/api/usuarios/15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No se encontró el usuario con id 15"))
                .andExpect(jsonPath("$.path").value("/api/usuarios/15"))
                .andExpect(jsonPath("$.detalles").doesNotExist());
    }
}
