package com.campuslands.tickets.exception;

import com.campuslands.tickets.entity.Usuario;

public class TecnicoInvalidoException extends RuntimeException {

    public TecnicoInvalidoException(Usuario usuario) {
        super("El usuario con id %d tiene rol %s; solo un usuario con rol SOPORTE puede ser asignado como técnico"
                .formatted(usuario.getId(), usuario.getRol()));
    }
}
