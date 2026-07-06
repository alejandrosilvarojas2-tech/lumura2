CREATE DATABASE IF NOT EXISTS publico CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE publico;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS rel_usuario_compra;
DROP TABLE IF EXISTS carrito;
DROP TABLE IF EXISTS compras;
DROP TABLE IF EXISTS catalogo;
DROP TABLE IF EXISTS usuario;
SET FOREIGN_KEY_CHECKS = 1;

-- Usuarios
CREATE TABLE IF NOT EXISTS usuario (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre_usuario VARCHAR(100) NOT NULL,
  correo_usuario VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  telefono VARCHAR(20),
  edad INT,
  direccion_usuario TEXT,
  fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
  articulo TEXT,
  cantidad_objetos INT,
  metodo_pago VARCHAR(50),
  total DECIMAL(10,2),
  direccion_entrega TEXT,
  estado_pedido VARCHAR(50) DEFAULT 'pendiente',
  fecha_pedido TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
);

-- Productos de ejemplo
INSERT INTO catalogo (articulo, talla, color, precio, descripcion, categoria, stock) VALUES
('Camiseta Básica Premium', 'XS,S,M,L,XL', 'Rojo,Negro,Blanco', 45900, 'Camiseta de algodón premium 100%, suave al tacto.', 'Camisetas', 50),
('Jeans Slim Fit', '28,30,32,34,36', 'Azul,Negro', 89900, 'Jeans de mezclilla con ajuste slim fit.', 'Pantalones', 30),
('Chaqueta Deportiva', 'S,M,L,XL', 'Negro,Gris', 129900, 'Chaqueta deportiva impermeable y transpirable.', 'Chaquetas', 15),
('Vestido Casual', 'XS,S,M,L', 'Negro,Azul,Verde', 74900, 'Vestido casual para uso diario, fresco y elegante.', 'Vestidos', 25),
('Zapatillas Urbanas', '38,39,40,41,42', 'Blanco,Negro', 159900, 'Zapatillas cómodas para uso diario.', 'Calzado', 40);
