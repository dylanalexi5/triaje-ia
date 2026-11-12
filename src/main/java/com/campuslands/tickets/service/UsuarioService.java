package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.UsuarioCreateRequest;
import com.campuslands.tickets.dto.UsuarioResponse;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.CorreoDuplicadoException;
import com.campuslands.tickets.exception.RecursoNoEncontradoException;
import com.campuslands.tickets.mapper.UsuarioMapper;
import com.campuslands.tickets.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private static final Sort POR_ID = Sort.by("id");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(UsuarioCreateRequest request) {
        // El correo se guarda siempre en minúscula para que "Laura@Empresa.com"
        // y "laura@empresa.com" no terminen siendo dos personas distintas.
        String correo = request.correo().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException(correo);
        }

        Rol rol = request.rol() != null ? request.rol() : Rol.USUARIO;
        Usuario usuario = new Usuario(
                request.nombre(),
                correo,
                passwordEncoder.encode(request.password()),
                rol
        );

        try {
            // saveAndFlush para que el índice único falle aquí y no al cerrar
            // la transacción. El chequeo de arriba cubre el caso normal; esto
            // cubre dos registros simultáneos con el mismo correo.
            return UsuarioMapper.aResponse(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            throw new CorreoDuplicadoException(correo);
        }
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioMapper::aResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", id));
    }

    /**
     * Lista todos los usuarios, o solo los de un rol si viene el filtro.
     * El filtro existe para saber a quién se puede asignar un ticket
     * (GET /api/usuarios?rol=SOPORTE).
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Rol rol) {
        List<Usuario> usuarios = rol == null
                ? usuarioRepository.findAll(POR_ID)
                : usuarioRepository.findByRol(rol, POR_ID);

        return usuarios.stream()
                .map(UsuarioMapper::aResponse)
                .toList();
    }
}
