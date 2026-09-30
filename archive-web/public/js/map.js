import { CONFIG } from "./config.js";

const COLORES_TIPO = {
  ASESINATO: "#ff695e",
  SECUESTRO: "#f0bd61",
  DESAPARICION: "#67d9c1",
};

/**
 * Inicializa el mapa Leaflet oscuro y devuelve la actualización de marcadores.
 *
 * @returns {{actualizarIncidentes: (incidentes: Array<object>) => void}} controlador de mapa
 */
export function iniciarMapa() {
  const mapa = L.map("world-map", {
    zoomControl: false,
    scrollWheelZoom: true,
    minZoom: 2,
    maxZoom: 18,
    worldCopyJump: true,
  }).setView(CONFIG.MAP_CENTER, CONFIG.MAP_ZOOM);

  L.control.zoom({ position: "bottomright" }).addTo(mapa);
  const estadoProveedor = document.getElementById("map-provider");
  const capaCarto = L.tileLayer(CONFIG.MAP_STYLE, {
    subdomains: "abcd",
    maxZoom: 20,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
  });
  const capaOpenStreetMap = L.tileLayer(CONFIG.MAP_FALLBACK_STYLE, {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  });
  let erroresTiles = 0;
  let tilesCargados = 0;
  let temporizadorFallback;
  let fallbackActivo = false;

  function usarOpenStreetMap() {
    if (fallbackActivo) {
      return;
    }
    fallbackActivo = true;
    capaOpenStreetMap.addTo(mapa);
    estadoProveedor.textContent = "MAPA BASE · OPENSTREETMAP";
  }

  capaCarto.on("loading", () => {
    erroresTiles = 0;
    tilesCargados = 0;
    window.clearTimeout(temporizadorFallback);
    temporizadorFallback = window.setTimeout(() => {
      if (erroresTiles > tilesCargados) {
        usarOpenStreetMap();
      }
    }, 4000);
  });
  capaCarto.on("tileerror", () => {
    erroresTiles++;
  });
  capaCarto.on("tileload", () => {
    tilesCargados++;
  });
  capaCarto.on("load", () => {
    window.clearTimeout(temporizadorFallback);
    if (erroresTiles > tilesCargados) {
      usarOpenStreetMap();
    }
  });
  capaCarto.addTo(mapa);

  const capaIncidentes = L.layerGroup().addTo(mapa);
  requestAnimationFrame(() => mapa.invalidateSize());

  /**
   * Sustituye marcadores existentes por los incidentes más recientes.
   *
   * @param {Array<object>} incidentes incidentes recibidos de la API
   */
  function actualizarIncidentes(incidentes) {
    capaIncidentes.clearLayers();
    let marcadores = 0;

    incidentes.forEach((incidente) => {
      const latitud = Number(incidente.lat);
      const longitud = Number(incidente.lon);
      if (!Number.isFinite(latitud) || !Number.isFinite(longitud)
          || latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
        return;
      }

      const color = COLORES_TIPO[incidente.tipo] ?? "#8eb99a";
      const marcador = L.circleMarker([latitud, longitud], {
        radius: 7,
        color,
        fillColor: color,
        fillOpacity: 0.85,
        weight: 1.5,
      });
      marcador.bindPopup(crearContenidoPopup(incidente));
      marcador.addTo(capaIncidentes);
      marcadores++;
    });

    const estado = document.getElementById("map-status");
    estado.textContent = marcadores === 1
      ? "1 INCIDENTE GEOLOCALIZADO"
      : `${marcadores} INCIDENTES GEOLOCALIZADOS`;
  }

  return { actualizarIncidentes };
}

/**
 * Construye el popup como nodos de texto para no interpretar datos de la API como HTML.
 *
 * @param {object} incidente incidente que se muestra
 * @returns {HTMLElement} contenido seguro del popup
 */
function crearContenidoPopup(incidente) {
  const contenedor = document.createElement("div");
  contenedor.className = "map-popup";
  const tipo = document.createElement("strong");
  tipo.textContent = String(incidente.tipo ?? "INCIDENTE").replaceAll("_", " ");
  const lugar = document.createElement("span");
  lugar.textContent = [incidente.ciudad, incidente.pais].filter(Boolean).join(", ") || "Ubicación no indicada";
  contenedor.append(tipo, lugar);
  return contenedor;
}
