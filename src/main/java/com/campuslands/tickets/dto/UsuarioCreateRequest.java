package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un usuario.
 *
 * El rol es opcional y por defecto es USUARIO. Como todavía no hay
 * autenticación, cualquiera podría crear un ADMIN; está anotado como
 * limitación conocida en DECISIONS.md (D-15).
 */
public record UsuarioCreateRequest(

        @Schema(example = "Sofía Herrera")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String nombre,

        @Schema(example = "sofia@empresa.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        // BCrypt solo usa los primeros 72 bytes; más allá de eso se ignoraría
        // en silencio, así que mejor rechazarlo de frente.
        @Schema(example = "secreta123", minLength = 8, maxLength = 72)
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Schema(description = "Opcional; por defecto USUARIO", example = "SOPORTE")
        Rol rol
) {

    /**
     * Quita espacios de los extremos ANTES de validar. Sin esto, un correo
     * pegado con un espacio al final falla @Email aunque esté bien escrito.
     * La contraseña no se toca: ahí un espacio es parte del secreto.
     */
    public UsuarioCreateRequest {
        nombre = nombre != null ? nombre.strip() : null;
        correo = correo != null ? correo.strip() : null;
    }
}
