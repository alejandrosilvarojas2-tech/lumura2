-- ============================================================
-- Flyway V1 — Esquema inicial de LUMURA
-- DDL extraído de la BD real (publico) el 08/09/2026.
-- Esta migración define TODAS las tablas/columnas que las
-- entidades JPA esperan (Hibernate corre con ddl-auto=none).
-- ============================================================

CREATE TABLE usuario (
  id_usuario INT NOT NULL AUTO_INCREMENT,
  nombre_usuario VARCHAR(100) NOT NULL,
  correo_usuario VARCHAR(150) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  telefono VARCHAR(20) DEFAULT NULL,
  edad INT DEFAULT NULL,
  direccion_usuario TEXT,
  fecha_registro TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  rol VARCHAR(20) DEFAULT NULL,
  nit VARCHAR(40) DEFAULT NULL,
  nombre_negocio VARCHAR(255) DEFAULT NULL,
  persona_contacto VARCHAR(255) DEFAULT NULL,
  categoria_productos VARCHAR(60) DEFAULT NULL,
  reset_token VARCHAR(64) DEFAULT NULL,
  reset_token_expira DATETIME(6) DEFAULT NULL,
  licencia_distribuidor TEXT,
  bloqueado BIT(1) DEFAULT NULL,
  bloqueo_hasta DATETIME(6) DEFAULT NULL,
  motivo_bloqueo TEXT,
  token_version INT DEFAULT NULL,
  membresia_activada_en DATETIME(6) DEFAULT NULL,
  membresia_codigo VARCHAR(40) DEFAULT NULL,
  membresia_plan VARCHAR(20) DEFAULT NULL,
  membresia_vence DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (id_usuario),
  UNIQUE KEY correo_usuario (correo_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE catalogo (
  id_catalogo INT NOT NULL AUTO_INCREMENT,
  articulo VARCHAR(150) NOT NULL,
  talla VARCHAR(50) DEFAULT NULL,
  color VARCHAR(50) DEFAULT NULL,
  precio DECIMAL(10,2) NOT NULL,
  precio_descuento DECIMAL(10,2) DEFAULT NULL,
  descripcion TEXT,
  categoria VARCHAR(100) DEFAULT NULL,
  stock INT DEFAULT 0,
  imagen_url VARCHAR(255) DEFAULT NULL,
  estado VARCHAR(20) DEFAULT 'activo',
  fecha_creacion TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  id_aliado INT DEFAULT NULL,
  codigo VARCHAR(30) DEFAULT NULL,
  PRIMARY KEY (id_catalogo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE carrito (
  id_carrito INT NOT NULL AUTO_INCREMENT,
  id_usuario INT NOT NULL,
  articulo VARCHAR(150) NOT NULL,
  talla VARCHAR(50) DEFAULT NULL,
  color VARCHAR(50) DEFAULT NULL,
  cantidad INT DEFAULT 1,
  id_catalogo INT DEFAULT NULL,
  PRIMARY KEY (id_carrito),
  KEY id_usuario (id_usuario),
  CONSTRAINT carrito_ibfk_1 FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE compras (
  id_compra INT NOT NULL AUTO_INCREMENT,
  id_usuario INT NOT NULL,
  articulo TEXT,
  cantidad_objetos INT DEFAULT NULL,
  metodo_pago VARCHAR(50) DEFAULT NULL,
  total DECIMAL(10,2) DEFAULT NULL,
  direccion_entrega TEXT,
  estado_pedido VARCHAR(50) DEFAULT 'pendiente',
  fecha_pedido TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  historial_envio TEXT,
  numero_guia VARCHAR(60) DEFAULT NULL,
  transportadora VARCHAR(60) DEFAULT NULL,
  PRIMARY KEY (id_compra),
  KEY id_usuario (id_usuario),
  CONSTRAINT compras_ibfk_1 FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE detalle_compra (
  id_detalle INT NOT NULL AUTO_INCREMENT,
  articulo VARCHAR(255) NOT NULL,
  cantidad INT NOT NULL,
  id_catalogo INT DEFAULT NULL,
  id_compra INT NOT NULL,
  precio_unitario DECIMAL(10,2) DEFAULT NULL,
  PRIMARY KEY (id_detalle)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE favoritos (
  id_favorito INT NOT NULL AUTO_INCREMENT,
  fecha_agregado DATETIME(6) DEFAULT NULL,
  id_catalogo INT NOT NULL,
  id_usuario INT NOT NULL,
  PRIMARY KEY (id_favorito),
  UNIQUE KEY PK_favorito (id_usuario, id_catalogo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;