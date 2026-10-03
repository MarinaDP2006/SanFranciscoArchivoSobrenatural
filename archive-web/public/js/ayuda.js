import { api } from "./api.js";
import { crearMapa } from "./mapa.js";
import { el, etiquetaTipo, fechaHora } from "./ui.js";

const BARRIOS = ["Bayview", "Bernal Heights", "Castro", "Chinatown", "Dogpatch", "Embarcadero", "Excelsior",
  "Financial District", "Fisherman's Wharf", "Golden Gate Park", "Haight-Ashbury", "Hayes Valley", "Inner Sunset",
  "Japantown", "Lands End", "Marina", "Mission", "Nob Hill", "Noe Valley", "North Beach", "Outer Sunset",
  "Pacific Heights", "Potrero Hill", "Presidio", "Richmond", "SoMa", "Telegraph Hill", "Tenderloin", "Twin Peaks",
  "Union Square", "Alcatraz", "Oakland", "Berkeley", "Sausalito", "Otro"];

const form = document.getElementById("form-ayuda");
const desc = document.getElementById("descripcion");
const resultado = document.getElementById("resultado");
const coordsTxt = document.getElementById("coords-txt");
const selBarrio = document.getElementById("barrio");
BARRIOS.forEach((b) => selBarrio.append(el("option", { value: b, text: b })));

// Preselección de tipo si se llega desde un expediente (?tipo=SECUESTRO)
const tipoUrl = new URLSearchParams(location.search).get("tipo");
form.querySelector(`input[name=tipo][value="${tipoUrl}"]`)?.setAttribute("checked", "");

desc.addEventListener("input", () => {
  document.getElementById("contador").textContent = `${desc.value.length} / 2000 · mínimo 20 caracteres`;
});

// --- Mapa: clic para marcar el lugar ---
const m = crearMapa("mapa-ayuda", { zoom: 12 });
api.zonas().then((z) => m.zonas(z)).catch(() => {});
let pin = null;
let posicion = null;
m.mapa.on("click", (e) => {
  posicion = { lat: +e.latlng.lat.toFixed(6), lng: +e.latlng.lng.toFixed(6) };
  if (pin) pin.setLatLng(e.latlng);
  else pin = L.circleMarker(e.latlng, { radius: 9, color: "#e8b04b", fillColor: "#e8b04b", fillOpacity: 0.9 }).addTo(m.mapa);
  coordsTxt.textContent = `Lugar marcado: ${posicion.lat}, ${posicion.lng} · pulsa de nuevo para moverlo.`;
});

form.addEventListener("submit", async (ev) => {
  ev.preventDefault();
  const datos = Object.fromEntries(new FormData(form));
  if (!datos.tipo) return mostrarError("Indica qué ha ocurrido.");
  if ((datos.descripcion || "").trim().length < 20) return mostrarError("Describe lo ocurrido con al menos 20 caracteres.");
  const boton = document.getElementById("enviar");
  boton.disabled = true;
  boton.textContent = "Enviando…";
  try {
    const r = await api.pedirAyuda({ ...datos, ...(posicion ?? {}) });
    form.reset();
    if (pin) { pin.remove(); pin = null; posicion = null; }
    resultado.replaceChildren(el("div", { class: "alert ok" },
      el("p", { style: "margin:0 0 6px", text: "Aviso recibido. Este es tu código de seguimiento:" }),
      el("div", { class: "codigo", text: r.codigo }),
      el("p", { class: "hint", style: "margin:6px 0 0", text: "Guárdalo: es la única forma de consultar el estado de tu aviso. No guardamos ningún dato que te identifique." })));
    document.getElementById("codigo").value = r.codigo;
  } catch (e) {
    mostrarError(e.message);
  } finally {
    boton.disabled = false;
    boton.textContent = "Enviar aviso anónimo";
  }
});

function mostrarError(msg) {
  resultado.replaceChildren(el("div", { class: "alert error", role: "alert", text: msg }));
}

const ESTADOS = {
  PENDIENTE: "Recibido · pendiente de revisión",
  EN_REVISION: "En revisión por el equipo",
  ATENDIDA: "Atendido",
  DESCARTADA: "Cerrado sin incidente",
};

document.getElementById("form-estado").addEventListener("submit", async (ev) => {
  ev.preventDefault();
  const out = document.getElementById("estado-res");
  const codigo = document.getElementById("codigo").value;
  if (!codigo.trim()) return;
  out.replaceChildren(el("p", { class: "hint", text: "Consultando…" }));
  try {
    const r = await api.estadoAyuda(codigo);
    out.replaceChildren(el("div", { class: "dossier" }, el("dl", {},
      el("dt", { text: "Código" }), el("dd", { text: r.codigo }),
      el("dt", { text: "Tipo" }), el("dd", { text: etiquetaTipo(r.tipo) }),
      el("dt", { text: "Recibido" }), el("dd", { text: fechaHora(r.recibida) }),
      el("dt", { text: "Estado" }), el("dd", { text: ESTADOS[r.estado] ?? r.estado }),
      r.respuesta ? el("dt", { text: "Respuesta" }) : null, r.respuesta ? el("dd", { text: r.respuesta }) : null)));
  } catch (e) {
    out.replaceChildren(el("div", { class: "alert error", text: e.message }));
  }
});

if (location.hash === "#estado") document.getElementById("codigo").focus();
