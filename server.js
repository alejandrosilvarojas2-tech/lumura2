const express = require('express');
const cors = require('cors');
const bcrypt = require('bcrypt');
const db = require('./conexion');
const connection = require('./conexion');

const app = express();
const PORT = 3000;
const JWT_SECRET = 'lumura_secret_2025';

app.use(cors());
app.use(express.json());
app.use(express.static(__dirname));

// ==================== AUTH ====================

// REGISTRO
app.post('/api/auth/register', (req, res) => {
  const { nombre_usuario, correo_usuario, password, telefono, edad, direccion_usuario } = req.body;

  if (!nombre_usuario || !correo_usuario || !password) {
    return res.status(400).json({ error: 'Faltan campos obligatorios' });
  }

  bcrypt.hash(password, 10, (err, hash) => {
    if (err) return res.status(500).json({ error: 'Error al encriptar contraseña' });

    const sql = `INSERT INTO usuario (nombre_usuario, correo_usuario, password_hash, telefono, edad, direccion_usuario)
                 VALUES (?, ?, ?, ?, ?, ?)`;

    connection.query(sql, [nombre_usuario, correo_usuario, hash, telefono, edad, direccion_usuario], (err, result) => {
      if (err) {
        if (err.code === 'ER_DUP_ENTRY') return res.status(400).json({ error: 'El correo ya está registrado' });
        return res.status(500).json({ error: 'Error al registrar usuario' });
      }
      res.json({ mensaje: 'Usuario registrado correctamente', id: result.insertId });
    });
  });
});

// LOGIN
app.post('/api/auth/login', (req, res) => {
  const { correo_usuario, password } = req.body;

  connection.query('SELECT * FROM usuario WHERE correo_usuario = ?', [correo_usuario], (err, results) => {
    if (err) return res.status(500).json({ error: 'Error en el servidor' });
    if (results.length === 0) return res.status(401).json({ error: 'Correo o contraseña incorrectos' });

    const usuario = results[0];
    bcrypt.compare(password, usuario.password_hash, (err, match) => {
      if (!match) return res.status(401).json({ error: 'Correo o contraseña incorrectos' });

      const token = jwt.sign({ id: usuario.id_usuario, correo: usuario.correo_usuario }, JWT_SECRET, { expiresIn: '8h' });
      res.json({ token, usuario: { id: usuario.id_usuario, nombre: usuario.nombre_usuario, correo: usuario.correo_usuario } });
    });
  });
});

// ==================== PRODUCTOS ====================

// LISTAR PRODUCTOS
app.get('/api/productos', (req, res) => {
  connection.query("SELECT * FROM catalogo WHERE estado = 'activo'", (err, results) => {
    if (err) return res.status(500).json({ error: 'Error al obtener productos' });
    res.json(results);
  });
});

// CREAR PRODUCTO (admin)
app.post('/api/productos', (req, res) => {
  const { articulo, talla, color, precio, precio_descuento, descripcion, categoria, stock, imagen_url } = req.body;
  const sql = `INSERT INTO catalogo (articulo, talla, color, precio, precio_descuento, descripcion, categoria, stock, imagen_url)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`;
  connection.query(sql, [articulo, talla, color, precio, precio_descuento, descripcion, categoria, stock, imagen_url], (err, result) => {
    if (err) return res.status(500).json({ error: 'Error al crear producto' });
    res.json({ mensaje: 'Producto creado', id: result.insertId });
  });
});

// ==================== CARRITO ====================

// VER CARRITO
app.get('/api/carrito/:id_usuario', (req, res) => {
  const sql = `SELECT c.*, cat.precio, cat.imagen_url FROM carrito c
               JOIN catalogo cat ON c.articulo = cat.articulo
               WHERE c.id_usuario = ?`;
  connection.query(sql, [req.params.id_usuario], (err, results) => {
    if (err) return res.status(500).json({ error: 'Error al obtener carrito' });
    res.json(results);
  });
});

