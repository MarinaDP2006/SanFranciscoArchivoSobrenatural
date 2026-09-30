# Importante: ELECTRON PARA PASAR DE COD A .EXE EN APLICACION

# APP — San Francisco Archive
Aplicación principal del proyecto San Francisco Archive, compuesta por un backend Java y una interfaz administrativa desktop para gestionar incidentes, contratos, potenciales y reportes sobrenaturales.

La app centraliza la operación del sistema en tres líneas principales:

- gestión de incidentes públicos y verificados,
- administración de contratos y recompensas,
- control de potenciales, grupos tácticos y estado operativo.

## Estructura del módulo
- `core/`: API REST, modelos del dominio, DAO y configuración de acceso a base de datos.
- `desktop-admin/`: cliente administrativo con JavaFX para consultar y operar sobre la API.
- `db/`: scripts SQL para crear la base de datos y sembrar datos iniciales.
- `android-potencial/`: esquema de cliente o extensión asociada al proyecto, pendiente de integración formal.

## Tecnologías
- Java 21
- Maven
- Javalin 6
- JavaFX 21
- MySQL Connector/J
- Gson

## Configuración inicial
1. Instala Java 21 y Maven.
2. Crea la base de datos MySQL.
3. Ejecuta el esquema de `db/schema.sql`.
4. Ajusta la conexión en `core/src/main/resources/application.properties`.

## Compilación
Desde la carpeta `archive-app`:

```bash
mvn clean package
```

## Ejecución del backend
La API se configura con Javalin y expone rutas bajo `/api`:

- `/api/salud` — comprobación del servicio
- `/auth/login` y `/auth/logout` — autenticación
- `/incidentes/publicos` — listado de incidentes
- `/contratos` — gestión de contratos
- `/potenciales` — consulta y administración de potenciales

## Ejecución del dashboard administrativo

El módulo desktop se ejecuta con JavaFX:

```bash
mvn -pl desktop-admin javafx:run
```

## Base de datos
El esquema incluye tablas para:

- administradores,
- reporteros,
- grupos tácticos,
- potenciales,
- incidentes públicos,
- contratos de anomalía,
- transacciones de monedero,
- vínculos familiares,
- informes clasificados.

## Estado del proyecto
La base funcional del sistema ya está organizada en módulos y la API principal está conectada a la capa de datos. La aplicación sigue en desarrollo para cerrar la integración completa entre backend, desktop y frontend web.

## Siguientes pasos
- terminar la integración de autenticación y permisos,
- revisar la API de contratos y potenciales con datos reales,
- consolidar la UI desktop con el flujo operativo final,
- validar el frontend web contra la API en entorno real.
  
# Parte Web
Portal web público para consultar incidentes documentados en un feed y un mapa interactivo. Incluye también el acceso y el panel de contratos para usuarios reporter.

## Requisitos
- Python 3 para servir los archivos estáticos durante el desarrollo.
- La API de San Francisco Archive disponible en `http://localhost:8080/api` para cargar incidentes y usar el acceso reporter.
- Conexión a Internet para descargar Leaflet y los mapas de CARTO.

No se necesita Node.js ni un paso de compilación: el frontend usa módulos JavaScript nativos y carga Leaflet desde un CDN.

## Ejecución local
Desde la carpeta `archive-web`, inicia un servidor HTTP estático:

```powershell
python -m http.server 5500 --directory public
```

En Windows también puedes usar `py -m http.server 5500 --directory public`. Abre [http://localhost:5500](http://localhost:5500) en el navegador. No abras `public/index.html` directamente con `file://`, porque el navegador bloqueará los módulos JavaScript.

## API
La URL base se configura en [`public/js/config.js`](public/js/config.js):

```js
API_BASE: "http://localhost:8080/api"
```

El feed solicita incidentes públicos y se actualiza automáticamente cada 30 segundos. La pantalla de acceso y el panel reporter también requieren que la API esté activa. Si ejecutas el frontend y el backend en orígenes distintos, configura CORS en el backend para permitir el origen del frontend. En un despliegue, cambia `API_BASE` a la URL pública de la API y sirve ambos extremos mediante HTTPS.

Endpoints utilizados:
| Método | Ruta | Uso |
| --- | --- | --- |
| `GET` | `/incidentes/publicos` | Feed y selección de incidentes |
| `POST` | `/auth/login` | Iniciar sesión reporter |
| `POST` | `/auth/logout` | Cerrar sesión |
| `GET` | `/contratos/mios` | Consultar contratos del reporter autenticado |
| `POST` | `/contratos` | Registrar un contrato |

## Funciones
- Feed público con búsqueda por texto y filtro por tipo de incidente.
- Mapa Leaflet con marcadores geolocalizados y actualización junto con el feed.
- Inicio y cierre de sesión reporter.
- Panel reporter para consultar contratos y solicitar uno asociado a un incidente público.

## Solución de problemas
- **No se pudo conectar con la API:** confirma que el backend esté disponible en la URL configurada en `API_BASE` y que el navegador tenga permiso CORS.
- **No carga el mapa:** comprueba la conexión a Internet y que el CDN de Leaflet y los mapas de CARTO sean accesibles.
- **Los módulos JavaScript no cargan:** inicia el servidor HTTP desde `archive-web` y entra por `http://localhost:5500`.

# Cambios Necesarios:
- API Local con Roles
- Crear un servidor local con roles para autenticación.
- Integrar Contratos y Monedero
- Integrar funcionalidades de contratos y monedero.
- Completar Modos y Navegación
- Completar los modos y navegación de la aplicación.
- Probar Seguridad y Responsividad
- Probar la seguridad y la responsividad de la aplicación.
- Subir a un Servidor como Netlify, y subir la aplicación a un servidor como Netlify.
