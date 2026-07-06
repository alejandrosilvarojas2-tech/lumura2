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
  mostrarMensaje('🔧 ' + (func || 'Función') + ' — Próximamente', 'info');
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

async function cargarProductos() {
  try {
    state.productos = await api.get('/api/productos');
    renderProductos();
    renderAdminCatalogo();
  } catch (err) {
    mostrarMensaje('Error al cargar productos: ' + err.message, 'error');
  }
}

const iconosProducto = ['👕', '👖', '🧥', '👗', '👟', '🧢', '🧣', '👒'];

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
    const icono = iconosProducto[i % iconosProducto.length];
    const precioF = '$' + Number(p.precio).toLocaleString('es-CO');
    return '<div class="product-card" onclick="verProducto(' + p.id_catalogo + ')">' +
      '<div class="img-placeholder" style="background:linear-gradient(135deg,#fce4ec,#f8bbd9);font-size:40px;">' + icono + '</div>' +
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
    const icono = iconosProducto[id % iconosProducto.length];
    const precioF = '$' + Number(prod.precio).toLocaleString('es-CO');
    document.getElementById('prod-detail-img').textContent = icono;
    document.getElementById('prod-detail-nombre').textContent = prod.articulo;
    document.getElementById('prod-detail-precio').textContent = precioF;
    document.getElementById('prod-detail-desc').textContent = prod.descripcion || 'Sin descripción disponible.';
    document.getElementById('prod-detail-btn').onclick = function () { agregarAlCarrito(id); };
    const esFav = state.favoritos.includes(id);
    document.getElementById('prod-detail-fav').textContent = esFav ? '❤️' : '🤍';
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
    mostrarMensaje(prod.articulo + ' agregado a favoritos ❤️', 'success');
  } else {
    state.favoritos.splice(idx, 1);
    mostrarMensaje(prod.articulo + ' eliminado de favoritos', 'info');
  }
  localStorage.setItem('lumura_favs', JSON.stringify(state.favoritos));
  const el = document.getElementById('prod-detail-fav');
  if (el) el.textContent = idx === -1 ? '❤️' : '🤍';
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
  const iconos = ['👕', '👖', '🧥', '👗', '👟'];
  let total = 0;
  cont.innerHTML = state.carrito.map((item, i) => {
    const precio = Number(item.precio) || 0;
    const cant = Number(item.cantidad) || 1;
    const subtotal = precio * cant;
    total += subtotal;
    const icono = iconos[i % iconos.length];
    return '<div class="cart-item">' +
      '<div class="icon-box" style="background:linear-gradient(135deg,#fce4ec,#f8bbd9);">' + icono + '</div>' +
      '<div class="detail">' +
      '<div class="name">' + item.articulo + '</div>' +
      '<div class="sub">Talla: ' + (item.talla || 'Única') + ' · Color: ' + (item.color || 'Único') + '</div>' +
      '<div class="qty-ctrl"><button onclick="cambiarCantidad(' + item.id_carrito + ',-1)">−</button>' +
      '<span style="font-size:14px;font-weight:600;">' + cant + '</span>' +
      '<button onclick="cambiarCantidad(' + item.id_carrito + ',1)">+</button></div>' +
      '</div>' +
      '<div style="text-align:right;"><div class="price">$' + subtotal.toLocaleString('es-CO') + '</div>' +
      '<span style="font-size:18px;cursor:pointer;color:var(--gray);" onclick="eliminarDelCarrito(' + item.id_carrito + ')">🗑️</span></div>' +
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
      return '<div style="background:#f8f8fc;border-radius:10px;padding:12px;margin-bottom:10px;">' +
        '<div style="display:flex;justify-content:space-between;margin-bottom:6px;">' +
        '<span style="font-weight:700;">#LUM-' + p.id_compra + '</span>' +
        '<span class="badge ' + badge + '">' + est + '</span></div>' +
        '<div style="font-size:13px;color:var(--gray);">' + (p.articulo || '') + '</div>' +
        '<div style="display:flex;justify-content:space-between;margin-top:6px;font-size:13px;">' +
        '<span>' + new Date(p.fecha_pedido).toLocaleDateString('es-CO') + '</span>' +
        '<span style="font-weight:700;color:var(--accent);">$' + Number(p.total).toLocaleString('es-CO') + '</span></div>' +
        '</div>';
    }).join('');
  } catch (err) {
    mostrarMensaje('Error al cargar pedidos', 'error');
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
  const iconos = ['👕', '👖', '🧥', '👗', '👟'];
  tbody.innerHTML = items.map((p, i) => {
    const stock = Number(p.stock);
    const badge = stock > 0 ? '<span class="badge badge-green">Activo</span>' : '<span class="badge badge-red">Sin stock</span>';
    return '<tr><td>#' + String(p.id_catalogo || i + 1).padStart(3, '0') + '</td>' +
      '<td><div style="display:flex;align-items:center;gap:10px;"><span style="font-size:24px;">' + iconos[i % iconos.length] + '</span>' +
      '<div><div style="font-weight:600;">' + p.articulo + '</div><div style="font-size:12px;color:var(--gray);">' + (p.categoria || '') + '</div></div></div></td>' +
      '<td>' + (p.categoria || '-') + '</td>' +
      '<td style="font-weight:700;color:var(--accent);">$' + Number(p.precio).toLocaleString('es-CO') + '</td>' +
      '<td>' + (p.talla || '-') + '</td>' +
      '<td>' + badge + '</td>' +
      '<td><button class="btn-sm" style="background:#e3f2fd;color:#1565c0;margin-right:4px;" onclick="abrirModalProducto(' + p.id_catalogo + ')">✏️</button>' +
      '<button class="btn-sm" style="background:#fce4ec;color:var(--accent);" onclick="eliminarProducto(' + p.id_catalogo + ')">🗑️</button></td></tr>';
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
  mostrarMensaje('Mostrando ' + favs.length + ' favorito(s) ❤️', 'success');
  setTimeout(() => { cargarProductos(); }, 5000);
}

function exportarPDF() {
  mostrarMensaje('📄 Generando reporte PDF...', 'info');
  setTimeout(() => mostrarMensaje('✅ PDF exportado (simulado)', 'success'), 1500);
}

function actualizarUI() {
  const estaLogueado = !!state.token;
  document.querySelectorAll('.auth-only').forEach(el => el.style.display = estaLogueado ? '' : 'none');
  document.querySelectorAll('.no-auth').forEach(el => el.style.display = estaLogueado ? 'none' : '');
  if (estaLogueado && state.user) {
    document.querySelectorAll('.user-name').forEach(el => el.textContent = state.user.nombre);
    const esAdmin = state.user.rol === 'ADMIN';
    document.querySelectorAll('.admin-only').forEach(el => el.style.display = esAdmin ? '' : 'none');
  }
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
  document.querySelectorAll('.proto-nav button').forEach(b => b.classList.remove('active'));
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
