# San Francisco Archive · Sistema de Gestión de Anomalías

> TFG · Información, localización y resolución de incidentes (normales y sobrenaturales) en San Francisco, California.

El Archivo es una organización privada dirigida desde San Francisco por **James Pratt** (62 años, 190 de experiencia sobrenatural) y **Sarah Summers** (53 años, 35 de experiencia), matrimonio, con **Nina** como administradora del sistema. Gestionan una red de **10 Potenciales** que resuelven incidentes que para el público son noticias normales: **secuestros, asesinatos y desapariciones**. Detrás de cada uno hay una anomalía que solo se resuelve con un **Contrato**.

El proyecto activo es una aplicación web completa: toda la gestión del archivo se hace desde la web, con login de administración incluido. La carpeta `archive-app/` se conserva en el repositorio como legado histórico, pero no es la base del proyecto activo.

| Capa | Qué es | Quién la usa | Login |
| --- | --- | --- | --- |
| **Pública** · `archive-web/` | Web informativa: feed en vivo, mapa oscuro, noticias y formulario anónimo «Pedir ayuda» | Ciudadanos | **Nunca** |
| **Gestión web** · `archive-web/public/admin-login.html` | Portal de administración dentro de la web, con login real y dashboard de archivo | 3 administradores + potenciales activos | **Sí** |
| **Legacy** · `archive-app/` | Código antiguo de escritorio JavaFX, mantenido solo como referencia histórica | desarrolladores | opcional |

```
                  escribe / valida                       lee (vistas públicas)
 ┌─────────────────────────────┐        ┌─────────┐        ┌────────────────────┐        ┌──────────────┐
 │  Gestión web (admin login)  │ ─────▶ │  MySQL  │ ◀───── │ API Javalin (8080) │ ◀───── │ Web pública  │
 │  /admin-login.html + panel  │  JDBC  │sf_archive│  JDBC │  solo lectura +    │  fetch │ (sin login)  │
 │  de administración          │        └─────────┘        │  avisos anónimos   │  30 s  └──────────────┘
 └─────────────────────────────┘                               └────────────────────┘
```

Todo lo que se publica desde la web de gestión (incidentes, noticias, zonas seguras, cierres de casos) aparece en la web pública en el siguiente refresco automático (30 s). Los avisos que envían los ciudadanos desde la web llegan a la gestión y a la base de datos en «Avisos ciudadanos».

> 📘 **¿Empiezas con el proyecto?** Lee el [TUTORIAL.md](TUTORIAL.md): explica cómo arrancar MySQL, la API y la web.

## Estructura del repositorio

```
database/                  Base de datos MySQL
  01_schema.sql            13 tablas, 2 vistas públicas, triggers y usuarios MySQL
  02_datos.sql             Datos de San Francisco: 3 admins, 10 potenciales, grupos, incidentes,
                           contratos, monederos, informes, noticias, zonas seguras…
archive-app/               LEGACY (se mantiene en Git, pero no es la aplicación activa)
  core/                    Modelo, DAO JDBC y servicios del backend original
  desktop-admin/           Versión antigua de escritorio JavaFX (no activa en el flujo principal)
  api-web/                 Backend Javalin activo de la web y sus operaciones admin protegidas
archive-web/               APLICACIÓN ACTIVA (web + gestión web)
  public/                  HTML + CSS + JS nativo + Leaflet (incluido), sin compilación
  public/admin-login.html  Login de administración desde la web
  public/admin.html        Panel web para la gestión administrativa
```

## Puesta en marcha (local)

Requisitos: **Java 21**, **Maven 3.9+**, **MySQL Server 8** (o MariaDB 10.6+) y MySQL Workbench para administrar la base gráficamente.

### 1. Base de datos

```bash
mysql -u root -p < database/01_schema.sql
mysql -u root -p < database/02_datos.sql
```

