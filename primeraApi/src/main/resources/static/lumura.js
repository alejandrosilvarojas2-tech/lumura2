const state = {
  token: localStorage.getItem('lumura_token'),
  user: JSON.parse(localStorage.getItem('lumura_user') || 'null'),
  productos: [],
  carrito: [],
  productoActual: null,
  favoritos: JSON.parse(localStorage.getItem('lumura_favs') || '[]'),
};

const api = {
  async request(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    if (state.token) headers['Authorization'] = `Bearer ${state.token}`;
    const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Error del servidor');
    return data;
  },
  get: (path) => api.request('GET', path),
  post: (path, body) => api.request('POST', path, body),
  put: (path, body) => api.request('PUT', path, body),
  delete: (path) => api.request('DELETE', path),
};

function mostrarMensaje(texto, tipo) {
  const el = document.getElementById('msg');
  el.textContent = texto;
  el.className = 'msg msg-' + (tipo || 'info');
  el.style.display = 'block';
  setTimeout(() => { el.style.display = 'none'; }, 4000);
}

function proximamente(func) {
  mostrarMensaje('<img src="images/wrench.svg" class="icon" alt="" style="vertical-align:middle"> ' + (func || 'Función') + ' — Próximamente', 'info');
}

async function handleLogin(e) {
  e.preventDefault();
  const correo = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-pass').value;
  if (!correo || !password) return mostrarMensaje('Completa todos los campos', 'error');
  try {
    const data = await api.post('/api/auth/login', { correo_usuario: correo, password });
    state.token = data.token;
    state.user = data.usuario;
    localStorage.setItem('lumura_token', data.token);
    localStorage.setItem('lumura_user', JSON.stringify(data.usuario));
    actualizarUI();
    mostrarMensaje('Bienvenido, ' + data.usuario.nombre, 'success');
    showScreen('home');
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const nombre = document.getElementById('reg-nombre').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const tel = document.getElementById('reg-tel').value.trim();
  const dir = document.getElementById('reg-dir').value.trim();
  const pass = document.getElementById('reg-pass').value;
  const pass2 = document.getElementById('reg-pass2').value;
  if (!nombre || !email || !pass) return mostrarMensaje('Completa los campos obligatorios', 'error');
  if (pass !== pass2) return mostrarMensaje('Las contraseñas no coinciden', 'error');
  try {
    await api.post('/api/auth/register', {
      nombre_usuario: nombre,
      correo_usuario: email,
      telefono: tel,
      direccion_usuario: dir,
      password: pass,
    });
    mostrarMensaje('Registro exitoso. Inicia sesión.', 'success');
    showScreen('login');
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function cerrarSesion() {
  state.token = null;
  state.user = null;
  localStorage.removeItem('lumura_token');
  localStorage.removeItem('lumura_user');
  actualizarUI();
  mostrarMensaje('Sesión cerrada', 'info');
  showScreen('login');
}

function confirmarEliminarCuenta() {
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = '<div class="modal-content" style="max-width:360px;text-align:center;">' +
    '<div style="font-size:40px;margin-bottom:12px;"><img src="images/warning.svg" class="icon" alt="" style="width:40px;height:40px;"></div>' +
    '<h3 style="margin-bottom:8px;">Eliminar cuenta</h3>' +
    '<p style="color:var(--gray);margin-bottom:20px;">¿Está seguro de eliminar su cuenta? Esta acción no se puede deshacer.</p>' +
    '<div style="display:flex;gap:10px;justify-content:center;">' +
    '<button class="btn-primary" style="width:auto;padding:10px 28px;background:#dc3545;" onclick="eliminarCuenta()">Sí, eliminar</button>' +
    '<button class="btn-secondary" style="width:auto;padding:10px 28px;" onclick="this.closest(\'.modal-overlay\').remove()">Cancelar</button>' +
    '</div></div>';
  document.body.appendChild(overlay);
}

async function eliminarCuenta() {
  document.querySelector('.modal-overlay')?.remove();
  try {
    await api.delete('/api/auth/cuenta');
    mostrarMensaje('Cuenta eliminada correctamente', 'success');
    cerrarSesion();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function cargarProductos() {
  try {
    state.productos = await api.get('/api/productos');
    renderProductos();
    renderAdminCatalogo();
    if (document.getElementById('screen-admin-inv')?.classList.contains('active')) cargarInventario();
  } catch (err) {
    mostrarMensaje('Error al cargar productos: ' + err.message, 'error');
  }
}

const imagenesProducto = {
  1: 'images/camisetabasicapremium.jpg',
  2: 'images/jeansslimfit.jpg',
  3: 'images/chaquetadeportiva.jpg',
  4: 'images/vestidocasual.jfif',
  5: 'images/zapatillasurbanas.jfif'
};

function renderProductos(filtroCat, filtroTexto) {
  const grid = document.getElementById('prod-grid');
  if (!grid) return;
  let items = state.productos;
  if (filtroCat) items = items.filter(p => p.categoria === filtroCat);
  if (filtroTexto) items = items.filter(p =>
    p.articulo.toLowerCase().includes(filtroTexto.toLowerCase()) ||
    (p.categoria || '').toLowerCase().includes(filtroTexto.toLowerCase())
  );
  if (items.length === 0) {
    grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:40px;color:var(--gray);">No hay productos disponibles</div>';
    return;
  }
  grid.innerHTML = items.map((p, i) => {
    const imgSrc = imagenesProducto[p.id_catalogo] || 'images/tshirt.svg';
    const precioF = '$' + Number(p.precio).toLocaleString('es-CO');
    return '<div class="product-card" onclick="verProducto(' + p.id_catalogo + ')">' +
      '<div class="img-placeholder" style="background-image:url(' + imgSrc + ');background-size:cover;background-position:center;background-repeat:no-repeat;background-color:#fce4ec;"></div>' +
      '<div class="info">' +
      '<div class="name">' + p.articulo + '</div>' +
      '<div class="price">' + precioF + '</div>' +
      (p.talla ? '<div style="font-size:11px;color:var(--gray);margin-top:2px;">Tallas: ' + p.talla + '</div>' : '') +
      '</div>' +
      '<button class="add-btn" onclick="event.stopPropagation();agregarAlCarrito(' + p.id_catalogo + ')">+ Agregar al carrito</button>' +
      '</div>';
  }).join('');
}

let productoCache = {};

async function verProducto(id) {
  try {
    let prod = productoCache[id] || state.productos.find(p => p.id_catalogo === id);
    if (!prod) {
      prod = await api.get('/api/productos/' + id);
      productoCache[id] = prod;
    }
    state.productoActual = prod;
    const imgSrc = imagenesProducto[prod.id_catalogo] || 'images/tshirt.svg';
    const precioF = '$' + Number(prod.precio).toLocaleString('es-CO');
    document.getElementById('prod-detail-img').innerHTML = '<img src="' + imgSrc + '" alt="' + prod.articulo + '" style="max-width:100%;max-height:100%;object-fit:contain;border-radius:8px;">';
    document.getElementById('prod-detail-nombre').textContent = prod.articulo;
    document.getElementById('prod-detail-precio').textContent = precioF;
    document.getElementById('prod-detail-desc').textContent = prod.descripcion || 'Sin descripción disponible.';
    document.getElementById('prod-detail-btn').onclick = function () { agregarAlCarrito(id); };
    const esFav = state.favoritos.includes(id);
    document.getElementById('prod-detail-fav').innerHTML = esFav ? '<img src="images/heart.svg" class="icon" alt="" style="vertical-align:middle">' : '<img src="images/heart-outline.svg" class="icon" alt="" style="vertical-align:middle">';
    showScreen('product');
  } catch (err) {
    mostrarMensaje('Error al cargar producto', 'error');
  }
}

function toggleFavorito() {
  const prod = state.productoActual;
  if (!prod) return mostrarMensaje('Selecciona un producto primero', 'error');
  const idx = state.favoritos.indexOf(prod.id_catalogo);
  if (idx === -1) {
    state.favoritos.push(prod.id_catalogo);
    mostrarMensaje(prod.articulo + ' agregado a favoritos <img src="images/heart.svg" class="icon" alt="" style="vertical-align:middle">', 'success');
  } else {
    state.favoritos.splice(idx, 1);
    mostrarMensaje(prod.articulo + ' eliminado de favoritos', 'info');
  }
  localStorage.setItem('lumura_favs', JSON.stringify(state.favoritos));
  const el = document.getElementById('prod-detail-fav');
  if (el) el.innerHTML = idx === -1 ? '<img src="images/heart.svg" class="icon" alt="" style="vertical-align:middle">' : '<img src="images/heart-outline.svg" class="icon" alt="" style="vertical-align:middle">';
}

async function agregarAlCarrito(idProducto) {
  if (!state.token) return mostrarMensaje('Debes iniciar sesión primero', 'error');
  const prod = state.productos.find(p => p.id_catalogo === idProducto) || await api.get('/api/productos/' + idProducto);
  try {
    await api.post('/api/carrito', {
      id_usuario: state.user.id,
      articulo: prod.articulo,
      talla: prod.talla || 'Única',
      color: prod.color || 'Único',
      cantidad: 1,
    });
    mostrarMensaje(prod.articulo + ' agregado al carrito', 'success');
    if (document.getElementById('screen-cart').classList.contains('active')) cargarCarrito();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function cargarCarrito() {
  if (!state.token) return;
  try {
    const items = await api.get('/api/carrito/' + state.user.id);
    state.carrito = items;
    renderCarrito();
  } catch (err) {
    mostrarMensaje('Error al cargar carrito', 'error');
  }
}

function renderCarrito() {
  const cont = document.getElementById('cart-items');
  const totalEl = document.getElementById('cart-total');
  const subtotalEl = document.getElementById('cart-subtotal');
  const contador = document.getElementById('cart-count');
  if (!cont) return;
  if (state.carrito.length === 0) {
    cont.innerHTML = '<p style="padding:40px 16px;text-align:center;color:var(--gray);">Tu carrito está vacío.</p>';
    if (subtotalEl) subtotalEl.textContent = '$0';
    if (totalEl) totalEl.textContent = '$0';
    if (contador) contador.textContent = '0';
    return;
  }
  let total = 0;
  cont.innerHTML = state.carrito.map((item, i) => {
    const precio = Number(item.precio) || 0;
    const cant = Number(item.cantidad) || 1;
    const subtotal = precio * cant;
    total += subtotal;
    const prod = state.productos && state.productos.find(p => p.articulo === item.articulo);
    const imgSrc = prod ? (imagenesProducto[prod.id_catalogo] || 'images/tshirt.svg') : 'images/tshirt.svg';
    return '<div class="cart-item">' +
      '<div class="icon-box" style="background:linear-gradient(135deg,#fce4ec,#f8bbd9);"><img src="' + imgSrc + '" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:8px;"></div>' +
      '<div class="detail">' +
      '<div class="name">' + item.articulo + '</div>' +
      '<div class="sub">Talla: ' + (item.talla || 'Única') + ' · Color: ' + (item.color || 'Único') + '</div>' +
      '<div class="qty-ctrl"><button onclick="cambiarCantidad(' + item.id_carrito + ',-1)">−</button>' +
      '<span style="font-size:14px;font-weight:600;">' + cant + '</span>' +
      '<button onclick="cambiarCantidad(' + item.id_carrito + ',1)">+</button></div>' +
      '</div>' +
      '<div style="text-align:right;"><div class="price">$' + subtotal.toLocaleString('es-CO') + '</div>' +
      '<button class="btn-remove" onclick="eliminarDelCarrito(' + item.id_carrito + ')"><img src="images/trash.svg" class="icon" alt="" style="vertical-align:middle"> Quitar</button></div>' +
      '</div>';
  }).join('');
  if (subtotalEl) subtotalEl.textContent = '$' + total.toLocaleString('es-CO');
  if (totalEl) totalEl.textContent = '$' + total.toLocaleString('es-CO');
  if (contador) contador.textContent = state.carrito.reduce((s, i) => s + Number(i.cantidad), 0);
}

async function cambiarCantidad(idCarrito, delta) {
  const item = state.carrito.find(c => c.id_carrito === idCarrito);
  if (!item) return;
  const nuevaCant = Number(item.cantidad) + delta;
  if (nuevaCant <= 0) return eliminarDelCarrito(idCarrito);
  try {
    await api.put('/api/carrito/' + idCarrito, { cantidad: nuevaCant });
    await cargarCarrito();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function cargarUsuariosAdmin() {
  try {
    const usuarios = await api.get('/api/admin/usuarios');
    const tbody = document.getElementById('admin-users-body');
    if (!tbody) return;
    if (usuarios.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px;">No hay usuarios</td></tr>';
      return;
    }
    tbody.innerHTML = usuarios.map(u => {
      const isAdmin = u.correo_usuario === 'admin@lumura.com';
      return '<tr>'
        + '<td>' + u.id_usuario + '</td>'
        + '<td style="font-weight:600;">' + u.nombre_usuario + '</td>'
        + '<td>' + u.correo_usuario + '</td>'
        + '<td>' + (u.telefono || '-') + '</td>'
        + '<td><span class="badge ' + (isAdmin ? 'badge-green' : 'badge-blue') + '">' + u.rol + '</span></td>'
        + '<td>' + (u.fecha_registro ? new Date(u.fecha_registro).toLocaleDateString('es-CO') : '-') + '</td>'
        + '<td>' + (isAdmin
            ? '<span style="color:var(--gray);font-size:12px;">Protegido</span>'
            : '<button class="btn-danger-sm" onclick="eliminarUsuarioAdmin(' + u.id_usuario + ')">Eliminar</button>')
        + '</td>'
        + '</tr>';
    }).join('');
  } catch (err) {
    mostrarMensaje('Error al cargar usuarios', 'error');
  }
}

async function eliminarUsuarioAdmin(id) {
  if (!confirm('¿Eliminar este usuario? Los datos asociados también se eliminarán.')) return;
  try {
    await api.delete('/api/admin/usuarios/' + id);
    mostrarMensaje('Usuario eliminado correctamente', 'success');
    cargarUsuariosAdmin();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function eliminarDelCarrito(idCarrito) {
  try {
    await api.delete('/api/carrito/' + idCarrito);
    mostrarMensaje('Producto eliminado del carrito', 'info');
    await cargarCarrito();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function handleCheckout() {
  if (!state.token) return mostrarMensaje('Debes iniciar sesión', 'error');
  if (state.carrito.length === 0) return mostrarMensaje('El carrito está vacío', 'error');
  const total = state.carrito.reduce((s, i) => s + Number(i.precio) * Number(i.cantidad), 0);
  const articulos = state.carrito.map(i => i.articulo).join(', ');
  const cantTotal = state.carrito.reduce((s, i) => s + Number(i.cantidad), 0);
  const metodoPago = document.getElementById('checkout-pago')?.value || 'Tarjeta';
  const direccion = document.getElementById('checkout-dir')?.value.trim() || 'Por definir';
  try {
    const res = await api.post('/api/pedidos', {
      id_usuario: state.user.id,
      articulo: articulos,
      cantidad_objetos: cantTotal,
      metodo_pago: metodoPago,
      total: total,
      direccion_entrega: direccion,
    });
    document.getElementById('confirm-numero').textContent = '#LUM-' + res.id;
    const fecha = new Date().toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
    document.getElementById('confirm-fecha').textContent = fecha;
    document.getElementById('confirm-total').textContent = '$' + total.toLocaleString('es-CO');
    showScreen('confirm');
    state.carrito = [];
    renderCarrito();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function verEstadoPedido() {
  showScreen('orders');
}

async function cargarPedidos() {
  if (!state.token) return;
  try {
    const pedidos = await api.get('/api/pedidos/' + state.user.id);
    const cont = document.getElementById('orders-list');
    if (!cont) return;
    if (pedidos.length === 0) {
      cont.innerHTML = '<p style="padding:20px;text-align:center;color:var(--gray);">No tienes pedidos aún.</p>';
      return;
    }
    cont.innerHTML = pedidos.map(p => {
      const est = p.estado_pedido || 'pendiente';
      const badge = est === 'entregado' ? 'badge-green' : est === 'enviado' ? 'badge-blue' : 'badge-red';
      const puedeCancelar = est !== 'cancelado' && est !== 'entregado';
      return '<div style="background:#f8f8fc;border-radius:10px;padding:12px;margin-bottom:10px;">' +
        '<div style="display:flex;justify-content:space-between;margin-bottom:6px;">' +
        '<span style="font-weight:700;">#LUM-' + p.id_compra + '</span>' +
        '<span class="badge ' + badge + '">' + est + '</span></div>' +
        '<div style="font-size:13px;color:var(--gray);">' + (p.articulo || '') + '</div>' +
        '<div style="display:flex;justify-content:space-between;margin-top:6px;font-size:13px;align-items:center;">' +
        '<span>' + new Date(p.fecha_pedido).toLocaleDateString('es-CO') + '</span>' +
        '<span style="font-weight:700;color:var(--accent);">$' + Number(p.total).toLocaleString('es-CO') + '</span>' +
        (puedeCancelar ? '<button class="btn-danger-sm" onclick="confirmarCancelar(' + p.id_compra + ')">Cancelar pedido</button>' : '') +
        '</div></div>';
    }).join('');
  } catch (err) {
    mostrarMensaje('Error al cargar pedidos', 'error');
  }
}

function confirmarCancelar(idPedido) {
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = '<div class="modal-content" style="max-width:360px;text-align:center;">' +
    '<div style="font-size:40px;margin-bottom:12px;"><img src="images/warning.svg" class="icon" alt="" style="width:40px;height:40px;"></div>' +
    '<h3 style="margin-bottom:8px;">Cancelar pedido</h3>' +
    '<p style="color:var(--gray);margin-bottom:20px;">¿Desea cancelar su pedido?</p>' +
    '<div style="display:flex;gap:10px;justify-content:center;">' +
    '<button class="btn-primary" style="width:auto;padding:10px 28px;background:var(--accent);" onclick="cancelarPedido(' + idPedido + ')">Sí</button>' +
    '<button class="btn-secondary" style="width:auto;padding:10px 28px;" onclick="this.closest(\'.modal-overlay\').remove()">No</button>' +
    '</div></div>';
  document.body.appendChild(overlay);
}

async function cancelarPedido(id) {
  document.querySelector('.modal-overlay')?.remove();
  try {
    await api.put('/api/pedidos/' + id + '/cancelar');
    mostrarMensaje('Pedido cancelado correctamente', 'success');
    cargarPedidos();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function cargarPedidosAdmin() {
  try {
    const pedidos = await api.get('/api/admin/pedidos');
    const filtro = document.getElementById('orders-filter')?.value || '';
    const filtrados = filtro ? pedidos.filter(p => p.estado_pedido === filtro) : pedidos;
    const tbody = document.getElementById('admin-orders-body');
    if (!tbody) return;
    if (filtrados.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px;">No hay pedidos</td></tr>';
      return;
    }
    tbody.innerHTML = filtrados.map(p => {
      const badge = p.estado_pedido === 'entregado' ? 'badge-green'
        : p.estado_pedido === 'enviado' ? 'badge-blue'
        : p.estado_pedido === 'cancelado' ? 'badge-red' : 'badge-red';
      return '<tr>'
        + '<td>#LUM-' + p.id_compra + '</td>'
        + '<td>' + (p.id_usuario || '-') + '</td>'
        + '<td>' + (p.articulo || '').split(',').slice(0, 3).join(', ') + '</td>'
        + '<td style="font-weight:700;">$' + Number(p.total).toLocaleString('es-CO') + '</td>'
        + '<td>' + new Date(p.fecha_pedido).toLocaleDateString('es-CO') + '</td>'
        + '<td><span class="badge ' + badge + '">' + (p.estado_pedido || 'pendiente') + '</span></td>'
        + '<td><select onchange="actualizarEstadoPedido(' + p.id_compra + ', this.value)" style="padding:4px 8px;border:1px solid var(--light);border-radius:6px;font-size:12px;outline:none;">'
        + '<option value="">Cambiar a...</option>'
        + '<option value="pendiente">Pendiente</option>'
        + '<option value="enviado">Enviado</option>'
        + '<option value="entregado">Entregado</option>'
        + '<option value="cancelado">Cancelado</option>'
        + '</select></td>'
        + '</tr>';
    }).join('');
  } catch (err) {
    mostrarMensaje('Error al cargar pedidos admin', 'error');
  }
}

async function actualizarEstadoPedido(id, estado) {
  if (!estado) return;
  try {
    await api.put('/api/admin/pedidos/' + id, { estado_pedido: estado });
    mostrarMensaje('Pedido #LUM-' + id + ' actualizado a ' + estado, 'success');
    cargarPedidosAdmin();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function cargarDashboard() {
  try {
    const data = await api.get('/api/admin/dashboard');
    document.getElementById('admin-prod-count').textContent = data.total_productos || 0;
    document.getElementById('admin-user-count').textContent = data.total_usuarios || 0;
    document.getElementById('admin-order-count').textContent = data.total_pedidos || 0;
    document.getElementById('admin-revenue').textContent = '$' + Number(data.ingresos || 0).toLocaleString('es-CO');
  } catch (err) {
    mostrarMensaje('Error al cargar dashboard', 'error');
  }
}

function renderAdminCatalogo(filtroTexto, filtroCat) {
  const tbody = document.getElementById('admin-cat-body');
  if (!tbody) return;
  let items = state.productos;
  if (filtroTexto) items = items.filter(p => p.articulo.toLowerCase().includes(filtroTexto.toLowerCase()));
  if (filtroCat && filtroCat !== 'Todas las categorías') items = items.filter(p => p.categoria === filtroCat);
  if (items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px;">No hay productos</td></tr>';
    return;
  }
  tbody.innerHTML = items.map((p, i) => {
    const stock = Number(p.stock);
    const imgSrc = imagenesProducto[p.id_catalogo] || 'images/tshirt.svg';
    const badge = stock > 0 ? '<span class="badge badge-green">Activo</span>' : '<span class="badge badge-red">Sin stock</span>';
    return '<tr><td>#' + String(p.id_catalogo || i + 1).padStart(3, '0') + '</td>' +
      '<td><div style="display:flex;align-items:center;gap:10px;"><img src="' + imgSrc + '" alt="" style="width:32px;height:32px;object-fit:cover;border-radius:4px;">' +
      '<div><div style="font-weight:600;">' + p.articulo + '</div><div style="font-size:12px;color:var(--gray);">' + (p.categoria || '') + '</div></div></div></td>' +
      '<td>' + (p.categoria || '-') + '</td>' +
      '<td style="font-weight:700;color:var(--accent);">$' + Number(p.precio).toLocaleString('es-CO') + '</td>' +
      '<td>' + (p.talla || '-') + '</td>' +
      '<td>' + badge + '</td>' +
      '<td><button class="btn-sm" style="background:#e3f2fd;color:#1565c0;margin-right:4px;" onclick="abrirModalProducto(' + p.id_catalogo + ')"><img src="images/edit.svg" class="icon" alt="" style="vertical-align:middle"></button>' +
      '<button class="btn-sm" style="background:#fce4ec;color:var(--accent);" onclick="eliminarProducto(' + p.id_catalogo + ')"><img src="images/trash.svg" class="icon" alt="" style="vertical-align:middle"></button></td></tr>';
  }).join('');
}

function filtrarAdminCatalogo() {
  const texto = document.getElementById('admin-search-input')?.value || '';
  const cat = document.getElementById('admin-cat-filter')?.value || '';
  renderAdminCatalogo(texto, cat);
}

function abrirModalProducto(idEditar) {
  const modal = document.getElementById('modal-producto');
  if (!modal) return;
  modal.style.display = 'flex';
  document.getElementById('modal-prod-titulo').textContent = idEditar ? 'Editar prenda' : 'Nueva prenda';
  document.getElementById('modal-prod-id').value = idEditar || '';
  const prod = idEditar ? state.productos.find(p => p.id_catalogo === idEditar) : null;
  document.getElementById('modal-prod-articulo').value = prod ? prod.articulo : '';
  document.getElementById('modal-prod-categoria').value = prod ? (prod.categoria || '') : '';
  document.getElementById('modal-prod-precio').value = prod ? prod.precio : '';
  document.getElementById('modal-prod-talla').value = prod ? (prod.talla || '') : '';
  document.getElementById('modal-prod-color').value = prod ? (prod.color || '') : '';
  document.getElementById('modal-prod-stock').value = prod ? (prod.stock || '') : '';
  document.getElementById('modal-prod-desc').value = prod ? (prod.descripcion || '') : '';
}

function cerrarModalProducto() {
  const modal = document.getElementById('modal-producto');
  if (modal) modal.style.display = 'none';
}

async function guardarProducto() {
  const id = document.getElementById('modal-prod-id').value;
  const articulo = document.getElementById('modal-prod-articulo').value.trim();
  if (!articulo) return mostrarMensaje('El nombre del artículo es obligatorio', 'error');
  const body = {
    articulo,
    categoria: document.getElementById('modal-prod-categoria').value,
    precio: document.getElementById('modal-prod-precio').value || '0',
    talla: document.getElementById('modal-prod-talla').value,
    color: document.getElementById('modal-prod-color').value,
    stock: document.getElementById('modal-prod-stock').value || '0',
    descripcion: document.getElementById('modal-prod-desc').value,
  };
  try {
    if (id) {
      await api.put('/api/admin/productos/' + id, body);
      mostrarMensaje('Prenda actualizada', 'success');
    } else {
      await api.post('/api/admin/productos', body);
      mostrarMensaje('Prenda creada', 'success');
    }
    cerrarModalProducto();
    await cargarProductos();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function eliminarProducto(id) {
  if (!confirm('¿Eliminar este producto?')) return;
  try {
    await api.delete('/api/admin/productos/' + id);
    mostrarMensaje('Producto eliminado', 'success');
    await cargarProductos();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function mostrarBusqueda() {
  const input = document.getElementById('search-input');
  if (input) { input.focus(); input.scrollIntoView({ behavior: 'smooth' }); }
}

let searchTimeout;
function filtrarBusqueda() {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    const term = document.getElementById('search-input')?.value || '';
    const catActiva = document.querySelector('.tag.active')?.dataset?.cat || '';
    renderProductos(catActiva, term);
  }, 200);
}

function mostrarFavoritos() {
  const favs = state.productos.filter(p => state.favoritos.includes(p.id_catalogo));
  if (favs.length === 0) return mostrarMensaje('No tienes favoritos aún', 'info');
  state.productos = favs;
  renderProductos();
  showScreen('home');
  mostrarMensaje('Mostrando ' + favs.length + ' favorito(s) <img src="images/heart.svg" class="icon" alt="" style="vertical-align:middle">', 'success');
  setTimeout(() => { cargarProductos(); }, 5000);
}

function exportarPDF() {
  mostrarMensaje('<img src="images/document.svg" class="icon" alt="" style="vertical-align:middle"> Generando reporte PDF...', 'info');
  setTimeout(() => mostrarMensaje('<img src="images/check.svg" class="icon" alt="" style="vertical-align:middle"> PDF exportado (simulado)', 'success'), 1500);
}

function cargarInventario() {
  const productos = state.productos;
  const totalUnidades = productos.reduce((s, p) => s + Number(p.stock || 0), 0);
  const agotados = productos.filter(p => Number(p.stock) === 0);
  const stockBajo = productos.filter(p => Number(p.stock) > 0 && Number(p.stock) < 10);
  document.getElementById('inv-total-unidades').textContent = totalUnidades;
  document.getElementById('inv-agotados').textContent = agotados.length;
  document.getElementById('inv-stock-bajo').textContent = stockBajo.length;
  const tbody = document.getElementById('inv-body');
  if (!tbody) return;
  if (productos.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px;">No hay productos</td></tr>';
    return;
  }
  tbody.innerHTML = productos.map((p, i) => {
    const stock = Number(p.stock);
    const imgSrc = imagenesProducto[p.id_catalogo] || 'images/tshirt.svg';
    const estado = stock === 0 ? '<span class="status-dot red"></span>Agotado'
      : stock < 10 ? '<span class="status-dot yellow"></span>Bajo'
      : '<span class="status-dot green"></span>Normal';
    const color = stock === 0 ? 'var(--accent)' : stock < 10 ? 'var(--warning)' : 'inherit';
    const btnClass = stock === 0 ? 'background:#fce4ec; color:var(--accent);'
      : stock < 10 ? 'background:#fff9e6; color:#856404;'
      : 'background:var(--light);';
    const btnText = stock === 0 ? 'Urgente' : stock < 10 ? 'Reabastecer' : 'Ajustar';
    return '<tr><td><img src="' + imgSrc + '" alt="" style="width:24px;height:24px;object-fit:cover;border-radius:3px;vertical-align:middle;margin-right:6px;">' + p.articulo + '</td>'
      + '<td>' + (p.categoria || '-') + '</td>'
      + '<td>' + (p.talla || '-') + '</td>'
      + '<td>' + (p.color || '-') + '</td>'
      + '<td><span style="font-weight:700;color:' + color + ';">' + stock + '</span> uds.</td>'
      + '<td>' + estado + '</td>'
      + '<td><button class="btn-sm" style="' + btnClass + '" onclick="proximamente(\'Ajuste de stock\')">' + btnText + '</button></td>'
      + '</tr>';
  }).join('');
}

async function cargarReportes() {
  try {
    const data = await api.get('/api/admin/dashboard');
    document.getElementById('rep-ingresos').textContent = '$' + Number(data.ingresos || 0).toLocaleString('es-CO');
    document.getElementById('rep-pedidos').textContent = data.total_pedidos || 0;
    const pedidos = await api.get('/api/admin/pedidos');
    const tbody = document.getElementById('rep-transacciones');
    if (tbody && pedidos.length > 0) {
      tbody.innerHTML = pedidos.slice(0, 5).map(p => {
        const badge = p.estado_pedido === 'entregado' ? 'badge-green'
          : p.estado_pedido === 'enviado' ? 'badge-blue' : 'badge-red';
        return '<tr>'
          + '<td>#LUM-' + p.id_compra + '</td>'
          + '<td>' + (p.articulo || '').split(',').slice(0, 2).join(', ') + '</td>'
          + '<td style="font-weight:700;">$' + Number(p.total).toLocaleString('es-CO') + '</td>'
          + '<td>' + new Date(p.fecha_pedido).toLocaleDateString('es-CO') + '</td>'
          + '<td><span class="badge ' + badge + '">' + (p.estado_pedido || 'pendiente') + '</span></td>'
          + '</tr>';
      }).join('');
    }
  } catch (err) {
    mostrarMensaje('Error al cargar reportes', 'error');
  }
}

function actualizarUI() {
  const estaLogueado = !!state.token;
  document.querySelectorAll('.auth-only').forEach(el => el.style.display = estaLogueado ? '' : 'none');
  document.querySelectorAll('.no-auth').forEach(el => el.style.display = estaLogueado ? 'none' : '');
  const userLink = document.getElementById('header-user-link');
  if (userLink) {
    if (estaLogueado && state.user) {
      userLink.innerHTML = '<span class="user-menu-wrap" style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;" onclick="toggleUserMenu(event)"><img src="images/user.svg" class="icon" alt="" style="width:16px;height:16px;vertical-align:middle"> ' + state.user.nombre + ' <span style="font-size:10px;margin-left:2px;">▾</span></span>';
      userLink.onclick = null;
    } else {
      userLink.innerHTML = '<img src="images/user.svg" class="icon" alt="" style="width:16px;height:16px;vertical-align:middle"> Iniciar sesión';
      userLink.onclick = function() { showScreen('login'); };
    }
  }
  const adminLink = document.getElementById('header-admin-link');
  if (adminLink) {
    adminLink.style.display = (estaLogueado && state.user?.rol === 'ADMIN') ? 'flex' : 'none';
  }
}

function toggleUserMenu(e) {
  e.stopPropagation();
  const existing = document.querySelector('.user-dropdown');
  if (existing) { existing.remove(); return; }
  const wrap = e.currentTarget;
  const menu = document.createElement('div');
  menu.className = 'user-dropdown';
  menu.innerHTML =
    '<a onclick="cerrarSesion(); document.querySelector(\'.user-dropdown\')?.remove();"><img src="images/logout.svg" class="icon" alt="" style="width:14px;height:14px;"> Cerrar sesión</a>' +
    '<a class="danger" onclick="confirmarEliminarCuenta(); document.querySelector(\'.user-dropdown\')?.remove();"><img src="images/trash.svg" class="icon" alt="" style="width:14px;height:14px;"> Eliminar cuenta</a>';
  wrap.parentNode.appendChild(menu);
  setTimeout(() => document.addEventListener('click', cerrarUserMenu), 10);
}

function cerrarUserMenu() {
  document.querySelector('.user-dropdown')?.remove();
  document.removeEventListener('click', cerrarUserMenu);
}

function filtrarCategoria(cat) {
  document.querySelectorAll('.tag').forEach(t => t.classList.remove('active'));
  if (cat) {
    document.querySelector('.tag[data-cat="' + cat + '"]').classList.add('active');
  } else {
    document.querySelector('.tag[data-cat=""]').classList.add('active');
  }
  const term = document.getElementById('search-input')?.value || '';
  renderProductos(cat, term);
}

function showScreen(name) {
  if (name.startsWith('admin-') && (!state.token || state.user?.rol !== 'ADMIN')) {
    mostrarMensaje('Acceso denegado — Solo administradores', 'error');
    return;
  }
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  const screen = document.getElementById('screen-' + name);
  if (screen) screen.classList.add('active');
  const btn = document.getElementById('btn-' + name);
  if (btn) btn.classList.add('active');
  window.scrollTo(0, 0);
  if (name === 'home') cargarProductos();
  if (name === 'cart' && state.token) cargarCarrito();
  if (name === 'checkout' && state.token) {
    const total = state.carrito.reduce((s, i) => s + Number(i.precio) * Number(i.cantidad), 0);
    const cant = state.carrito.reduce((s, i) => s + Number(i.cantidad), 0);
    document.getElementById('checkout-count').textContent = cant + ' productos - $' + total.toLocaleString('es-CO');
    document.getElementById('checkout-total').textContent = '$' + total.toLocaleString('es-CO');
    const dirEl = document.getElementById('checkout-dir');
    if (dirEl) dirEl.value = state.user?.direccion || '';
  }
  if (name === 'admin-dash') { cargarDashboard(); cargarProductos(); }
  if (name === 'admin-cat') cargarProductos();
  if (name === 'admin-inv') cargarProductos();
  if (name === 'admin-rep') { cargarProductos(); cargarReportes(); }
  if (name === 'admin-orders') cargarPedidosAdmin();
  if (name === 'admin-users') cargarUsuariosAdmin();
  if (name === 'orders' && state.token) cargarPedidos();
}

document.addEventListener('DOMContentLoaded', function () {
  const loginForm = document.getElementById('login-form');
  if (loginForm) loginForm.addEventListener('submit', handleLogin);
  const regForm = document.getElementById('reg-form');
  if (regForm) regForm.addEventListener('submit', handleRegister);
  document.querySelectorAll('.logout-btn').forEach(b => b.addEventListener('click', cerrarSesion));
  document.querySelectorAll('.tag').forEach(t => {
    t.addEventListener('click', function () {
      filtrarCategoria(this.dataset.cat);
    });
  });
  actualizarUI();
  if (state.token) cargarProductos();
});
