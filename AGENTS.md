# LUMURA - E-commerce de Ropa

Backend Spring Boot + MySQL, frontend HTML/CSS/JS single-page.

## Stack

- **Backend**: Spring Boot 3.2.5, Java 17+, JPA/Hibernate, MySQL, JWT (jjwt)
- **Frontend**: HTML/CSS/JS vanilla, single-page (11 pantallas)
- **Node.js Express**: Eliminado — reemplazado por Spring Boot

## Comandos

```bash
cd primeraApi
.\mvnw.cmd spring-boot:run      # servidor en http://localhost:8080
```

O desde la raíz:
```bash
npm start                        # ejecuta Maven Spring Boot
```

## Arquitectura

- **`primeraApi/`** — proyecto Spring Boot con toda la API REST
  - `src/main/java/com/lumura/primeraApi/entity/` — 6 entidades JPA (Catalogo, Usuario, Carrito, Compra, DetalleCompra, Favorito)
  - `src/main/java/com/lumura/primeraApi/repository/` — 6 repositorios Spring Data JPA
  - `src/main/java/com/lumura/primeraApi/controller/` — 8 controladores REST (Auth, Producto, Carrito, Pedido, Favorito, Aliado, Admin, Pago)
  - `src/main/java/com/lumura/primeraApi/service/PagoSimuladoService.java` — pasarela de pago simulada (Luhn, sin guardar datos)
  - `src/main/java/com/lumura/primeraApi/util/JwtUtil.java` — generación y validación de tokens JWT
  - `src/main/java/com/lumura/primeraApi/config/WebConfig.java` — CORS para desarrollo
  - `src/main/resources/static/` — frontend (index.html, lumura.js, lumura.css)
  - `src/main/resources/application.properties` — conexión MySQL + JWT config
- **Raíz del proyecto** — contiene los archivos originales del frontend (referencia)

## Rutas API

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Registro de usuario (requiere `confirmar_password`) |
| POST | `/api/auth/register-aliado` | No | Registro de aliado/vendedor |
| POST | `/api/auth/login` | No | Login, devuelve JWT + usuario. 403 si la cuenta está bloqueada (motivo + fecha fin); si el bloqueo venció, lo limpia y permite entrar |
| POST | `/api/auth/recuperar` | No | Solicita enlace de recuperación `{correo_usuario}`. No revela si el correo existe. En modo offline devuelve `enlace_demo` |
| POST | `/api/auth/reset-password` | No | Restablece contraseña `{token, nueva_password, confirmar_password}`. Token de 30 min de vida, se invalida al usarse |
| PUT | `/api/auth/cuenta` | JWT | Actualizar datos de la cuenta propia |
| DELETE | `/api/auth/cuenta` | JWT | Eliminar la cuenta propia |
| GET | `/api/productos` | No | Lista completa de productos |
| GET | `/api/productos/page?page=0&size=12` | No | Productos paginados |
| GET | `/api/productos/:id` | No | Detalle de producto (+ info del aliado) |
| GET | `/api/carrito/:id_usuario` | JWT | Ver carrito propio (403 si es ajeno). Cada ítem incluye además `vendedor_nombre`, `vendedor_correo`, `vendedor_telefono` y `vendedor_negocio` (contacto del aliado dueño del producto) |
| POST | `/api/carrito` | JWT | Agregar al carrito (por `id_catalogo`; valida stock) |
| PUT | `/api/carrito/:id_carrito` | JWT | Cambiar cantidad de un item propio |
| DELETE | `/api/carrito/:id_carrito` | JWT | Eliminar item propio del carrito |
| POST | `/api/pedidos` | JWT | Confirmar pedido: total e items se calculan server-side desde el carrito, descuenta stock, vacía el carrito. 400 si el carrito está vacío o sin stock. Si `metodo_pago` es Tarjeta valida Luhn/fecha/CVV (no guarda) y devuelve `referencia_pago` |
| POST | `/api/pago/procesar` | No | Pasarela simulada: valida `numero_tarjeta`+`mes_expiracion`+`anio_expiracion`+`cvv`, devuelve `{aprobado, referencia}` o 400. Tarjeta de prueba: `4111 1111 1111 1111` |
| GET | `/api/pedidos/:id_usuario` | JWT | Pedidos propios con desglose (`detalles[]`; cada detalle incluye además `vendedor` con `{vendedor_nombre, vendedor_correo, vendedor_telefono, vendedor_negocio}` del aliado dueño del producto); 403 si es ajeno |
| PUT | `/api/pedidos/:id/cancelar` | JWT | Cancelar pedido propio (no permitido si ya fue entregado/cancelado) |
| GET | `/api/favoritos` | JWT | Lista de ids de productos favoritos del usuario |
| POST | `/api/favoritos` | JWT | Agregar favorito `{id_catalogo}` (idempotente) |
| DELETE | `/api/favoritos/:id_catalogo` | JWT | Quitar favorito |
| GET | `/api/aliado/productos` | JWT | Productos del aliado autenticado |
| GET | `/api/aliado/dashboard` | JWT | KPIs del panel del aliado |
| GET | `/api/aliado/licencia` | JWT | Devuelve `{licencia}` (URL) de la licencia de distribuidor del aliado ("" si no tiene) |
| PUT | `/api/aliado/licencia` | JWT | Guarda la URL `{licencia: "url"}` de la licencia de distribuidor del aliado (la imagen se sube antes por `/api/admin/upload`) |
| GET | `/api/admin/dashboard` | JWT Admin | KPIs del dashboard admin |
| GET | `/api/admin/pedidos` | JWT Admin | Todos los pedidos |
| PUT | `/api/admin/pedidos/:id` | JWT Admin | Actualizar estado de un pedido (pendiente/enviado/entregado/cancelado). Al marcar `enviado` opcional `numero_guia` y `transportadora`. Registra cada cambio en `historial_envio` |
| POST | `/api/admin/productos` | JWT Admin | Crear producto |
| PUT | `/api/admin/productos/:id` | JWT Admin | Editar producto |
| DELETE | `/api/admin/productos/:id` | JWT Admin | Eliminar producto |
| GET | `/api/admin/usuarios` | JWT Admin | Lista de usuarios (excluye al admin autenticado; incluye `bloqueado`, `motivo_bloqueo`, `bloqueo_hasta`) |
| PUT | `/api/admin/usuarios/{id}/bloquear` | JWT Admin | Bloquear usuario `{motivo, dias}` (400 si es rol ADMIN, motivo vacío o días < 1; setea `bloqueo_hasta`) |
| PUT | `/api/admin/usuarios/{id}/desbloquear` | JWT Admin | Desbloquear usuario (limpia motivo y fecha) |
| DELETE | `/api/admin/usuarios/:id` | JWT Admin | Eliminar usuario (rol ADMIN, no por email) |
| POST | `/api/admin/upload` | JWT Admin | Subir imagen de producto |
| POST | `/api/admin/seed` | JWT Admin | Poblar catálogo con datos de muestra |

