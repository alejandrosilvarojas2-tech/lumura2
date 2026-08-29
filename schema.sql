-- ============================================================
-- ESQUEMA DE REFERENCIA — LUMURA
-- ⚠ ADVERTENCIA: Spring Boot maneja el schema automáticamente
-- (ddl-auto=update). Este archivo es SOLO documentación.
-- NO ejecutar en producción — los INSERT de ejemplo duplican
-- datos si ya existen.
-- ============================================================

CREATE DATABASE IF NOT EXISTS publico CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE publico;

-- La creación de tablas se hace automáticamente via JPA/Hibernate.
-- Este SQL es solo para referencia de la estructura.

-- Usuarios
CREATE TABLE IF NOT EXISTS usuario (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre_usuario VARCHAR(100) NOT NULL,
  correo_usuario VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  telefono VARCHAR(20),
  edad INT,
  direccion_usuario TEXT,
  fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  rol VARCHAR(20) DEFAULT 'USER',          -- USER | ALIADO | ADMIN
  nombre_negocio VARCHAR(100),             -- solo aliados
  nit VARCHAR(40),                         -- solo aliados
  persona_contacto VARCHAR(100),           -- solo aliados
  categoria_productos VARCHAR(60),          -- categoría que vende el aliado (Zapatos, Vestidos, Ropa deportiva, ...)
  reset_token VARCHAR(64),                  -- token de recuperación de contraseña (se limpia al usarse)
  reset_token_expira DATETIME(6),           -- fecha de expiración del token (30 min de vida)
  licencia_distribuidor TEXT,               -- URL de la licencia de distribuidor autorizado del aliado
  bloqueado BOOLEAN DEFAULT FALSE,          -- true = perfil bloqueado por el admin
  motivo_bloqueo TEXT,                       -- motivo del bloqueo registrado por el admin
  bloqueo_hasta DATETIME(6)                  -- fecha de fin del bloqueo (NULL = indefinido)
);

-- Catálogo de productos
CREATE TABLE IF NOT EXISTS catalogo (
  id_catalogo INT AUTO_INCREMENT PRIMARY KEY,
  articulo VARCHAR(150) NOT NULL,
  talla VARCHAR(50),
  color VARCHAR(50),
  precio DECIMAL(10,2) NOT NULL,
  precio_descuento DECIMAL(10,2),
  descripcion TEXT,
  categoria VARCHAR(100),
  stock INT DEFAULT 0,
  imagen_url VARCHAR(255),
  estado VARCHAR(20) DEFAULT 'activo',
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Carrito de compras
CREATE TABLE IF NOT EXISTS carrito (
  id_carrito INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL,
  id_catalogo INT NULL,  -- FK lógica a catalogo; NULL solo en filas creadas antes de esta columna
  articulo VARCHAR(150) NOT NULL,
  talla VARCHAR(50),
  color VARCHAR(50),
  cantidad INT DEFAULT 1,
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
);

-- Pedidos / Compras
CREATE TABLE IF NOT EXISTS compras (
  id_compra INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL,
  articulo TEXT,               -- resumen legible; el desglose real vive en detalle_compra
  cantidad_objetos INT,
  metodo_pago VARCHAR(50),
  total DECIMAL(10,2),         -- calculado por el servidor desde el carrito
  direccion_entrega TEXT,
  estado_pedido VARCHAR(50) DEFAULT 'pendiente',
  fecha_pedido TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  numero_guia VARCHAR(60),               -- guía de envío (se llena al marcar como enviado)
  transportadora VARCHAR(60),            -- transportadora de envío
  historial_envio TEXT,                  -- "ESTADO@timestamp|ESTADO@timestamp" (seguimiento)
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
);

-- Desglose normalizado de cada pedido (fuente de verdad para estadísticas)
CREATE TABLE IF NOT EXISTS detalle_compra (
  id_detalle INT AUTO_INCREMENT PRIMARY KEY,
  id_compra INT NOT NULL,
  id_catalogo INT NULL,        -- FK lógica a catalogo; NULL solo en datos creados antes de esta columna
  articulo VARCHAR(150) NOT NULL,  -- snapshot del nombre al momento de la venta
  cantidad INT NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,  -- snapshot del precio al momento de la venta
  FOREIGN KEY (id_compra) REFERENCES compras(id_compra) ON DELETE CASCADE
);

-- Favoritos por usuario (sincronizados entre dispositivos; reemplaza localStorage)
CREATE TABLE IF NOT EXISTS favoritos (
  id_favorito INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL,
  id_catalogo INT NOT NULL,
  fecha_agregado TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_favorito (id_usuario, id_catalogo),
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
);

-- Datos de ejemplo (solo si la tabla está vacía)
INSERT INTO catalogo (articulo, talla, color, precio, descripcion, categoria, stock)
SELECT * FROM (SELECT 'Camiseta Básica Premium' AS articulo, 'XS,S,M,L,XL' AS talla, 'Rojo,Negro,Blanco' AS color, 45900 AS precio, 'Camiseta de algodón premium 100%, suave al tacto.' AS descripcion, 'Camisetas' AS categoria, 50 AS stock) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM catalogo);

INSERT INTO catalogo (articulo, talla, color, precio, descripcion, categoria, stock)
SELECT * FROM (SELECT 'Jeans Slim Fit' AS articulo, '28,30,32,34,36' AS talla, 'Azul,Negro' AS color, 89900 AS precio, 'Jeans de mezclilla con ajuste slim fit.' AS descripcion, 'Pantalones' AS categoria, 30 AS stock) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM catalogo WHERE articulo = 'Jeans Slim Fit');

INSERT INTO catalogo (articulo, talla, color, precio, descripcion, categoria, stock)
SELECT * FROM (SELECT 'Chaqueta Deportiva' AS articulo, 'S,M,L,XL' AS talla, 'Negro,Gris' AS color, 129900 AS precio, 'Chaqueta deportiva impermeable y transpirable.' AS descripcion, 'Chaquetas' AS categoria, 15 AS stock) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM catalogo WHERE articulo = 'Chaqueta Deportiva');

INSERT INTO catalogo (articulo, talla, color, precio, descripcion, categoria, stock)
SELECT * FROM (SELECT 'Vestido Casual' AS articulo, 'XS,S,M,L' AS talla, 'Negro,Azul,Verde' AS color, 74900 AS precio, 'Vestido casual para uso diario, fresco y elegante.' AS descripcion, 'Vestidos' AS categoria, 25 AS stock) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM catalogo WHERE articulo = 'Vestido Casual');

INSERT INTO catalogo (articulo, talla, color, precio, descripcion, categoria, stock)
SELECT * FROM (SELECT 'Zapatillas Urbanas' AS articulo, '38,39,40,41,42' AS talla, 'Blanco,Negro' AS color, 159900 AS precio, 'Zapatillas cómodas para uso diario.' AS descripcion, 'Calzado' AS categoria, 40 AS stock) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM catalogo WHERE articulo = 'Zapatillas Urbanas');
