// Configuración global del portal web de San Francisco Archive.
export const CONFIG = {
  API_BASE: globalThis.archiveDesktop?.apiBase ?? "",
  MAP_REFRESH_MS: 30000,
  MAP_STYLE: "https://{s}.basemaps.cartocdn.com/dark_matter/{z}/{x}/{y}{r}.png",
  MAP_FALLBACK_STYLE: "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
  MAP_CENTER: [37.7749, -122.4194],
  MAP_ZOOM: 3,
};
