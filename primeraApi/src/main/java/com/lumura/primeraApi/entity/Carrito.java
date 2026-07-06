package com.lumura.primeraApi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "carrito")
public class Carrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrito")
    private Integer idCarrito;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(nullable = false)
    private String articulo;

    private String talla;
    private String color;

    @Column(nullable = false)
    private Integer cantidad;

    public Integer getIdCarrito() { return idCarrito; }
    public void setIdCarrito(Integer idCarrito) { this.idCarrito = idCarrito; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getArticulo() { return articulo; }
    public void setArticulo(String articulo) { this.articulo = articulo; }
    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
