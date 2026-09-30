# San Francisco Archive

Sitio web estático para consultar un archivo público de incidentes y verlos en un mapa.

## Ejecutar localmente

Sirve la carpeta `public` con cualquier servidor estático. Por ejemplo:

```powershell
python -m http.server 8000 --directory public
```

Abre `http://localhost:8000`. El feed y el mapa usan `public/data/mock-incidents.json` mientras no se configure una API.

## Publicar en GitHub Pages

1. Sube este proyecto a un repositorio de GitHub.
2. En **Settings > Pages**, selecciona **GitHub Actions** como fuente de publicación.
3. Sube los cambios a la rama `main` o `master`, o ejecuta manualmente el workflow **Deploy GitHub Pages** en **Actions**.

El workflow publica la carpeta `public`. GitHub mostrará la URL en **Settings > Pages** y en el resultado del workflow.

## API y acceso reporter

El sitio público funciona sin servidor usando los datos de demostración. Para conectar una API, configura `API_BASE` en `public/js/config.js` con la URL HTTPS de la API y el prefijo `/api`; el servidor debe permitir CORS desde el dominio publicado. La API debe implementar los endpoints consumidos en `public/js/api.js`.

GitHub Pages solo sirve archivos estáticos. El inicio de sesión, las sesiones y la creación de contratos requieren una API desplegada por separado; no se simulan en el navegador ni se almacenan allí como si fueran seguros.