// AGREGAR AL CARRITO
app.post('/api/carrito', (req, res) => {
  const { id_usuario, articulo, talla, color, cantidad } = req.body;
  const sql = `INSERT INTO carrito (id_usuario, articulo, talla, color, cantidad) VALUES (?, ?, ?, ?, ?)`;
  connection.query(sql, [id_usuario, articulo, talla, color, cantidad], (err, result) => {
    if (err) return res.status(500).json({ error: 'Error al agregar al carrito' });
    res.json({ mensaje: 'Producto agregado al carrito' });
  });
});

// ELIMINAR DEL CARRITO
app.delete('/api/carrito/:id_carrito', (req, res) => {
  connection.query('DELETE FROM carrito WHERE id_carrito = ?', [req.params.id_carrito], (err) => {
    if (err) return res.status(500).json({ error: 'Error al eliminar del carrito' });
    res.json({ mensaje: 'Producto eliminado del carrito' });
  });
});

// ==================== PEDIDOS ====================

// CREAR PEDIDO
app.post('/api/pedidos', (req, res) => {
  const { id_usuario, articulo, cantidad_objetos, metodo_pago, total, direccion_entrega } = req.body;
  const sql = `INSERT INTO compras (id_usuario, articulo, cantidad_objetos, metodo_pago, total, direccion_entrega)
               VALUES (?, ?, ?, ?, ?, ?)`;
  connection.query(sql, [id_usuario, articulo, cantidad_objetos, metodo_pago, total, direccion_entrega], (err, result) => {
    if (err) return res.status(500).json({ error: 'Error al crear pedido' });
    res.json({ mensaje: 'Pedido creado correctamente', id: result.insertId });
  });
});

// VER PEDIDOS DE UN USUARIO
app.get('/api/pedidos/:id_usuario', (req, res) => {
  connection.query('SELECT * FROM compras WHERE id_usuario = ? ORDER BY fecha_pedido DESC', [req.params.id_usuario], (err, results) => {
    if (err) return res.status(500).json({ error: 'Error al obtener pedidos' });
    res.json(results);
  });
});

// ==================== ADMIN ====================

// DASHBOARD
app.get('/api/admin/dashboard', (req, res) => {
  const queries = {
    total_productos: 'SELECT COUNT(*) as total FROM catalogo',
    sin_stock: "SELECT COUNT(*) as total FROM catalogo WHERE stock = 0",
    total_usuarios: 'SELECT COUNT(*) as total FROM usuario',
    total_pedidos: 'SELECT COUNT(*) as total FROM compras',
    ingresos: 'SELECT SUM(total) as total FROM compras'
  };

  const resultados = {};
  let completados = 0;
  const keys = Object.keys(queries);

  keys.forEach(key => {
    connection.query(queries[key], (err, result) => {
      resultados[key] = err ? 0 : result[0].total;
      completados++;
      if (completados === keys.length) res.json(resultados);
    });
  });
});

// TODOS LOS PEDIDOS (admin)
app.get('/api/admin/pedidos', (req, res) => {
  connection.query('SELECT c.*, u.nombre_usuario, u.correo_usuario FROM compras c JOIN usuario u ON c.id_usuario = u.id_usuario ORDER BY fecha_pedido DESC', (err, results) => {
    if (err) return res.status(500).json({ error: 'Error al obtener pedidos' });
    res.json(results);
  });
});

// ACTUALIZAR ESTADO PEDIDO
app.put('/api/admin/pedidos/:id', (req, res) => {
  const { estado_pedido } = req.body;
  connection.query('UPDATE compras SET estado_pedido = ? WHERE id_compra = ?', [estado_pedido, req.params.id], (err) => {
    if (err) return res.status(500).json({ error: 'Error al actualizar pedido' });
    res.json({ mensaje: 'Estado actualizado' });
  });
});

// ==================== ARRANCAR SERVIDOR ====================
app.listen(PORT, () => {
  console.log(`Servidor LUMURA corriendo en http://localhost:${PORT}`);
});