package com.campuslands.tickets.entity;

import com.campuslands.tickets.exception.TransicionEstadoInvalidaException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ticket de soporte.
 *
 * Tiene dos relaciones hacia Usuario: quién lo pidió y quién lo atiende.
 * Por eso las columnas se llaman usuario_solicitante_id y tecnico_asignado_id,
 * y no un "usuario_id" ambiguo.
 *
 * Los tickets eliminados no se borran de la tabla (DECISIONS.md, D-03);
 * la restricción de abajo hace que JPA simplemente no los vea.
 */
@Entity
@Table(name = "tickets")
@SQLRestriction("fecha_eliminacion IS NULL")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ABIERTO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_solicitante_id", nullable = false)
    private Usuario usuarioSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_asignado_id")
    private Usuario tecnicoAsignado;

    @Column(name = "resumen_ia", length = 300)
    private String resumenIA;

    @ElementCollection
    @CollectionTable(name = "ticket_etiquetas", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "etiqueta", nullable = false, length = 40)
    private List<String> etiquetasIA = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_analisis", nullable = false, length = 10)
    private OrigenAnalisis origenAnalisis;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    protected Ticket() {
        // requerido por JPA
    }

    public Ticket(String titulo, String descripcion, Usuario usuarioSolicitante) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.usuarioSolicitante = usuarioSolicitante;
    }

    /**
     * Guarda el resultado del análisis. Recibe tipos ya validados: si algo
     * llegó hasta aquí como enum, es porque pasó por la normalización de la
     * capa de IA.
     */
    public void aplicarAnalisis(Categoria categoria, Prioridad prioridad, String resumen,
                                List<String> etiquetas, OrigenAnalisis origen) {
        this.categoria = categoria;
        this.prioridad = prioridad;
        this.resumenIA = resumen;
        this.etiquetasIA.clear();
        this.etiquetasIA.addAll(etiquetas);
        this.origenAnalisis = origen;
    }

    public void cambiarEstado(Estado nuevoEstado) {
        if (!estado.puedePasarA(nuevoEstado)) {
            throw new TransicionEstadoInvalidaException(estado, nuevoEstado);
        }
        this.estado = nuevoEstado;
    }

    /**
     * Ojo: aquí no se valida el rol. Esa regla vive en TicketService
     * (CONTEXT.md, regla 7) para que el error salga con su propio mensaje.
     */
    public void asignarTecnico(Usuario tecnico) {
        this.tecnicoAsignado = tecnico;
    }

    public void eliminar() {
        this.fechaEliminacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public Estado getEstado() {
        return estado;
    }

    public Usuario getUsuarioSolicitante() {
        return usuarioSolicitante;
    }

    public Usuario getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public String getResumenIA() {
        return resumenIA;
    }

    public List<String> getEtiquetasIA() {
        return List.copyOf(etiquetasIA);
    }

    public OrigenAnalisis getOrigenAnalisis() {
        return origenAnalisis;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