**En este PC el servidor ya está instalado**: existe el servicio `MySQL80`, pero está detenido. No instales otro servidor encima. Abre `services.msc`, busca `MySQL80` y pulsa **Iniciar** (o abre PowerShell como administrador y ejecuta `Start-Service MySQL80`). Instala [MySQL Workbench](https://dev.mysql.com/downloads/workbench/) si no lo tienes; sirve para conectarte a `localhost:3306` y ejecutar los SQL. Si el instalador de la captura es lo único que descargaste, el paquete completo se llama `mysql-installer-community`; el paquete `mysql-installer-web-community` es solo un instalador pequeño que descarga componentes durante la instalación.

Para una base nueva, en Workbench ejecuta `database/01_schema.sql` y después `database/02_datos.sql`. Si `sf_archive` ya existe, no ejecutes `01_schema.sql`, porque elimina y crea de nuevo la base. La API de gestión usa `sfa_admin`; configura esa cuenta como secreto del servidor, nunca en JavaScript. `sfa_web` queda disponible para una API exclusivamente pública.

Se crean dos usuarios MySQL. La API de gestión usa `sfa_admin` en el servidor; `sfa_web` solo tiene los permisos necesarios para servir información pública y guardar avisos anónimos.

| Usuario MySQL | Para | Permisos |
| --- | --- | --- |
| `sfa_admin` | API web de gestión y backend legacy | SELECT/INSERT/UPDATE/DELETE en `sf_archive` |
| `sfa_web` | API pública sin gestión | Vistas públicas y envío de avisos |

Las rutas públicas de la web solo devuelven las vistas públicas; las rutas de gestión requieren sesión ADMIN. La interfaz HTML/JS nunca se conecta directamente a MySQL.

### 2. Compilar

```bash
cd archive-app
mvn clean package
```

### 3. Arrancar la API + la web

La API sirve también la web en el mismo origen; la URL de `/api` se resuelve automáticamente y no requiere configurar un dominio en `config.js`:

```bash
cd archive-app
mvn -pl api-web -am package
java -jar api-web/target/sfa-api.jar
```

Deja esta terminal abierta: la API y la web están sirviéndose juntas. Abre **http://localhost:8080** y comprueba **http://localhost:8080/api/salud**; debe decir `"baseDatos":"CONECTADA"`.

La aplicación activa es web y no usa Docker ni instaladores `.exe`; la carpeta `archive-app/` conserva el backend Java necesario para MySQL y el código JavaFX legado, pero JavaFX no se usa como interfaz activa.

### 4. Abrir el acceso de administración web

Con la API funcionando, abre el login:

```text
http://localhost:8080/admin-login.html
```

El panel web actual incluye login, sesión, cierre de sesión y estadísticas del archivo. Las funciones de gestión avanzada que tenía JavaFX todavía no están migradas a pantallas web.

## Cuentas de acceso

Los ciudadanos **no tienen cuenta**. Las cuentas activas se cargan desde `database/02_datos.sql`; el login acepta usuario o correo. No se publican contraseñas en la interfaz web ni en este README. Las contraseñas se guardan como hashes **PBKDF2-HMAC-SHA256** (65 536 iteraciones y sal aleatoria).

## Qué hace cada parte

### Web pública (`archive-web/`) — solo consulta, sin login
- **Inicio**: feed mundial de incidentes publicados (búsqueda y filtro por tipo) + mapa de San Francisco (Leaflet + OpenStreetMap) con pins por tipo y **zonas seguras en verde**. Auto-refresco cada 30 s.
- **Expediente** de cada incidente con mini-mapa y noticias relacionadas (la anomalía aparece tachada).
- **Noticias** con filtro por categoría y página de detalle.
- **Pedir ayuda**: formulario **anónimo** (tipo, descripción, barrio, punto en el mapa, contacto opcional) → devuelve un **código de seguimiento** (`SF-XXXXXX`) para consultar el estado. Anti-spam: campo trampa y límite de 5 avisos/10 min por IP.
- **El archivo**: quiénes son, cómo trabajan y preguntas frecuentes.
- Si la API no responde, la web muestra una **copia de demostración** (`data/demo.json`) para que siga siendo navegable en un hosting estático.

### Gestión web (`archive-web/public/admin.html`)

La gestión web permite a las cuentas ADMIN trabajar desde el navegador: resumen, incidentes y clasificación, avisos ciudadanos, ciclo de contratos y asignaciones, potenciales, grupos tácticos, noticias, zonas seguras, monederos, Archivo Restringido, usuarios y registro de actividad. `archive-app/desktop-admin` se conserva en Git como legado, pero no hace falta iniciar JavaFX. Java/Javalin sigue siendo el backend que aplica reglas y conecta con MySQL.

### Ciclo de un caso

```
Ciudadano (web) ──aviso anónimo──▶ Aviso PENDIENTE ──admin convierte──▶ Incidente + Contrato SOLICITADO
   ──ASIGNAR (grupo + potencial, transporte automático)──▶ ASIGNADO ──potencial──▶ EN CURSO ──▶ PENDIENTE REVISIÓN
   ──admin completa (paga recompensa)──▶ COMPLETADO · incidente RESUELTO (web: CERRADO) ──▶ Informe en el Archivo Restringido
```

## API pública

| Método | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/salud` | Estado del servicio y de la base de datos |
| GET | `/api/incidentes?tipo=&q=&barrio=` | Feed y mapa (también `/api/incidentes/publicos`) |
| GET | `/api/incidentes/{id}` | Expediente + noticias relacionadas |
| GET | `/api/noticias?categoria=&limite=` · `/api/noticias/{slug}` | Noticias publicadas |
| GET | `/api/zonas-seguras` | Pins verdes |
| GET | `/api/estadisticas` | Contadores por tipo, estado y barrio |
| POST | `/api/admin/login` | Iniciar sesión en gestión web |
| GET | `/api/admin/session` | Consultar sesión activa |
| POST | `/api/admin/logout` | Cerrar sesión |
| GET/POST | `/api/admin/incidentes` | Listar, crear y editar expedientes (requiere ADMIN) |
| GET/POST | `/api/admin/solicitudes...` | Revisar, descartar o convertir avisos ciudadanos (requiere ADMIN) |
| GET/POST | `/api/admin/contratos...` | Crear, asignar y cerrar contratos (requiere ADMIN) |
| GET/POST | `/api/admin/potenciales...` · `/api/admin/grupos...` | Red de potenciales y grupos tácticos (requiere ADMIN) |
| GET/POST | `/api/admin/noticias...` · `/api/admin/zonas-seguras` | Publicación web (requiere ADMIN) |
| GET/POST | `/api/admin/monederos...` · `/api/admin/informes...` | Monederos y Archivo Restringido (requiere ADMIN) |
| GET/POST | `/api/admin/usuarios...` · `/api/admin/actividad` | Cuentas, restablecimiento de contraseñas y auditoría (requiere ADMIN) |
| POST | `/api/ayuda` | Aviso anónimo → `{ "codigo": "SF-XXXXXX" }` |
| GET | `/api/ayuda/{codigo}` | Estado del aviso |

## Pruebas

```bash
cd archive-app
mvn test                              # pruebas unitarias (hash, transporte, slugs…)
mvn test -Dsfa.it=true -Dtest=FlujoContratoIT,CoreTest -Dsurefire.failIfNoSpecifiedTests=false
# ↑ integración contra MySQL: login, asignar→completar, aviso→contrato, máx. 6 por grupo.
#   Modifica los datos: vuelve a cargar database/*.sql después.
```

## Despliegue web

- **Base de datos** → Railway / cualquier MySQL 8: ejecuta los dos scripts.
- **API + web con gestión** → despliega la API Javalin y sirve los archivos estáticos de `archive-web/public` en el mismo dominio. Configura `SFA_DB_URL`, `SFA_DB_USER=sfa_admin`, `SFA_DB_PASSWORD` y `PORT` como secretos del servidor. No incluyas esos valores en el frontend.
- **Web estática separada (solo portal público)** → Cloudflare Pages / Netlify publicando `archive-web/public`; define `SFA_API_BASE` con la URL HTTPS completa de la API antes de cargar los módulos. Para usar administración, web y API deben compartir origen HTTPS para la cookie de sesión.
- **Legacy** → la carpeta `archive-app/` queda guardada en Git como referencia histórica, pero no es el producto principal ni se empaqueta ni se despliega en este flujo activo.

## Tecnologías

Java 21 · Javalin 6 · JDBC + HikariCP · MySQL 8 · HTML/CSS/JavaScript nativo · Leaflet 1.9 + OpenStreetMap · Maven. JavaFX permanece solo en el código legacy.

---
*Proyecto de ficción para un TFG. Personas, entidades e incidentes son inventados; los lugares de San Francisco son reales.*
