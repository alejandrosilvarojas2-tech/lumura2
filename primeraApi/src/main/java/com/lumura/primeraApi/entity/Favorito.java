package com.lumura.primeraApi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "favoritos", uniqueConstraints = @UniqueConstraint(columnNames = {"id_usuario", "id_catalogo"}))
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_favorito")
    private Integer idFavorito;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_catalogo", nullable = false)
    private Integer idCatalogo;

    @Column(name = "fecha_agregado")
    private java.time.LocalDateTime fechaAgregado;

    public Integer getIdFavorito() { return idFavorito; }
    public void setIdFavorito(Integer idFavorito) { this.idFavorito = idFavorito; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(Integer idCatalogo) { this.idCatalogo = idCatalogo; }
    public java.time.LocalDateTime getFechaAgregado() { return fechaAgregado; }
    public void setFechaAgregado(java.time.LocalDateTime fechaAgregado) { this.fechaAgregado = fechaAgregado; }
}
