package com.lumura.primeraApi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombre_usuario", nullable = false)
    private String nombreUsuario;

    @Column(name = "correo_usuario", nullable = false, unique = true)
    private String correoUsuario;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String telefono;
    private Integer edad;

    @Column(name = "direccion_usuario", columnDefinition = "TEXT")
    private String direccionUsuario;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(length = 20)
    private String rol = "USER";

    @Column(name = "nombre_negocio")
    private String nombreNegocio;

    @Column(name = "nit", length = 40)
    private String nit;

    @Column(name = "persona_contacto")
    private String personaContacto;

    @Column(name = "categoria_productos", length = 60)
    private String categoriaProductos;

    @Column(name = "reset_token", length = 64)
    private String resetToken;

    @Column(name = "reset_token_expira")
    private LocalDateTime resetTokenExpira;

    @Column(name = "licencia_distribuidor", columnDefinition = "TEXT")
    private String licenciaDistribuidor;

    @Column(name = "bloqueado")
    private Boolean bloqueado = false;

    @Column(name = "motivo_bloqueo", columnDefinition = "TEXT")
    private String motivoBloqueo;

    @Column(name = "bloqueo_hasta")
    private LocalDateTime bloqueoHasta;

    public Boolean getBloqueado() { return bloqueado; }
    public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }
    public String getMotivoBloqueo() { return motivoBloqueo; }
    public void setMotivoBloqueo(String motivoBloqueo) { this.motivoBloqueo = motivoBloqueo; }
    public LocalDateTime getBloqueoHasta() { return bloqueoHasta; }
    public void setBloqueoHasta(LocalDateTime bloqueoHasta) { this.bloqueoHasta = bloqueoHasta; }

    public String getLicenciaDistribuidor() { return licenciaDistribuidor; }
    public void setLicenciaDistribuidor(String licenciaDistribuidor) { this.licenciaDistribuidor = licenciaDistribuidor; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public LocalDateTime getResetTokenExpira() { return resetTokenExpira; }
    public void setResetTokenExpira(LocalDateTime resetTokenExpira) { this.resetTokenExpira = resetTokenExpira; }

    public String getNombreNegocio() { return nombreNegocio; }
    public void setNombreNegocio(String nombreNegocio) { this.nombreNegocio = nombreNegocio; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public String getPersonaContacto() { return personaContacto; }
    public void setPersonaContacto(String personaContacto) { this.personaContacto = personaContacto; }
    public String getCategoriaProductos() { return categoriaProductos; }
    public void setCategoriaProductos(String categoriaProductos) { this.categoriaProductos = categoriaProductos; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getCorreoUsuario() { return correoUsuario; }
    public void setCorreoUsuario(String correoUsuario) { this.correoUsuario = correoUsuario; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }
    public String getDireccionUsuario() { return direccionUsuario; }
    public void setDireccionUsuario(String direccionUsuario) { this.direccionUsuario = direccionUsuario; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
