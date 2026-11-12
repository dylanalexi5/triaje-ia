package com.campuslands.tickets.repository;

import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.entity.Usuario;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    List<Usuario> findByRol(Rol rol, Sort sort);
}
