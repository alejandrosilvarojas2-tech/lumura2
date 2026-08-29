package com.lumura.primeraApi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compras")
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Integer idCompra;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(columnDefinition = "TEXT")
    private String articulo;

    @Column(name = "cantidad_objetos")
    private Integer cantidadObjetos;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "direccion_entrega", columnDefinition = "TEXT")
    private String direccionEntrega;

    @Column(name = "estado_pedido", length = 50)
    private String estadoPedido;

    @Column(name = "fecha_pedido", updatable = false)
    private LocalDateTime fechaPedido;

    @Column(name = "numero_guia", length = 60)
    private String numeroGuia;

    @Column(name = "transportadora", length = 60)
    private String transportadora;

    // Historial de estados: "ESTADO@yyyy-MM-ddTHH:mm:ss" separados por "|" (ej. PENDIENTE@...|ENVIADO@...)
    @Column(name = "historial_envio", columnDefinition = "TEXT")
    private String historialEnvio;

    @Transient
    private List<DetalleCompra> detalles = new ArrayList<>();

    public Integer getIdCompra() { return idCompra; }
    public void setIdCompra(Integer idCompra) { this.idCompra = idCompra; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getArticulo() { return articulo; }
    public void setArticulo(String articulo) { this.articulo = articulo; }
    public Integer getCantidadObjetos() { return cantidadObjetos; }
    public void setCantidadObjetos(Integer cantidadObjetos) { this.cantidadObjetos = cantidadObjetos; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public String getEstadoPedido() { return estadoPedido; }
    public void setEstadoPedido(String estadoPedido) { this.estadoPedido = estadoPedido; }
    public LocalDateTime getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(LocalDateTime fechaPedido) { this.fechaPedido = fechaPedido; }
    public String getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(String numeroGuia) { this.numeroGuia = numeroGuia; }
    public String getTransportadora() { return transportadora; }
    public void setTransportadora(String transportadora) { this.transportadora = transportadora; }
    public String getHistorialEnvio() { return historialEnvio; }
    public void setHistorialEnvio(String historialEnvio) { this.historialEnvio = historialEnvio; }
    public List<DetalleCompra> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCompra> detalles) { this.detalles = detalles; }
}
