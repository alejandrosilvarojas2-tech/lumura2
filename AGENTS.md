# LUMURA - E-commerce de Ropa

Node.js/Express + MySQL, con frontend HTML/CSS/JS single-page.

## Stack

- **Backend**: Express 5, MySQL (`mysql` npm), JWT (`jsonwebtoken`), bcrypt
- **Frontend**: HTML/CSS/JS vanilla, single-page (11 screens)
- **Java API** (separado): `primeraApi/` — Spring Boot 4.0.6, Java 17 (experimental)

## Comandos

```bash
npm install               # instalar dependencias
npm start                 # servidor en http://localhost:3000
npm run dev               # con nodemon (no instalado globalmente — requiere instalación)
```

## Arquitectura

- **`server.js`** — API REST con rutas para auth, productos, carrito, pedidos, admin
- **`conexion.js`** — conexión MySQL (hardcodeada: user `alejandro`, db `publico`)
- **`LUMURA.html`** — frontend single-page con 11 pantallas (navegación por `showScreen()`)
- **`lumura.js`** — lógica frontend: estado, API calls, render dinámico
- **`lumura.css`** — tema oscuro con acento rojo (`--accent: #e94560`)

## Rutas API

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/login` | No | Login, devuelve JWT |
| POST | `/api/auth/register` | No | Registro de usuario |
| GET | `/api/productos` | No | Lista productos activos |
| GET | `/api/productos/:id` | No | Detalle de producto |
| POST | `/api/productos` | JWT | Crear producto (admin) |
| GET | `/api/carrito/:id_usuario` | JWT | Ver carrito |
| POST | `/api/carrito` | JWT | Agregar al carrito |
| DELETE | `/api/carrito/:id_carrito` | JWT | Eliminar del carrito |
| POST | `/api/pedidos` | JWT | Crear pedido |
| GET | `/api/pedidos/:id_usuario` | JWT | Pedidos del usuario |
| GET | `/api/admin/dashboard` | JWT | KPIs del dashboard |
| GET | `/api/admin/pedidos` | JWT | Todos los pedidos (admin) |
| PUT | `/api/admin/pedidos/:id` | JWT | Actualizar estado pedido |

## Estado del proyecto

- Frontend conectado al backend via `fetch()` con JWT almacenado en `localStorage`
- Middleware JWT (`autenticar`) protege rutas de carrito, pedidos y admin
- DB config hardcodeada en `conexion.js` — requiere servidor MySQL con base `publico`
- `primeraApi/` es un proyecto Spring Boot separado y experimental (no integrado)
- Sin tests automatizados
- Git remote: `git@github.com:alejandrosilvarojas2-tech/lumura2.git`
