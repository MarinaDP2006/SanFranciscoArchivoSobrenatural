// Cabecera y pie comunes. Cada página marca su sección con <body data-page="...">.
import { el } from "./ui.js";

const ENLACES = [
  ["inicio", "index.html", "Inicio"],
  ["mapa", "index.html#monitor", "Mapa en vivo"],
  ["noticias", "noticias.html", "Noticias"],
  ["archivo", "archivo.html", "El archivo"],
  ["admin", "admin-login.html", "Acceso admin"],
];

export function montarLayout() {
  const pagina = document.body.dataset.page;

  const nav = el("nav", { class: "nav", id: "nav", "aria-label": "Principal" },
    ENLACES.map(([id, href, txt]) => el("a", { href, "aria-current": id === pagina ? "page" : null, text: txt })),
    el("a", { class: "nav-help", href: "ayuda.html", "aria-current": pagina === "ayuda" ? "page" : null, text: "Pedir ayuda" }));

  const toggle = el("button", { class: "nav-toggle", type: "button", "aria-controls": "nav", "aria-expanded": "false", text: "Menú" });
  toggle.addEventListener("click", () => {
    const abierta = nav.classList.toggle("abierta");
    toggle.setAttribute("aria-expanded", String(abierta));
  });

  const header = el("header", { class: "site-header" },
    el("div", { class: "wrap" },
      el("a", { class: "brand", href: "index.html", "aria-label": "San Francisco Archive, inicio" },
        el("span", { class: "brand-seal", "aria-hidden": "true", text: "SFA" }),
        el("span", { class: "brand-name" }, "San Francisco Archive", el("small", { text: "Registro público de incidentes" }))),
      el("span", { class: "live" }, el("span", { class: "live-dot", "aria-hidden": "true" }), "Transmisión en vivo"),
      toggle, nav));

  const footer = el("footer", { class: "site-footer" },
    el("div", { class: "wrap" },
      el("div", {},
        el("h4", { text: "San Francisco Archive" }),
        el("p", { text: "Registro público e independiente de secuestros, asesinatos y desapariciones en la bahía de San Francisco. Solo informativo: no necesitas cuenta para consultar ni para pedir ayuda." })),
      el("div", {},
        el("h4", { text: "Consultar" }),
        el("ul", {},
          el("li", {}, el("a", { href: "index.html#monitor", text: "Feed y mapa" })),
          el("li", {}, el("a", { href: "noticias.html", text: "Noticias" })),
          el("li", {}, el("a", { href: "archivo.html", text: "Sobre el archivo" })))),
      el("div", {},
        el("h4", { text: "Ayuda" }),
        el("ul", {},
          el("li", {}, el("a", { href: "ayuda.html", text: "Pedir ayuda anónima" })),
          el("li", {}, el("a", { href: "ayuda.html#estado", text: "Consultar mi aviso" })),
          el("li", {}, "Emergencias: ", el("a", { href: "tel:911", text: "911" })))),
      el("div", { class: "legal" },
        el("span", { text: `© ${new Date().getFullYear()} San Francisco Archive · 37°46′ N 122°25′ W` }),
        el("span", { text: "Proyecto de ficción (TFG). Personas y sucesos inventados." }))));

  document.body.prepend(el("a", { class: "skip-link", href: "#contenido", text: "Saltar al contenido" }), header);
  document.body.append(footer);

  document.addEventListener("sfa:demo", () => {
    if (document.querySelector(".demo-banner")) return;
    header.after(el("div", { class: "demo-banner", role: "status",
      text: "Sin conexión con la API: mostrando una copia de demostración de los datos." }));
  });
}

montarLayout();
