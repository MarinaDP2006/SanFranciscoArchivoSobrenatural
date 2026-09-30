# San Francisco Archive Web

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
