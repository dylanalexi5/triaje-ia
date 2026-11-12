package com.campuslands.tickets.mapper;

import com.campuslands.tickets.dto.UsuarioResponse;
import com.campuslands.tickets.entity.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse aResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getFechaCreacion()
        );
    }
}
