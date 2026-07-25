# LUMURA — Documentación Técnica del Proyecto

**Versión 1.0 — Julio 2026**  
**Proyecto Formativo — Análisis y Desarrollo de Software**

---

## Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Arquitectura del Sistema](#2-arquitectura-del-sistema)
3. [Base de Datos](#3-base-de-datos)
4. [Información Sensible y Seguridad](#4-información-sensible-y-seguridad)
5. [Componentes del Frontend](#5-componentes-del-frontend)
6. [Funcionalidad del Repositorio](#6-funcionalidad-del-repositorio)
7. [Aspectos Generales](#7-aspectos-generales)

---

# 1. Introducción

## 1.1 Descripción del Proyecto

**LUMURA** es una plataforma de comercio electrónico (e-commerce) especializada en la venta de ropa. El sistema consta de un backend desarrollado con **Spring Boot 3.2.5** y **Java 17**, una base de datos **MySQL 8.0**, y un frontend de página única (**SPA**) construido con **HTML5**, **CSS3** y **JavaScript vanilla**.

El proyecto se desarrolla como parte de un proyecto formativo en Análisis y Desarrollo de Software, abarcando desde el diseño de la base de datos hasta la implementación de servicios REST y la interfaz de usuario.

## 1.2 Objetivos del Sistema

- Permitir a los usuarios explorar un catálogo de productos de ropa
- Gestionar un carrito de compras con selección de talla, color y cantidad
- Procesar pedidos y asignar métodos de pago
- Proporcionar un panel administrativo para la gestión del negocio
- Implementar autenticación segura mediante tokens JWT
- Soportar dos entornos de ejecución: desarrollo local (dev) y producción (prod)

---

# 2. Arquitectura del Sistema

## 2.1 Visión General

El sistema sigue una **arquitectura monolítica de tres capas**. Toda la aplicación se despliega como un único artefacto JAR de Spring Boot que incluye tanto los servicios REST como el frontend estático. La comunicación entre capas sigue el flujo:

```
Cliente (Navegador)
       ↓  HTTP (JSON)
Controladores REST (Spring MVC)
       ↓
Lógica de Negocio (en Controllers)
       ↓
Repositorios JPA (Spring Data)
       ↓
Base de Datos MySQL
```

## 2.2 Diagrama de Capas

| Capa | Ubicación | Tecnología | Responsabilidad |
|------|-----------|------------|-----------------|
| **Presentación** | `src/main/resources/static/` | HTML, CSS, JavaScript | Interfaz de usuario SPA servida estáticamente |
| **API REST** | `src/main/java/.../controller/` | Spring MVC, Jackson | Exposición de endpoints `/api/**`, serialización JSON |
| **Persistencia** | `src/main/java/.../repository/` | Spring Data JPA, Hibernate | Operaciones CRUD contra MySQL |
| **Datos** | `src/main/java/.../entity/` | JPA, Jakarta Persistence | Mapeo objeto-relacional (ORM) |
| **Seguridad** | `src/main/java/.../util/JwtUtil.java` | jjwt 0.12.5, BCrypt | Generación y validación de tokens JWT |
| **Configuración** | `src/main/java/.../config/` | Spring @Configuration | CORS, beans, perfiles |

## 2.3 Estructura de Directorios

```
Desktop/lumura/
├── AGENTS.md                    # Notas de contexto para el asistente
├── primeraApi/                  # Proyecto Spring Boot
│   ├── pom.xml                  # Dependencias Maven
│   ├── mvnw / mvnw.cmd          # Maven wrapper (Unix / Windows)
│   ├── Dockerfile               # Construcción Docker multi-stage
│   ├── docker-compose.yml       # Orquestación app + MySQL
│   ├── .dockerignore            # Exclusiones para build Docker
│   ├── .gitignore               # Exclusiones Git
│   └── src/
│       ├── main/
│       │   ├── java/com/lumura/primeraApi/
│       │   │   ├── PrimeraApiApplication.java    # Punto de entrada
│       │   │   ├── config/WebConfig.java         # Configuración CORS
│       │   │   ├── util/JwtUtil.java             # Utilidad JWT
│       │   │   ├── entity/                       # 4 entidades JPA
│       │   │   ├── repository/                   # 4 repositorios
│       │   │   └── controller/                   # 5 controladores REST
│       │   └── resources/
│       │       ├── application.properties        # Config compartida
│       │       ├── application-dev.properties    # Dev: MySQL local
│       │       ├── application-prod.properties   # Prod: variables entorno
│       │       └── static/                       # Frontend SPA
│       │           ├── index.html                # Página principal
│       │           ├── lumura.js                 # Lógica JS
│       │           ├── lumura.css                # Estilos
│       │           └── images/                   # Recursos gráficos
│       └── test/                                 # Tests
└── (raíz del proyecto)
    ├── package.json              # Script npm start
    └── schema.sql                # Esquema de base de datos
```

## 2.4 Perfiles de Entorno

Spring Boot soporta dos perfiles de ejecución mediante `spring.profiles.active`:

| Perfil | Archivo | Propósito | Conexión BD | JWT Secret |
|--------|---------|-----------|-------------|------------|
| **dev** | `application-dev.properties` | Desarrollo local | MySQL en `localhost:3306`, usuario `alejandro`/`123456` | `LUMURA_SECRET_KEY_2026_CHANGE_IN_PROD` |
| **prod** | `application-prod.properties` | Producción | Variables de entorno: `DB_URL`, `DB_USER`, `DB_PASSWORD` | Variable de entorno: `JWT_SECRET` |

**Mecanismo de activación:**
- Por defecto se activa **dev** (definido en `application.properties`)
- Para producción: `set SPRING_PROFILES_ACTIVE=prod` antes de ejecutar
- En Docker: configurado vía variable de entorno en `docker-compose.yml`

---

# 3. Base de Datos

## 3.1 Gestor y Configuración

| Propiedad | Valor |
|-----------|-------|
| Gestor | MySQL 8.0 |
| Base de datos | `publico` |
| Puerto | 3306 (desarrollo), 3307 (Docker, para evitar conflicto) |
| DDL | `spring.jpa.hibernate.ddl-auto=update` — Hibernate sincroniza el esquema automáticamente |
| Dialecto | `org.hibernate.dialect.MySQLDialect` |

## 3.2 Modelo Entidad-Relación

El sistema cuenta con **4 tablas** que se relacionan entre sí:

```
┌─────────────┐       ┌──────────────┐       ┌─────────────┐
│   usuario   │──1:N──│   carrito    │       │  catalogo   │
│ (id_usuario)│       │ (id_usuario) │       │ (id_catalogo)│
└─────────────┘       └──────────────┘       └─────────────┘
       │                                            │
       │                                            │
       │1:N                                         │(ref. por nombre)
       │                                            │
       ▼                                            ▼
┌──────────────┐                           ┌─────────────┐
│   compras    │                           │  carrito /  │
│ (id_usuario) │                           │  compras    │
└──────────────┘                           │ (articulo)  │
                                           └─────────────┘
```

**Nota:** Las relaciones con `catalogo` se hacen por nombre de artículo (`articulo`) en lugar de clave foránea, debido a que el carrito y las compras almacenan el nombre textualmente.

## 3.3 Tabla: `usuario`

Almacena los datos de los usuarios registrados en la plataforma.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id_usuario` | `INT` | PRIMARY KEY, AUTO_INCREMENT | Identificador único del usuario |
| `nombre_usuario` | `VARCHAR(255)` | NOT NULL | Nombre completo |
| `correo_usuario` | `VARCHAR(255)` | NOT NULL, UNIQUE | Correo electrónico (usado para login) |
| `password_hash` | `VARCHAR(255)` | NOT NULL | Hash BCrypt de la contraseña |
| `telefono` | `VARCHAR(255)` | NULLABLE | Número de contacto |
| `edad` | `INT` | NULLABLE | Edad del usuario |
| `direccion_usuario` | `TEXT` | NULLABLE | Dirección de envío |
| `fecha_registro` | `DATETIME` | NULLABLE | Fecha de creación de la cuenta |
| `rol` | `VARCHAR(20)` | DEFAULT 'USER' | Rol: `USER` (cliente) o `ADMIN` (administrador) |

**Datos precargados:**
- `admin@lumura.com` — cuenta de administrador con rol `ADMIN`
- Varios usuarios de prueba registrados

## 3.4 Tabla: `catalogo`

Catálogo de productos disponibles para la venta.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id_catalogo` | `INT` | PRIMARY KEY, AUTO_INCREMENT | Identificador único del producto |
| `articulo` | `VARCHAR(255)` | NOT NULL | Nombre del artículo |
| `talla` | `VARCHAR(255)` | NULLABLE | Talla(s) disponible(s) |
| `color` | `VARCHAR(255)` | NULLABLE | Color(es) disponible(s) |
| `precio` | `DECIMAL(10,2)` | NOT NULL | Precio actual |
| `precio_descuento` | `DECIMAL(10,2)` | NULLABLE | Precio con descuento (opcional) |
| `descripcion` | `TEXT` | NULLABLE | Descripción detallada |
| `categoria` | `VARCHAR(255)` | NULLABLE | Categoría (Camisetas, Pantalones, Chaquetas, Vestidos, Accesorios) |
| `stock` | `INT` | NULLABLE | Unidades disponibles |
| `imagen_url` | `VARCHAR(255)` | NULLABLE | URL de la imagen del producto |
| `estado` | `VARCHAR(20)` | DEFAULT 'activo' | Estado (`activo` / `inactivo`) |
| `fecha_creacion` | `DATETIME` | NULLABLE | Fecha de alta del producto |

## 3.5 Tabla: `carrito`

Carrito de compras temporal de cada usuario.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id_carrito` | `INT` | PRIMARY KEY, AUTO_INCREMENT | Identificador único del ítem |
| `id_usuario` | `INT` | NOT NULL | ID del usuario propietario |
| `articulo` | `VARCHAR(255)` | NOT NULL | Nombre del artículo |
| `talla` | `VARCHAR(255)` | NULLABLE | Talla seleccionada |
| `color` | `VARCHAR(255)` | NULLABLE | Color seleccionado |
| `cantidad` | `INT` | NOT NULL | Cantidad de unidades |

## 3.6 Tabla: `compras`

Historial de pedidos realizados por los usuarios.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id_compra` | `INT` | PRIMARY KEY, AUTO_INCREMENT | Identificador único del pedido |
| `id_usuario` | `INT` | NOT NULL | ID del usuario que realizó el pedido |
| `articulo` | `TEXT` | NULLABLE | Descripción de los artículos comprados |
| `cantidad_objetos` | `INT` | NULLABLE | Cantidad total de artículos |
| `metodo_pago` | `VARCHAR(50)` | NULLABLE | Método de pago seleccionado |
| `total` | `DECIMAL(10,2)` | NULLABLE | Valor total del pedido |
| `direccion_entrega` | `TEXT` | NULLABLE | Dirección donde se entrega |
| `estado_pedido` | `VARCHAR(50)` | NULLABLE | Estado del pedido |
| `fecha_pedido` | `DATETIME` | NULLABLE | Fecha de realización |

**Estados de pedido posibles:** `pendiente`, `confirmado`, `enviado`, `entregado`, `cancelado`

---

# 4. Información Sensible y Seguridad

## 4.1 Datos Sensibles Identificados

| Tipo de Dato | Dónde se Almacena | Nivel de Sensibilidad |
|-------------|-------------------|-----------------------|
| Contraseñas | `usuario.password_hash` | **Alto** — hash BCrypt, no texto plano |
| Token JWT | En memoria del navegador (`localStorage`) | **Alto** — permite acceso a la cuenta |
| Correo electrónico | `usuario.correo_usuario` | **Medio** — dato de identificación |
| Dirección física | `usuario.direccion_usuario`, `compras.direccion_entrega` | **Medio** — dato personal |
| Teléfono | `usuario.telefono` | **Medio** — dato de contacto |
| JWT Secret | `application-dev.properties` y variable de entorno `JWT_SECRET` | **Crítico** — firma de tokens |
| Credenciales BD | `application-dev.properties` (dev) y variables de entorno (prod) | **Crítico** — acceso a base de datos |

## 4.2 Medidas de Seguridad Implementadas

### 4.2.1 Contraseñas con BCrypt

Todas las contraseñas se almacenan utilizando **BCrypt** con salt incorporado:

```java
// Registro: hash al guardar
usuario.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));

// Login: verificación
BCrypt.checkpw(password, storedHash)
```

BCrypt genera automáticamente un salt aleatorio, garantizando que dos usuarios con la misma contraseña tengan hashes distintos.

### 4.2.2 Tokens JWT

La autenticación utiliza **JSON Web Tokens (JWT)** con las siguientes características:

- **Algoritmo de firma:** HMAC-SHA256 (clave simétrica)
- **Claims incluidos:** `sub` (ID de usuario), `email`, `rol`
- **Expiración:** 24 horas (configurable mediante `app.jwt.expiration`)
- **Transmisión:** Header `Authorization: Bearer <token>`
- **Almacenamiento en frontend:** `localStorage`

```java
// Generación del token
Jwts.builder()
    .subject(userId.toString())
    .claim("email", email)
    .claim("rol", rol)
    .issuedAt(now)
    .expiration(new Date(now.getTime() + expiration))
    .signWith(key)
    .compact();
```

### 4.2.3 Control de Acceso por Roles

- **Rol USER:** Acceso a endpoints de carrito, pedidos y gestión propia de cuenta
- **Rol ADMIN:** Acceso completo a endpoints de administración (dashboard, pedidos, productos, usuarios)
- **Protección en backend:** Cada endpoint verifica el token y el rol antes de procesar

```java
// Verificación de token y rol ADMIN
private boolean validarAdmin(String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) return false;
    String token = auth.substring(7);
    if (!jwtUtil.validateToken(token)) return false;
    return "ADMIN".equals(jwtUtil.getRolFromToken(token));
}
```

### 4.2.4 Separación de Configuración Sensible por Perfil

| Perfil | Práctica de Seguridad |
|--------|----------------------|
| **dev** | Credenciales en texto plano en archivo `.properties` (solo desarrollo local) |
| **prod** | **NUNCA** se hardcodean credenciales. Se usan exclusivamente variables de entorno |

### 4.2.5 Protecciones Adicionales

- **Cuenta admin protegida:** No se puede eliminar `admin@lumura.com` desde los endpoints `DELETE /api/auth/cuenta` ni `DELETE /api/admin/usuarios/{id}`
- **CORS configurable:** En dev abierto (`*`), en prod restringido por `CORS_ORIGINS`
- **Validación de tokens:** Cada petición protegida verifica el token antes de procesar
- **Transaccionalidad:** La creación de pedidos es atómica (se crea compra y se vacía carrito en una transacción)

## 4.3 Recomendaciones para Producción

1. **Cambiar `JWT_SECRET`** por una clave larga y aleatoria (mínimo 256 bits)
2. **Restringir `CORS_ORIGINS`** al dominio específico del frontend
3. **Usar HTTPS** en el servidor de producción
4. **Rotar el JWT Secret** periódicamente
5. **No commitear** `application-dev.properties` con credenciales reales al repositorio público

---

# 5. Componentes del Frontend

## 5.1 Arquitectura del Frontend

El frontend es una **Single-Page Application (SPA)** construida con JavaScript vanilla. Toda la lógica reside en un único archivo HTML (`index.html`) que contiene las 12 pantallas del sistema, más un archivo de estilos (`lumura.css`) y un archivo de lógica JavaScript (`lumura.js`)

### Archivos que Componen el Frontend

| Archivo | Propósito | Líneas Aprox. |
|---------|-----------|---------------|
| `index.html` | Estructura HTML de todas las pantallas, modales, formularios | ~900 |
| `lumura.js` | Lógica de negocio del cliente, llamadas AJAX, manipulación del DOM | ~900 |
| `lumura.css` | Estilos visuales, diseño responsive, animaciones | ~900 |

## 5.2 Pantallas del Sistema

El frontend implementa **12 pantallas** que se muestran/ocultan mediante la función `showScreen()`:

| # | ID de Pantalla | Nombre | Visibilidad | Descripción |
|---|----------------|--------|-------------|-------------|
| 1 | `screen-login` | Inicio de Sesión | Usuario no autenticado | Formulario de login con gradiente, logo LUMURA, opción de registro |
| 2 | `screen-register` | Registro | Usuario no autenticado | Formulario con nombre, correo, teléfono, dirección, contraseña |
| 3 | `screen-home` | Catálogo / Inicio | Todos | Banner promocional, filtro por categorías, buscador, grid de productos |
| 4 | `screen-product` | Detalle de Producto | Todos | Imagen grande, nombre, precio, descripción, botón de agregar al carrito |
| 5 | `screen-cart` | Carrito de Compras | Todos (autenticado para comprar) | Lista de ítems con precios, resumen (subtotal, envío, total) |
| 6 | `screen-checkout` | Checkout / Pago | Usuario autenticado | Steps (carrito → pago → confirmación), método de pago, dirección de entrega |
| 7 | `screen-confirm` | Confirmación | Usuario autenticado | Steps completados, icono de éxito, detalle del pedido, botones de acción |
| 8 | `screen-orders` | Mis Pedidos | Usuario autenticado | Historial de pedidos con opción de cancelar |
| 9 | `screen-admin-dash` | Dashboard Admin | ADMIN | KPIs (ingresos, pedidos, clientes, productos), gráfico de ventas, pedidos recientes |
| 10 | `screen-admin-cat` | Gestión de Catálogo | ADMIN | Tabla de productos, filtros, modal para crear/editar, CRUD completo |
| 11 | `screen-admin-inv` | Control de Inventario | ADMIN | KPIs de stock, tabla con alertas de productos agotados y stock bajo |
| 12 | `screen-admin-rep` | Reportes de Ventas | ADMIN | KPIs de ventas, gráfico semanal, top productos, últimas transacciones |
| 13 | `screen-admin-users` | Gestión de Usuarios | ADMIN | Tabla de usuarios con botón de eliminar |

### 5.2.1 Mecanismo de Navegación

```javascript
function showScreen(id) {
    // Oculta todas las pantallas
    document.querySelectorAll('.screen').forEach(s => s.style.display = 'none');
    // Muestra la solicitada
    document.getElementById('screen-' + id).style.display = 'block';
    // Actualiza la UI según el estado de autenticación
    actualizarUI();
}
```

## 5.3 Componentes Compartidos

### 5.3.1 Header / Barra de Navegación

Presente en todas las pantallas principales. Incluye:
- **Logo:** LUMURA (estilo Playfair Display)
- **Enlaces:** Inicio, Productos, Mis pedidos (solo autenticado), Carrito (con contador), Admin (solo ADMIN)
- **Menú de usuario:** Al hacer clic en el nombre, despliega dropdown con "Actualizar datos", "Cerrar sesión", "Eliminar cuenta"

### 5.3.2 Sistema de Mensajes

```html
<div id="msg" class="msg" style="display:none;"></div>
```

Mensajes flotantes que se muestran en la parte superior con estilo de éxito (verde) o error (rojo).

### 5.3.3 Modales

- **Descripción de producto:** Popup con overlay oscuro, nombre + descripción completa, cierra con ✕ o clic fuera
- **Actualizar datos:** Modal con campos de nombre, teléfono, dirección prellenados
- **Confirmar eliminación de cuenta:** Modal de confirmación con "¿Estás seguro de eliminar tu cuenta?"
- **Cancelar pedido:** Modal "¿Desea cancelar su pedido?" con Sí/No
- **Modal producto (admin):** Formulario completo para crear/editar productos del catálogo

### 5.3.4 Filtros y Búsqueda

- **Categorías:** Filtro por tags (`Todo`, `Camisetas`, `Pantalones`, `Chaquetas`, `Vestidos`, `Accesorios`)
- **Buscador:** Filtro por nombre de artículo en tiempo real (`oninput`)
- **Admin catálogo:** Búsqueda + filtro por categoría

## 5.4 Gestión de Estado (state)

El frontend mantiene un objeto global `state` que almacena:

```javascript
const state = {
    productos: [],       // Catálogo completo
    carrito: [],         // Items del carrito del usuario
    user: null,          // Datos del usuario autenticado (o null)
    token: null,         // Token JWT (o null)
    cartCount: 0         // Conteo de items en carrito
};
```

**Persistencia:** `state.user` y `state.token` se sincronizan con `localStorage` para mantener la sesión entre recargas de página.

---

# 6. Funcionalidad del Repositorio

## 6.1 Integración con Git

El repositorio está alojado en **GitHub** bajo la URL:

```
git@github.com:alejandrosilvarojas2-tech/lumura2.git
```

### Flujo de trabajo

- **Rama principal:** `main`
- **Commits:** Directo a `main` (desarrollo unipersonal)

### Comandos de uso diario

```bash
# Ver estado
git status

# Ver cambios
git diff

# Agregar cambios
git add -A
# o archivos específicos:
git add primeraApi/src/main/java/...

# Commit
git commit -m "descripción del cambio"

# Subir
git push

# Ver historial
git log --oneline -10
```

## 6.2 Archivos Versionados

El repositorio incluye:

```
AGENTS.md
Dockerfile
docker-compose.yml
.dockerignore
.gitignore
package.json
schema.sql
primeraApi/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/lumura/primeraApi/
    │   │   ├── PrimeraApiApplication.java
    │   │   ├── config/
    │   │   ├── util/
    │   │   ├── entity/
    │   │   ├── repository/
    │   │   └── controller/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       ├── application-prod.properties
    │       └── static/
    │           ├── index.html
    │           ├── lumura.js
    │           ├── lumura.css
    │           └── images/
    └── test/
```

### Archivos Excluidos (.gitignore)

| Patrón | Razón |
|--------|-------|
| `target/` | Artefactos compilados, no se versionan |
| `.mvn/wrapper/maven-wrapper.jar` | Archivo binario grande, se descarga automáticamente |
| `.idea/`, `*.iml` | Archivos de configuración de IntelliJ IDEA |
| `.vscode/` | Archivos de configuración de VS Code |
| `.settings/`, `.classpath`, `.project` | Archivos de Eclipse/STS |

## 6.3 Estado Actual del Repositorio

- **Commits realizados:** 11 (todos los features del proyecto)
- **Working tree:** Clean (sin cambios sin committear)
- **Rama:** `main`
- **Ahead of origin:** Sí (pendiente de push para algunos cambios)

---

# 7. Aspectos Generales

## 7.1 Stack Tecnológico Completo

| Componente | Tecnología | Versión | Propósito |
|------------|-----------|---------|-----------|
| **Lenguaje** | Java | 17 (LTS) | Desarrollo backend |
| **Framework** | Spring Boot | 3.2.5 | Aplicación web REST |
| **ORM** | Hibernate / JPA | 3.x (Jakarta) | Persistencia de datos |
| **Base de datos** | MySQL | 8.0 | Almacenamiento |
| **Seguridad** | jjwt | 0.12.5 | Tokens JWT |
| **Hash** | jBCrypt | 0.4 | Hash de contraseñas |
| **Serialización** | Jackson | (incluido en Spring Boot) | JSON snake_case |
| **Frontend** | HTML5 / CSS3 / JS | Vanilla | Interfaz de usuario |
| **Tipografía** | Playfair Display + Inter | Google Fonts | Diseño visual |
| **Construcción** | Maven | 3.9+ | Compilación y dependencias |
| **Contenedor** | Docker | latest | Despliegue |
| **Orquestación** | Docker Compose | 3.8 | App + MySQL |
| **Control de versiones** | Git / GitHub | - | Código fuente |
| **CLI Node** | npm | 24.x | Script `npm start` para desarrollo |

## 7.2 Lista de Dependencias (pom.xml)

| Dependencia | GroupId | Versión | Propósito |
|------------|---------|---------|-----------|
| `spring-boot-starter-web` | org.springframework.boot | 3.2.5 | Servidor web embebido (Tomcat) + MVC REST |
| `spring-boot-starter-data-jpa` | org.springframework.boot | 3.2.5 | Spring Data JPA + Hibernate |
| `mysql-connector-j` | com.mysql | 8.x | Driver JDBC para MySQL |
| `jjwt-api` | io.jsonwebtoken | 0.12.5 | API de JWT |
| `jjwt-impl` | io.jsonwebtoken | 0.12.5 | Implementación de JWT |
| `jjwt-jackson` | io.jsonwebtoken | 0.12.5 | Serialización JWT con Jackson |
| `jbcrypt` | org.mindrot | 0.4 | Hash de contraseñas BCrypt |
| `spring-boot-starter-test` | org.springframework.boot | 3.2.5 | Testing (JUnit, Mockito) |

## 7.3 APIs REST — Resumen (21 Endpoints)

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Registro de usuario |
| POST | `/api/auth/login` | No | Inicio de sesión |
| PUT | `/api/auth/cuenta` | JWT | Actualizar datos personales |
| DELETE | `/api/auth/cuenta` | JWT | Eliminar cuenta |
| GET | `/api/productos` | No | Listar catálogo completo |
| GET | `/api/productos/{id}` | No | Detalle de producto |
| GET | `/api/carrito/{idUsuario}` | JWT | Ver carrito del usuario |
| POST | `/api/carrito` | JWT | Agregar al carrito |
| PUT | `/api/carrito/{id}` | JWT | Actualizar cantidad |
| DELETE | `/api/carrito/{id}` | JWT | Eliminar del carrito |
| POST | `/api/pedidos` | JWT | Crear pedido |
| GET | `/api/pedidos/{idUsuario}` | JWT | Historial de pedidos |
| PUT | `/api/pedidos/{id}/cancelar` | JWT | Cancelar pedido |
| GET | `/api/admin/dashboard` | JWT+ADMIN | KPIs del negocio |
| GET | `/api/admin/pedidos` | JWT+ADMIN | Todos los pedidos |
| PUT | `/api/admin/pedidos/{id}` | JWT+ADMIN | Actualizar estado pedido |
| POST | `/api/admin/productos` | JWT+ADMIN | Crear producto |
| PUT | `/api/admin/productos/{id}` | JWT+ADMIN | Actualizar producto |
| DELETE | `/api/admin/productos/{id}` | JWT+ADMIN | Eliminar producto |
| GET | `/api/admin/usuarios` | JWT+ADMIN | Listar usuarios |
| DELETE | `/api/admin/usuarios/{id}` | JWT+ADMIN | Eliminar usuario |

## 7.4 Comandos de Desarrollo

```bash
# Iniciar servidor (desarrollo)
cd Desktop/lumura
npm start

# O directamente con Maven
cd Desktop/lumura/primeraApi
.\mvnw.cmd spring-boot:run

# Con perfil específico
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod

# Construir JAR
.\mvnw.cmd package -DskipTests

# Construir y ejecutar con Docker
cd Desktop/lumura/primeraApi
docker compose up --build
```

## 7.5 Cómo Contribuir al Proyecto

1. **Clonar el repositorio:**
   ```bash
   git clone git@github.com:alejandrosilvarojas2-tech/lumura2.git
   ```

2. **Asegurar MySQL corriendo** con la base de datos `publico` creada

3. **Iniciar el servidor** con `npm start`

4. **Verificar funcionamiento** en `http://localhost:8080`

5. **Para cambios en Java:** el servidor debe reiniciarse (Maven no tiene hot reload por defecto)

6. **Para cambios en frontend** (HTML/CSS/JS): solo recargar el navegador, Spring Boot sirve archivos estáticos sin reinicio

7. **Sincronizar imágenes** a ambas carpetas:
   ```bash
   cp images/*.jpg primeraApi/src/main/resources/static/images/
   cp images/*.jpg primeraApi/target/classes/static/images/
   ```

## 7.6 Despliegue a Producción

**Opción 1: Railway (recomendado)**
1. Subir código a GitHub
2. Crear proyecto en Railway desde el repositorio
3. Configurar root directory como `primeraApi`
4. Agregar MySQL como servicio
5. Configurar variables de entorno (DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, CORS_ORIGINS)
6. Generar dominio público

**Opción 2: Docker manual**
```bash
cd primeraApi
docker compose up --build
```

---

# 8. Diagnóstico del Proyecto — Actualización 25/07/2026

## 8.1 Puntuación General: 60/100

| Categoría | Máximo | Puntaje | Notas |
|-----------|--------|---------|-------|
| Arquitectura y Calidad | 20 | 13 | MVC limpio, separación correcta, sin service layer ni tests |
| Seguridad | 25 | 8 | BCrypt + JWT + RBAC presentes, pero 3 vulnerabilidades críticas |
| Funcionalidad | 20 | 15 | CRUD completo, panel admin, paginación, búsqueda, favoritos |
| Calidad Frontend | 15 | 9 | SPA funcional con buena UX, inline styles, XSS en datos de usuario |
| DevOps | 10 | 7 | Docker multi-stage, Compose con health checks, config multi-env |
| Documentación | 10 | 8 | Documentación técnica exhaustiva (635 líneas), AGENTS.md |
| **TOTAL** | **100** | **60** | |

## 8.2 Vulnerabilidades Críticas Activas

| # | Vulnerabilidad | Ubicación | Impacto |
|---|---------------|-----------|---------|
| C1 | **Reset de contraseña admin sin autenticación** | `AdminController.fixAdminPassword()` | Cualquier usuario anónimo puede hacer POST a `/api/admin/fix-admin-password` y restablecer la contraseña del admin a "123456". **Takeover completo de la cuenta admin.** |
| C2 | **Inserción de productos sin autenticación** | `AdminController.sebrarProductosPublico()` | Cualquier usuario anónimo puede hacer POST a `/api/admin/seed-public` para inyectar productos en la base de datos. |
| C3 | **XSS almacenado vía perfil de usuario** | `lumura.js` líneas 130-132, 744 | El nombre, teléfono y dirección del usuario se renderizan en `innerHTML` sin `escHtml()`. Un usuario malicioso obtiene XSS ejecutándose en el navegador de todos. |

## 8.3 Vulnerabilidades Altas

| # | Vulnerabilidad | Ubicación | Impacto |
|---|---------------|-----------|---------|
| H1 | **IDOR: Manipulación de carrito** | `CarritoController` | `id_usuario` viene del body, no del JWT. Cualquier usuario puede modificar el carrito de otro. |
| H2 | **IDOR: Cancelación de pedidos** | `PedidoController` | Cualquier usuario autenticado puede cancelar el pedido de otro por ID. |
| H3 | **IDOR: Creación de pedidos** | `PedidoController` | `id_usuario` del body permite crear pedidos como otro usuario. |
| H4 | **JWT secret en git** | `application-dev.properties` | `LUMURA_SECRET_KEY_2026_CHANGE_IN_PROD` comprometido en el repositorio. |
| H5 | **DB credentials en git** | `application-dev.properties` | `alejandro`/`123456` comprometidos. |

## 8.4 Bugs Medios

| # | Bug | Ubicación |
|---|-----|-----------|
| M1 | Memory leak en Rate Limiter (entradas nunca se limpian) | `RateLimitFilter` |
| M2 | Rate Limiter no detecta IP real detrás de proxy | `RateLimitFilter.getRemoteAddr()` |
| M3 | Password fallback SHA-256 aún activo | `AuthController` |
| M4 | Falta `@Modifying` en `deleteByIdUsuario()` | `CarritoRepository`, `CompraRepository` |
| M5 | `ddl-auto=update` activo en producción | `application.properties` |
| M6 | Admin auto-promocionado si registra `admin@lumura.com` | `AuthController` |
| M7 | Sin validación de confirmación de contraseña en backend | `AuthController.register()` |

## 8.5 Bugs Bajos

| # | Bug | Ubicación |
|---|-----|-----------|
| L1 | Sin unit tests (dependencia `spring-boot-starter-test` existe sin tests) | Todo el proyecto |
| L2 | Cache de productos nunca se limpia | `productoCache` global |
| L3 | Imágenes hardcodeadas por ID | `imagenesProducto` mapa |
| L4 | Admin panels sin responsive | `lumura.css` sin `@media` |
| L5 | Sidebar admin duplicado 6 veces en HTML | `index.html` |
| L6 | Gráficas dashboard hardcodeadas | `index.html` |
| L7 | Fecha "15 Abr 2025" hardcodeada en header admin | `index.html:304` |
| L8 | `<img>` dentro de `<option>` (inválido) | `index.html:219-222` |
| L9 | `</div>` huérfanos después de `screen-admin-users` | `index.html:377-379` |
| L10 | Sin mecanismo de revocación de JWT | `JwtUtil` |

## 8.6 Endpoints Completos (25)

| # | Método | Ruta | Auth | Descripción |
|---|--------|------|------|-------------|
| 1 | POST | `/api/auth/register` | No | Registro de usuario |
| 2 | POST | `/api/auth/login` | No | Login, devuelve JWT |
| 3 | PUT | `/api/auth/cuenta` | JWT | Actualizar nombre/teléfono/dirección |
| 4 | DELETE | `/api/auth/cuenta` | JWT | Eliminar cuenta + datos |
| 5 | GET | `/api/productos` | No | Listar todos los productos |
| 6 | GET | `/api/productos/page` | No | Productos paginados (?page=0&size=12) |
| 7 | GET | `/api/productos/{id}` | No | Detalle de producto |
| 8 | GET | `/api/carrito/{idUsuario}` | JWT | Ver carrito |
| 9 | POST | `/api/carrito` | JWT | Agregar al carrito (con validación stock) |
| 10 | PUT | `/api/carrito/{idCarrito}` | JWT | Actualizar cantidad |
| 11 | DELETE | `/api/carrito/{idCarrito}` | JWT | Eliminar del carrito |
| 12 | POST | `/api/pedidos` | JWT | Crear pedido (limpia carrito) |
| 13 | GET | `/api/pedidos/{idUsuario}` | JWT | Historial de pedidos |
| 14 | PUT | `/api/pedidos/{id}/cancelar` | JWT | Cancelar pedido |
| 15 | GET | `/api/admin/dashboard` | JWT+ADMIN | KPIs del dashboard |
| 16 | GET | `/api/admin/pedidos` | JWT+ADMIN | Todos los pedidos |
| 17 | PUT | `/api/admin/pedidos/{id}` | JWT+ADMIN | Actualizar estado de pedido |
| 18 | POST | `/api/admin/productos` | JWT+ADMIN | Crear producto |
| 19 | PUT | `/api/admin/productos/{id}` | JWT+ADMIN | Actualizar producto |
| 20 | DELETE | `/api/admin/productos/{id}` | JWT+ADMIN | Eliminar producto |
| 21 | GET | `/api/admin/usuarios` | JWT+ADMIN | Listar usuarios |
| 22 | DELETE | `/api/admin/usuarios/{id}` | JWT+ADMIN | Eliminar usuario |
| 23 | POST | `/api/admin/seed` | JWT+ADMIN | Sembrar productos |
| 24 | POST | `/api/admin/seed-public` | **NINGUNO** | Sembrar productos (sin auth!) |
| 25 | POST | `/api/admin/fix-admin-password` | **NINGUNO** | Resetear password admin (sin auth!) |

## 8.7 Medidas Correctivas Recomendadas (Prioridad)

### Prioridad 1 — Seguridad Inmediata
1. **Eliminar o proteger** `fix-admin-password` (agregar auth o eliminar endpoint)
2. **Eliminar o proteger** `seed-public` (agregar auth o eliminar endpoint)
3. **Corregir XSS** en `mostrarActualizarDatos()` y `actualizarUI()` — aplicar `escHtml()` a todos los datos de usuario
4. **Usar userId del JWT** en lugar del body en carrito y pedidos (corregir IDOR)
5. **Mover secrets** de `application-dev.properties` a variables de entorno

### Prioridad 2 — Integridad de Datos
6. Agregar `@Modifying` a `deleteByIdUsuario()` en repositorios
7. Cambiar `ddl-auto=update` a `ddl-auto=validate` en producción
8. Eliminar fallback SHA-256 de contraseñas
9. Agregar validación de confirmación de contraseña en backend

### Prioridad 3 — Calidad de Código
10. Agregar unit tests para controllers
11. Refactorizar `validarToken()`/`validarAdmin()` a un interceptor o filter
12. Agregar responsive design para admin panels
13. Agregar Content Security Policy header

---

*Fin del documento — LUMURA Documentación Técnica v1.1 (Actualizado 25/07/2026)*
