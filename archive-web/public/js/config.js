// =====================================================================
// Configuración del portal público y de la gestión web.
// La web pública solo lee datos, y la gestión de administración vive en la
// misma web con login y backend. La API sigue sirviendo los datos JSON aquí.
// =====================================================================

/**
 * URL base de la API. Prioridad:
 *  1. window.SFA_API_BASE (si se define antes de cargar los scripts)
 *  2. Mismo origen si la web la sirve la propia API (puerto 8080 o despliegue conjunto)
 *  3. http://localhost:8080 en desarrollo (web servida con python/npx en otro puerto)
 * Para desplegar (Netlify/Cloudflare + Railway) cambia API_PRODUCCION.
 */
const API_PRODUCCION = "";

function resolverApi() {
  if (globalThis.SFA_API_BASE) return globalThis.SFA_API_BASE.replace(/\/$/, "");
  if (API_PRODUCCION) return API_PRODUCCION.replace(/\/$/, "");
  const { hostname, port, protocol } = window.location;
  const esLocal = hostname === "localhost" || hostname === "127.0.0.1";
  if (protocol === "file:") return "http://localhost:8080";
  if (esLocal && port && port !== "8080") return "http://localhost:8080";
  return "";
}

export const CONFIG = {
  API_BASE: resolverApi() + "/api",
  REFRESCO_MS: 30000,
  MAPA_CENTRO: [37.7749, -122.4194], // San Francisco
  MAPA_ZOOM: 12,
  TILES_OSM: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
  TILES_OSM_ATRIBUCION: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  DEMO_JSON: "data/demo.json",
};
