// Utilidades de interfaz compartidas por todas las páginas.

export const TIPOS = {
  SECUESTRO: { etiqueta: "Secuestro", color: "#f5a524" },
  ASESINATO: { etiqueta: "Asesinato", color: "#ff4d4d" },
  DESAPARICION: { etiqueta: "Desaparición", color: "#4fd1c5" },
};

export const ZONAS = {
  HOSPITAL: "Hospital",
  POLICIA: "Policía",
  BOMBEROS: "Bomberos",
  REFUGIO: "Refugio",
  TEMPLO: "Templo",
};

export const CATEGORIAS = {
  SUCESOS: "Sucesos",
  CIUDAD: "Ciudad",
  AVISO: "Aviso",
  COMUNIDAD: "Comunidad",
  HISTORIA: "Historia",
};

/** Crea un elemento con atributos e hijos (texto seguro, nunca innerHTML con datos). */
export function el(tag, attrs = {}, ...hijos) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v === null || v === undefined || v === false) continue;
    if (k === "class") e.className = v;
    else if (k === "text") e.textContent = v;
    else if (k.startsWith("on")) e.addEventListener(k.slice(2), v);
    else e.setAttribute(k, v === true ? "" : v);
  }
  for (const h of hijos.flat()) {
    if (h === null || h === undefined || h === false) continue;
    e.append(h instanceof Node ? h : document.createTextNode(String(h)));
  }
  return e;
}

const fmtFecha = new Intl.DateTimeFormat("es-ES", { day: "2-digit", month: "short", year: "numeric" });
const fmtFechaHora = new Intl.DateTimeFormat("es-ES", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });

export const fecha = (iso) => (iso ? fmtFecha.format(new Date(iso)) : "—");
export const fechaHora = (iso) => (iso ? fmtFechaHora.format(new Date(iso)) : "—");

/** "hace 3 h", "hace 2 días"... */
export function haceCuanto(iso) {
  if (!iso) return "";
  const seg = (Date.now() - new Date(iso).getTime()) / 1000;
  if (seg < 0) return fecha(iso);
  const unidades = [[86400 * 30, "mes", "meses"], [86400, "día", "días"], [3600, "h", "h"], [60, "min", "min"]];
  for (const [s, uno, varios] of unidades) {
    const n = Math.floor(seg / s);
    if (n >= 1) return `hace ${n} ${n === 1 ? uno : varios}`;
  }
  return "ahora mismo";
}

export const etiquetaTipo = (t) => TIPOS[t]?.etiqueta ?? t;

export function tagTipo(tipo) {
  return el("span", { class: `tag t-${tipo}`, text: etiquetaTipo(tipo) });
}

export function tagEstado(estado) {
  const clase = estado === "SIN CONFIRMAR" ? "SIN" : estado;
  return el("span", { class: `tag e-${clase}`, text: estado });
}

/** Convierte texto plano con saltos de línea en párrafos. */
export function parrafos(texto) {
  return String(texto || "").split(/\n{2,}/).map((p) => el("p", {}, ...p.split("\n").flatMap((l, i) => (i ? [el("br"), l] : [l]))));
}

export function param(nombre) {
  return new URLSearchParams(window.location.search).get(nombre);
}

export function portada(noticia) {
  const div = el("div", { class: "cover" + (noticia.imagenUrl ? "" : " gen") });
  if (noticia.imagenUrl) div.style.backgroundImage = `url("${encodeURI(noticia.imagenUrl)}")`;
  div.append(el("span", { class: "stamp", text: CATEGORIAS[noticia.categoria] ?? noticia.categoria }));
  return div;
}

export function tarjetaNoticia(n, destacada = false) {
  return el("a", { class: "news-card" + (destacada ? " destacada" : ""), href: `noticia.html?slug=${encodeURIComponent(n.slug)}` },
    portada(n),
    el("div", { class: "body" },
      el("div", { class: "meta" }, el("span", { class: "tag cat", text: CATEGORIAS[n.categoria] ?? n.categoria }), fecha(n.fecha), n.barrio ? `· ${n.barrio}` : null),
      el("h3", { text: n.titulo }),
      el("p", { text: n.resumen })));
}
