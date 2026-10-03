import { CONFIG } from "./config.js";

let modoDemo = false;
let demoCache = null;

/** Indica si la web está mostrando la copia de demostración por falta de API. */
export const enModoDemo = () => modoDemo;

async function cargarDemo() {
  if (!demoCache) {
    const r = await fetch(CONFIG.DEMO_JSON);
    if (!r.ok) throw new Error("Sin conexión con el archivo");
    demoCache = await r.json();
  }
  if (!modoDemo) {
    modoDemo = true;
    document.dispatchEvent(new CustomEvent("sfa:demo"));
  }
  return demoCache;
}

/**
 * Petición GET a la API. Si la API no responde, usa la instantánea data/demo.json
 * para que la web siga siendo navegable (por ejemplo, en un hosting estático).
 */
async function get(ruta, demo) {
  try {
    const r = await fetch(CONFIG.API_BASE + ruta, { headers: { Accept: "application/json" } });
    if (r.status === 404) {
      const e = new Error((await r.json().catch(() => ({}))).error || "No encontrado");
      e.status = 404;
      throw e;
    }
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    modoDemo = false;
    return await r.json();
  } catch (err) {
    if (err.status === 404 || !demo) throw err;
    return demo(await cargarDemo());
  }
}

export const api = {
  incidentes: (filtros = {}) => {
    const qs = new URLSearchParams(Object.entries(filtros).filter(([, v]) => v)).toString();
    return get("/incidentes" + (qs ? "?" + qs : ""), (d) => filtrarIncidentes(d.incidentes, filtros));
  },
  incidente: (id) => get(`/incidentes/${encodeURIComponent(id)}`, (d) => {
    const inc = d.incidentes.find((i) => String(i.id) === String(id));
    if (!inc) throw Object.assign(new Error("Incidente no encontrado"), { status: 404 });
    return { incidente: inc, noticias: d.noticias.filter((n) => n.incidenteId === inc.id) };
  }),
  noticias: (categoria, limite = 20) => {
    const qs = new URLSearchParams({ limite });
    if (categoria) qs.set("categoria", categoria);
    return get("/noticias?" + qs, (d) => d.noticias
      .filter((n) => !categoria || n.categoria === categoria)
      .slice(0, limite));
  },
  noticia: (slug) => get(`/noticias/${encodeURIComponent(slug)}`, (d) => {
    const n = d.noticias.find((x) => x.slug === slug);
    if (!n) throw Object.assign(new Error("Noticia no encontrada"), { status: 404 });
    return n;
  }),
  zonas: () => get("/zonas-seguras", (d) => d.zonas),
  estadisticas: () => get("/estadisticas", (d) => d.estadisticas),

  /** Envía una solicitud de ayuda anónima. Nunca usa la demo: necesita la API real. */
  async pedirAyuda(datos) {
    let r;
    try {
      r = await fetch(CONFIG.API_BASE + "/ayuda", {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(datos),
      });
    } catch {
      throw new Error("No hay conexión con el archivo. Si es una emergencia, llama al 911.");
    }
    const json = await r.json().catch(() => ({}));
    if (!r.ok) throw new Error(json.error || `Error ${r.status}`);
    return json;
  },

  async estadoAyuda(codigo) {
    const r = await fetch(CONFIG.API_BASE + "/ayuda/" + encodeURIComponent(codigo.trim().toUpperCase()))
      .catch(() => { throw new Error("No hay conexión con el archivo."); });
    const json = await r.json().catch(() => ({}));
    if (!r.ok) throw new Error(json.error || `Error ${r.status}`);
    return json;
  },
};

function filtrarIncidentes(lista, { tipo, q, barrio } = {}) {
  const texto = (q || "").toLowerCase();
  return lista.filter((i) => (!tipo || i.tipo === tipo)
    && (!barrio || i.barrio === barrio)
    && (!texto || [i.titulo, i.descripcion, i.barrio].join(" ").toLowerCase().includes(texto)));
}