Errores estándar: 401 sin token, 403 si el recurso es de otro usuario o falta rol, 404 si no existe, 400 validación (mensaje en `{error}`)

## Base de datos

- MySQL 8, database `publico`
- Credenciales por variables de entorno: `DB_USER` (default `alejandro`) y `DB_PASSWORD` (obligatoria, sin default)
- Docker Compose lee un archivo `.env` — ver plantilla `primeraApi/.env.example`
- Tablas: `usuario`, `catalogo`, `carrito`, `compras`, `detalle_compra` (desglose de cada pedido con snapshot de precio), `favoritos`
- Columna `usuario.licencia_distribuidor` (TEXT): URL de la licencia de distribuidor del aliado
- El total del pedido y el descuento de stock se calculan en el servidor al confirmar; el cliente no envía totales
- Schema: `schema.sql` en la raíz del proyecto

## Estado del proyecto

- Frontend servido por Spring Boot en `localhost:8080`
- API REST funcional: productos, auth, carrito, pedidos, admin dashboard
- JWT implementado con jjwt 0.12.5
  - Sin `JWT_SECRET` definido: prod no arranca (fail-fast, mínimo 32 caracteres); dev genera un secreto efímero por ejecución (los tokens no sobreviven reinicios)
- Jackson snake_case para serialización JSON
- CORS abierto para desarrollo
- Cabeceras de seguridad en `SecurityHeaderFilter`: CSP estricto (img-src permite http/https para `imagen_url` externa), X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy, X-XSS-Protection
- Admin detectado por campo `rol` (`ADMIN`) en BD y JWT — sin lógica basada en email
- Recursos estáticos se sirven con `Cache-Control: no-cache, must-revalidate` (config en `application.properties`) para que el navegador siempre revalide y no queden versiones viejas de `index.html`/`lumura.js`/`lumura.css`. Los assets se referencian además con query string versionado (`lumura.js?v=3`, `lumura.css?v=3`) que hay que incrementar al cambiar su contenido si un usuario sigue viendo pantallas antiguas (recargar con Ctrl+F5)
- El contacto del vendedor del producto se muestra en todas las etapas del flujo de compra: tarjeta del carrito (`vendedor_nombre/vendedor_correo/vendedor_telefono/vendedor_negocio` de `GET /api/carrito/:id`), lista de ítems del checkout (`#checkout-items`, función `renderCheckoutItems`), confirmación del pedido (`#confirm-items`) y desglose de "Mis pedidos" (`renderDetallesPedido`, servido por `DetalleCompra.vendedor` en `GET /api/pedidos/:id_usuario`)
- `api.request` (lumura.js) detecta 401 con token vigente → cierra sesión y redirige a login ("Tu sesión expiró"). Clave en dev: al reiniciar el servidor el secreto JWT cambia y los tokens viejos mueren, por lo que hay que volver a iniciar sesión
- Acceso aliado: pantalla `screen-aliado-login` (botón "Soy aliado" en `choose-role`) pide correo+contraseña; valida con `POST /api/auth/login`; si el rol es `ALIADO` va a `aliado-add` (subir artículo); si no, muestra "No eres aliado". Ofrece enlace "Regístrate como aliado" (`screen-register-aliado`). La pantalla `aliado-login` está exenta del bloqueo por rol en `showScreen`
- Suite de tests: 122 tests unitarios (JUnit 5 + Mockito) — `.\mvnw.cmd test`
- Git remote: `git@github.com:alejandrosilvarojas2-tech/lumura2.git`

