package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Rol;

import java.time.LocalDateTime;

/**
 * Lo que la API muestra de un usuario. El hash de la contraseña no está
 * aquí a propósito: si no existe en el DTO, no hay forma de filtrarlo.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        Rol rol,
        LocalDateTime fechaCreacion
) {
}
