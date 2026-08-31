# LUMURA — Documentación Técnica del Proyecto

**Versión 2.0 — Agosto 2026**  
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
8. [Diagnóstico del Proyecto — 25/07/2026](#8-diagnóstico-del-proyecto--actualización-25072026)
9. [Diagnóstico Actualizado y Plan de Refuerzo Ejecutado — 21/08/2026](#9-diagnóstico-actualizado-y-plan-de-refuerzo-ejecutado--21082026)

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
| **dev** | `application-dev.properties` | Desarrollo local | MySQL en `localhost:3306`, variables de entorno `DB_USER` (default `alejandro`) y `DB_PASSWORD` (obligatoria) | Variable de entorno `JWT_SECRET`; si falta, se usa secreto **efímero** por ejecución (con advertencia en el log) |
| **prod** | `application-prod.properties` | Producción | Variables de entorno: `DB_URL`, `DB_USER`, `DB_PASSWORD` | Variable de entorno `JWT_SECRET` obligatoria (≥32 caracteres); si falta o es corta, la app **no arranca** |

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
| `rol` | `VARCHAR(20)` | DEFAULT 'USER' | Rol: `USER` (cliente), `ALIADO` (vendedor) o `ADMIN` (administrador) |
| `nombre_negocio` | `VARCHAR(100)` | NULLABLE | Solo aliados: nombre del negocio |
| `nit` | `VARCHAR(40)` | NULLABLE | Solo aliados: NIT |
| `persona_contacto` | `VARCHAR(100)` | NULLABLE | Solo aliados: persona de contacto |
| `categoria_productos` | `VARCHAR(60)` | NULLABLE | Categoría que vende el aliado (Zapatos, Vestidos, Ropa deportiva, ...) |
| `reset_token` | `VARCHAR(64)` | NULLABLE | Token de recuperación de contraseña (se limpia al usarse) |
| `reset_token_expira` | `DATETIME(6)` | NULLABLE | Fecha de expiración del token (30 min de vida) |
| `licencia_distribuidor` | `TEXT` | NULLABLE | URL de la licencia de distribuidor autorizado del aliado |

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

- **Cuentas admin protegidas por rol:** No se puede eliminar ningún usuario con rol `ADMIN` desde los endpoints `DELETE /api/auth/cuenta` ni `DELETE /api/admin/usuarios/{id}` (protección basada en el rol almacenado en BD, no en el correo)
- **Sin promoción automática:** El rol nunca se deriva del correo; se asigna exclusivamente al registrar o por edición directa en BD
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
| 13 | `screen-admin-users` | Gestión de Usuarios | ADMIN | Tabla de usuarios (id, nombre, email, teléfono, rol, registro, estado) con botones **Ver** (modal `modal-perfil-usuario` con perfil completo, incluidos datos de aliado: negocio, NIT, contacto, categoría, licencia; el admin queda "Protegido"), **Bloquear** (modal `modal-bloqueo-usuario` con motivo + días), **Desbloquear** y **Eliminar**; el admin autenticado no aparece en la lista |
| 14 | `screen-aliado-dash` | Dashboard Aliado | ALIADO | KPIs de ventas del aliado, productos, accesos rápidos |
| 15 | `screen-aliado-add` | Añadir Artículo | ALIADO | Formulario para publicar un producto del aliado |
| 16 | `screen-aliado-stock` | Stock Aliado | ALIADO | Control de inventario de los productos del aliado |
| 17 | `screen-aliado-desc` | Descripción de Artículo | ALIADO | Editar descripciones de los productos del aliado |
| 18 | `screen-aliado-licencia` | Licencia de Distribuidor | ALIADO | Sube la imagen de la licencia de distribuidor autorizado (anuncio: "Sube tu licencia de distribuidor autorizado"); muestra la licencia actual |

### 5.2.1 Mecanismo de Navegación

```javascript
function showScreen(name) {
    // Oculta todas las pantallas
    document.querySelectorAll('.screen').forEach(s => {
        s.classList.remove('active');
        s.style.display = 'none';   // display controlado por JS (no solo clase CSS)
    });
    // Muestra la solicitada
    const screen = document.getElementById('screen-' + name);
    if (screen) { screen.classList.add('active'); screen.style.display = 'block'; }
    // Marcas el ítem del menú lateral y aplicas políticas de rol por nombre
    ...
}
```

> Nota: el display lo controla JavaScript (`s.style.display`), no solo la clase `active`. Así ningún `style="display:none;"` inline residual puede mantener oculta una pantalla aunque tenga la clase `active` (caso detectado y corregido en `screen-admin-users`).

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
| POST | `/api/auth/logout` | JWT | Cierra sesión e invalida todos los tokens del usuario |
| PUT | `/api/auth/cuenta` | JWT | Actualizar datos personales |
| DELETE | `/api/auth/cuenta` | JWT | Eliminar cuenta |
| POST | `/api/auth/recuperar` | No | Solicita enlace de recuperación (offline: `enlace_demo`) |
| POST | `/api/auth/reset-password` | No | Restablece contraseña (revoca los JWT previos) |
| GET | `/api/productos` | No | Listar catálogo completo |
| GET | `/api/productos/{id}` | No | Detalle de producto |
| GET | `/api/carrito/{idUsuario}` | JWT | Ver carrito del usuario |
| POST | `/api/carrito` | JWT | Agregar al carrito |
| PUT | `/api/carrito/{id}` | JWT | Actualizar cantidad |
| DELETE | `/api/carrito/{id}` | JWT | Eliminar del carrito |
| POST | `/api/pedidos` | JWT | Crear pedido |
| GET | `/api/pedidos/{idUsuario}` | JWT | Historial de pedidos (con `detalles[]`; cada detalle incluye `vendedor` con datos del aliado) |
| PUT | `/api/pedidos/{id}/cancelar` | JWT | Cancelar pedido |
| GET | `/api/admin/dashboard` | JWT+ADMIN | KPIs del negocio |
| GET | `/api/admin/pedidos` | JWT+ADMIN | Todos los pedidos |
| PUT | `/api/admin/pedidos/{id}` | JWT+ADMIN | Actualizar estado pedido |
| POST | `/api/admin/productos` | JWT+ADMIN | Crear producto |
| PUT | `/api/admin/productos/{id}` | JWT+ADMIN | Actualizar producto |
| DELETE | `/api/admin/productos/{id}` | JWT+ADMIN | Eliminar producto |
| GET | `/api/admin/usuarios` | JWT+ADMIN | Listar usuarios (excluye al admin autenticado; incluye `bloqueado`, `motivo_bloqueo`, `bloqueo_hasta`) |
| PUT | `/api/admin/usuarios/{id}/bloquear` | JWT+ADMIN | Bloquear usuario `{motivo, dias}` (400 si es ADMIN, motivo vacío o días < 1) |
| PUT | `/api/admin/usuarios/{id}/desbloquear` | JWT+ADMIN | Desbloquear usuario (limpia motivo y fecha) |
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
| H1 | ~~IDOR: Manipulación de carrito~~ ✅ CORREGIDO | `CarritoController` | PUT y DELETE ahora verifican que el item pertenezca al usuario del JWT (403 si no). Además el carrito guarda `id_catalogo` y resuelve precio/stock por FK, no por nombre. |
| H2 | **IDOR: Cancelación de pedidos** | `PedidoController` | Cualquier usuario autenticado puede cancelar el pedido de otro por ID. |
| H3 | **IDOR: Creación de pedidos** | `PedidoController` | `id_usuario` del body permite crear pedidos como otro usuario. |
| H4 | ~~JWT secret en git~~ ✅ CORREGIDO | `application.properties` | Ya no hay ningún secreto commiteado. Sin `JWT_SECRET`: prod **no arranca** (fail-fast, ≥32 caracteres) y dev usa secreto efímero por ejecución. |
| H5 | ~~DB credentials en git~~ ✅ CORREGIDO | `application-dev.properties` | Ahora usa variables de entorno `DB_USER`/`DB_PASSWORD`; sin credenciales en el repo. |

## 8.4 Bugs Medios

| # | Bug | Ubicación |
|---|-----|-----------|
| M1 | Memory leak en Rate Limiter (entradas nunca se limpian) | `RateLimitFilter` |
| M2 | Rate Limiter no detecta IP real detrás de proxy | `RateLimitFilter.getRemoteAddr()` |
| M3 | Password fallback SHA-256 aún activo | `AuthController` |
| M4 | Falta `@Modifying` en `deleteByIdUsuario()` | `CarritoRepository`, `CompraRepository` |
| M5 | `ddl-auto=update` activo en producción | `application.properties` |
| M6 | ~~Admin auto-promocionado si registra `admin@lumura.com`~~ ✅ CORREGIDO — rol solo desde BD; protección admin por rol en `AuthController` y `AdminController` | `AuthController` |
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
| 13 | GET | `/api/pedidos/{idUsuario}` | JWT | Historial de pedidos (cada detalle con `vendedor` del aliado) |
| 14 | PUT | `/api/pedidos/{id}/cancelar` | JWT | Cancelar pedido |
| 15 | GET | `/api/admin/dashboard` | JWT+ADMIN | KPIs del dashboard |
| 16 | GET | `/api/admin/pedidos` | JWT+ADMIN | Todos los pedidos |
| 17 | PUT | `/api/admin/pedidos/{id}` | JWT+ADMIN | Actualizar estado de pedido |
| 18 | POST | `/api/admin/productos` | JWT+ADMIN | Crear producto |
| 19 | PUT | `/api/admin/productos/{id}` | JWT+ADMIN | Actualizar producto |
| 20 | DELETE | `/api/admin/productos/{id}` | JWT+ADMIN | Eliminar producto |
| 21 | GET | `/api/admin/usuarios` | JWT+ADMIN | Listar usuarios (excluye admin autenticado) |
| 22 | PUT | `/api/admin/usuarios/{id}/bloquear` | JWT+ADMIN | Bloquear usuario |
| 23 | PUT | `/api/admin/usuarios/{id}/desbloquear` | JWT+ADMIN | Desbloquear usuario |
| 24 | DELETE | `/api/admin/usuarios/{id}` | JWT+ADMIN | Eliminar usuario |
| 25 | POST | `/api/admin/seed` | JWT+ADMIN | Sembrar productos |
| 26 | POST | `/api/admin/seed-public` | **NINGUNO** | Sembrar productos (sin auth!) |
| 27 | POST | `/api/admin/fix-admin-password` | **NINGUNO** | Resetear password admin (sin auth!) |

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

# 9. Diagnóstico Actualizado y Plan de Refuerzo Ejecutado — 21/08/2026

Este capítulo re-evalúa el diagnóstico de la sección 8 tras ejecutar el plan de refuerzo
(10 puntos). Cada hallazgo se verificó contra el código actual y, donde aplica, con pruebas
en vivo contra el servidor real. La sección 8 se conserva como registro histórico.

## 9.1 Puntuación Re-evaluada: 60/100 → 88/100

| Categoría | Máximo | Antes | Ahora | Cambios principales |
|-----------|--------|-------|-------|---------------------|
| Arquitectura y Calidad | 20 | 13 | 17 | Pedidos normalizados (`detalle_compra`), favoritos en backend, 67 tests unitarios |
| Seguridad | 25 | 8 | 22 | Sin secretos en git (fail-fast), IDOR eliminados (carrito/pedidos/favoritos), total calculado server-side, admin por rol |
| Funcionalidad | 20 | 15 | 20 | Favoritos persistentes multi-dispositivo, desglose de pedido por línea, descuento de stock al confirmar, pasarela de pago simulada offline, recuperación de contraseña por token |
| Calidad Frontend | 15 | 9 | 12 | XSS de datos de usuario mitigado con `escHtml`, manejo robusto de respuestas del servidor |
| DevOps | 10 | 7 | 9 | Credenciales 100% por variables de entorno, `.env.example`, prod con `ddl-auto=validate` |
| Documentación | 10 | 8 | 10 | AGENTS.md con las 30 rutas reales, schema.sql documentado, este diagnóstico actualizado |
| **TOTAL** | **100** | **60** | **90** | |

## 9.2 Estado Final de los Hallazgos

### Críticas (sección 8.2)

| # | Hallazgo original | Estado | Verificación |
|---|-------------------|--------|--------------|
| C1 | Reset de contraseña admin sin auth | ✅ **CORREGIDO** — endpoint eliminado del código | No existe ninguna ruta `fix-admin-password` en `AdminController`; el inventario de rutas lo confirma |
| C2 | Inserción de productos sin auth | ✅ **CORREGIDO** — endpoint eliminado; queda `/api/admin/seed` protegido con JWT+ADMIN | Inventario de rutas + test `AdminControllerTest` |
| C3 | XSS almacenado vía perfil | ✅ **CORREGIDO** — datos de usuario renderizados con `escHtml()` | Revisión de código (`lumura.js`); helper aplicado en menú de usuario y pantallas de perfil |

### Altas (sección 8.3)

| # | Hallazgo original | Estado | Verificación |
|---|-------------------|--------|--------------|
| H1 | IDOR carrito | ✅ CORREGIDO (25/07) + FK `id_catalogo` | Prueba en vivo: renombrar producto en BD no altera el precio |
| H2 | IDOR cancelación de pedidos | ✅ **CORREGIDO** — solo el dueño cancela; no se puede cancelar entregado/cancelado | Prueba en vivo HTTP 403 + tests `cancelar_pedidoAjeno_retorna403`, `cancelar_pedidoEntregado_retorna400` |
| H3 | IDOR creación de pedidos | ✅ **CORREGIDO** — userId siempre del JWT; total e items calculados server-side desde el carrito | **Prueba antifraude en vivo**: cliente envió `total=1` con artículos falsos → servidor calculó 181700 correcto |
| H4 | JWT secret en git | ✅ CORREGIDO (fail-fast prod ≥32 chars, dev efímero) | Suite `JwtUtilTest` (7 tests) + arranque real en ambos modos |
| H5 | DB credentials en git | ✅ CORREGIDO (variables de entorno) | `docker-compose.yml` exige `${DB_PASSWORD:?}`; sin defaults |

### Medias (sección 8.4)

| # | Bug original | Estado | Detalle |
|---|--------------|--------|---------|
| M1 | Memory leak Rate Limiter | ⏳ Abierto (baja prioridad: app monolítica local) | Limpieza periódica del mapa pendiente si se despliega públicamente |
| M2 | Rate limiter no detecta IP real detrás de proxy | ⏳ Abierto | Relevante solo tras despliegue público |
| M3 | Password fallback SHA-256 | ✅ **VERIFICADO AUSENTE** — no queda ningún hash SHA-256 en el código | Búsqueda global en `src/main/java` |
| M4 | Falta `@Modifying` en deletes derivados | ✅ **RESUELTO EN LA PRÁCTICA** — los deletes derivados corren dentro de métodos `@Transactional` del controller | Verificado operando correctamente en vivo (carrito se vacía al confirmar pedido) |
| M5 | `ddl-auto=update` en producción | ✅ **CORREGIDO** — prod usa `validate` | `application-prod.properties` |
| M6 | Admin auto-promocionado por email | ✅ CORREGIDO (rol solo desde BD) | Tests de Auth/Admin |
| M7 | Sin confirmación de contraseña en backend | ✅ **CORREGIDO** — register exige `confirmar_password` | `AuthController.register()`; comprobado en vivo durante las pruebas E2E |

### Bajas (sección 8.5)

| # | Bug original | Estado | Detalle |
|---|--------------|--------|---------|
| L1 | Sin unit tests | ✅ **CORREGIDO** — 110 tests en 9 suites, todos en verde | `mvnw.cmd test` |
| L2-L9 | Frontend menor (cache, responsive, HTML duplicado…) | ⏳ Abiertos | Cosméticos / baja prioridad; no afectan integridad |
| L10 | Sin revocación de JWT | ⏳ Abierto (inherente a JWT stateless) | Mitigación actual: tokens de vida corta configurable vía `JWT_EXPIRATION` |

## 9.3 Plan de Refuerzo Ejecutado (los 10 puntos)

| # | Punto de refuerzo | Implementación |
|---|-------------------|----------------|
| 1 | Credenciales MySQL fuera del código | `${DB_USER}`/`${DB_PASSWORD}` en dev; docker-compose con variables obligatorias; plantilla `.env.example` |
| 2 | Secreto JWT sin fallback débil | `JwtUtil.resolveSecret()`: prod falla al arrancar si falta o es corto; dev genera secreto efímero SecureRandom |
| 3 | Admin por rol, no email | Eliminada auto-promoción en login; protección por `rol == "ADMIN"` en backend y frontend |
| 4 | Carrito con FK | Columna `id_catalogo` en carrito; precio/stock resueltos por FK (nombre solo como fallback legado); propiedad validada contra JWT |
| 5 | Pedidos normalizados | Nueva tabla `detalle_compra` (snapshot de precio/cantidad/artículo por línea); campo transitorio `detalles[]` en la API de pedidos; checkout rechaza carrito vacío (400) |
| 6 | Favoritos persistentes | Nueva tabla `favoritos` + `/api/favoritos` (GET/POST idempotente/DELETE); frontend sincroniza al iniciar sesión y migra los favoritos locales del invitado |
| 7 | Cobertura de tests | De 0 a **110 tests**: Auth 19, Admin 13, Producto 4, Carrito 9, Pedido 13, Favorito 9, Aliado 26, JwtUtil 7, PagoSimulado 10 |
| 8 | Documentación de rutas | AGENTS.md con las 30 rutas reales, auth requerida y códigos de error estándar |
| 9 | Robustez del frontend ante respuestas | `parseRespuesta()` tolera JSON/texto/HTML/vacío; mensajes claros para 404 vacío, 500 y red caída |
| 10 | Descuento de stock | Al confirmar: valida stock de todo el carrito antes de crear nada (rechazo completo con mensaje), luego descuenta y guarda; verificado en vivo (50→48) |
| 11 | **Pasarela de pago simulada offline** | `PagoSimuladoService` (algoritmo Luhn + fecha + CVV, sin almacenar datos) + `POST /api/pago/procesar` + checkout con tarjeta. En `crear()`: si `metodo_pago` es tarjeta valida antes de crear y devuelve `referencia_pago`. Frontend con bloque de datos de tarjeta (modo prueba, tarjeta `4111...`). Verificado en vivo: OK → `SIM-BF2181C6`, rechazo Luhn → 400, vencida → 400, efectivo sin tarjeta OK |
| 12 | **Recuperación de contraseña** | Columnas `reset_token`/`reset_token_expira` en `usuario` + `POST /api/auth/recuperar` (token UUID de 30 min, no revela existencia del correo) + `POST /api/auth/reset-password` (valida vigencia, invalida al usar). Frontend: 2 pantallas (recuperar + reset) con enlace de prueba en modo offline. Verificado en vivo: reset OK → login con nueva pass, reuso de token → 400, correo inexistente → respuesta idéntica |
| 13 | **Seguimiento de envío** | Columnas `numero_guia`/`transportadora`/`historial_envio` en `compras`. Cada cambio de estado (crear, enviar, entregar, cancelar) registra un evento `ESTADO@timestamp` en `historial_envio`. Admin captura guía/transportadora al marcar "enviado". Frontend: línea de tiempo en "Mis pedidos" y columna de seguimiento en el panel admin. Verificado en vivo: PENDIENTE→ENVIADO (guía GUIA-E2E-777, Servientrega)→ENTREGADO |
| 14 | **Cabeceras de seguridad (CSP)** | `SecurityHeaderFilter` refina el CSP: `img-src` permite http/https (productos con `imagen_url` externa), añade `frame-ancestors 'none'`, `base-uri 'self'`, `form-action 'self'` y el header `X-Permitted-Cross-Domain-Policies`. Cabeceras verificadas en vivo sobre `/` y `/api/productos` sin romper la app |
| 15 | **Responsive paneles admin/aliado** | Media queries en `lumura.css`: los grids inline de varias columnas de los paneles admin/aliado y de los formularios de registro se apilan a una columna en móvil; tablas con scroll horizontal táctil; modales a pantalla completa; encabezados compactos; KPIs apilados. Verificado: CSS servido (200) con los selectores aplicados sobre las pantallas `screen-admin-*`/`screen-aliado-*` |
| 16 | **Licencia de distribuidor (aliado)** | Botón "Licencia de distribuidor" en el sidebar del panel aliado → nueva pantalla `screen-aliado-licencia` con el anuncio "Sube tu licencia de distribuidor autorizado", selector de imagen, vista previa y guardado. Backend: columna `usuario.licencia_distribuidor` + `GET/PUT /api/aliado/licencia`; la imagen se sube reutilizando `POST /api/admin/upload` (acepta ALIADO) y la URL se persiste. Verificado E2E por UI (Puppeteer): login ALIADO → clic en botón → selección de imagen (preview) → guardar → "Licencia guardada correctamente" con imagen servida (200 image/png) |
| 17 | **Gestión de usuarios (admin) — ver perfil y eliminar** | `GET /api/admin/usuarios` enriquecido (añade `edad`, `nombre_negocio`, `nit`, `persona_contacto`, `categoria_productos`, `licencia_distribuidor`); `DELETE /api/admin/usuarios/{id}` elimina con 200 y protege al admin (400 "No se puede eliminar el usuario admin", 404 si no existe, 401 sin token admin). Frontend: corrección de bug — `showScreen` ahora aplica `s.style.display` por JS, por lo que el `style="display:none;"` inline que ocultaba `screen-admin-users` ya no impide mostrarla; botón **Ver** abre `modal-perfil-usuario` (datos personales + bloque de aliado con negocio/NIT/contacto/categoría/licencia; el admin sale "Protegido" sin botón de eliminar) y botón **Eliminar** con confirmación + recarga. Verificado E2E (Puppeteer): clic real en "Usuarios" → pantalla visible con 27 filas → modal del aliado con perfil y sin licencia → protección del admin oculta el botón. Sin errores de página |
| 18 | **Contacto del vendedor en el carrito** | `GET /api/carrito/{id}` resuelve el aliado (`catalogo.id_aliado`) de cada ítem y añade `vendedor_nombre`, `vendedor_correo`, `vendedor_telefono` y `vendedor_negocio`. Frontend: cada ítem del carrito muestra el bloque "Vendedor: {nombre} · {negocio}" con su correo y teléfono. Verificado E2E (Puppeteer): cliente añade producto del aliado → ítem del carrito con "Vendedor: aguacate - sandra rojas", correo `sandrarojasmoda@gmail.com` y teléfono `3182244198`; 1 test unitario nuevo en CarritoControllerTest (111 total) |
| 19 | **Bloqueo de usuarios (admin)** | `GET /api/admin/usuarios` ahora **excluye al admin autenticado** (filtra por id del token) y añade `bloqueado`, `motivo_bloqueo`, `bloqueo_hasta`. Nuevos endpoints `PUT /api/admin/usuarios/{id}/bloquear` (valida admin; 404 si no existe; 400 si es rol ADMIN, si falta `motivo` o si `dias` < 1; setea `bloqueado=true`, `motivo_bloqueo`, `bloqueo_hasta = now + dias`) y `PUT /api/admin/usuarios/{id}/desbloquear` (limpia los tres campos), ambos `@Transactional`. `AuthController.login` valida el bloqueo: si la cuenta está bloqueada y el bloqueo está vigente → 403 "Cuenta bloqueada. Motivo: ... Vuelve a intentarlo después del ..."; si el bloqueo venció → lo limpia y permite entrar. Frontend: columna **Estado** (badge verde "Activo" / naranja "Bloqueado hasta DD/MM/AAAA"), botones **Bloquear** (abre `modal-bloqueo-usuario` con motivo + días) / **Desbloquear** (con confirmación) / **Eliminar**; el modal perfil muestra el estado y permite bloquear/desbloquear. Verificado E2E: bloqueo por API y por UI con badge Bloqueado, login del bloqueado → 403 con motivo, desbloqueo → login 200, admin excluido de la lista, "no se puede bloquear al admin" → 400, `dias 0` → 400. 10 tests unitarios nuevos (AdminControllerTest + AuthControllerTest) → 122 total |
| 20 | **Detalles del vendedor en checkout, confirmación y pedidos** | Reportado: "al comprar el producto desde la sección cliente no salen los detalles del aliado que subió el producto". El carrito ya los mostraba (entrada 18), pero el **checkout** no listaba los ítems y la **confirmación** no tenía el desglose. Backend: `DetalleCompra` añade campo `@Transient Map<String,Object> vendedor` y `PedidoController.adjuntarDetalles` resuelve el aliado de cada detalle (`catalogo.id_aliado` → `UsuarioRepository`, filtra rol ALIADO) exponiendo `vendedor_nombre`, `vendedor_correo`, `vendedor_telefono`, `vendedor_negocio`. Frontend: nuevo bloque `#checkout-items` en `screen-checkout` renderizado por `renderCheckoutItems()` (hook de `showScreen`), bloque `#confirm-items` en `screen-confirm` poblado al confirmar, y `renderDetallesPedido` muestra el vendedor por ítem en "Mis pedidos". Verificado E2E (Puppeteer): cliente compra "zapatillas urbanas" → detalle del producto con "Vendido por: aguacate" (+NIT, contacto, teléfono, dirección) → carrito, `#checkout-items`, confirmación (`#LUM-39`) y pedidos muestran "Vendedor: aguacate - sandra rojas · aguacate" con correo `sandrarojasmoda@gmail.com` y teléfono `3182244198`; 0 errores JS; 1 test unitario nuevo en PedidoControllerTest. Además: assets versionados (`lumura.js?v=3`, `lumura.css?v=3`) para forzar recarga del frontend ante caché del navegador — verificado de nuevo el botón "Usuarios" del sidebar → 32 filas con columna Estado y botones |
| 21 | **Rate limit por IP y revocación de tokens JWT** | (1) `RateLimitFilter` **corregido**: el cálculo de ventana anterior guardaba el minuto como timestamp y comparaba milisegundos, por lo que la ventana se reiniciaba en cada petición y nunca llegaba a 429. Ahora usa ventana fija de 60 s por IP (login/registro 10 req/min, `/api/admin` 60, resto 120) con reloj inyectable y limpieza de entradas viejas; 5 tests unitarios (límite login en 11ª, límites independientes por ruta e IP, reinicio de ventana, límite admin). (2) **Revocación de tokens**: nuevo `JwtAuthFilter` central (config) que valida firma/expiración y el claim `tv` contra `usuario.token_version` (columna nueva, alias `token_version`); se incrementa la versión en `POST /api/auth/logout` (nuevo), en `reset-password` y en bloqueo/desbloqueo por el admin → el token viejo muere de inmediato (401 "Tu sesión expiró"), el frontend ya redirige a login en 401. `generateToken` ganó una sobrecarga con versión; los tokens usan el claim `tv`. Verificado E2E: login → token A sirve `/api/admin/usuarios` 200 → logout 200 → token A revocado 401 "Tu sesión expiró. Inicia sesión de nuevo" → re-login 200 → ráfaga de logins fallidos → 8× 429. Columnas `token_version` verificadas en MySQL. 12 tests nuevos (AuthControllerTest, AdminControllerTest, JwtUtilTest, RateLimitFilterTest) → **134 total** |
| 22 | **Rate limit por clase de ruta (fix "botones admin vacíos")** | Reportado: "ninguno de los botones del panel admin muestra el contenido". Causa: `RateLimitFilter` usaba **un solo contador por IP** compartido entre todas las URIs; una carga normal del SPA (assets estáticos + llamadas API) agotaba el bucket del login (10) o de `/api/admin`, y las respuestas 429 dejaban las pantallas con `display:block` pero sin datos (tablas/fetch vacíos). Fix: buckets **independientes por clase** (login/registro, `/api/admin`, resto) con tres mapas por IP, misma ventana fija de 60 s y limpieza por clase; test nuevo `traficoGeneral_noAgotaElBucketDeLogin` (30 assets + 1 login → 200). Verificado E2E (Puppeteer): login 200, las 6 pantallas admin (`admin-dash`, `admin-cat`, `admin-inv`, `admin-rep`, `admin-users`, `admin-orders`) renderizan sus datos; stress (2 cargas de página + 2 logins + 36 clics rápidos) → **0 respuestas 429/401** en APIs. 1 test nuevo → **135 total** |

## 9.4 Evidencia de Pruebas Integrales (21/08/2026)

- Suite unitaria: **135/135 verde** (`.\mvnw.cmd test`)
- E2E sobre servidor real (localhost:8080): registro/login, catálogo, carrito con FK,
  checkout antifraude (total server-side), desglose de pedido, cancelación con permisos,
  favoritos (agregar/duplicado/borrar/aislamiento entre usuarios), descuento y bloqueo
  de stock, acceso cruzado bloqueado con 403.
- **Licencia de distribuidor (E2E por UI, 28/08/2026)**: login ALIADO real → clic en el
  botón "Licencia de distribuidor" del sidebar → pantalla con anuncio "Sube tu licencia de
  distribuidor autorizado" → selección de imagen (vista previa `block`) → Guardar → mensaje
  "Licencia guardada correctamente" → "Licencia actual" visible con la imagen servida
  (HTTP 200, `image/png`). Backend verificado adicionalmente por API
  (`PUT/GET /api/aliado/licencia`).
- **Gestión de usuarios + caché del frontend (E2E, 29/08/2026)**: síntoma reportado — "al
  clicar Usuarios no aparecen los usuarios, roles ni eliminar". Causas dobles resueltas:
  (1) el navegador servía `index.html`/`lumura.js` viejos (sin `Cache-Control`); ahora los
  estáticos se sirven con `Cache-Control: no-cache, must-revalidate` y la UI se vuelve a
  cargar con Ctrl+F5; (2) tras reiniciar el servidor el secreto JWT efímero invalida la
  sesión — `api.request` detecta el 401 y redirige a login ("Tu sesión expiró") en lugar de
  dejar la tabla en "Cargando...". Verificado E2E: login correcto → dashboard → clic
  Usuarios → 29 filas con badge de rol y botones Ver/Eliminar; login con contraseña errónea
  sigue mostrando "Correo o contraseña incorrectos" sin efectos secundarios; token muerto →
  cierre de sesión y regreso a login. Sin errores de página.
- **Bloqueo de usuarios (E2E, 29/08/2026)**: flujo completo verificado por API y por UI
  (Puppeteer). Por API: registro de usuario de prueba → login admin → `GET
  /api/admin/usuarios` sin el admin (`admin@lumura.com` ausente de la lista, 0 coincidencias)
  → bloquear con motivo+días → 200 con `bloqueado=true` y `bloqueo_hasta`; login del
  bloqueado → **403** "Cuenta bloqueada. Motivo: ..." → desbloquear → 200 y login **200**;
  bloquear al admin real (rol ADMIN) → **400** "No se puede bloquear el usuario admin";
  `dias 0` → **400**. Por UI: login admin → Usuarios → columna Estado ("Activo"), botón
  Bloquear abre `modal-bloqueo-usuario` con nombre del usuario → motivo + días → badge
  "Bloqueado" + botón Desbloquear (confirm) → badge "Activo" + botón Bloquear. Usuario de
  prueba registrado y eliminado al final (BD queda sin residuales y sin ningún bloqueado).
- **Contacto del vendedor en el flujo de compra + Usuarios tras caché (E2E, 29/08/2026)**:
  dos reportes resueltos. (1) "La sección Usuarios sigue sin arrojar resultado": verificado
  con la app actual — clic real en el botón Usuarios del sidebar tras login admin → pantalla
  visible con **32 filas** (columna Estado, botones Ver/Bloquear/Desbloquear/Eliminar), API
  `GET /api/admin/usuarios` → 200; la causa del síntoma era que el navegador tenía
  `lumura.js`/`lumura.css` cacheados de una versión vieja → ahora `index.html` referencia
  los assets con query string versionado (`?v=3`) además de los headers `no-cache`
  existentes. (2) "Al comprar no salen los detalles del aliado": flujo completo del cliente
  verificado por UI (Puppeteer) — detalle del producto muestra "Vendido por: aguacate"
  (NIT, contacto sandra rojas, teléfono 3182244198, dirección); carrito con bloque
  "Vendedor"; checkout (`#checkout-items`) lista el ítem con vendedor; confirmación del
  pedido `#LUM-39` con ítem + vendedor; "Mis pedidos" muestra el vendedor por ítem; 0
  errores JS; usuario de prueba eliminado al final. Suite unitaria completa en verde.
- **Pasarela simulada (E2E, 27/08/2026)**: tarjeta válida `4111...` → pedido creado con
  `referencia_pago` (`SIM-BF2181C6`); tarjeta Luhn inválida → 400 "Pago rechazado";
  vencida → 400; endpoint `/api/pago/procesar` responde `{aprobado, referencia}`;
  `metodo_pago` distinto de tarjeta (efectivo) no exige tarjeta.
- **Recuperación de contraseña (E2E, 27/08/2026)**: `/api/auth/recuperar` genera token
  + enlace; `/api/auth/reset-password` permite login con la nueva contraseña; reuso del
  token → 400; correo inexistente → respuesta idéntica (no revela existencia); columnas
  `reset_token`/`reset_token_expira` verificadas en MySQL.
- **Seguimiento de envío (E2E, 27/08/2026)**: pedido nuevo inicia con `PENDIENTE@...`;
  admin marca "enviado" con guía `GUIA-E2E-777` y transportadora `Servientrega` →
  historial agrega `ENVIADO@`; el usuario ve estado/guía/transportadora; al marcar
  "entregado" se agrega `ENTREGADO@`; columnas `numero_guia`/`transportadora`/
  `historial_envio` verificadas en MySQL.
- **Cabeceras de seguridad (E2E, 27/08/2026)**: `Content-Security-Policy`,
  `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`,
  `Permissions-Policy`, `X-XSS-Protection: 1; mode=block` y
  `X-Permitted-Cross-Domain-Policies: none` presentes en `/` y `/api/*`; login y
  catálogo siguen funcionando con el CSP activo.
- Persistencia verificada directamente en MySQL (`detalle_compra`, `favoritos`).
- **Separación de interfaces por rol (E2E, 30/08/2026)**: admin y aliado tienen ahora
  **una sola pantalla de panel** cada uno (`#screen-admin` y `#screen-aliado`), con sidebar
  `.admin-sidebar` e idéntica agrupación funcional: admin → Dashboard/Catálogo/Inventario/
  Reportes/Usuarios/Pedidos (+ Configuración con aviso "Próximamente"), aliado → Panel/
  Añadir artículo/Stock/Descripción/Licencia de distribuidor. El contenido alterna mediante
  `mostrarMenuAdmin(panel)`/`mostrarMenuAliado(panel)` sobre `.admin-panel`/`.aliado-panel`
  manteniendo los mismos ids (KPIs, tablas, formularios) que ya usaban las funciones de
  render. Se eliminó la pantalla intermedia `choose-role` y el enlace "Admin" de la navbar
  de tienda; el acceso del aliado es un enlace **"Soy aliado — Acceder a mi panel"** dentro
  de `#screen-login` que lleva a `screen-aliado-login`. **Aislamiento 100% por rol**:
  `showScreen` aplica whitelist de pantallas por rol (ADMIN→solo `admin`, ALIADO→solo
  `aliado`, USER/invitado→tienda); con sesión de ADMIN/ALIADO el `.app-header` de la tienda
  se oculta (`body.rol-admin`/`body.rol-aliado`) y cualquier intento de abrir la tienda
  redirige al propio panel (hay que cerrar sesión para comprar). El avatar del header del
  panel agrupa Actualizar datos / Cerrar sesión / Eliminar cuenta (`toggleUserMenu`).
  Assets versionados a `?v=4`. Verificado E2E (Puppeteer): login admin → 6 botones del
  panel renderizan datos (dashboard KPIs $, catálogo, inventario, reportes, usuarios con
  filas, pedidos) + bloqueo de tienda + dropdown del avatar + cierre de sesión; login
  aliado real (registrado por API) → 5 botones del panel + bloqueo de tienda; login cliente
  → modal de políticas → home + bloqueo de paneles admin/aliado. Suite unitaria 135/135 en
verde.
- **Registro con elección de perfil Cliente/Aliado (E2E, 30/08/2026)**: el enlace "Regístrate"
  del login abre una nueva pantalla intermedia `register-choose` ("Crear cuenta — Elige el
  tipo de perfil") con dos tarjetas: **Crear perfil de Cliente** → formulario `register`, y
  **Crear perfil de Aliado** → formulario `register-aliado` (datos del negocio). Al registrar
  un **cliente** el frontend hace auto-login (`POST /api/auth/register` + `POST
  /api/auth/login` con las mismas credenciales; el endpoint de registro no emite token) y sigue
  el flujo estándar de cliente: `modal-politicas` → "Acepto las políticas" → **home (la
  tienda)** con sesión activa. El registro de aliado conserva su flujo (éxito → login). Los
  enlaces cruzados entre formularios ahora pasan por el selector de perfil ("Únete al Programa
  de Aliados" en `register` y "Volver" en `register-aliado` → `register-choose`). De paso se
  corrigió un doble `<script>` heredado en `index.html` (se cargaba `lumura.js?v=3` y quedaba
  un tag abierto); ahora hay `<script src="lumura.js?v=5"></script>` único. Verificado E2E
  (Puppeteer): chooser con ambas opciones, cliente→form→auto-login→políticas→tienda (token
  en localStorage), logout→chooser→aliado→form del negocio, y enlaces cruzados. Suite
  unitaria 135/135 en verde.
- **Ventana de membresías del aliado (E2E, 30/08/2026)**: tras **publicar un artículo** el
  panel del aliado muestra una pantalla completa `#aliado-panel-membresias` con las 3 tarjetas
  cuyos textos fueron aportados por el cliente: **Membresía Básico** ($10.000 COP/mes, 1er mes
  gratis, renovación automática mensual y un día de gracia adicional), **Membresía Medio**
  ($60.000 COP, vigencia 8 meses, dos meses gratis, renovación automática) y **Membresía
  Premium (Empresas)** ($90.000 COP, vigencia 12 meses, renovación automática + publicidad
  aleatoria en redes sociales y plataformas asociadas de Lumura). Al pulsar un plan →
  `#aliado-panel-pago` con resumen dinámico del plan (`#pago-plan-detalle`, datos del objeto
  JS `datosPlanes` vía `elegirPlanAliado(plan)`); **Confirmar pago** (`confirmarPagoAliado`)
  es un pago **simulado** (sin pasarela ni tarjeta, sin endpoint nuevo): mensaje de éxito y
  vuelta al Stock. Enlace "Más tarde — Volver al Stock" permite saltarla. Redirección
  implementada en el éxito de `publicarArticuloAliado` (`mostrarMenuAliado('membresias')` en
  vez de `'stock'`). Assets versionados a `?v=6` (CSS sin cambios, `?v=4`). Verificado E2E
  (Puppeteer): aliado real por API → login → añadir artículo → aceptar el aviso de compromiso
  → membresías con los 3 textos completos → Básico → confirmación de pago con resumen →
  Confirmar → Stock. Suite unitaria 135/135 en verde.
- **Aviso de Derecho de Retracto en membresías (E2E, 30/08/2026)**: al elegir un plan de
  membresía se abre el modal `#modal-aviso-retracto` con el texto verbatim del usuario basado
  en el **artículo 47 de la Ley 1480 de 2011** (Estatuto del Consumidor): derecho a retractarse
  dentro de los 5 días hábiles, reintegro total sin descuentos en máximo 30 días calendario,
  excepciones contempladas en la ley y la nota de que la información es orientativa y no
  reemplaza asesoría legal. `elegirPlanAliado(plan)` ahora rellena `#pago-plan-detalle` y abre
  el modal dejando el plan pendiente en `planAliadoPendiente`; **"Volver a los planes"**
  (`cancelarRetracto`) cierra el modal y regresa a las tarjetas, **"Acepto y continuar al
  pago"** (`aceptarRetracto`) cierra y muestra `#aliado-panel-pago`. Assets versionados a
  `?v=7` (CSS sin cambios). Verificado E2E (Puppeteer): elección → modal visible con el texto
  del artículo 47 y de la nota legal → "Volver a los planes" regresa a membresías → nueva
  elección → "Acepto y continuar" → confirmación de pago con resumen → Confirmar → Stock.
Suite unitaria 135/135 en verde.
- **Panel de categorías limpio en el home del cliente (E2E, 30/08/2026)**: se **eliminó el
  banner** del home (badge "OFERTA ESPECIAL" y botón "Ver colección →") y las **etiquetas fijas**
  del HTML del `#tag-filter` ("Camisetas, Pantalones, Chaquetas, Vestidos, Accesorios").
  `renderCategoriasTags()` ahora genera únicamente **"Todo" + las categorías normalizadas que
  existen en el catálogo** (`state.productos`; orden canónico `CATEGORIAS`; sin categorías
  vacías), por lo que el botón de una categoría aparece cuando un aliado publica un artículo de
  esa categoría (p. ej. "Blusas" si publica blusas). Los clics siguen con delegación de eventos
  y el filtro normalizado de `normalizarCategoria()`. El filtro del admin (`#admin-cat-filter`)
  y los `<select>` de formularios conservan la lista canónica completa. Verificado E2E
  (Puppeteer): sin "OFERTA ESPECIAL"/"Ver colección"; panel = Todo + exactamente las categorías
  del catálogo (todas presentes, ninguna ajena, sin etiquetas fijas sin productos); aliado publica
  camiseta con "Camisetas" → cliente ve el botón y al hacer clic ve la camiseta nueva + la legacy
  en minúscula; búsqueda "camiset" la encuentra; catálogo admin filtrado sigue mostrando la
  prenda. Assets versionados a `?v=9` (CSS sin cambios). Suite unitaria 135/135 en verde.
- **Banner restaurado y búsqueda del cliente eliminada (E2E, 31/08/2026)**: se restauró el
  **banner `.hero-banner`** en el home con la imagen **`closet.gif` de fondo** (el CSS
  `.hero-banner` de `lumura.css` ya apuntaba a `images/closet.gif`; se reintrodujo el contenedor
  en `#screen-home` con el titular "Hasta 40% OFF / en Temporada" y su subtítulo, **sin** el badge
  "OFERTA ESPECIAL" ni el botón "Ver colección →" que antes se retiraron) y se **eliminó la barra
  de búsqueda del cliente** (`#search-input` y su `div` en `index.html`, más el código muerto
  `mostrarBusqueda()`/`searchTimeout`/`filtrarBusqueda()` en `lumura.js`; `filtrarCategoria`
  ahora llama `renderProductos(cat, '')`). El buscador del **admin** (`#admin-search-input`) se
  conserva. El único filtro del home es el de categorías. Verificado E2E (Puppeteer): sin
  `#search-input`; `.hero-banner` visible con `background-image` `closet.gif`; siguen ausentes
  "OFERTA ESPECIAL" y "Ver colección"; filtro por "Camisetas" sigue listando la camiseta nueva +
  legacy; catálogo admin OK. Assets versionados a `?v=10` (CSS sin cambios). Suite unitaria
  135/135 en verde.
- **Anuncio de membresías del aliado solo una vez (E2E, 31/08/2026)**: el panel
  `#aliado-panel-membresias` ya no se muestra en **cada** publicación. `confirmarPagoAliado`
  marca la elección como persistente con `marcarMembAliadoElegida()` en `localStorage`
  (llave `lumura_membresia_elegida_<id_usuario>` según `state.user.id`) y
  `publicarArticuloAliado` decide con `membAliadoElegida()`: si el aliado ya confirmó el pago de
  una membresía va **directo a Stock**, si no, muestra el anuncio. El enlace "Más tarde — Volver
  al Stock" (sin escoger plan) no marca la elección, por lo que el anuncio vuelve a salir en la
  siguiente publicación. Sin cambios de backend ni de HTML (solo `lumura.js`; los paneles se
  conservan). Verificado E2E (Puppeteer): flag persistido en localStorage (1 llave); 2º artículo
  publicado → anuncio ausente y aterrizaje directo en Stock; **tras recargar la página** la sesión
  se restaura (`state.token` → `irInicioPorRol()`) y un 3er artículo también va directo a Stock
  sin el anuncio (persistencia del flag). Assets versionados a `?v=11` (CSS sin cambios). Suite
  unitaria 135/135 en verde.
- **Inventario del admin con botón de borrar artículo (E2E, 31/08/2026)**: el panel
  `#admin-panel-inv` (`cargarInventario` en `lumura.js`) mostraba en la columna "Acción" solo el
  botón de stock ("Urgente/Reabastecer/Ajustar", todos con `proximamente`). Ahora cada fila
  incluye además un botón de **borrar** (papelera, `images/trash.svg`) que reusa la función
  existente `eliminarProducto(id)` del admin (igual que en Catálogo): confirma con `confirm`,
  llama `DELETE /api/admin/productos/:id` y `cargarProductos()` re-renderiza el inventario si el
  panel está activo. Sin cambios de backend ni de estructura del panel. Verificado E2E
  (Puppeteer): filas del inventario con `onclick="eliminarProducto(...)`; el borrado (aceptando el
  `confirm`) elimina la prenda demo del inventario. Assets versionados a `?v=12` (CSS sin
  cambios). Suite unitaria 135/135 en verde.
- **Categorías unificadas y filtrables por rol (E2E, 30/08/2026)**: se unificó el sistema de
  categorías en **una lista canónica `CATEGORIAS`** de 23 valores (Camisetas, Camisas, Blusas,
  Pantalones, Jeans, Faldas, Vestidos, Chaquetas, Abrigos, Sueter, Chalecos, Trajes, Ropa
  interior, Calcetines, Zapatos, Sandalias, Botas, Accesorios, Sombreros, Cinturones, Bufandas,
  Tennis, Ropa deportiva) definida en `lumura.js`. Todos los formularios que antes eran **texto
  libre** pasaron a **`<select>`** rellenados por `cargarOpcionesCategorias()`: artículo del
  aliado `#aliado-art-categoria` (**obligatorio**), modal de producto del admin
  `#modal-prod-categoria`, modal de edición del aliado `#edit-art-categoria` (renderizado en JS),
  registro del aliado `#aliado-categoria` y filtro del catálogo admin `#admin-cat-filter`. En la
  tienda del cliente los botones de categorías (`#tag-filter`, hoy con id propio) se **generan
  dinámicamente** con `renderCategoriasTags()` desde `CATEGORIAS` + las categorías distintas
  presentes en los productos, y sus clics usan **delegación de eventos** (antes se bindeaban una
  sola vez y morían al regenerarse). `normalizarCategoria()` (trim + minúsculas + tabla
  `SINONIMOS` para legacy: `camiseta→Camisetas`, `camisas y blusas→Camisas`, `jeans y
  pantalones→Jeans`, `calzado→Zapatos`, `tenis/zapatillas/sneakers→Tennis`, etc.) normaliza las
  comparaciones de `renderProductos` y `renderAdminCatalogo`, el valor pre-seleccionado del modal
  admin y el select de edición — por eso los productos viejos en minúscula (`camiseta`,
  `chaquetas`…) aparecen bajo sus botones **sin migrar la BD**. La búsqueda del cliente sigue
  combinando término + categoría activa. Verificado E2E (Puppeteer): aliado real publica una
  camiseta eligiendo "Camisetas" en el select → cliente logueado → el botón "Camisetas" existe en
  Categorías → clic → aparecen la camiseta nueva **y** la legacy en minúscula del seed → no se
  filtran otras categorías → búsqueda "camiset" las encuentra → el filtro del catálogo admin por
  "Camisetas" muestra la camiseta. Assets versionados a `?v=8` (CSS sin cambios). Suite unitaria
  135/135 en verde.

Pendiente recomendado para despliegue público: M2 (rate limiter por clave/parcialidad——ya existe por IP en la entrada 21) y la limpieza cosmética del HTML del panel admin. La pasarela simulada
está lista para migrarse a Stripe/PayU (mismos contratos de `metodo_pago`/
`referencia_pago`), la recuperación de contraseña requiere conectar un servidor SMTP
real (hoy el enlace se muestra en la respuesta/log) y los pedidos ya tienen seguimiento
de envío con historial de estados. Cabeceras de seguridad aplicadas vía
`SecurityHeaderFilter` (CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy,
Permissions-Policy, X-XSS-Protection).

---

*Fin del documento — LUMURA Documentación Técnica v2.0 (Actualizado 30/08/2026)*