## Archivos relevantes

- `primeraApi/pom.xml` — dependencias Maven (Spring Boot, JPA, MySQL, jjwt)
- `primeraApi/src/main/resources/application.properties` — configuración DB y JWT
- `primeraApi/src/main/resources/static/` — frontend (index.html, lumura.js, lumura.css)
- `primeraApi/src/main/java/com/lumura/primeraApi/controller/AuthController.java` — registro y login
- `primeraApi/src/main/java/com/lumura/primeraApi/controller/ProductoController.java` — catálogo
- `primeraApi/src/main/java/com/lumura/primeraApi/controller/CarritoController.java` — carrito de compras
- `primeraApi/src/main/java/com/lumura/primeraApi/controller/PedidoController.java` — pedidos
- `primeraApi/src/main/java/com/lumura/primeraApi/controller/AdminController.java` — panel admin

## Panel admin: Gestión de Usuarios

- `GET /api/admin/usuarios` devuelve cada usuario con `id_usuario, nombre_usuario, correo_usuario, telefono, edad, direccion_usuario, rol, fecha_registro, nombre_negocio, nit, persona_contacto, categoria_productos, licencia_distribuidor, bloqueado, motivo_bloqueo, bloqueo_hasta` (los campos de aliado vienen `null` para roles no-ALIADO). **Excluye al admin autenticado** (filtra por id del token: `jwtUtil.getUserIdFromToken`)
- `PUT /api/admin/usuarios/{id}/bloquear` (`{motivo, dias}`) y `PUT /api/admin/usuarios/{id}/desbloquear` gestionan el bloqueo temporal por días; `@Transactional`, validan admin, 404 si no existe, 400 si es rol ADMIN
- `DELETE /api/admin/usuarios/{id}` elimina usuario (400 si es rol ADMIN, 404 si no existe, 401 si no es admin)
- `AuthController.login` valida el bloqueo: cuenta bloqueada con fecha vigente → 403 "Cuenta bloqueada. Motivo: ... Vuelve a intentarlo después del ..."; si el bloqueo venció lo limpia y permite entrar
- Columna `bloqueado` (BOOLEAN default false), `motivo_bloqueo` (TEXT), `bloqueo_hasta` (DATETIME) en `usuario` (documentadas en `schema.sql`)
- Pantalla `screen-admin-users` (botón "Usuarios" en el sidebar) muestra la tabla con columna **Estado** (badge Activo/Bloqueado), botones "Ver" (abre `modal-perfil-usuario` con perfil completo + datos de aliado si aplica), "Bloquear" (abre `modal-bloqueo-usuario` con motivo + días), "Desbloquear" (con confirmación) y "Eliminar" (admin queda "Protegido", sin botón)
- `showScreen` ahora controla el `display` por JS (`s.style.display='none'` para las demás y `screen.style.display='block'` para la activa) y no depende de la clase CSS; esto evita que un `style="display:none;"` inline residual oculte una pantalla aunque tenga la clase `active`
