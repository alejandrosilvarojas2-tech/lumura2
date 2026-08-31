const state = {
  token: localStorage.getItem('lumura_token'),
  user: JSON.parse(localStorage.getItem('lumura_user') || 'null'),
  productos: [],
  carrito: [],
  productoActual: null,
  favoritos: JSON.parse(localStorage.getItem('lumura_favs') || '[]'),
};

function escHtml(s) {
  if (!s) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

async function parseRespuesta(res) {
  const texto = await res.text();
  if (!texto) return null;
  try {
    return JSON.parse(texto);
  } catch {
    return texto;
  }
}

const api = {
  async request(method, path, body) {
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (state.token) headers['Authorization'] = `Bearer ${state.token}`;
    let res;
    try {
      res = await fetch(path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
    } catch {
      throw new Error('No hay conexión con el servidor');
    }
    const data = await parseRespuesta(res);
    if (res.status === 401 && state.token) {
      cerrarSesion();
      mostrarMensaje('Tu sesión expiró. Inicia sesión de nuevo', 'error');
      throw new Error('Sesión expirada');
    }
    if (!res.ok) {
      const msg = (data && typeof data === 'object' && (data.error || data.message))
        || `Error del servidor (${res.status})`;
      throw new Error(msg);
    }
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
    sincronizarFavoritos();
    mostrarMensaje('Bienvenido, ' + data.usuario.nombre, 'success');
    if (data.usuario.rol === 'ADMIN') {
      showScreen('admin');
    } else if (data.usuario.rol === 'ALIADO') {
      showScreen('aliado');
    } else {
      document.getElementById('modal-politicas').style.display = 'flex';
    }
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function solicitarRecuperacion() {
  const correo = document.getElementById('recuperar-email').value.trim();
  if (!correo) return mostrarMensaje('Ingresa tu correo', 'error');
  try {
    const data = await api.post('/api/auth/recuperar', { correo_usuario: correo });
    mostrarMensaje(data.mensaje, 'success');
    const enlaceEl = document.getElementById('recuperar-enlace');
    if (enlaceEl && data.enlace_demo) {
      enlaceEl.value = data.enlace_demo;
      window._resetToken = data.enlace_demo.split('token=')[1];
    }
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function usarEnlaceRecuperacion() {
  if (!window._resetToken) return mostrarMensaje('Primero solicita el enlace de recuperación', 'error');
  showScreen('reset-password');
}

async function confirmarReset() {
  const nueva = document.getElementById('reset-pass').value;
  const confirmar = document.getElementById('reset-pass2').value;
  if (!window._resetToken) return mostrarMensaje('Token no disponible', 'error');
  if (!nueva || nueva.length < 6) return mostrarMensaje('La contraseña debe tener al menos 6 caracteres', 'error');
  if (nueva !== confirmar) return mostrarMensaje('Las contraseñas no coinciden', 'error');
  try {
    const data = await api.post('/api/auth/reset-password', {
      token: window._resetToken,
      nueva_password: nueva,
      confirmar_password: confirmar,
    });
    mostrarMensaje(data.mensaje, 'success');
    window._resetToken = null;
    document.getElementById('reset-pass').value = '';
    document.getElementById('reset-pass2').value = '';
    showScreen('login');
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function aceptarPoliticas() {
  document.getElementById('modal-politicas').style.display = 'none';
  irInicioPorRol();
}

function rechazarPoliticas() {
  document.getElementById('modal-politicas').style.display = 'none';
  cerrarSesion();
  mostrarMensaje('Debes aceptar las políticas para usar LUMURA', 'error');
}

function accederAliado() {
  const correo = document.getElementById('aliado-login-email').value.trim();
  const password = document.getElementById('aliado-login-pass').value;
  if (!correo || !password) return mostrarMensaje('Ingresa tu correo y contraseña', 'error');
  api.post('/api/auth/login', { correo_usuario: correo, password })
    .then(function(data) {
      const rol = data.usuario?.rol;
      if (rol !== 'ALIADO') {
        mostrarMensaje('No eres aliado', 'error');
        return;
      }
      state.token = data.token;
      state.user = data.usuario;
      localStorage.setItem('lumura_token', data.token);
      localStorage.setItem('lumura_user', JSON.stringify(data.usuario));
      actualizarUI();
      sincronizarFavoritos();
      mostrarMensaje('Bienvenido, ' + data.usuario.nombre, 'success');
      showScreen('aliado');
    })
    .catch(function(err) {
      mostrarMensaje(err.message || 'Credenciales inválidas', 'error');
    });
}

function abrirRegistroAliado() {
  const emailInput = document.getElementById('aliado-email');
  if (emailInput && state.user?.correo) emailInput.value = state.user.correo;
  showScreen('register-aliado');
  mostrarMensaje('Completa tu registro para vender como aliado', 'info');
}

function postLoginRedirect(tipo) {
  if (tipo === 'aliado') {
    showScreen('aliado-login');
  } else {
    showScreen('home');
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
      confirmar_password: pass2,
    });
    try {
      const data = await api.post('/api/auth/login', { correo_usuario: email, password: pass });
      state.token = data.token;
      state.user = data.usuario;
      localStorage.setItem('lumura_token', data.token);
      localStorage.setItem('lumura_user', JSON.stringify(data.usuario));
      actualizarUI();
      sincronizarFavoritos();
    } catch (err) { /* si el auto-login falla, el usuario entra por el login */ }
    if (state.token) {
      mostrarMensaje('Cuenta creada. Bienvenido, ' + (state.user?.nombre || email), 'success');
      document.getElementById('modal-politicas').style.display = 'flex';
    } else {
      mostrarMensaje('Registro exitoso. Inicia sesión.', 'success');
      showScreen('login');
    }
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function handleRegisterAliado(e) {
  e.preventDefault();
  const nombreNegocio = document.getElementById('aliado-nombre').value.trim();
  const nit = document.getElementById('aliado-nit').value.trim();
  const tel = document.getElementById('aliado-tel').value.trim();
  const contacto = document.getElementById('aliado-contacto').value.trim();
  const email = document.getElementById('aliado-email').value.trim();
  const categoria = document.getElementById('aliado-categoria').value;
  const dir = document.getElementById('aliado-direccion').value.trim();
  const pass = document.getElementById('aliado-pass').value;
  const pass2 = document.getElementById('aliado-pass2').value;
  if (!nombreNegocio || !nit || !contacto || !email || !pass) {
    return mostrarMensaje('Completa los campos obligatorios', 'error');
  }
  if (!categoria) return mostrarMensaje('Selecciona la categoría de productos a vender', 'error');
  if (!dir) return mostrarMensaje('Ingresa la dirección del punto de venta', 'error');
  if (pass !== pass2) return mostrarMensaje('Las contraseñas no coinciden', 'error');
  try {
    await api.post('/api/auth/register-aliado', {
      nombre_negocio: nombreNegocio,
      nit: nit,
      telefono: tel,
      persona_contacto: contacto,
      correo_usuario: email,
      direccion: dir,
      categoria_productos: categoria,
      password: pass,
      confirmar_password: pass2,
    });
    mostrarMensaje('Aliado registrado exitosamente. Inicia sesión.', 'success');
    showScreen('login');
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

let aliadoImgUrl = '';

function handleAliadoImgSelect(input) {
  const file = input.files[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    mostrarMensaje('La imagen no puede superar 5MB', 'error');
    input.value = '';
    return;
  }
  const reader = new FileReader();
  reader.onload = function(e) {
    const preview = document.getElementById('aliado-img-preview');
    preview.src = e.target.result;
    preview.style.width = '120px';
    preview.style.height = '120px';
    preview.style.objectFit = 'cover';
    preview.style.borderRadius = '8px';
    preview.style.opacity = '1';
    document.getElementById('aliado-img-text').textContent = file.name;
  };
  reader.readAsDataURL(file);
}

function handleAliadoImgDrop(e) {
  const file = e.dataTransfer.files[0];
  if (file && file.type.startsWith('image/')) {
    const input = document.getElementById('aliado-art-img');
    input.files = e.dataTransfer.files;
    handleAliadoImgSelect(input);
  }
}

async function subirImagenAliado() {
  const input = document.getElementById('aliado-art-img');
  if (!input.files[0]) return null;
  const formData = new FormData();
  formData.append('file', input.files[0]);
  const res = await fetch('/api/admin/upload', {
    method: 'POST',
    headers: { 'Authorization': 'Bearer ' + state.token },
    body: formData,
  });
  const data = await parseRespuesta(res);
  if (!res.ok) {
    throw new Error((data && typeof data === 'object' && data.error) || `Error al subir imagen (${res.status})`);
  }
  return data && data.url;
}

function handleLicenciaImgSelect(input) {
  const file = input.files[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    mostrarMensaje('La imagen no puede superar 5MB', 'error');
    input.value = '';
    return;
  }
  const reader = new FileReader();
  reader.onload = function(e) {
    const prev = document.getElementById('aliado-licencia-img-preview');
    prev.src = e.target.result;
    document.getElementById('aliado-licencia-preview').style.display = 'block';
  };
  reader.readAsDataURL(file);
  document.getElementById('aliado-licencia-msg').textContent = '';
}

async function cargarLicenciaAliado() {
  const msg = document.getElementById('aliado-licencia-msg');
  const actual = document.getElementById('aliado-licencia-actual');
  const prevBlock = document.getElementById('aliado-licencia-preview');
  const input = document.getElementById('aliado-licencia-img');
  if (msg) msg.textContent = '';
  if (prevBlock) prevBlock.style.display = 'none';
  if (input) input.value = '';
  try {
    const res = await api.get('/api/aliado/licencia');
    const licencia = res?.licencia || '';
    if (licencia) {
      document.getElementById('aliado-licencia-img-actual').src = licencia;
      actual.style.display = 'block';
    } else {
      actual.style.display = 'none';
    }
  } catch (e) {
    if (actual) actual.style.display = 'none';
  }
}

async function guardarLicenciaAliado() {
  const input = document.getElementById('aliado-licencia-img');
  if (!input.files[0]) {
    mostrarMensaje('Selecciona una imagen de tu licencia de distribuidor', 'error');
    return;
  }
  let url;
  try {
    mostrarMensaje('Subiendo imagen...', 'info');
    const formData = new FormData();
    formData.append('file', input.files[0]);
    const res = await fetch('/api/admin/upload', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + state.token },
      body: formData,
    });
    const data = await parseRespuesta(res);
    if (!res.ok) throw new Error((data && data.error) || `Error al subir imagen (${res.status})`);
    url = data.url;
  } catch (err) {
    return mostrarMensaje(err.message, 'error');
  }
  try {
    const res = await api.put('/api/aliado/licencia', { licencia: url });
    mostrarMensaje('Licencia de distribuidor guardada correctamente', 'éxito');
    document.getElementById('aliado-licencia-msg').textContent = 'Licencia guardada correctamente';
    document.getElementById('aliado-licencia-img-actual').src = url;
    document.getElementById('aliado-licencia-actual').style.display = 'block';
    document.getElementById('aliado-licencia-preview').style.display = 'none';
  } catch (err) {
    mostrarMensaje(err.message || 'No se pudo guardar la licencia', 'error');
  }
}

function volverPanelLicencia() {
  mostrarMenuAliado('dash');
}

const datosPlanes = {
  basico: {
    nombre: 'Membresía Básico',
    precio: '$10.000 COP/mes',
    detalle: 'Con la Membresía Básico de Lumura, disfruta de tu primer mes completamente gratis. A partir de ahí, el valor mensual es de $10.000 COP, con renovación automática cada mes. Además, cuentas con un día de gracia adicional después del cierre del mes para que el descuento se genere sin contratiempos en tu tarjeta de crédito o débito asociada.'
  },
  medio: {
    nombre: 'Membresía Medio',
    precio: '$60.000 COP',
    detalle: 'La Membresía Medio te ofrece dos meses completamente gratis para que aproveches al máximo los beneficios de ser aliado de Lumura. Su valor es de $60.000 COP y tiene una vigencia de 8 meses, renovándose automáticamente al finalizar cada periodo, garantizando continuidad sin necesidad de trámites adicionales.'
  },
  premium: {
    nombre: 'Membresía Premium (Empresas)',
    precio: '$90.000 COP',
    detalle: 'Diseñada para empresas que buscan crecer junto a Lumura, la Membresía Premium tiene una vigencia de 12 meses por un valor de $90.000 COP, renovándose automáticamente al finalizar el periodo. Como beneficio exclusivo, tu marca recibe publicidad aleatoria en redes sociales y plataformas asociadas de Lumura, aumentando tu visibilidad ante una audiencia más amplia sin esfuerzo adicional de tu parte, posicionando tu negocio de forma constante y estratégica.'
  }
};

let planAliadoPendiente = null;

function elegirPlanAliado(plan) {
  const p = datosPlanes[plan];
  if (!p) return;
  const cont = document.getElementById('pago-plan-detalle');
  if (cont) {
    cont.innerHTML = '<div style="font-size:18px;font-weight:800;margin-bottom:4px;">' + p.nombre + '</div>'
      + '<div style="font-size:24px;font-weight:800;color:var(--accent);margin-bottom:8px;">' + p.precio + '</div>'
      + '<p style="font-size:13px;color:var(--gray);line-height:1.6;margin:0;">' + p.detalle + '</p>';
  }
  planAliadoPendiente = plan;
  const modal = document.getElementById('modal-aviso-retracto');
  if (modal) modal.style.display = 'flex';
}

function aceptarRetracto() {
  const modal = document.getElementById('modal-aviso-retracto');
  if (modal) modal.style.display = 'none';
  mostrarMenuAliado(planAliadoPendiente ? 'pago' : 'membresias');
}

function cancelarRetracto() {
  const modal = document.getElementById('modal-aviso-retracto');
  if (modal) modal.style.display = 'none';
  mostrarMenuAliado('membresias');
}

function confirmarPagoAliado() {
  const det = document.getElementById('pago-plan-detalle');
  const nombre = det?.querySelector('div')?.textContent || 'tu plan';
  mostrarMensaje('Pago de ' + nombre + ' confirmado correctamente', 'success');
  setTimeout(function () { mostrarMenuAliado('stock'); }, 1200);
}


async function guardarArticuloAliado(e) {
  e.preventDefault();
  const nombre = document.getElementById('aliado-art-nombre').value.trim();
  const precio = document.getElementById('aliado-art-precio').value.trim();
  if (!nombre) return mostrarMensaje('El nombre del artículo es obligatorio', 'error');
  if (!precio || parseFloat(precio) < 0) return mostrarMensaje('Ingresa un precio válido', 'error');
  if (!document.getElementById('aliado-art-categoria').value) return mostrarMensaje('Selecciona la categoría del artículo', 'error');

  const stock = parseInt(document.getElementById('aliado-art-stock').value) || 0;
  if (stock > 10000) return mostrarMensaje('El stock no puede superar 10,000 unidades', 'error');

  document.getElementById('modal-aviso-compromiso').style.display = 'flex';
}

async function publicarArticuloAliado() {
  const nombre = document.getElementById('aliado-art-nombre').value.trim();
  const precio = document.getElementById('aliado-art-precio').value.trim();
  const fileInput = document.getElementById('aliado-art-img');
  let url = null;
  if (fileInput.files[0]) {
    try {
      mostrarMensaje('Subiendo imagen...', 'info');
      url = await subirImagenAliado();
    } catch (err) {
      return mostrarMensaje(err.message, 'error');
    }
  }

  const stock = parseInt(document.getElementById('aliado-art-stock').value) || 0;

  try {
    const producto = await api.post('/api/aliado/productos', {
      articulo: nombre,
      precio: precio,
      categoria: document.getElementById('aliado-art-categoria').value.trim(),
      talla: document.getElementById('aliado-art-talla').value.trim(),
      color: document.getElementById('aliado-art-color').value.trim(),
      stock: String(stock),
      descripcion: document.getElementById('aliado-art-descripcion').value.trim(),
      imagen_url: url,
    });
    mostrarMensaje('Artículo "' + producto.articulo + '" publicado correctamente', 'success');
    document.getElementById('aliado-add-form').reset();
    resetAliadoFormulario();
    await cargarProductos();
    mostrarMenuAliado('membresias');
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function aceptarAvisoCompromiso() {
  document.getElementById('modal-aviso-compromiso').style.display = 'none';
  publicarArticuloAliado();
}

function rechazarAvisoCompromiso() {
  document.getElementById('modal-aviso-compromiso').style.display = 'none';
  mostrarMensaje('Debes aceptar el aviso de compromiso para publicar el artículo', 'error');
}

function resetAliadoFormulario() {
  const preview = document.getElementById('aliado-img-preview');
  if (preview) {
    preview.src = 'images/upload.svg';
    preview.style.width = '64px';
    preview.style.height = '64px';
    preview.style.objectFit = '';
    preview.style.borderRadius = '';
    preview.style.opacity = '0.4';
  }
  const txt = document.getElementById('aliado-img-text');
  if (txt) txt.textContent = 'Arrastra una imagen o haz clic aquí';
  const count = document.getElementById('aliado-art-desc-count');
  if (count) count.textContent = '0';
}

function actualizarContadorArtDesc(textarea) {
  const counter = document.getElementById('aliado-art-desc-count');
  if (counter) counter.textContent = textarea.value.length;
}

let aliadoProductosCache = [];

async function cargarAliadoStock() {
  try {
    const productos = await api.get('/api/aliado/productos');
    aliadoProductosCache = productos;
    const tbody = document.getElementById('aliado-stock-body');
    if (!tbody) return;
    if (productos.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px;">No hay productos</td></tr>';
      return;
    }
    tbody.innerHTML = productos.map(p => {
      const estado = p.stock === 0 ? '<span class="badge badge-red">Sin stock</span>'
        : p.stock < 10 ? '<span class="badge badge-yellow">Bajo</span>'
        : '<span class="badge badge-green">OK</span>';
      return '<tr>'
        + '<td>' + (p.codigo ? escHtml(p.codigo) : '#' + String(p.id_catalogo).padStart(3, '0')) + '</td>'
        + '<td>' + escHtml(p.articulo) + '</td>'
        + '<td>' + escHtml(p.categoria || '-') + '</td>'
        + '<td style="font-weight:700;">' + p.stock + '</td>'
        + '<td style="display:flex;align-items:center;gap:6px;">'
        + '<input type="number" min="0" max="10000" value="0" style="width:70px;padding:4px 8px;border:1.5px solid var(--light);border-radius:6px;text-align:center;" id="stock-add-' + p.id_catalogo + '">'
        + '<button class="btn-sm" style="padding:4px 10px;font-size:12px;background:var(--accent);color:white;" onclick="agregarStockAliado(' + p.id_catalogo + ', ' + p.stock + ')">Agregar</button>'
        + '</td>'
        + '<td>' + estado + '</td>'
        + '<td style="display:flex;gap:6px;">'
        + '<button class="btn-sm" style="padding:4px 10px;font-size:12px;" onclick="abrirEditarProductoAliado(' + p.id_catalogo + ')">Editar</button>'
        + '<button class="btn-sm" style="padding:4px 10px;font-size:12px;background:#dc3545;color:white;" onclick="confirmarEliminarProductoAliado(' + p.id_catalogo + ')">Eliminar</button>'
        + '</td>'
        + '</tr>';
    }).join('');
  } catch (err) {
    mostrarMensaje('Error al cargar stock', 'error');
  }
}

async function agregarStockAliado(id, stockActual) {
  const input = document.getElementById('stock-add-' + id);
  if (!input) return;
  const cantidad = parseInt(input.value);
  if (!cantidad || cantidad <= 0) {
    return mostrarMensaje('Ingresa una cantidad válida', 'error');
  }
  if (cantidad > 10000) {
    return mostrarMensaje('No se pueden agregar más de 10,000 unidades', 'error');
  }
  const nuevoStock = stockActual + cantidad;
  if (nuevoStock > 10000) {
    return mostrarMensaje('El stock total no puede superar 10,000 unidades', 'error');
  }
  try {
    await api.put('/api/aliado/productos/' + id, { stock: nuevoStock });
    mostrarMensaje('+' + cantidad + ' unidades agregadas. Stock total: ' + nuevoStock, 'success');
    input.value = 0;
    cargarAliadoStock();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function cargarAliadoDesc() {
  try {
    const productos = await api.get('/api/aliado/productos');
    const container = document.getElementById('aliado-desc-list');
    if (!container) return;
    if (productos.length === 0) {
      container.innerHTML = '<div class="card" style="padding:24px;text-align:center;color:var(--gray);">No hay productos</div>';
      return;
    }
    container.innerHTML = productos.map(p => {
      const desc = escHtml(p.descripcion || '');
      return '<div class="card" style="padding:16px;margin-bottom:12px;">'
        + '<div style="font-weight:700;margin-bottom:8px;">' + escHtml(p.articulo) + ' <span style="color:var(--gray);font-size:12px;">' + (p.codigo ? escHtml(p.codigo) : '#' + String(p.id_catalogo).padStart(3, '0')) + '</span></div>'
        + '<textarea id="desc-' + p.id_catalogo + '" rows="3" maxlength="500" oninput="actualizarContadorDesc(this)" style="width:100%;padding:10px;border:1.5px solid var(--light);border-radius:8px;font-family:inherit;resize:vertical;">' + desc + '</textarea>'
        + '<div style="display:flex;justify-content:space-between;align-items:center;margin-top:6px;">'
        + '<span style="font-size:12px;color:var(--gray);"><span class="desc-count" data-for="' + p.id_catalogo + '">' + (p.descripcion || '').length + '</span>/500</span>'
        + '<button class="btn-primary" style="padding:8px 16px;font-size:13px;" onclick="guardarDescripcionAliado(' + p.id_catalogo + ')">Guardar descripción</button>'
        + '</div></div>';
    }).join('');
  } catch (err) {
    mostrarMensaje('Error al cargar descripciones', 'error');
  }
}

function actualizarContadorDesc(textarea) {
  const id = textarea.id.replace('desc-', '');
  const counter = document.querySelector('.desc-count[data-for="' + id + '"]');
  if (counter) counter.textContent = textarea.value.length;
}

async function guardarDescripcionAliado(id) {
  const textarea = document.getElementById('desc-' + id);
  if (!textarea) return;
  const texto = textarea.value.trim();
  if (texto.length > 500) {
    return mostrarMensaje('La descripción no puede superar 500 caracteres', 'error');
  }
  try {
    await api.put('/api/aliado/productos/' + id, { descripcion: texto });
    mostrarMensaje('Descripción actualizada', 'success');
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function confirmarEliminarProductoAliado(id) {
  const p = aliadoProductosCache.find(x => x.id_catalogo === id);
  const nombre = p ? escHtml(p.articulo) : '#' + id;
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = '<div class="modal-content" style="max-width:380px;text-align:center;">'
    + '<h3 style="margin-bottom:8px;">Eliminar producto</h3>'
    + '<p style="color:var(--gray);margin-bottom:20px;">¿Eliminar <strong>' + nombre + '</strong>? Esta acción no se puede deshacer.</p>'
    + '<div style="display:flex;gap:10px;justify-content:center;">'
    + '<button class="btn-primary" style="width:auto;padding:10px 28px;background:#dc3545;" onclick="eliminarProductoAliado(' + id + ')">Sí, eliminar</button>'
    + '<button class="btn-secondary" style="width:auto;padding:10px 28px;" onclick="this.closest(\'.modal-overlay\').remove()">Cancelar</button>'
    + '</div></div>';
  document.body.appendChild(overlay);
}

async function eliminarProductoAliado(id) {
  document.querySelector('.modal-overlay')?.remove();
  try {
    await api.delete('/api/aliado/productos/' + id);
    mostrarMensaje('Producto eliminado correctamente', 'success');
    cargarAliadoStock();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function abrirEditarProductoAliado(id) {
  const p = aliadoProductosCache.find(x => x.id_catalogo === id);
  if (!p) return mostrarMensaje('No se encontró el producto', 'error');
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.style.overflowY = 'auto';
  overlay.innerHTML = '<div class="modal-content" style="max-width:480px;margin:40px auto;">'
    + '<h3 style="margin-bottom:16px;">Editar producto</h3>'
    + '<div class="form-group"><label>Nombre</label><input id="edit-art-nombre" value="' + escHtml(p.articulo) + '" maxlength="150" style="width:100%;padding:10px;border:1.5px solid var(--light);border-radius:8px;"></div>'
    + '<div style="display:flex;gap:12px;">'
    + '<div class="form-group" style="flex:1;"><label>Precio (COP)</label><input type="number" id="edit-art-precio" min="0" value="' + Number(p.precio) + '" style="width:100%;padding:10px;border:1.5px solid var(--light);border-radius:8px;"></div>'
    + '<div class="form-group" style="flex:1;"><label>Precio con descuento (opcional)</label><input type="number" id="edit-art-descuento" min="0" value="' + (p.precio_descuento != null ? Number(p.precio_descuento) : '') + '" placeholder="Sin descuento" style="width:100%;padding:10px;border:1.5px solid var(--light);border-radius:8px;"></div>'
    + '</div>'
    + '<div style="display:flex;gap:12px;">'
    + '<div class="form-group" style="flex:1;"><label>Categoría</label><select id="edit-art-categoria" style="width:100%;padding:9px;border:1.5px solid var(--light);border-radius:8px;background:white;">'
    + '<option value=""' + (!p.categoria ? ' selected' : '') + '>Sin categoría</option>'
    + CATEGORIAS.map(c => '<option value="' + c + '"' + (normalizarCategoria(p.categoria) === c ? ' selected' : '') + '>' + c + '</option>').join('')
    + '</select></div>'
    + '<div class="form-group" style="flex:1;"><label>Talla</label><input id="edit-art-talla" value="' + escHtml(p.talla || '') + '" style="width:100%;padding:10px;border:1.5px solid var(--light);border-radius:8px;"></div>'
    + '<div class="form-group" style="flex:1;"><label>Color</label><input id="edit-art-color" value="' + escHtml(p.color || '') + '" style="width:100%;padding:10px;border:1.5px solid var(--light);border-radius:8px;"></div>'
    + '</div>'
    + '<div class="form-group"><label>Stock (0 - 10.000)</label><input type="number" id="edit-art-stock" min="0" max="10000" value="' + (p.stock || 0) + '" style="width:100%;padding:10px;border:1.5px solid var(--light);border-radius:8px;"></div>'
    + '<div style="display:flex;gap:10px;justify-content:flex-end;margin-top:8px;">'
    + '<button class="btn-secondary" style="width:auto;padding:10px 24px;" onclick="this.closest(\'.modal-overlay\').remove()">Cancelar</button>'
    + '<button class="btn-primary" style="width:auto;padding:10px 24px;" onclick="guardarEdicionProductoAliado(' + id + ')">Guardar cambios</button>'
    + '</div></div>';
  document.body.appendChild(overlay);
}

async function guardarEdicionProductoAliado(id) {
  const nombre = document.getElementById('edit-art-nombre').value.trim();
  const precio = parseFloat(document.getElementById('edit-art-precio').value);
  const descuento = document.getElementById('edit-art-descuento').value;
  if (!nombre) return mostrarMensaje('El nombre es obligatorio', 'error');
  if (isNaN(precio) || precio <= 0) return mostrarMensaje('Ingresa un precio válido', 'error');
  if (descuento !== '' && (isNaN(parseFloat(descuento)) || parseFloat(descuento) <= 0)) {
    return mostrarMensaje('Descuento inválido', 'error');
  }
  const body = {
    articulo: nombre,
    precio: precio,
    categoria: document.getElementById('edit-art-categoria').value.trim(),
    talla: document.getElementById('edit-art-talla').value.trim(),
    color: document.getElementById('edit-art-color').value.trim(),
    stock: parseInt(document.getElementById('edit-art-stock').value) || 0,
  };
  if (descuento !== '') body.precio_descuento = descuento;
  try {
    await api.put('/api/aliado/productos/' + id, body);
    document.querySelector('.modal-overlay')?.remove();
    mostrarMensaje('Producto actualizado correctamente', 'success');
    cargarAliadoStock();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

function cerrarSesion() {
  if (state.token) {
    // Revoca el token en el servidor; si ya expiró el 401 se ignora.
    const t = state.token;
    fetch('/api/auth/logout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + t }
    }).catch(() => {});
  }
  state.token = null;
  state.user = null;
  state.favoritos = [];
  localStorage.removeItem('lumura_token');
  localStorage.removeItem('lumura_user');
  localStorage.removeItem('lumura_favs');
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

function mostrarActualizarDatos() {
  if (!state.user) return;
  const u = state.user;
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = '<div class="modal-content" style="max-width:400px;">' +
    '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">' +
    '<h3 style="margin:0;">Actualizar datos</h3>' +
    '<span style="font-size:24px;cursor:pointer;" onclick="this.closest(\'.modal-overlay\').remove()">&times;</span></div>' +
    '<div class="form-group"><label>Nombre</label><input id="upd-nombre" value="' + escHtml(u.nombre || '') + '"></div>' +
    '<div class="form-group"><label>Teléfono</label><input id="upd-tel" value="' + escHtml(u.telefono || '') + '"></div>' +
    '<div class="form-group"><label>Dirección</label><input id="upd-dir" value="' + escHtml(u.direccion || '') + '"></div>' +
    '<div style="display:flex;gap:10px;margin-top:16px;">' +
    '<button class="btn-primary" style="flex:1;" onclick="actualizarDatos()">Guardar</button>' +
    '<button class="btn-secondary" style="flex:1;" onclick="this.closest(\'.modal-overlay\').remove()">Cancelar</button></div></div>';
  document.body.appendChild(overlay);
}

async function actualizarDatos() {
  try {
    const body = {
      nombre_usuario: document.getElementById('upd-nombre').value,
      telefono: document.getElementById('upd-tel').value,
      direccion_usuario: document.getElementById('upd-dir').value
    };
    const res = await api.put('/api/auth/cuenta', body);
    state.user = res.usuario;
    localStorage.setItem('lumura_user', JSON.stringify(state.user));
    document.querySelector('.modal-overlay')?.remove();
    mostrarMensaje('Datos actualizados correctamente', 'success');
    actualizarUI();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function cargarProductos() {
  try {
    state.productos = await api.get('/api/productos');
    renderProductos();
    renderCategoriasTags();
    renderAdminCatalogo();
    if (document.getElementById('admin-panel-inv')?.classList.contains('active')) cargarInventario();
  } catch (err) {
    mostrarMensaje('Error al cargar productos: ' + err.message, 'error');
  }
}

const imagenesProducto = {
  1: 'images/camisetabasicapremium.jpg',
  2: 'images/jeansslimfit.jpg',
  3: 'images/vestidocasual.jfif',
  4: 'images/chaquetadeportiva.jpg',
  5: 'images/camisetabasicapremium.jpg'
};

function renderProductos(filtroCat, filtroTexto) {
  const grid = document.getElementById('prod-grid');
  if (!grid) return;
  let items = state.productos;
  if (filtroCat) items = items.filter(p => normalizarCategoria(p.categoria) === normalizarCategoria(filtroCat));
  if (filtroTexto) items = items.filter(p =>
    p.articulo.toLowerCase().includes(filtroTexto.toLowerCase()) ||
    (p.categoria || '').toLowerCase().includes(filtroTexto.toLowerCase())
  );
  if (items.length === 0) {
    grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:40px;color:var(--gray);">No hay productos disponibles</div>';
    return;
  }
    grid.innerHTML = items.map((p, i) => {
    const imgSrc = p.imagen_url || imagenesProducto[p.id_catalogo] || 'images/tshirt.svg';
    const precioF = '$' + Number(p.precio).toLocaleString('es-CO');
    return '<div class="product-card" onclick="verProducto(' + p.id_catalogo + ')">' +
      '<div class="img-placeholder" style="background-image:url(' + imgSrc + ');background-size:cover;background-position:center;background-repeat:no-repeat;background-color:#fce4ec;cursor:pointer;" onclick="event.stopPropagation();mostrarDescripcion(' + p.id_catalogo + ')" title="Click para ver descripción"></div>' +
      '<div class="info">' +
      '<div class="name">' + escHtml(p.articulo) + '</div>' +
      '<div class="price">' + precioF + '</div>' +
      (p.codigo ? '<div style="font-size:11px;color:var(--gray);margin-top:2px;">Código: ' + escHtml(p.codigo) + '</div>' : '') +
      (p.talla ? '<div style="font-size:11px;color:var(--gray);margin-top:2px;">Tallas: ' + escHtml(p.talla) + '</div>' : '') +
      '</div>' +
      '<button class="add-btn" onclick="event.stopPropagation();agregarAlCarrito(' + p.id_catalogo + ')">+ Agregar al carrito</button>' +
      '</div>';
  }).join('');
}

const CATEGORIAS = ['Camisetas', 'Camisas', 'Blusas', 'Pantalones', 'Jeans', 'Faldas', 'Vestidos', 'Chaquetas', 'Abrigos', 'Sueter', 'Chalecos', 'Trajes', 'Ropa interior', 'Calcetines', 'Zapatos', 'Sandalias', 'Botas', 'Accesorios', 'Sombreros', 'Cinturones', 'Bufandas', 'Tennis', 'Ropa deportiva'];

const SINONIMOS = {
  'camiseta': 'Camisetas',
  'camisas y blusas': 'Camisas',
  'jeans y pantalones': 'Jeans',
  'calzado': 'Zapatos',
  'sweater': 'Sueter',
  'sueteres': 'Sueter',
  'tenis': 'Tennis',
  'sneakers': 'Tennis',
  'zapatillas': 'Tennis'
};

function normalizarCategoria(v) {
  const s = String(v || '').trim().toLowerCase();
  if (!s) return '';
  if (SINONIMOS[s]) return SINONIMOS[s];
  const exacta = CATEGORIAS.find(c => c.toLowerCase() === s);
  if (exacta) return exacta;
  return String(v).trim();
}

function renderCategoriasTags() {
  const cont = document.getElementById('tag-filter');
  if (!cont) return;
  const orden = CATEGORIAS.slice();
  const presentes = [];
  (state.productos || []).forEach(p => {
    const n = normalizarCategoria(p.categoria || '');
    if (n && !presentes.includes(n)) presentes.push(n);
  });
  presentes.sort((a, b) => {
    const ia = orden.indexOf(a);
    const ib = orden.indexOf(b);
    return (ia === -1 ? 999 : ia) - (ib === -1 ? 999 : ib);
  });
  const activa = (cont.querySelector('.tag.active')?.dataset.cat) || '';
  cont.innerHTML = '<span class="tag' + (activa === '' ? ' active' : '') + '" data-cat="">Todo</span>'
    + presentes.map(c => '<span class="tag' + (activa === c ? ' active' : '') + '" data-cat="' + escHtml(c) + '">' + escHtml(c) + '</span>').join('');
}

function cargarOpcionesCategorias() {
  const especificos = {
    'aliado-categoria': { placeholder: 'Selecciona una categoría', value: '' },
    'aliado-art-categoria': { placeholder: 'Selecciona la categoría del artículo', value: '' },
    'modal-prod-categoria': { placeholder: 'Selecciona una categoría', value: '' },
    'admin-cat-filter': { placeholder: 'Todas las categorías', value: 'Todas las categorías' }
  };
  Object.keys(especificos).forEach(id => {
    const sel = document.getElementById(id);
    if (!sel) return;
    const spec = especificos[id];
    sel.innerHTML = '<option value="' + spec.value + '" selected' + (spec.value === '' ? ' disabled' : '') + '>' + spec.placeholder + '</option>'
      + CATEGORIAS.map(c => '<option value="' + c + '">' + c + '</option>').join('');
  });
}

function mostrarDescripcion(id) {
  const prod = state.productos.find(p => p.id_catalogo === id);
  if (!prod || !prod.descripcion) {
    mostrarMensaje('Sin descripción disponible', 'info');
    return;
  }
  const desc = prod.descripcion;
  const popup = document.createElement('div');
  popup.className = 'desc-popup';
  popup.innerHTML =
    '<div class="desc-popup-content">' +
    '<span class="desc-popup-close" onclick="this.parentElement.parentElement.remove()">&times;</span>' +
    '<div class="desc-popup-title">' + escHtml(prod.articulo) + '</div>' +
    '<div class="desc-popup-text">' + escHtml(desc) + '</div>' +
    '</div>';
  document.body.appendChild(popup);
  setTimeout(() => popup.addEventListener('click', function(e) { if (e.target === this) this.remove(); }), 10);
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
    const imgSrc = prod.imagen_url || imagenesProducto[prod.id_catalogo] || 'images/tshirt.svg';
    const precioF = '$' + Number(prod.precio).toLocaleString('es-CO');
    document.getElementById('prod-detail-img').innerHTML = '<img src="' + imgSrc + '" alt="' + escHtml(prod.articulo) + '" style="max-width:100%;max-height:100%;object-fit:contain;border-radius:8px;">';
    document.getElementById('prod-detail-nombre').textContent = prod.articulo;
    document.getElementById('prod-detail-precio').textContent = precioF;
    document.getElementById('prod-detail-codigo').textContent = prod.codigo || '-';
    const specsEl = document.getElementById('prod-detail-specs');
    const specs = [
      ['Categoría', prod.categoria],
      ['Tallas', prod.talla],
      ['Colores', prod.color],
      ['Stock disponible', (prod.stock != null ? prod.stock : 0) + ' unidades']
    ].filter(s => s[1] != null && s[1] !== '').map(s =>
      '<div style="display:flex;justify-content:space-between;gap:16px;"><span style="color:var(--gray);">' + s[0] + '</span><span style="font-weight:600;text-align:right;">' + escHtml(String(s[1])) + '</span></div>'
    ).join('');
    specsEl.innerHTML = specs || '';
    const aliadoEl = document.getElementById('prod-detail-aliado');
    if (prod.aliado && prod.aliado.nombre_negocio) {
      aliadoEl.style.display = 'block';
      aliadoEl.innerHTML =
        '<div style="font-weight:700;margin-bottom:6px;">Vendido por: ' + escHtml(prod.aliado.nombre_negocio) + '</div>' +
        (prod.aliado.nit ? '<div>NIT: <strong>' + escHtml(prod.aliado.nit) + '</strong></div>' : '') +
        (prod.aliado.persona_contacto ? '<div>Contacto: ' + escHtml(prod.aliado.persona_contacto) + '</div>' : '') +
        (prod.aliado.telefono ? '<div>Teléfono: ' + escHtml(prod.aliado.telefono) + '</div>' : '') +
        (prod.aliado.direccion ? '<div>Dirección: ' + escHtml(prod.aliado.direccion) + '</div>' : '');
    } else {
      aliadoEl.style.display = 'none';
    }
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
  const agregando = idx === -1;
  if (agregando) {
    state.favoritos.push(prod.id_catalogo);
    mostrarMensaje(prod.articulo + ' agregado a favoritos <img src="images/heart.svg" class="icon" alt="" style="vertical-align:middle">', 'success');
  } else {
    state.favoritos.splice(idx, 1);
    mostrarMensaje(prod.articulo + ' eliminado de favoritos', 'info');
  }
  localStorage.setItem('lumura_favs', JSON.stringify(state.favoritos));
  // Con sesión activa el favorito vive en el servidor; localStorage queda como respaldo para invitados
  if (state.token) {
    const idCat = prod.id_catalogo;
    const llamada = agregando
      ? api.post('/api/favoritos', { id_catalogo: idCat })
      : api.delete('/api/favoritos/' + idCat);
    llamada.catch(() => mostrarMensaje('No se pudo sincronizar tu favorito con el servidor', 'error'));
  }
  const el = document.getElementById('prod-detail-fav');
  if (el) el.innerHTML = agregando ? '<img src="images/heart.svg" class="icon" alt="" style="vertical-align:middle">' : '<img src="images/heart-outline.svg" class="icon" alt="" style="vertical-align:middle">';
}

// Al iniciar sesión: sube favoritos guardados como invitado y trae la lista del servidor
async function sincronizarFavoritos() {
  if (!state.token) return;
  try {
    let remotos = await api.get('/api/favoritos');
    if (!Array.isArray(remotos)) remotos = [];
    const locales = JSON.parse(localStorage.getItem('lumura_favs') || '[]')
      .filter(id => !remotos.includes(id));
    for (const id of locales) {
      try { await api.post('/api/favoritos', { id_catalogo: id }); } catch (e) { /* producto pudo dejar de existir */ }
    }
    if (locales.length) remotos = await api.get('/api/favoritos');
    state.favoritos = remotos;
    localStorage.setItem('lumura_favs', JSON.stringify(state.favoritos));
  } catch (err) { /* sin conexión: se conservan los locales */ }
}

async function agregarAlCarrito(idProducto) {
  if (!state.token) return mostrarMensaje('Debes iniciar sesión primero', 'error');
  const prod = state.productos.find(p => p.id_catalogo === idProducto) || await api.get('/api/productos/' + idProducto);
  try {
    await api.post('/api/carrito', {
      id_catalogo: prod.id_catalogo,
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
    const prod = state.productos && item.id_catalogo != null
      ? state.productos.find(p => p.id_catalogo === item.id_catalogo)
      : state.productos && state.productos.find(p => p.articulo === item.articulo);
    const imgSrc = prod ? (prod.imagen_url || imagenesProducto[prod.id_catalogo] || 'images/tshirt.svg') : 'images/tshirt.svg';
    const vendedorHtml = item.vendedor_nombre ? (
      '<div style="margin-top:8px;padding-top:8px;border-top:1px dashed #e5e7eb;display:flex;flex-direction:column;gap:3px;">' +
        '<div class="sub" style="font-weight:700;color:#c73652;">Vendedor: ' + escHtml(item.vendedor_nombre) + (item.vendedor_negocio && item.vendedor_negocio !== item.vendedor_nombre ? ' · ' + escHtml(item.vendedor_negocio) : '') + '</div>' +
        '<div class="sub">Correo: ' + escHtml(item.vendedor_correo || '-') + '</div>' +
        '<div class="sub">Teléfono: ' + escHtml(item.vendedor_telefono || '-') + '</div>' +
      '</div>'
    ) : '';
    return '<div class="cart-item">' +
      '<div class="icon-box" style="background:linear-gradient(135deg,#fce4ec,#f8bbd9);"><img src="' + imgSrc + '" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:8px;"></div>' +
      '<div class="detail">' +
      '<div class="name">' + escHtml(item.articulo) + '</div>' +
      '<div class="sub">Talla: ' + escHtml(item.talla || 'Única') + ' · Color: ' + escHtml(item.color || 'Único') + '</div>' +
      vendedorHtml +
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
    window.__usuariosAdmin = usuarios;
    const tbody = document.getElementById('admin-users-body');
    if (!tbody) return;
    if (usuarios.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px;">No hay usuarios</td></tr>';
      return;
    }
    tbody.innerHTML = usuarios.map(u => {
      const isAdmin = u.rol === 'ADMIN';
      const bloqueado = !!u.bloqueado;
      const estadoHasta = u.bloqueo_hasta ? ' hasta ' + new Date(u.bloqueo_hasta).toLocaleDateString('es-CO') : '';
      const estadoHtml = bloqueado
        ? '<span class="badge badge-orange">Bloqueado' + estadoHasta + '</span>'
        : '<span class="badge badge-green">Activo</span>';
      return '<tr>'
        + '<td>' + u.id_usuario + '</td>'
        + '<td style="font-weight:600;">' + escHtml(u.nombre_usuario) + '</td>'
        + '<td>' + escHtml(u.correo_usuario) + '</td>'
        + '<td>' + escHtml(u.telefono || '-') + '</td>'
        + '<td><span class="badge ' + (isAdmin ? 'badge-green' : 'badge-blue') + '">' + u.rol + '</span></td>'
        + '<td>' + (u.fecha_registro ? new Date(u.fecha_registro).toLocaleDateString('es-CO') : '-') + '</td>'
        + '<td>' + estadoHtml + '</td>'
        + '<td style="white-space:nowrap;"><button class="btn-secondary-sm" onclick="verPerfilUsuario(' + u.id_usuario + ')">Ver</button> '
        + (isAdmin
            ? '<span style="color:var(--gray);font-size:12px;">Protegido</span>'
            : (bloqueado
                ? '<button class="btn-secondary-sm" onclick="desbloquearUsuario(' + u.id_usuario + ')">Desbloquear</button> '
                : '<button class="btn-block-sm" onclick="abrirModalBloqueo(' + u.id_usuario + ')">Bloquear</button> ')
                + '<button class="btn-danger-sm" onclick="eliminarUsuarioAdmin(' + u.id_usuario + ')">Eliminar</button>')
        + '</td>'
        + '</tr>';
    }).join('');
  } catch (err) {
    mostrarMensaje('Error al cargar usuarios', 'error');
  }
}

let bloqueoUsuarioActual = null;

function abrirModalBloqueo(id) {
  const usuarios = window.__usuariosAdmin || [];
  const u = usuarios.find(x => x.id_usuario === id);
  if (!u) return;
  bloqueoUsuarioActual = u;
  document.getElementById('bloqueo-usuario-id').value = u.id_usuario;
  document.getElementById('bloqueo-usuario-nombre').textContent = u.nombre_usuario + ' (' + u.correo_usuario + ')';
  document.getElementById('bloqueo-motivo').value = '';
  document.getElementById('bloqueo-dias').value = '';
  document.getElementById('modal-bloqueo-usuario').style.display = 'flex';
}

function cerrarModalBloqueo() {
  document.getElementById('modal-bloqueo-usuario').style.display = 'none';
  bloqueoUsuarioActual = null;
}

function abrirModalBloqueoDesdePerfil(id) {
  cerrarModalPerfil();
  abrirModalBloqueo(id);
}

async function desbloquearYcerrarPerfil(id) {
  try {
    const res = await api.put('/api/admin/usuarios/' + id + '/desbloquear');
    cerrarModalPerfil();
    mostrarMensaje(res.mensaje || 'Usuario desbloqueado correctamente', 'success');
    cargarUsuariosAdmin();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function confirmarBloqueo() {
  if (!bloqueoUsuarioActual) return;
  const motivo = document.getElementById('bloqueo-motivo').value.trim();
  const dias = document.getElementById('bloqueo-dias').value;
  if (!motivo) return mostrarMensaje('Indica el motivo del bloqueo', 'error');
  if (!dias || parseInt(dias, 10) < 1) return mostrarMensaje('Indica una cantidad válida de días (mínimo 1)', 'error');
  try {
    const res = await api.put('/api/admin/usuarios/' + bloqueoUsuarioActual.id_usuario + '/bloquear', {
      motivo: motivo,
      dias: dias
    });
    cerrarModalBloqueo();
    mostrarMensaje(res.mensaje || 'Usuario bloqueado correctamente', 'success');
    cargarUsuariosAdmin();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

async function desbloquearUsuario(id) {
  if (!confirm('¿Desbloquear este usuario?')) return;
  try {
    const res = await api.put('/api/admin/usuarios/' + id + '/desbloquear');
    mostrarMensaje(res.mensaje || 'Usuario desbloqueado correctamente', 'success');
    cargarUsuariosAdmin();
  } catch (err) {
    mostrarMensaje(err.message, 'error');
  }
}

let perfilUsuarioActual = null;

function verPerfilUsuario(id) {
  const usuarios = window.__usuariosAdmin || [];
  const u = usuarios.find(x => x.id_usuario === id);
  if (!u) return;
  perfilUsuarioActual = u;
  document.getElementById('perfil-inicial').textContent = (u.nombre_usuario || '?').charAt(0).toUpperCase();
  document.getElementById('perfil-nombre').textContent = u.nombre_usuario || '-';
  document.getElementById('perfil-correo').textContent = u.correo_usuario || '-';
  document.getElementById('perfil-id').value = u.id_usuario ?? '';
  document.getElementById('perfil-rol').value = u.rol || '-';
  document.getElementById('perfil-telefono').value = u.telefono || '-';
  document.getElementById('perfil-edad').value = (u.edad != null && u.edad !== '') ? u.edad : '-';
  document.getElementById('perfil-direccion').value = u.direccion_usuario || '-';
  document.getElementById('perfil-fecha').value = u.fecha_registro ? new Date(u.fecha_registro).toLocaleString('es-CO') : '-';
  const bloqueAliado = document.getElementById('perfil-bloque-aliado');
  const esAliado = u.rol === 'ALIADO';
  bloqueAliado.style.display = esAliado ? 'block' : 'none';
  if (esAliado) {
    document.getElementById('perfil-negocio').value = u.nombre_negocio || '-';
    document.getElementById('perfil-nit').value = u.nit || '-';
    document.getElementById('perfil-contacto').value = u.persona_contacto || '-';
    document.getElementById('perfil-categoria').value = u.categoria_productos || '-';
    const lic = u.licencia_distribuidor;
    document.getElementById('perfil-licencia').innerHTML = (lic && lic.trim())
      ? '<a href="' + escHtml(lic) + '" target="_blank" rel="noopener">Ver licencia</a>'
      : '<span style="color:var(--gray);">Sin licencia</span>';
  }
  const estadoBloqueo = document.getElementById('perfil-estado');
  if (estadoBloqueo) {
    if (u.bloqueado) {
      const hasta = u.bloqueo_hasta ? ' · Hasta ' + new Date(u.bloqueo_hasta).toLocaleDateString('es-CO') : '';
      estadoBloqueo.value = 'BLOQUEADO' + hasta + (u.motivo_bloqueo ? ' · Motivo: ' + u.motivo_bloqueo : '');
    } else {
      estadoBloqueo.value = 'ACTIVO';
    }
  }
  const elimBtn = document.getElementById('perfil-eliminar-btn');
  if (u.rol === 'ADMIN') {
    elimBtn.style.display = 'none';
  } else {
    elimBtn.style.display = 'block';
  }
  const blockBtn = document.getElementById('perfil-bloquear-btn');
  if (blockBtn) {
    if (u.rol === 'ADMIN') {
      blockBtn.style.display = 'none';
    } else {
      blockBtn.textContent = u.bloqueado ? 'Desbloquear' : 'Bloquear';
      blockBtn.onclick = u.bloqueado ? () => desbloquearYcerrarPerfil(u.id_usuario) : () => abrirModalBloqueoDesdePerfil(u.id_usuario);
      blockBtn.style.display = 'block';
    }
  }
  document.getElementById('modal-perfil-usuario').style.display = 'flex';
}

function cerrarModalPerfil() {
  document.getElementById('modal-perfil-usuario').style.display = 'none';
}

function eliminarDesdePerfil() {
  if (!perfilUsuarioActual) return;
  const id = perfilUsuarioActual.id_usuario;
  if (!confirm('¿Eliminar a ' + (perfilUsuarioActual.nombre_usuario || 'este usuario') + '? Los datos asociados también se eliminarán.')) return;
  (async () => {
    try {
      await api.delete('/api/admin/usuarios/' + id);
      perfilUsuarioActual = null;
      cerrarModalPerfil();
      mostrarMensaje('Usuario eliminado correctamente', 'success');
      cargarUsuariosAdmin();
    } catch (err) {
      mostrarMensaje(err.message, 'error');
    }
  })();
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

function toggleDatosTarjeta() {
  const metodo = document.getElementById('checkout-pago')?.value || '';
  const el = document.getElementById('datos-tarjeta');
  if (el) el.style.display = metodo.toLowerCase().includes('tarjeta') ? 'block' : 'none';
}

function renderCheckoutItems() {
  const cont = document.getElementById('checkout-items');
  if (!cont) return;
  if (!state.carrito || state.carrito.length === 0) {
    cont.innerHTML = '<div style="font-size:13px;color:var(--gray);padding:6px 0;">Tu carrito está vacío</div>';
    return;
  }
  cont.innerHTML = state.carrito.map(item => {
    const vendedorHtml = item.vendedor_nombre
      ? '<div class="sub" style="font-weight:700;font-size:12px;color:#c73652;margin-top:2px;">Vendedor: ' + escHtml(item.vendedor_nombre)
        + (item.vendedor_negocio && item.vendedor_negocio !== item.vendedor_nombre ? ' · ' + escHtml(item.vendedor_negocio) : '') + '</div>'
        + (item.vendedor_correo || item.vendedor_telefono
          ? '<div class="sub" style="font-size:11px;color:var(--gray);">'
            + (item.vendedor_correo ? 'Correo: ' + escHtml(item.vendedor_correo) + (item.vendedor_telefono ? ' · ' : '') : '')
            + (item.vendedor_telefono ? 'Tel: ' + escHtml(item.vendedor_telefono) : '') + '</div>'
          : '')
      : '';
    return '<div style="border-bottom:1px dashed var(--light);padding:8px 0;">'
      + '<div style="display:flex;justify-content:space-between;gap:10px;">'
      + '<div style="font-size:13px;font-weight:600;">' + escHtml(item.articulo) + (item.talla ? ' · Talla ' + escHtml(item.talla) : '') + ' <span style="color:var(--gray);font-weight:400;">x' + item.cantidad + '</span></div>'
      + '<div style="font-size:13px;font-weight:700;white-space:nowrap;">$' + (Number(item.precio) * Number(item.cantidad)).toLocaleString('es-CO') + '</div>'
      + '</div>' + vendedorHtml + '</div>';
  }).join('');
}

async function handleCheckout() {
  if (!state.token) return mostrarMensaje('Debes iniciar sesión', 'error');
  if (state.carrito.length === 0) return mostrarMensaje('El carrito está vacío', 'error');
  // El servidor calcula el total y los items desde el carrito; aquí solo se envían datos de pago/entrega.
  const total = state.carrito.reduce((s, i) => s + Number(i.precio) * Number(i.cantidad), 0);
  const metodoPago = document.getElementById('checkout-pago')?.value || 'Tarjeta';
  const direccion = document.getElementById('checkout-dir')?.value.trim() || 'Por definir';
  const body = {
    metodo_pago: metodoPago,
    direccion_entrega: direccion,
  };
  if (metodoPago.toLowerCase().includes('tarjeta')) {
    // Se envía la tarjeta a la pasarela simulada (el backend valida, no la guarda)
    body.numero_tarjeta = document.getElementById('checkout-tarjeta')?.value.trim() || '';
    body.mes_expiracion = document.getElementById('checkout-mes')?.value.trim() || '';
    body.anio_expiracion = document.getElementById('checkout-anio')?.value.trim() || '';
    body.cvv = document.getElementById('checkout-cvv')?.value.trim() || '';
  }
  try {
    const res = await api.post('/api/pedidos', body);
    document.getElementById('confirm-numero').textContent = '#LUM-' + res.id;
    const fecha = new Date().toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
    document.getElementById('confirm-fecha').textContent = fecha;
    document.getElementById('confirm-total').textContent = '$' + total.toLocaleString('es-CO');
    const contItems = document.getElementById('confirm-items');
    if (contItems) {
      contItems.innerHTML = state.carrito.map(item => {
        const vendedorHtml = item.vendedor_nombre
          ? '<div class="sub" style="font-weight:600;font-size:12px;color:#c73652;">Vendedor: ' + escHtml(item.vendedor_nombre)
            + (item.vendedor_negocio && item.vendedor_negocio !== item.vendedor_nombre ? ' · ' + escHtml(item.vendedor_negocio) : '') + '</div>'
            + (item.vendedor_correo ? '<div class="sub" style="font-size:11px;color:var(--gray);">Correo: ' + escHtml(item.vendedor_correo) + '</div>' : '')
          : '';
        return '<div style="padding:6px 0;border-bottom:1px dashed var(--light);">'
          + '<div style="display:flex;justify-content:space-between;gap:10px;">'
          + '<span style="font-size:13px;font-weight:600;">' + escHtml(item.articulo) + ' <span style="color:var(--gray);font-weight:400;">x' + item.cantidad + '</span></span>'
          + '<span style="font-size:13px;font-weight:700;">$' + (Number(item.precio) * Number(item.cantidad)).toLocaleString('es-CO') + '</span>'
          + '</div>' + vendedorHtml + '</div>';
      }).join('');
    }
    if (res.referencia_pago) {
      document.getElementById('confirm-ref-row').style.display = 'flex';
      document.getElementById('confirm-ref').textContent = res.referencia_pago;
    } else {
      document.getElementById('confirm-ref-row').style.display = 'none';
    }
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
        '<span class="badge ' + badge + '">' + est.toUpperCase() + '</span></div>' +
        '<div style="font-size:13px;color:var(--gray);">' + escHtml(p.articulo || '') + '</div>' +
        renderDetallesPedido(p) +
        '<div style="display:flex;justify-content:space-between;margin-top:6px;font-size:13px;align-items:center;">' +
        '<span>' + new Date(p.fecha_pedido).toLocaleDateString('es-CO') + '</span>' +
        '<span style="font-weight:700;color:var(--accent);">$' + Number(p.total).toLocaleString('es-CO') + '</span>' +
        (puedeCancelar ? '<button class="btn-danger-sm" onclick="confirmarCancelar(' + p.id_compra + ')">Cancelar pedido</button>' : '') +
        '</div>' +
        (renderSeguimiento(p) ? '<div style="margin-top:10px;border-top:1px dashed var(--light);padding-top:10px;">' + renderSeguimiento(p) + '</div>' : '') +
        '</div>';
    }).join('');
  } catch (err) {
    mostrarMensaje('Error al cargar pedidos', 'error');
  }
}

function renderDetallesPedido(p) {
  const detalles = p.detalles || [];
  if (detalles.length === 0) return '';
  return '<div style="margin-top:8px;border-top:1px dashed var(--light);padding-top:8px;">' + detalles.map(d => {
    const v = d.vendedor || null;
    const vendedorHtml = v
      ? '<div style="font-size:11px;font-weight:600;color:#c73652;">Vendedor: ' + escHtml(v.vendedor_nombre || '')
        + (v.vendedor_negocio && v.vendedor_negocio !== v.vendedor_nombre ? ' · ' + escHtml(v.vendedor_negocio) : '') + '</div>'
        + '<div style="font-size:11px;color:var(--gray);">' + (v.vendedor_correo ? 'Correo: ' + escHtml(v.vendedor_correo) : '') + (v.vendedor_telefono ? (v.vendedor_correo ? ' · ' : '') + 'Tel: ' + escHtml(v.vendedor_telefono) : '') + '</div>'
      : '';
    return '<div style="display:flex;justify-content:space-between;font-size:12px;padding:2px 0;">'
      + '<span>' + escHtml(d.articulo || '') + ' <span style="color:var(--gray);">x' + d.cantidad + '</span></span>'
      + '<span>$' + Number(d.precio_unitario * d.cantidad).toLocaleString('es-CO') + '</span></div>' + vendedorHtml;
  }).join('') + '</div>';
}

function renderSeguimiento(p) {
  if (!p.historial_envio) {
    return p.estado_pedido && p.estado_pedido !== 'cancelado'
      ? '<div style="font-size:12px;color:var(--gray);">Sin seguimiento de envío aún</div>'
      : '';
  }
  // historial_envio viene como "ESTADO@yyyy-MM-ddTHH:mm:ss|ESTADO@..."
  const eventos = String(p.historial_envio).split('|').filter(Boolean).map(function (h) {
    const partes = h.split('@');
    const estado = (partes[0] || '').toLowerCase();
    const fecha = partes[1] ? new Date(partes[1]).toLocaleString('es-CO') : '';
    const icono = estado === 'entregado' ? '✅' : estado === 'enviado' ? '📦' : estado === 'cancelado' ? '❌' : '🕐';
    const actual = (p.estado_pedido || '').toLowerCase() === estado;
    return '<div style="display:flex;gap:8px;align-items:flex-start;margin-bottom:6px;">' +
      '<span>' + icono + '</span>' +
      '<div style="font-size:12px;' + (actual ? 'font-weight:700;' : 'color:var(--gray);') + '">' +
      (estado.charAt(0).toUpperCase() + estado.slice(1)) + (fecha ? ' — ' + fecha : '') +
      '</div></div>';
  }).join('');
  let guia = '';
  if (p.numero_guia || p.transportadora) {
    guia = '<div style="font-size:12px;color:var(--gray);margin-top:6px;padding-top:6px;border-top:1px solid var(--light);">' +
      '📦 Envío: <strong>' + escHtml(p.transportadora || '-') + '</strong>' +
      (p.numero_guia ? ' — Guía: <strong>' + escHtml(p.numero_guia) + '</strong>' : '') +
      '</div>';
  }
  return eventos + guia;
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
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px;">No hay pedidos</td></tr>';
      return;
    }
    tbody.innerHTML = filtrados.map(p => {
      const badge = p.estado_pedido === 'entregado' ? 'badge-green'
        : p.estado_pedido === 'enviado' ? 'badge-blue'
        : p.estado_pedido === 'cancelado' ? 'badge-red' : 'badge-red';
      const seguimiento = (p.numero_guia || p.transportadora)
        ? escHtml((p.transportadora || '') + (p.numero_guia ? ' / ' + p.numero_guia : ''))
        : (p.estado_pedido === 'enviado' ? p.historial_envio || '-' : '-');
      return '<tr>'
        + '<td>#LUM-' + p.id_compra + '</td>'
        + '<td>' + (p.id_usuario || '-') + '</td>'
        + '<td>' + escHtml((p.articulo || '').split(',').slice(0, 3).join(', ')) + '</td>'
        + '<td style="font-weight:700;">$' + Number(p.total).toLocaleString('es-CO') + '</td>'
        + '<td>' + new Date(p.fecha_pedido).toLocaleDateString('es-CO') + '</td>'
        + '<td><span class="badge ' + badge + '">' + (p.estado_pedido || 'pendiente') + '</span></td>'
        + '<td style="font-size:12px;color:var(--gray);max-width:140px;">' + seguimiento + '</td>'
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
  const body = { estado_pedido: estado };
  if (estado === 'enviado') {
    const numeroGuia = (prompt('Número de guía de envío:') || '').trim();
    if (!numeroGuia) { mostrarMensaje('Debes indicar el número de guía para marcar como enviado', 'error'); cargarPedidosAdmin(); return; }
    const transportadora = (prompt('Transportadora (ej. Interrapidisimo, Servientrega):') || '').trim();
    body.numero_guia = numeroGuia;
    body.transportadora = transportadora;
  }
  try {
    const res = await api.put('/api/admin/pedidos/' + id, body);
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
  if (filtroCat && filtroCat !== 'Todas las categorías') items = items.filter(p => normalizarCategoria(p.categoria) === normalizarCategoria(filtroCat));
  if (items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px;">No hay productos</td></tr>';
    return;
  }
  tbody.innerHTML = items.map((p, i) => {
    const stock = Number(p.stock);
    const imgSrc = p.imagen_url || imagenesProducto[p.id_catalogo] || 'images/tshirt.svg';
    const badge = stock > 0 ? '<span class="badge badge-green">Activo</span>' : '<span class="badge badge-red">Sin stock</span>';
    return '<tr><td>#' + String(p.id_catalogo || i + 1).padStart(3, '0') + '</td>' +
      '<td><div style="display:flex;align-items:center;gap:10px;"><img src="' + imgSrc + '" alt="" style="width:32px;height:32px;object-fit:cover;border-radius:4px;">' +
      '<div><div style="font-weight:600;">' + escHtml(p.articulo) + '</div><div style="font-size:12px;color:var(--gray);">' + escHtml(p.categoria || '') + '</div></div></div></td>' +
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
  document.getElementById('modal-prod-categoria').value = prod ? normalizarCategoria(prod.categoria) : '';
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
  const grid = document.getElementById('prod-grid');
  if (!grid) return;
  grid.innerHTML = favs.map(p => {
    const imgSrc = p.imagen_url || imagenesProducto[p.id_catalogo] || 'images/tshirt.svg';
    const precioF = '$' + Number(p.precio).toLocaleString('es-CO');
    return '<div class="product-card" onclick="verProducto(' + p.id_catalogo + ')">' +
      '<div class="img-placeholder" style="background-image:url(' + imgSrc + ');background-size:cover;background-position:center;background-repeat:no-repeat;background-color:#fce4ec;cursor:pointer;" onclick="event.stopPropagation();mostrarDescripcion(' + p.id_catalogo + ')" title="Click para ver descripción"></div>' +
      '<div class="info">' +
      '<div class="name">' + escHtml(p.articulo) + '</div>' +
      '<div class="price">' + precioF + '</div>' +
      (p.talla ? '<div style="font-size:11px;color:var(--gray);margin-top:2px;">Tallas: ' + escHtml(p.talla) + '</div>' : '') +
      '</div>' +
      '<button class="add-btn" onclick="event.stopPropagation();agregarAlCarrito(' + p.id_catalogo + ')">+ Agregar al carrito</button>' +
      '</div>';
  }).join('');
  showScreen('home');
  mostrarMensaje('Mostrando ' + favs.length + ' favorito(s) <img src="images/heart.svg" class="icon" alt="" style="vertical-align:middle">', 'success');
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
    const imgSrc = p.imagen_url || imagenesProducto[p.id_catalogo] || 'images/tshirt.svg';
    const estado = stock === 0 ? '<span class="status-dot red"></span>Agotado'
      : stock < 10 ? '<span class="status-dot yellow"></span>Bajo'
      : '<span class="status-dot green"></span>Normal';
    const color = stock === 0 ? 'var(--accent)' : stock < 10 ? 'var(--warning)' : 'inherit';
    const btnClass = stock === 0 ? 'background:#fce4ec; color:var(--accent);'
      : stock < 10 ? 'background:#fff9e6; color:#856404;'
      : 'background:var(--light);';
    const btnText = stock === 0 ? 'Urgente' : stock < 10 ? 'Reabastecer' : 'Ajustar';
    return '<tr><td><img src="' + imgSrc + '" alt="" style="width:24px;height:24px;object-fit:cover;border-radius:3px;vertical-align:middle;margin-right:6px;">' + escHtml(p.articulo) + '</td>'
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
          + '<td>' + escHtml((p.articulo || '').split(',').slice(0, 2).join(', ')) + '</td>'
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
  document.body.classList.remove('rol-admin', 'rol-aliado', 'rol-user');
  if (estaLogueado && state.user) {
    document.body.classList.add(state.user.rol === 'ADMIN' ? 'rol-admin' : (state.user.rol === 'ALIADO' ? 'rol-aliado' : 'rol-user'));
  }
  const adminAvatar = document.getElementById('admin-avatar');
  if (adminAvatar) adminAvatar.textContent = (state.user?.nombre || 'A').charAt(0).toUpperCase();
  const aliadoAvatar = document.getElementById('aliado-avatar');
  if (aliadoAvatar) aliadoAvatar.textContent = (state.user?.nombre || 'A').charAt(0).toUpperCase();
  const aliadoSaludo = document.getElementById('aliado-saludo');
  if (aliadoSaludo) aliadoSaludo.textContent = state.user?.nombre || 'Aliado';
  document.querySelectorAll('.auth-only').forEach(el => el.style.display = estaLogueado ? '' : 'none');
  document.querySelectorAll('.no-auth').forEach(el => el.style.display = estaLogueado ? 'none' : '');
  const userLink = document.getElementById('header-user-link');
  if (userLink) {
    if (estaLogueado && state.user) {
      userLink.innerHTML = '<span class="user-menu-wrap" style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;" onclick="toggleUserMenu(event)"><img src="images/user.svg" class="icon" alt="" style="width:16px;height:16px;vertical-align:middle"> ' + escHtml(state.user.nombre) + ' <span style="font-size:10px;margin-left:2px;">▾</span></span>';
      userLink.onclick = null;
    } else {
      userLink.innerHTML = '<img src="images/user.svg" class="icon" alt="" style="width:16px;height:16px;vertical-align:middle"> Iniciar sesión';
      userLink.onclick = function() { showScreen('login'); };
    }
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
    '<a onclick="mostrarActualizarDatos(); document.querySelector(\'.user-dropdown\')?.remove();"><img src="images/edit.svg" class="icon" alt="" style="width:14px;height:14px;"> Actualizar datos</a>' +
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
  const objetivo = document.querySelector('.tag[data-cat="' + (cat || '') + '"]');
  if (objetivo) {
    objetivo.classList.add('active');
  } else {
    const todo = document.querySelector('.tag[data-cat=""]');
    if (todo) todo.classList.add('active');
  }
  const term = document.getElementById('search-input')?.value || '';
  renderProductos(cat, term);
}

async function cargarAliadoDashboard() {
  try {
    const d = await api.get('/api/aliado/dashboard');
    const el = document.getElementById('aliado-metric-productos');
    if (el) el.textContent = d.total_productos;
    const el2 = document.getElementById('aliado-metric-stock');
    if (el2) el2.textContent = d.unidades_stock;
    const el3 = document.getElementById('aliado-metric-valor');
    if (el3) el3.textContent = '$' + Number(d.valor_inventario).toLocaleString('es-CO');
  } catch (err) {
    mostrarMensaje('Error al cargar las métricas', 'error');
    return;
  }
  try {
    const v = await api.get('/api/aliado/ventas');
    const elU = document.getElementById('aliado-metric-vendidas');
    if (elU) elU.textContent = v.unidades_vendidas;
    const elI = document.getElementById('aliado-metric-ingresos');
    if (elI) elI.textContent = '$' + Number(v.ingresos_totales).toLocaleString('es-CO');
    const tbody = document.getElementById('aliado-ventas-body');
    if (tbody) {
      if (!v.ultimas_ventas || v.ultimas_ventas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:16px;color:var(--gray);">Aún no tienes ventas registradas</td></tr>';
      } else {
        tbody.innerHTML = v.ultimas_ventas.map(x => '<tr>'
          + '<td>Pedido #' + x.id_compra + '</td>'
          + '<td>' + escHtml(x.articulo) + '</td>'
          + '<td style="text-align:center;">' + x.cantidad + '</td>'
          + '<td style="font-weight:700;">$' + Number(x.precio_unitario).toLocaleString('es-CO') + '</td>'
          + '</tr>').join('');
      }
    }
  } catch (err) { /* ventas es complementario: no bloquea el panel */ }
}

function irInicioPorRol() {
  const rol = state.user?.rol;
  if (rol === 'ADMIN') return showScreen('admin');
  if (rol === 'ALIADO') return showScreen('aliado');
  return showScreen('home');
}

function mostrarMenuAdmin(panel) {
  const nombre = panel || 'dash';
  document.querySelectorAll('#screen-admin .admin-sidebar .menu-item[data-panel]').forEach(function (el) {
    el.classList.toggle('active', el.dataset.panel === nombre);
  });
  document.querySelectorAll('#screen-admin .admin-panel').forEach(function (el) {
    const activo = el.id === 'admin-panel-' + nombre;
    el.classList.toggle('active', activo);
    el.style.display = activo ? 'block' : 'none';
  });
  const titulos = { dash: 'Dashboard', cat: 'Catálogo', inv: 'Inventario', rep: 'Reportes', users: 'Usuarios', orders: 'Pedidos' };
  const titleEl = document.getElementById('admin-title');
  if (titleEl) titleEl.textContent = titulos[nombre] || 'Dashboard';
  if (nombre === 'dash') cargarDashboard();
  if (nombre === 'cat') cargarProductos();
  if (nombre === 'inv') { cargarProductos(); cargarInventario(); }
  if (nombre === 'rep') { cargarProductos(); cargarReportes(); }
  if (nombre === 'users') cargarUsuariosAdmin();
  if (nombre === 'orders') cargarPedidosAdmin();
}

function mostrarMenuAliado(panel) {
  const nombre = panel || 'dash';
  document.querySelectorAll('#screen-aliado .admin-sidebar .menu-item[data-panel]').forEach(function (el) {
    el.classList.toggle('active', el.dataset.panel === nombre);
  });
  document.querySelectorAll('#screen-aliado .aliado-panel').forEach(function (el) {
    const activo = el.id === 'aliado-panel-' + nombre;
    el.classList.toggle('active', activo);
    el.style.display = activo ? 'block' : 'none';
  });
  if (nombre === 'dash') cargarAliadoDashboard();
  if (nombre === 'stock') cargarAliadoStock();
  if (nombre === 'desc') cargarAliadoDesc();
  if (nombre === 'licencia') cargarLicenciaAliado();
}

function showScreen(name) {
  const rol = state.user?.rol || null;
  if (name === 'admin' && rol !== 'ADMIN') {
    mostrarMensaje('Acceso denegado — Solo administradores', 'error');
    return;
  }
  if (name === 'aliado' && rol !== 'ALIADO' && rol !== 'ADMIN') {
    mostrarMensaje('Acceso denegado — Solo aliados', 'error');
    return;
  }
  if (name === 'aliado-login' && estalogueadoLocal()) {
    mostrarMensaje('Ya tienes una sesión de rol activa', 'info');
    irInicioPorRol();
    return;
  }
  const tienda = ['home', 'product', 'cart', 'checkout', 'confirm', 'orders'];
  if (tienda.indexOf(name) !== -1 && (rol === 'ADMIN' || rol === 'ALIADO')) {
    mostrarMensaje('Sesión de rol activa — cierra sesión para acceder a la tienda', 'info');
    irInicioPorRol();
    return;
  }
  document.querySelectorAll('.screen').forEach(s => {
    s.classList.remove('active');
    s.style.display = 'none';
  });
  const screen = document.getElementById('screen-' + name);
  if (screen) {
    screen.classList.add('active');
    screen.style.display = 'block';
  }
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
    renderCheckoutItems();
    const dirEl = document.getElementById('checkout-dir');
    if (dirEl) dirEl.value = state.user?.direccion || '';
  }
  if (name === 'orders' && state.token) cargarPedidos();
  if (name === 'admin') mostrarMenuAdmin('dash');
  if (name === 'aliado') mostrarMenuAliado('dash');
}

function estalogueadoLocal() {
  return !!state.token;
}

document.addEventListener('DOMContentLoaded', function () {
  const loginForm = document.getElementById('login-form');
  if (loginForm) loginForm.addEventListener('submit', handleLogin);
  const regForm = document.getElementById('reg-form');
  if (regForm) regForm.addEventListener('submit', handleRegister);
  const aliadoForm = document.getElementById('aliado-form');
  if (aliadoForm) aliadoForm.addEventListener('submit', handleRegisterAliado);
  const aliadoAddForm = document.getElementById('aliado-add-form');
  if (aliadoAddForm) aliadoAddForm.addEventListener('submit', guardarArticuloAliado);
  const licenciaImg = document.getElementById('aliado-licencia-img');
  if (licenciaImg) licenciaImg.addEventListener('change', function() { handleLicenciaImgSelect(this); });
  document.querySelectorAll('.logout-btn').forEach(b => b.addEventListener('click', cerrarSesion));
  const tagFilter = document.getElementById('tag-filter');
  if (tagFilter) tagFilter.addEventListener('click', function (ev) {
    const tag = ev.target.closest('.tag');
    if (tag) filtrarCategoria(tag.dataset.cat);
  });
  cargarOpcionesCategorias();
  const adminAvatar = document.getElementById('admin-avatar');
  if (adminAvatar) adminAvatar.addEventListener('click', toggleUserMenu);
  const aliadoAvatar = document.getElementById('aliado-avatar');
  if (aliadoAvatar) aliadoAvatar.addEventListener('click', toggleUserMenu);
  actualizarUI();
  if (state.token) {
    cargarProductos();
    sincronizarFavoritos();
    irInicioPorRol();
  }
});
