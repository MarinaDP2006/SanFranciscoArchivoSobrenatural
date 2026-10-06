// Mapa de OpenStreetMap centrado en San Francisco.
import { CONFIG } from "./config.js";
import { TIPOS, ZONAS, el, etiquetaTipo, fecha } from "./ui.js";

/**
 * @param {string} id id del contenedor
 * @param {object} opciones { zoom, centro, onClick }
 */
export function crearMapa(id, opciones = {}) {
  const mapa = L.map(id, { zoomControl: false, minZoom: 3, worldCopyJump: true })
    .setView(opciones.centro ?? CONFIG.MAPA_CENTRO, opciones.zoom ?? CONFIG.MAPA_ZOOM);
  L.control.zoom({ position: "bottomright" }).addTo(mapa);
  const osm = L.tileLayer(CONFIG.TILES_OSM, {
    maxZoom: 19, attribution: CONFIG.TILES_OSM_ATRIBUCION,
  });
  osm.addTo(mapa);

  const capaIncidentes = L.layerGroup().addTo(mapa);
  const capaZonas = L.layerGroup().addTo(mapa);
  const marcadores = new Map();

  // Botón para volver a la vista de San Francisco
  const Centrar = L.Control.extend({
    options: { position: "topright" },
    onAdd() {
      const b = el("button", { class: "map-btn", type: "button", text: "⌖ San Francisco" });
      L.DomEvent.disableClickPropagation(b);
      b.addEventListener("click", () => mapa.flyTo(CONFIG.MAPA_CENTRO, CONFIG.MAPA_ZOOM));
      return b;
    },
  });
  new Centrar().addTo(mapa);
  setTimeout(() => mapa.invalidateSize(), 100);

  return {
    mapa,
    /** Pinta incidentes (rojo/ámbar/turquesa según el tipo). */
    incidentes(lista, alSeleccionar) {
      capaIncidentes.clearLayers();
      marcadores.clear();
      for (const i of lista) {
        if (!Number.isFinite(i.lat) || !Number.isFinite(i.lng)) continue;
        const color = TIPOS[i.tipo]?.color ?? "#ccc";
        const m = L.circleMarker([i.lat, i.lng], {
          radius: 8, color, fillColor: color, fillOpacity: i.estado === "CERRADO" ? 0.3 : 0.8, weight: 1.5,
        });
        m.bindPopup(() => el("div", {},
          el("strong", { style: `color:${color}`, text: etiquetaTipo(i.tipo).toUpperCase() }),
          el("div", { text: i.titulo }),
          el("div", { style: "color:#8a9a95", text: `${i.barrio ?? i.ciudad} · ${fecha(i.fecha)} · ${i.estado}` }),
          el("a", { href: `incidente.html?id=${i.id}`, text: "Ver expediente →" })));
        if (alSeleccionar) m.on("click", () => alSeleccionar(i.id));
        m.addTo(capaIncidentes);
        marcadores.set(i.id, m);
      }
    },
    /** Pinta las zonas seguras en verde. */
    zonas(lista) {
      capaZonas.clearLayers();
      for (const z of lista) {
        L.circleMarker([z.lat, z.lng], { radius: 6, color: "#3ddc84", fillColor: "#3ddc84", fillOpacity: 0.85, weight: 1 })
          .bindPopup(() => el("div", {},
            el("strong", { style: "color:#3ddc84", text: `ZONA SEGURA · ${ZONAS[z.tipo] ?? z.tipo}` }),
            el("div", { text: z.nombre }),
            el("div", { style: "color:#8a9a95", text: [z.direccion, z.barrio].filter(Boolean).join(" · ") }),
            el("div", { style: "color:#8a9a95", text: [z.horario, z.telefono ? `Tel. ${z.telefono}` : null].filter(Boolean).join(" · ") })))
          .addTo(capaZonas);
      }
    },
    enfocar(id) {
      const m = marcadores.get(id);
      if (m) {
        mapa.flyTo(m.getLatLng(), Math.max(mapa.getZoom(), 14), { duration: 0.6 });
        m.openPopup();
      }
    },
  };
}
