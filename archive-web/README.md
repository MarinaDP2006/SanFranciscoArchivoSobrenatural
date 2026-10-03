# Web pública · San Francisco Archive

Portal **solo informativo**. Los ciudadanos no inician sesión: consultan el feed y el mapa, leen noticias y pueden **pedir ayuda de forma anónima**. Todos los datos los gestiona la aplicación de escritorio (`../archive-app`), que escribe en MySQL; esta web los lee a través de la API.

## Páginas

| Archivo | Contenido |
| --- | --- |
| `index.html` | Portada: estadísticas, feed en vivo + mapa (auto-refresco 30 s), últimas noticias |
| `incidente.html?id=` | Expediente público de un incidente con mini-mapa |
| `noticias.html` · `noticia.html?slug=` | Sala de prensa |
| `ayuda.html` | Formulario anónimo «Pedir ayuda» y consulta del estado con el código |
| `archivo.html` | Quiénes somos, cómo trabajamos, FAQ |

## Ejecutar

La forma más sencilla es arrancar la API, que sirve también esta carpeta:

```bash
cd ../archive-app && java -jar api-web/target/sfa-api.jar   # → http://localhost:8080
```

Para servirla por separado (la web detecta la API en `localhost:8080`):

```bash
python -m http.server 5500 --directory public      # → http://localhost:5500 (o: npx serve public)
```

No abras los HTML con `file://`: los módulos JavaScript necesitan un servidor.

## Configuración

`public/js/config.js` → `API_PRODUCCION` con la URL pública de la API al desplegar (Netlify / Cloudflare Pages). Sin API disponible la web muestra `public/data/demo.json` con un aviso de «modo demostración».

## Tecnología

HTML + CSS + JavaScript nativo (módulos ES), sin compilación. Leaflet 1.9.4 incluido en `public/vendor/leaflet` (licencia BSD-2) y mapa base CARTO Dark Matter. Todo el contenido de la API se inserta como texto (sin `innerHTML`) para evitar XSS.
