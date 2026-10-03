# San Francisco Archive · Sistema de Gestión de Anomalías

> TFG · Información, localización y resolución de incidentes (normales y sobrenaturales) en San Francisco, California.

El Archivo es una organización privada dirigida desde San Francisco por **James Pratt** (62 años, 190 de experiencia sobrenatural) y **Sarah Summers** (53 años, 35 de experiencia), matrimonio, con **Nina** como administradora del sistema. Gestionan una red de **10 Potenciales** que resuelven incidentes que para el público son noticias normales: **secuestros, asesinatos y desapariciones**. Detrás de cada uno hay una anomalía que solo se resuelve con un **Contrato**.

El sistema está dividido en **dos capas**:

| Capa | Qué es | Quién la usa | Login |
| --- | --- | --- | --- |
| **Pública** · `archive-web/` | Web informativa: feed en vivo, mapa oscuro, noticias y formulario anónimo «Pedir ayuda» | Ciudadanos | **Nunca** |
| **Privada** · `archive-app/` | Aplicación de escritorio **JavaFX** que gestiona todo y actualiza la web | 3 administradores + 10 potenciales | Sí |

```
                  escribe                       lee (vistas públicas)
 ┌──────────────────────────┐        ┌─────────┐        ┌────────────────────┐        ┌──────────────┐
 │  App de gestión JavaFX   │ ─────▶ │  MySQL  │ ◀───── │ API Javalin (8080) │ ◀───── │ Web pública  │
 │  (admins y potenciales)  │  JDBC  │sf_archive│  JDBC │  solo lectura +    │  fetch │ (sin login)  │
 └──────────────────────────┘        └─────────┘        │  avisos anónimos   │  30 s  └──────────────┘
                                                        └────────────────────┘
```

Todo lo que se publica desde la app (incidentes, noticias, zonas seguras, cierres de casos) aparece en la web en el siguiente refresco automático (30 s). Los avisos que envían los ciudadanos desde la web llegan a la app en «Avisos ciudadanos».

## Estructura del repositorio

```
database/                  Base de datos MySQL
  01_schema.sql            13 tablas, 2 vistas públicas, triggers (máx. 6 por grupo) y usuarios MySQL
  02_datos.sql             Datos de San Francisco: 3 admins, 10 potenciales, 4 grupos, 20 incidentes,
                           16 contratos, monederos, informes, noticias, zonas seguras…
archive-app/               APLICACIÓN DE GESTIÓN (carpeta aparte de la web)
  core/                    Modelo, DAO JDBC, servicios (contratos, monederos, grupos…), seguridad PBKDF2
  desktop-admin/           Aplicación de escritorio JavaFX (lo que usan James, Sarah, Nina y los potenciales)
  api-web/                 API pública Javalin que alimenta la web (solo lectura + avisos anónimos)
  empaquetar-exe.bat       Genera el instalador .exe para Windows (jpackage)
archive-web/               WEB PÚBLICA (solo informativa)
  public/                  HTML + CSS + JS nativo + Leaflet (incluido), sin compilación
docker-compose.yml         MySQL 8 con los scripts cargados automáticamente (opcional)
```

## Puesta en marcha (local)

Requisitos: **Java 21**, **Maven 3.9+**, **MySQL 8** (o MariaDB 10.6+, XAMPP vale) y un navegador.

### 1. Base de datos

```bash
mysql -u root -p < database/01_schema.sql
mysql -u root -p < database/02_datos.sql
```

O con Docker: `docker compose up -d` (carga ambos scripts automáticamente).

Se crean dos usuarios MySQL con permisos mínimos:

| Usuario MySQL | Contraseña | Para | Permisos |
| --- | --- | --- | --- |
| `sfa_admin` | `sfa_admin_2026` | App de escritorio | SELECT/INSERT/UPDATE/DELETE en `sf_archive` |
| `sfa_web` | `sfa_web_2026` | API de la web | Solo las vistas públicas, zonas seguras y crear avisos |

> La web **no puede** leer la anomalía clasificada ni el Archivo Restringido: la API solo tiene acceso a las vistas `v_incidentes_publicos` y `v_noticias_publicas`.

### 2. Compilar

```bash
cd archive-app
mvn clean package
```

### 3. Arrancar la API + la web

```bash
cd archive-app
java -jar api-web/target/sfa-api.jar
```

Abre **http://localhost:8080** → la API sirve también la web (`archive-web/public`).
Si prefieres servir la web aparte: `python -m http.server 5500 --directory archive-web/public` y abre http://localhost:5500 (detecta la API en el puerto 8080).

### 4. Arrancar la aplicación de gestión

```bash
cd archive-app
mvn -pl desktop-admin javafx:run
# o bien:  java -jar desktop-admin/target/sfa-gestion.jar
```

Para cambiar la conexión sin recompilar copia `archive-app/archive.properties.example` como `archive.properties`.

## Cuentas de acceso a la app

Los ciudadanos **no tienen cuenta**. Solo existen estas 13:

