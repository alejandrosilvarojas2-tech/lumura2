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
  - `src/main/java/com/lumura/primeraApi/entity/` — 4 entidades JPA (Catalogo, Usuario, Carrito, Compra)
  - `src/main/java/com/lumura/primeraApi/repository/` — 4 repositorios Spring Data JPA
  - `src/main/java/com/lumura/primeraApi/controller/` — 5 controladores REST (Auth, Producto, Carrito, Pedido, Admin)
  - `src/main/java/com/lumura/primeraApi/util/JwtUtil.java` — generación y validación de tokens JWT
  - `src/main/java/com/lumura/primeraApi/config/WebConfig.java` — CORS para desarrollo
  - `src/main/resources/static/` — frontend (index.html, lumura.js, lumura.css)
  - `src/main/resources/application.properties` — conexión MySQL + JWT config
- **Raíz del proyecto** — contiene los archivos originales del frontend (referencia)

## Rutas API

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/login` | No | Login, devuelve JWT |
| POST | `/api/auth/register` | No | Registro de usuario |
| GET | `/api/productos` | No | Lista productos |
| GET | `/api/productos/:id` | No | Detalle de producto |
| GET | `/api/carrito/:id_usuario` | JWT | Ver carrito |
| POST | `/api/carrito` | JWT | Agregar al carrito |
| DELETE | `/api/carrito/:id_carrito` | JWT | Eliminar del carrito |
| POST | `/api/pedidos` | JWT | Crear pedido |
| GET | `/api/pedidos/:id_usuario` | JWT | Pedidos del usuario |
| GET | `/api/admin/dashboard` | JWT | KPIs del dashboard |
| GET | `/api/admin/pedidos` | JWT | Todos los pedidos (admin) |
| PUT | `/api/admin/pedidos/:id` | JWT | Actualizar estado pedido |

## Base de datos

- MySQL 8, database `publico`, usuario `alejandro`/`123456`
- Tablas: `usuario`, `catalogo`, `carrito`, `compras` (con datos de muestra)
- Schema: `schema.sql` en la raíz del proyecto

## Estado del proyecto

- Frontend servido por Spring Boot en `localhost:8080`
- API REST funcional: productos, auth, carrito, pedidos, admin dashboard
- JWT implementado con jjwt 0.12.5
- Jackson snake_case para serialización JSON
- CORS abierto para desarrollo
- Admin detecta admin por email `admin@lumura.com` (hardcodeado temporalmente)
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
