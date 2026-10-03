import { api } from "./api.js";
import { CONFIG } from "./config.js";
import { crearMapa } from "./mapa.js";
import { el, haceCuanto, tagEstado, tagTipo, tarjetaNoticia } from "./ui.js";

const feed = document.getElementById("feed");
const q = document.getElementById("f-q");
const tipo = document.getElementById("f-tipo");
const contador = document.getElementById("feed-count");
const sync = document.getElementById("feed-sync");
const mapa = crearMapa("mapa");

let incidentes = [];
let seleccionado = null;

function filtrados() {
  const texto = q.value.trim().toLowerCase();
  return incidentes.filter((i) => (!tipo.value || i.tipo === tipo.value)
    && (!texto || `${i.titulo} ${i.descripcion} ${i.barrio} ${i.ciudad}`.toLowerCase().includes(texto)));
}

function seleccionar(id) {
  seleccionado = id;
  feed.querySelectorAll(".card-inc").forEach((c) => c.classList.toggle("activo", Number(c.dataset.id) === id));
  const tarjeta = feed.querySelector(`.card-inc[data-id="${id}"]`);
  tarjeta?.scrollIntoView({ block: "nearest", behavior: "smooth" });
}

function pintar() {
  const lista = filtrados();
  contador.textContent = `${lista.length} de ${incidentes.length} expedientes`;
  feed.replaceChildren(...(lista.length ? lista.map((i) => el("li", {},
    el("a", {
      class: `card-inc t-${i.tipo}${i.id === seleccionado ? " activo" : ""}`,
      href: `incidente.html?id=${i.id}`,
      "data-id": i.id,
      onclick: (ev) => {
        // Primer clic: enfoca en el mapa. Segundo clic (ya seleccionado): abre el expediente.
        if (seleccionado !== i.id) {
          ev.preventDefault();
          seleccionar(i.id);
          mapa.enfocar(i.id);
        }
      },
    },
    el("div", { class: "meta" }, tagTipo(i.tipo), tagEstado(i.estado), el("span", { text: haceCuanto(i.fecha) })),
    el("h3", { text: i.titulo }),
    el("p", { text: i.descripcion }),
    el("div", { class: "meta" }, `${i.barrio ?? ""}${i.barrio ? " · " : ""}${i.ciudad}`, el("span", { text: i.codigo })))))
    : [el("li", { class: "empty", text: "Sin incidentes que coincidan con el filtro." })]));
  mapa.incidentes(lista, seleccionar);
}

async function cargar() {
  try {
    incidentes = await api.incidentes();
    sync.textContent = "Sync " + new Date().toLocaleTimeString("es-ES");
    pintar();
    document.getElementById("ticker").textContent = incidentes.slice(0, 5)
      .map((i) => `${i.titulo.toUpperCase()} (${i.barrio ?? i.ciudad})`).join("   ///   ") || "Sin novedades.";
  } catch (e) {
    feed.replaceChildren(el("li", { class: "empty", text: "No se pudo conectar con el archivo. Reintentando…" }));
    sync.textContent = "Sin conexión";
  }
  try {
    const st = await api.estadisticas();
    document.getElementById("st-total").textContent = st.total;
    for (const [t, n] of Object.entries(st.porTipo)) {
      const nodo = document.getElementById(`st-${t}`);
      if (nodo) nodo.textContent = n;
    }
  } catch { /* las estadísticas no son críticas */ }
}

async function cargarFijos() {
  try {
    mapa.zonas(await api.zonas());
  } catch { /* sin zonas */ }
  try {
    const noticias = await api.noticias(null, 5);
    const cont = document.getElementById("noticias");
    cont.replaceChildren(...(noticias.length
      ? noticias.slice(0, 5).map((n, idx) => tarjetaNoticia(n, idx === 0))
      : [el("p", { class: "empty", text: "Todavía no hay noticias publicadas." })]));
  } catch {
    document.getElementById("noticias").replaceChildren(el("p", { class: "empty", text: "No se pudieron cargar las noticias." }));
  }
}

q.addEventListener("input", pintar);
tipo.addEventListener("change", pintar);
document.getElementById("filtros").addEventListener("submit", (e) => e.preventDefault());
document.getElementById("refrescar").addEventListener("click", cargar);

cargar();
cargarFijos();
setInterval(cargar, CONFIG.REFRESCO_MS);