| Usuario | Nombre | Rol | Contraseña inicial |
| --- | --- | --- | --- |
| `james` | James Whitaker | Administrador (Director) | `Archivo1906!` |
| `sarah` | Sarah Whitaker | Administradora (Operaciones) | `Archivo1906!` |
| `nina` | Nina | Administradora (Sistema y archivista) | `Archivo1906!` |
| `niebla` | Elena Vargas · Tránsito por la niebla | Potencial (Unidad Mission) | `Potencial2026!` |
| `faro` | Marcus Lee · Lectura de rastros | Potencial (Unidad Chinatown) | `Potencial2026!` |
| `cable` | Danny O'Connor · Tecnopatía eléctrica | Potencial (Unidad Chinatown) | `Potencial2026!` |
| `marea` | Isabel Reyes · Hidroquinesis | Potencial (Unidad Golden Gate) | `Potencial2026!` |
| `eco` | Theo Brooks · Psicometría auditiva | Potencial (Unidad Golden Gate) | `Potencial2026!` |
| `ceniza` | Grace Kim · Pirocinesis | Potencial (Unidad Golden Gate) | `Potencial2026!` |
| `sombra` | Luis Navarro · Ocultación en sombras | Potencial (Unidad Chinatown) | `Potencial2026!` |
| `brujula` | Amara Okafor · Localización de personas | Potencial (Unidad Bahía Este) | `Potencial2026!` |
| `ancla` | Samuel Park · Sellado de entidades | Potencial (Unidad Bahía Este) | `Potencial2026!` |
| `roca` | Rosa Delgado · Inmunidad a la posesión | Potencial (sin grupo) | `Potencial2026!` |

Cada usuario puede cambiar su contraseña en **Mi perfil**; los administradores pueden restablecerlas en **Usuarios y actividad**. Las contraseñas se guardan con **PBKDF2-HMAC-SHA256** (65 536 iteraciones y sal aleatoria).

## Qué hace cada parte

### Web pública (`archive-web/`) — solo consulta, sin login
- **Inicio**: feed mundial de incidentes publicados (búsqueda y filtro por tipo) + **mapa oscuro** de San Francisco (Leaflet + CARTO Dark Matter) con pins por tipo y **zonas seguras en verde**. Auto-refresco cada 30 s.
- **Expediente** de cada incidente con mini-mapa y noticias relacionadas (la anomalía aparece tachada).
- **Noticias** con filtro por categoría y página de detalle.
- **Pedir ayuda**: formulario **anónimo** (tipo, descripción, barrio, punto en el mapa, contacto opcional) → devuelve un **código de seguimiento** (`SF-XXXXXX`) para consultar el estado. Anti-spam: campo trampa y límite de 5 avisos/10 min por IP.
- **El archivo**: quiénes son, cómo trabajan y preguntas frecuentes.
- Si la API no responde, la web muestra una **copia de demostración** (`data/demo.json`) para que siga siendo navegable en un hosting estático.

### Aplicación de gestión (`archive-app/desktop-admin`) — JavaFX

**Administradores (James, Sarah, Nina):**
| Pantalla | Funciones |
| --- | --- |
| Dashboard mundial | KPIs, tabla completa de incidentes (verificados / no verificados) y **mapa en tiempo real** con incidentes, potenciales y bases de grupos |
| Incidentes | Alta/edición, verificación, nivel de amenaza, anomalía clasificada y botón **Publicado en la web** |
| Avisos ciudadanos | Avisos anónimos de la web → convertir en incidente + contrato, poner en revisión o descartar (el ciudadano ve la respuesta con su código) |
| Contratos | Lista de **SOLICITADOS**, selector de **grupo táctico + potencial disponible** y botón **ASIGNAR**, que genera el **gasto de transporte automático** (25 USD + 1,80 USD/km por distancia Haversine). Completar (paga recompensa y cierra el caso en la web), fallido o cancelar (reembolsa transporte) |
| Potenciales | Fichas, reclutamiento con creación de cuenta, vínculos familiares |
| Grupos tácticos | Crear/editar por país y zona, **arrastrar miembros** entre listas, **máximo 6** (validado en app y con trigger MySQL) |
| Monederos | Saldo de cada potencial, historial de movimientos y ajustes (ajuste/bonus/penalización) |
| Archivo Restringido | Redactar y guardar el **Informe Final Clasificado** de cada contrato cerrado |
| Noticias / Zonas seguras | Contenido de la web pública (publicar, destacar, retirar) |
| Usuarios y actividad | Activar/desactivar cuentas, restablecer contraseñas y registro de auditoría |

**Potenciales:** *Mis misiones* (iniciar y reportar finalización con informe de campo → queda pendiente de revisión), *Mi monedero*, *Mis vínculos* y *Reportar incidente* (llega como NO VERIFICADO y sin publicar).

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

## Generar el .exe de la aplicación

No hace falta Electron: JavaFX se empaqueta con **jpackage** (incluido en el JDK 21).

```bat
cd archive-app
empaquetar-exe.bat      :: instalador "SF Archive" con acceso directo (requiere WiX Toolset)
```

Sin WiX, cambia `--type exe` por `--type app-image` en el script y obtendrás una carpeta con `SF Archive.exe` lista para usar. En Linux/macOS: `./empaquetar.sh`.

## Despliegue

- **Base de datos** → Railway / cualquier MySQL 8: ejecuta los dos scripts.
- **API** → Railway/Render: `java -jar sfa-api.jar` con `SFA_DB_URL`, `SFA_DB_USER=sfa_web`, `SFA_DB_PASSWORD` (el puerto se lee de `PORT`).
- **Web** → Cloudflare Pages / Netlify publicando `archive-web/public` y poniendo la URL de la API en `API_PRODUCCION` de `public/js/config.js`.
- **App de escritorio** → apunta a la misma base de datos con `archive.properties`.

## Tecnologías

Java 21 · JavaFX 21 (WebView + Leaflet) · Javalin 6 · JDBC + HikariCP · MySQL 8 · HTML/CSS/JS nativo · Leaflet 1.9 + CARTO Dark Matter · Maven.

---
*Proyecto de ficción para un TFG. Personas, entidades e incidentes son inventados; los lugares de San Francisco son reales.*
