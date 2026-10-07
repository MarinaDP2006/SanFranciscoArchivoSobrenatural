import { CONFIG } from "./config.js";

let solicitudes = [];
let incidentes = [];
let contratos = [];
let grupos = [];
let potencialesDisponibles = [];
let potenciales = [];
let noticiasAdmin = [];
let zonasSeguras = [];
let datosMonederos = { potenciales: [], movimientos: [], total: 0 };
let usuariosAdmin = [];
let actividadAdmin = [];
let informesAdmin = [];

const EDITORES = {
  potenciales: {
    titulo: "Ficha de potencial",
    endpoint: "/admin/potenciales",
    campos: [
      ["alias", "Alias", "text", true], ["nombreReal", "Nombre real", "text", true],
      ["username", "Usuario de acceso", "text", false, "reclutamiento"],
      ["email", "Email de acceso", "email", false, "reclutamiento"],
      ["password", "Contraseña inicial", "password", false, "reclutamiento"],
      ["edad", "Edad", "number"], ["habilidad", "Habilidad", "text", true],
      ["descripcion", "Descripción", "textarea"], ["nivel", "Nivel (1-5)", "number", true],
      ["estado", "Estado", "select", true, ["DISPONIBLE", "EN_MISION", "HERIDO", "INACTIVO"]],
      ["grupoId", "Grupo táctico", "select", false, "grupos"], ["barrio", "Barrio", "text"],
      ["ciudad", "Ciudad", "text"], ["pais", "País", "text"], ["lat", "Latitud", "number"],
      ["lng", "Longitud", "number"], ["fondoInicial", "Fondo inicial (USD)", "number", false, "reclutamiento"]
    ]
  },
  grupos: {
    titulo: "Grupo táctico",
    endpoint: "/admin/grupos",
    campos: [["nombre", "Nombre", "text", true], ["pais", "País", "text", true],
      ["ciudad", "Ciudad", "text"], ["zona", "Zona cubierta", "text"],
      ["lat", "Latitud", "number"], ["lng", "Longitud", "number"],
      ["descripcion", "Descripción", "textarea"], ["activo", "Operativo", "checkbox"]]
  },
  noticias: {
    titulo: "Noticia",
    endpoint: "/admin/noticias",
    campos: [["titulo", "Titular", "text", true], ["resumen", "Resumen", "textarea", true],
      ["contenido", "Contenido", "textarea"], ["categoria", "Categoría", "select", true,
        ["SUCESOS", "CIUDAD", "AVISO", "COMUNIDAD", "HISTORIA"]],
      ["imagenUrl", "URL de imagen", "url"], ["barrio", "Barrio", "text"],
      ["incidenteId", "ID de incidente relacionado", "number"],
      ["fechaPublicacion", "Fecha de publicación", "datetime-local"],
      ["publicada", "Publicada", "checkbox"], ["destacada", "Destacada", "checkbox"]]
  },
  zonas: {
    titulo: "Zona segura",
    endpoint: "/admin/zonas-seguras",
    campos: [["nombre", "Nombre", "text", true], ["tipo", "Tipo", "select", true,
      ["HOSPITAL", "POLICIA", "BOMBEROS", "REFUGIO", "TEMPLO"]],
      ["direccion", "Dirección", "text"], ["barrio", "Barrio", "text"],
      ["lat", "Latitud", "number", true], ["lng", "Longitud", "number", true],
      ["telefono", "Teléfono", "tel"], ["horario", "Horario", "text"],
      ["activa", "Abierta al público", "checkbox"]]
  }
};

async function api(path, options = {}) {
  const response = await fetch(`${CONFIG.API_BASE}${path}`, {
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  const text = await response.text();
  let payload = {};
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = { error: text };
    }
  }

  if (!response.ok) {
    throw new Error(payload.error || `Error ${response.status}`);
  }

  return payload;
}

function nodo(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}

function campoEditor([name, labelText, type, required, options], value = "") {
  const label = nodo("label", "admin-field");
  label.append(nodo("b", "", labelText));
  let input;
  if (type === "textarea") {
    input = document.createElement("textarea");
    input.rows = name === "contenido" || name === "descripcion" ? 5 : 3;
  } else if (type === "select") {
    input = document.createElement("select");
    if (options === "grupos") {
      input.append(new Option("Sin grupo", ""), ...grupos.map((group) => new Option(group.nombre, group.id)));
    } else {
      input.append(...options.map((option) => new Option(option.replaceAll("_", " "), option)));
    }
  } else {
    input = document.createElement("input");
    input.type = type === "checkbox" ? "checkbox" : type;
  }
  input.name = name;
  if (required) input.required = true;
  if (type === "checkbox") input.checked = Boolean(value);
  else if (value !== null && value !== undefined) input.value = value;
  label.append(input);
  if (type === "checkbox") label.classList.add("check-label");
  return label;
}

function setStatus(message, ok = true) {
  const status = document.querySelector("#login-status");
  if (!status) return;
  status.className = `alert ${ok ? "ok" : "error"}`;
  status.textContent = message;
}

function formatearFecha(value) {
  if (!value) return "Fecha desconocida";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Fecha desconocida" : new Intl.DateTimeFormat("es-ES", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

async function cargarDashboard() {
  try {
    const session = await api("/admin/session");
    const stats = await api("/estadisticas");
    const usuario = session.usuario;

    const role = usuario.rol === "ADMIN" ? "Administrador" : "Potencial";
    document.querySelector("#admin-role").textContent = role;
    document.querySelector("#admin-user").textContent = `${usuario.nombre} · ${usuario.username}`;

    const tipoStats = stats.porTipo || {};
    document.querySelector("#stat-total").textContent = stats.total ?? 0;
    document.querySelector("#stat-SECUESTRO").textContent = tipoStats.SECUESTRO ?? 0;
    document.querySelector("#stat-ASESINATO").textContent = tipoStats.ASESINATO ?? 0;
    document.querySelector("#stat-DESAPARICION").textContent = tipoStats.DESAPARICION ?? 0;
  } catch (error) {
    window.location.href = "admin-login.html";
  }
}

async function cargarContenido(tipo) {
  const listIds = {
    potenciales: "#potenciales-list",
    grupos: "#grupos-list",
    noticias: "#noticias-admin-list",
    zonas: "#zonas-list"
  };
  const list = document.querySelector(listIds[tipo]);
  list.replaceChildren(nodo("p", "empty", "Cargando…"));
  try {
    if (tipo === "potenciales") {
      [potenciales, grupos] = await Promise.all([api("/admin/potenciales"), api("/admin/grupos")]);
    } else {
      const data = await api(EDITORES[tipo].endpoint);
      if (tipo === "grupos") grupos = data;
      if (tipo === "noticias") noticiasAdmin = data;
      if (tipo === "zonas") zonasSeguras = data;
    }
    renderContenido(tipo);
  } catch (error) {
    if (error.message.includes("administrador") || error.message.includes("autenticado")) {
      window.location.href = "admin-login.html";
      return;
    }
    list.replaceChildren(nodo("p", "alert error", error.message));
  }
}

async function cargarModuloAdmin(tipo) {
  if (EDITORES[tipo]) {
    await cargarContenido(tipo);
    return;
  }
  const destinations = {
    monederos: "#monederos-list",
    informes: "#informes-list",
    usuarios: "#usuarios-list",
    actividad: "#actividad-list"
  };
  const list = document.querySelector(destinations[tipo]);
  list.replaceChildren(nodo("p", "empty", "Cargando…"));
  try {
    if (tipo === "monederos") {
      datosMonederos = await api("/admin/monederos");
      renderMonederos();
    } else if (tipo === "informes") {
      [informesAdmin, contratos] = await Promise.all([api("/admin/informes"), api("/admin/contratos")]);
      renderInformes();
    } else if (tipo === "usuarios") {
      usuariosAdmin = await api("/admin/usuarios");
      renderUsuarios();
    } else {
      actividadAdmin = await api("/admin/actividad");
      renderActividad();
    }
  } catch (error) {
    if (error.message.includes("administrador") || error.message.includes("autenticado")) {
      window.location.href = "admin-login.html";
      return;
    }
    list.replaceChildren(nodo("p", "alert error", error.message));
  }
}

function renderMonederos() {
  const list = document.querySelector("#monederos-list");
  document.querySelector("#wallet-total").textContent = `Saldo total en circulación: ${datosMonederos.total} USD`;
  const cards = datosMonederos.potenciales.map((person) => {
    const card = nodo("article", "request-item");
    card.append(nodo("div", "request-head", ""), nodo("strong", "request-code", person.alias),
      nodo("p", "request-meta", `${person.estado} · ${person.grupoNombre || "Sin grupo"}`),
      nodo("p", "wallet-balance", `${person.saldo} USD`),
      button("Registrar ajuste", "btn btn-primary", () => abrirAjuste(person)));
    return card;
  });
  list.replaceChildren(...cards);
}

function renderInformes() {
  const list = document.querySelector("#informes-list");
  const informes = new Map(informesAdmin.map((report) => [report.contratoId, report]));
  const cerrados = contratos.filter((item) => ["COMPLETADO", "FALLIDO"].includes(item.estado));
  if (!cerrados.length) {
    list.replaceChildren(nodo("p", "empty", "Aún no hay contratos cerrados para archivar."));
    return;
  }
  list.replaceChildren(...cerrados.map((contract) => {
    const report = informes.get(contract.id);
    const card = nodo("article", "request-item");
    card.append(nodo("strong", "request-code", contract.codigo),
      nodo("p", "request-description", contract.incidenteTitulo),
      nodo("p", "request-meta", report ? `${report.titulo} · ${report.clasificacion}` : "Sin informe final"),
      button(report ? "Editar informe" : "Redactar informe", "btn btn-primary", () => abrirInforme(contract, report)));
    return card;
  }));
}

function renderUsuarios() {
  const list = document.querySelector("#usuarios-list");
  list.replaceChildren(...usuariosAdmin.map((user) => {
    const card = nodo("article", "request-item");
    const head = nodo("div", "request-head");
    head.append(nodo("strong", "request-code", user.username),
      nodo("span", `tag request-state ${user.activo ? "state-ATENDIDA" : "state-DESCARTADA"}`, user.activo ? "ACTIVA" : "DESACTIVADA"));
    card.append(head, nodo("p", "request-meta", `${user.nombreCompleto} · ${user.email} · ${user.rol}`));
    const actions = nodo("div", "admin-actions");
    actions.append(button("Editar cuenta", "btn", () => abrirUsuario(user)),
      button("Restablecer contraseña", "btn", () => abrirResetPassword(user)));
    card.append(actions);
    return card;
  }));
}

function renderActividad() {
  const list = document.querySelector("#actividad-list");
  if (!actividadAdmin.length) {
    list.replaceChildren(nodo("p", "empty", "No hay actividad registrada."));
    return;
  }
  list.replaceChildren(...actividadAdmin.map((entry) => {
    const card = nodo("article", "request-item");
    card.append(nodo("div", "request-head", ""), nodo("strong", "request-code", `${entry.accion} · ${entry.entidad}`),
      nodo("p", "request-description", entry.detalle || "Sin detalle"),
      nodo("p", "request-meta", `${entry.usuario || "Sistema"} · ${formatearFecha(entry.fecha)}`));
    return card;
  }));
}

function abrirAjuste(person) {
  const form = document.querySelector("#wallet-form");
  form.elements.id.value = person.id;
  document.querySelector("#wallet-title").textContent = `Ajuste · ${person.alias}`;
  form.elements.importe.value = "";
  form.elements.concepto.value = "";
  document.querySelector("#wallet-dialog").showModal();
}

function abrirUsuario(user) {
  const form = document.querySelector("#user-form");
  form.elements.id.value = user.id;
  form.elements.nombre.value = user.nombreCompleto;
  form.elements.email.value = user.email;
  form.elements.activo.checked = user.activo;
  document.querySelector("#user-dialog").showModal();
}

function abrirResetPassword(user) {
  const form = document.querySelector("#password-form");
  form.elements.id.value = user.id;
  form.elements.password.value = "";
  document.querySelector("#password-dialog").showModal();
}

function abrirInforme(contract, report = null) {
  const form = document.querySelector("#report-form");
  const select = form.elements.contratoId;
  select.replaceChildren(...contratos.filter((item) => ["COMPLETADO", "FALLIDO"].includes(item.estado))
    .map((item) => new Option(`${item.codigo} · ${item.incidenteTitulo}`, item.id)));
  form.elements.contratoId.value = contract.id;
  form.elements.titulo.value = report?.titulo ?? contract.incidenteTitulo;
  form.elements.entidad.value = report?.entidad ?? "";
  form.elements.clasificacion.value = report?.clasificacion ?? "CONFIDENCIAL";
  form.elements.resumen.value = report?.resumen ?? "";
  form.elements.contenido.value = report?.contenido ?? "";
  form.elements.bajasCiviles.value = report?.bajasCiviles ?? 0;
  document.querySelector("#report-dialog").showModal();
}

function bindOperationsAdmin() {
  document.querySelectorAll("[data-refresh]").forEach((control) => {
    control.addEventListener("click", () => cargarModuloAdmin(control.dataset.refresh));
  });
  const walletDialog = document.querySelector("#wallet-dialog");
  const walletForm = document.querySelector("#wallet-form");
  document.querySelector("#cancel-wallet").addEventListener("click", () => walletDialog.close());
  walletForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(walletForm);
    try {
      await api(`/admin/monederos/${walletForm.elements.id.value}/ajustes`, { method: "POST", body: JSON.stringify({
        tipo: String(data.get("tipo")), importe: Number(data.get("importe")), concepto: String(data.get("concepto")).trim()
      }) });
      walletDialog.close();
      await cargarModuloAdmin("monederos");
    } catch (error) { window.alert(error.message || "No se pudo registrar el movimiento."); }
  });

  const userDialog = document.querySelector("#user-dialog");
  const userForm = document.querySelector("#user-form");
  document.querySelector("#cancel-user").addEventListener("click", () => userDialog.close());
  userForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await api(`/admin/usuarios/${userForm.elements.id.value}`, { method: "POST", body: JSON.stringify({
        nombre: userForm.elements.nombre.value.trim(), email: userForm.elements.email.value.trim(),
        activo: userForm.elements.activo.checked
      }) });
      userDialog.close();
      await cargarModuloAdmin("usuarios");
    } catch (error) { window.alert(error.message || "No se pudo actualizar la cuenta."); }
  });

  const passwordDialog = document.querySelector("#password-dialog");
  const passwordForm = document.querySelector("#password-form");
  document.querySelector("#cancel-password").addEventListener("click", () => passwordDialog.close());
  passwordForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await api(`/admin/usuarios/${passwordForm.elements.id.value}/password`, { method: "POST", body: JSON.stringify({ password: passwordForm.elements.password.value }) });
      passwordDialog.close();
      window.alert("Contraseña restablecida.");
    } catch (error) { window.alert(error.message || "No se pudo restablecer la contraseña."); }
  });

  const reportDialog = document.querySelector("#report-dialog");
  const reportForm = document.querySelector("#report-form");
  document.querySelector("#cancel-report").addEventListener("click", () => reportDialog.close());
  reportForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(reportForm);
    try {
      await api("/admin/informes", { method: "POST", body: JSON.stringify({
        contratoId: Number(data.get("contratoId")), titulo: String(data.get("titulo")).trim(),
        entidad: String(data.get("entidad")).trim() || null, clasificacion: String(data.get("clasificacion")),
        resumen: String(data.get("resumen")).trim(), contenido: String(data.get("contenido")),
        bajasCiviles: Number(data.get("bajasCiviles"))
      }) });
      reportDialog.close();
      await cargarModuloAdmin("informes");
    } catch (error) { window.alert(error.message || "No se pudo guardar el informe."); }
  });
}

function renderContenido(tipo) {
  const records = { potenciales, grupos, noticias: noticiasAdmin, zonas: zonasSeguras }[tipo];
  const listIds = {
    potenciales: "#potenciales-list",
    grupos: "#grupos-list",
    noticias: "#noticias-admin-list",
    zonas: "#zonas-list"
  };
  const list = document.querySelector(listIds[tipo]);
  if (!records.length) {
    list.replaceChildren(nodo("p", "empty", "No hay registros."));
    return;
  }
  const cards = records.map((item) => {
    const card = nodo("article", "request-item");
    const title = tipo === "potenciales" ? item.alias : tipo === "grupos" ? item.nombre : item.titulo ?? item.nombre;
    const detail = tipo === "potenciales"
      ? [item.nombreReal, item.estado, item.grupoNombre, item.habilidad].filter(Boolean).join(" · ")
      : tipo === "grupos" ? `${item.ciudad || ""} · ${item.pais} · ${item.miembros}/6 miembros`
        : tipo === "noticias" ? `${item.categoria} · ${item.publicada ? "Publicada" : "Borrador"}${item.destacada ? " · Destacada" : ""}`
          : `${item.tipo} · ${item.barrio || item.direccion || ""} · ${item.activa ? "Abierta" : "Cerrada"}`;
    card.append(nodo("div", "request-head", ""), nodo("strong", "request-code", title), nodo("p", "request-meta", detail));
    const actions = nodo("div", "admin-actions");
    actions.append(button("Editar", "btn", () => abrirEditor(tipo, item)));
    if (tipo === "potenciales") {
      const select = document.createElement("select");
      select.setAttribute("aria-label", `Grupo de ${item.alias}`);
      select.append(new Option("Sin grupo", ""), ...grupos.map((group) => new Option(group.nombre, group.id)));
      select.value = item.grupoId ?? "";
      actions.append(select, button("Guardar grupo", "btn", () => moverPotencial(item.id, select.value)));
    }
    card.append(actions);
    return card;
  });
  list.replaceChildren(...cards);
}

function abrirEditor(tipo, item = null) {
  const config = EDITORES[tipo];
  const dialog = document.querySelector("#content-dialog");
  const form = document.querySelector("#content-form");
  form.replaceChildren();
  form.dataset.type = tipo;
  form.append(nodo("p", "eyebrow", tipo === "potenciales" && !item ? "Alta de agente" : "Gestión del archivo"),
    nodo("h2", "", item ? `Editar · ${config.titulo}` : config.titulo));
  const id = document.createElement("input");
  id.name = "id";
  id.type = "hidden";
  id.value = item?.id ?? "";
  form.append(id);
  config.campos.forEach((field) => {
    if (field[4] === "reclutamiento" && item) return;
    let value = item?.[field[0]];
    if (field[0] === "fechaPublicacion" && value) value = value.slice(0, 16);
    if (field[0] === "grupoId" && value === null) value = "";
    form.append(campoEditor(field, value ?? ""));
  });
  const controls = nodo("div", "hero-actions");
  controls.append(button("Cancelar", "btn", () => dialog.close()));
  const submit = nodo("button", "btn btn-primary", "Guardar");
  submit.type = "submit";
  controls.append(submit);
  form.append(controls);
  dialog.showModal();
}

function editorPayload(tipo, formData, form) {
  const text = (name) => String(formData.get(name) ?? "").trim() || null;
  const number = (name) => text(name) === null ? null : Number(text(name));
  const flag = (name) => formData.has(name);
  if (tipo === "potenciales") {
    return {
      id: form.elements.id.value ? Number(form.elements.id.value) : null,
      alias: text("alias"), nombreReal: text("nombreReal"), username: text("username"), email: text("email"),
      password: text("password"), edad: number("edad"), habilidad: text("habilidad"), descripcion: text("descripcion"),
      nivel: number("nivel") || 1, estado: text("estado") || "DISPONIBLE", grupoId: number("grupoId"),
      barrio: text("barrio"), ciudad: text("ciudad"), pais: text("pais"), lat: number("lat"), lng: number("lng"),
      fondoInicial: number("fondoInicial"), fechaReclutamiento: new Date().toISOString().slice(0, 10)
    };
  }
  if (tipo === "grupos") return {
    id: form.elements.id.value ? Number(form.elements.id.value) : null,
    nombre: text("nombre"), pais: text("pais"), ciudad: text("ciudad"), zona: text("zona"),
    lat: number("lat"), lng: number("lng"), descripcion: text("descripcion"), activo: flag("activo")
  };
  if (tipo === "noticias") return {
    id: form.elements.id.value ? Number(form.elements.id.value) : null,
    titulo: text("titulo"), resumen: text("resumen"), contenido: text("contenido"), categoria: text("categoria"),
    imagenUrl: text("imagenUrl"), barrio: text("barrio"), incidenteId: number("incidenteId"),
    publicada: flag("publicada"), destacada: flag("destacada"),
    fechaPublicacion: text("fechaPublicacion") || new Date().toISOString().slice(0, 19)
  };
  return {
    id: form.elements.id.value ? Number(form.elements.id.value) : null,
    nombre: text("nombre"), tipo: text("tipo"), direccion: text("direccion"), barrio: text("barrio"),
    lat: number("lat"), lng: number("lng"), telefono: text("telefono"), horario: text("horario"), activa: flag("activa")
  };
}

async function guardarContenido(form) {
  const tipo = form.dataset.type;
  const data = new FormData(form);
  const submit = form.querySelector("button[type='submit']");
  submit.disabled = true;
  try {
    await api(EDITORES[tipo].endpoint, {
      method: "POST",
      body: JSON.stringify(editorPayload(tipo, data, form))
    });
    document.querySelector("#content-dialog").close();
    await cargarContenido(tipo);
  } catch (error) {
    window.alert(error.message || "No se pudo guardar el registro.");
  } finally {
    submit.disabled = false;
  }
}

async function moverPotencial(potencialId, grupoId) {
  try {
    await api(`/admin/potenciales/${potencialId}/grupo`, {
      method: "POST",
      body: JSON.stringify({ grupoId: grupoId ? Number(grupoId) : null })
    });
    await cargarContenido("potenciales");
  } catch (error) {
    window.alert(error.message || "No se pudo mover el potencial.");
  }
}

function bindContentAdmin() {
  const form = document.querySelector("#content-form");
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    guardarContenido(form);
  });
  document.querySelectorAll("[data-create]").forEach((control) => control.addEventListener("click", () => abrirEditor(control.dataset.create)));
}

async function cargarSolicitudes() {
  const list = document.querySelector("#solicitudes-list");
  list.replaceChildren(nodo("p", "empty", "Cargando avisos…"));
  try {
    solicitudes = await api("/admin/solicitudes");
    renderSolicitudes();
  } catch (error) {
    if (error.message.includes("administrador") || error.message.includes("autenticado")) {
      window.location.href = "admin-login.html";
      return;
    }
    list.replaceChildren(nodo("p", "alert error", error.message));
  }
}

async function cargarIncidentes() {
  const list = document.querySelector("#incidentes-list");
  list.replaceChildren(nodo("p", "empty", "Cargando incidentes…"));
  try {
    [incidentes, contratos] = await Promise.all([api("/admin/incidentes"), api("/admin/contratos")]);
    renderIncidentes();
  } catch (error) {
    if (error.message.includes("administrador") || error.message.includes("autenticado")) {
      window.location.href = "admin-login.html";
      return;
    }
    list.replaceChildren(nodo("p", "alert error", error.message));
  }
}

function renderIncidentes() {
  const list = document.querySelector("#incidentes-list");
  if (!incidentes.length) {
    list.replaceChildren(nodo("p", "empty", "No hay incidentes registrados."));
    return;
  }

  const cards = incidentes.map((item) => {
    const card = nodo("article", "request-item");
    const head = nodo("div", "request-head");
    const identity = nodo("div", "request-identity");
    identity.append(nodo("strong", "request-code", item.codigo), nodo("span", "tag cat", item.tipo));
    head.append(identity, nodo("span", `tag request-state state-${item.estado}`, item.estado.replaceAll("_", " ")));
    const details = nodo("p", "request-meta", [item.barrio, item.direccion, formatearFecha(item.fecha)].filter(Boolean).join(" · "));
    const description = nodo("p", "request-description", item.titulo);
    const publication = nodo("p", "request-meta", item.publicado ? "Publicado en la web" : "No publicado");
    const actions = nodo("div", "admin-actions");
    actions.append(button("Editar expediente", "btn", () => abrirEdicionIncidente(item)));
    const tieneContratoActivo = contratos.some((contract) => contract.incidenteId === item.id
      && !["COMPLETADO", "FALLIDO", "CANCELADO"].includes(contract.estado));
    if (!tieneContratoActivo && !["RESUELTO", "ARCHIVADO"].includes(item.estado)) {
      actions.append(button("Abrir contrato", "btn btn-primary", () => abrirCrearContrato(item.id)));
    }
    card.append(head, details, description, publication, actions);
    return card;
  });
  list.replaceChildren(...cards);
}

function abrirEdicionIncidente(item = null) {
async function cargarContratos() {
  const list = document.querySelector("#contratos-list");
  list.replaceChildren(nodo("p", "empty", "Cargando contratos…"));
  try {
    contratos = await api("/admin/contratos");
    renderContratos();
  } catch (error) {
    if (error.message.includes("administrador") || error.message.includes("autenticado")) {
      window.location.href = "admin-login.html";
      return;
    }
    list.replaceChildren(nodo("p", "alert error", error.message));
  }
}

function renderContratos() {
  const list = document.querySelector("#contratos-list");
  const filter = document.querySelector("#contratos-filtro").value;
  const abiertos = ["SOLICITADO", "ASIGNADO", "EN_CURSO", "PENDIENTE_REVISION"];
  const cerrados = ["COMPLETADO", "FALLIDO", "CANCELADO"];
  const visibles = contratos.filter((item) => {
    if (filter === "TODOS") return true;
    if (filter === "ABIERTOS") return abiertos.includes(item.estado);
    if (filter === "CERRADOS") return cerrados.includes(item.estado);
    return item.estado === filter;
  });
  document.querySelector("#contratos-count").textContent = `${visibles.length} contratos`;
  if (!visibles.length) {
    list.replaceChildren(nodo("p", "empty", "No hay contratos en este estado."));
    return;
  }

  const cards = visibles.map((item) => {
    const card = nodo("article", "request-item");
    const head = nodo("div", "request-head");
    head.append(nodo("strong", "request-code", item.codigo),
      nodo("span", `tag request-state state-${item.estado}`, item.estado.replaceAll("_", " ")));
    card.append(head,
      nodo("h3", "contract-title", item.incidenteTitulo),
      nodo("p", "request-meta", [item.incidenteCodigo, item.tipo, item.barrio, `Prioridad ${item.prioridad}`].filter(Boolean).join(" · ")),
      nodo("p", "request-meta", `Recompensa: ${item.recompensa} USD${item.grupoNombre ? ` · ${item.grupoNombre}` : ""}${item.potencialAlias ? ` · ${item.potencialAlias}` : ""}`));
    if (item.notasCampo) card.append(nodo("p", "request-response", `Informe de campo: ${item.notasCampo}`));

    const actions = nodo("div", "admin-actions");
    if (item.estado === "SOLICITADO") actions.append(button("Asignar", "btn btn-primary", () => abrirAsignacion(item)));
    if (["ASIGNADO", "EN_CURSO", "PENDIENTE_REVISION"].includes(item.estado)) {
      actions.append(
        button("Completar y pagar", "btn btn-primary", () => confirmarAccionContrato(item, "completar")),
        button("Marcar fallido", "btn", () => fallarContrato(item)),
        button("Cancelar", "btn btn-danger", () => confirmarAccionContrato(item, "cancelar"))
      );
    }
    if (actions.childElementCount) card.append(actions);
    list.append(card);
  });
  list.replaceChildren(...cards);
}

async function abrirAsignacion(item) {
  const form = document.querySelector("#asignar-form");
  try {
    [grupos, potencialesDisponibles] = await Promise.all([
      api("/admin/grupos"),
      api("/admin/potenciales-disponibles")
    ]);
    form.elements.contratoId.value = item.id;
    const groupSelect = form.elements.grupoId;
    groupSelect.replaceChildren(new Option("Selecciona grupo", ""), ...grupos
      .filter((group) => group.activo && group.miembros > 0)
      .map((group) => new Option(`${group.nombre} (${group.miembros}/6)`, group.id)));
    groupSelect.onchange = renderPotencialesDisponibles;
    renderPotencialesDisponibles();
    document.querySelector("#asignar-dialog").showModal();
  } catch (error) {
    window.alert(error.message || "No se pudieron cargar grupos y potenciales.");
  }
}

function renderPotencialesDisponibles() {
  const form = document.querySelector("#asignar-form");
  const groupId = Number(form.elements.grupoId.value);
  const select = form.elements.potencialId;
  select.replaceChildren(new Option("Selecciona potencial", ""), ...potencialesDisponibles
    .filter((person) => person.grupoId === groupId)
    .map((person) => new Option(`${person.alias} · ${person.estado}`, person.id)));
}

async function confirmarAccionContrato(item, accion) {
  const messages = {
    completar: `¿Completar ${item.codigo} y pagar ${item.recompensa} USD?`,
    cancelar: `¿Cancelar ${item.codigo}? Si estaba asignado, se reembolsará el transporte.`
  };
  if (!window.confirm(messages[accion])) return;
  try {
    await api(`/admin/contratos/${item.id}/${accion}`, { method: "POST" });
    await cargarContratos();
    await cargarDashboard();
  } catch (error) {
    window.alert(error.message || "No se pudo actualizar el contrato.");
  }
}

async function fallarContrato(item) {
  const respuesta = window.prompt("Motivo del fallo (opcional):", "");
  if (respuesta === null) return;
  try {
    await api(`/admin/contratos/${item.id}/fallido`, {
      method: "POST",
      body: JSON.stringify({ respuesta })
    });
    await cargarContratos();
  } catch (error) {
    window.alert(error.message || "No se pudo cerrar el contrato.");
  }
}

function abrirCrearContrato(incidenteId) {
  const form = document.querySelector("#crear-contrato-form");
  form.elements.incidenteId.value = incidenteId;
  document.querySelector("#crear-contrato-dialog").showModal();
}

function bindContratoForms() {
  const assignDialog = document.querySelector("#asignar-dialog");
  const assignForm = document.querySelector("#asignar-form");
  document.querySelector("#cancel-asignar").addEventListener("click", () => assignDialog.close());
  document.querySelector("#refresh-contratos").addEventListener("click", cargarContratos);
  document.querySelector("#contratos-filtro").addEventListener("change", renderContratos);

  assignForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submit = assignForm.querySelector("button[type='submit']");
    submit.disabled = true;
    try {
      const result = await api(`/admin/contratos/${assignForm.elements.contratoId.value}/asignar`, {
        method: "POST",
        body: JSON.stringify({
          grupoId: Number(assignForm.elements.grupoId.value),
          potencialId: Number(assignForm.elements.potencialId.value)
        })
      });
      assignDialog.close();
      window.alert(`Contrato asignado. Transporte: ${result.coste} USD · ${result.distanciaKm} km.`);
      await cargarContratos();
    } catch (error) {
      window.alert(error.message || "No se pudo asignar el contrato.");
    } finally {
      submit.disabled = false;
    }
  });

  const createDialog = document.querySelector("#crear-contrato-dialog");
  const createForm = document.querySelector("#crear-contrato-form");
  document.querySelector("#cancel-crear-contrato").addEventListener("click", () => createDialog.close());
  createForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(createForm);
    const submit = createForm.querySelector("button[type='submit']");
    submit.disabled = true;
    try {
      await api(`/admin/incidentes/${createForm.elements.incidenteId.value}/contrato`, {
        method: "POST",
        body: JSON.stringify({ prioridad: String(data.get("prioridad")), recompensa: Number(data.get("recompensa")) })
      });
      createDialog.close();
      await cargarIncidentes();
      await cargarContratos();
    } catch (error) {
      window.alert(error.message || "No se pudo crear el contrato.");
    } finally {
      submit.disabled = false;
    }
  });
}

  const dialog = document.querySelector("#incidente-dialog");
  const form = document.querySelector("#incidente-form");
  document.querySelector("#incidente-form-title").textContent = item ? "Editar incidente" : "Nuevo incidente";
  form.elements.id.value = item?.id ?? "";
  form.elements.titulo.value = item?.titulo ?? "";
  form.elements.tipo.value = item?.tipo ?? "SECUESTRO";
  form.elements.estado.value = item?.estado ?? "NO_VERIFICADO";
  form.elements.descripcionPublica.value = item?.descripcionPublica ?? "";
  form.elements.barrio.value = item?.barrio ?? "";
  form.elements.direccion.value = item?.direccion ?? "";
  form.elements.ciudad.value = item?.ciudad ?? "San Francisco";
  form.elements.pais.value = item?.pais ?? "Estados Unidos";
  form.elements.lat.value = item?.lat ?? "";
  form.elements.lng.value = item?.lng ?? "";
  form.elements.fecha.value = item?.fecha?.slice(0, 16) ?? new Date().toISOString().slice(0, 16);
  form.elements.nivelAmenaza.value = item?.nivelAmenaza ?? 2;
  form.elements.anomalia.value = item?.anomalia ?? "";
  form.elements.publicado.checked = item?.publicado ?? false;
  dialog.showModal();
}

function bindIncidenteForm() {
  const dialog = document.querySelector("#incidente-dialog");
  const form = document.querySelector("#incidente-form");
  document.querySelector("#new-incidente").addEventListener("click", () => abrirEdicionIncidente());
  document.querySelector("#cancel-incidente").addEventListener("click", () => dialog.close());
  document.querySelector("#refresh-incidentes").addEventListener("click", cargarIncidentes);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(form);
    const numberOrNull = (value) => value === "" ? null : Number(value);
    const payload = {
      id: form.elements.id.value ? Number(form.elements.id.value) : null,
      titulo: String(data.get("titulo")).trim(),
      tipo: String(data.get("tipo")),
      estado: String(data.get("estado")),
      descripcionPublica: String(data.get("descripcionPublica")).trim(),
      barrio: String(data.get("barrio")).trim() || null,
      direccion: String(data.get("direccion")).trim() || null,
      ciudad: String(data.get("ciudad")).trim(),
      pais: String(data.get("pais")).trim(),
      lat: numberOrNull(String(data.get("lat"))),
      lng: numberOrNull(String(data.get("lng"))),
      fecha: String(data.get("fecha")),
      nivelAmenaza: Number(data.get("nivelAmenaza")),
      anomalia: String(data.get("anomalia")).trim() || null,
      publicado: data.has("publicado")
    };
    const submit = form.querySelector("button[type='submit']");
    submit.disabled = true;
    try {
      await api("/admin/incidentes", { method: "POST", body: JSON.stringify(payload) });
      dialog.close();
      await cargarIncidentes();
      await cargarDashboard();
    } catch (error) {
      window.alert(error.message || "No se pudo guardar el incidente.");
    } finally {
      submit.disabled = false;
    }
  });
}

function renderSolicitudes() {
  const list = document.querySelector("#solicitudes-list");
  const filter = document.querySelector("#solicitudes-filtro").value;
  const visibles = filter === "TODAS" ? solicitudes : solicitudes.filter((item) => item.estado === filter);
  document.querySelector("#solicitudes-count").textContent = `${visibles.length} avisos`;

  if (!visibles.length) {
    list.replaceChildren(nodo("p", "empty", "No hay avisos en este estado."));
    return;
  }

  const cards = visibles.map((item) => {
    const card = nodo("article", "request-item");
    const head = nodo("div", "request-head");
    const identity = nodo("div", "request-identity");
    identity.append(nodo("strong", "request-code", item.codigo), nodo("span", "tag cat", item.tipo));
    head.append(identity, nodo("span", `tag request-state state-${item.estado}`, item.estado.replaceAll("_", " ")));
    const meta = nodo("p", "request-meta", [item.barrio, item.ubicacion, formatearFecha(item.recibidaEn)].filter(Boolean).join(" · "));
    const description = nodo("p", "request-description", item.descripcion);
    card.append(head, meta, description);
    if (item.contacto) card.append(nodo("p", "request-contact", `Contacto: ${item.contacto}`));
    if (item.respuestaPublica) card.append(nodo("p", "request-response", `Respuesta enviada: ${item.respuestaPublica}`));

    if (["PENDIENTE", "EN_REVISION"].includes(item.estado)) {
      const actions = nodo("div", "admin-actions");
      if (item.estado === "PENDIENTE") {
        actions.append(button("Poner en revisión", "btn", () => manejarAccion(item.id, "revision")));
      }
      actions.append(
        button("Convertir en incidente", "btn btn-primary", () => abrirConversion(item)),
        button("Descartar", "btn btn-danger", () => descartarSolicitud(item.id))
      );
      card.append(actions);
    }
    return card;
  });
  list.replaceChildren(...cards);
}

function button(label, className, action) {
  const element = nodo("button", className, label);
  element.type = "button";
  element.addEventListener("click", action);
  return element;
}

async function ejecutarAccion(id, accion, payload = {}) {
  await api(`/admin/solicitudes/${id}/${accion}`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
  await cargarSolicitudes();
}

async function manejarAccion(id, accion, payload = {}) {
  try {
    await ejecutarAccion(id, accion, payload);
  } catch (error) {
    window.alert(error.message || "No se pudo actualizar el aviso.");
  }
}

function descartarSolicitud(id) {
  if (!window.confirm("¿Descartar este aviso? El ciudadano verá una respuesta genérica.")) return;
  return manejarAccion(id, "descartar", { respuesta: "Gracias por tu aviso. No hemos encontrado indicios de un incidente." });
}

function abrirConversion(item) {
  const dialog = document.querySelector("#convert-dialog");
  const form = document.querySelector("#convert-form");
  dialog.dataset.solicitudId = item.id;
  form.elements.titulo.value = `${item.tipo[0]}${item.tipo.slice(1).toLowerCase()}${item.barrio ? ` en ${item.barrio}` : " en San Francisco"}`;
  dialog.showModal();
}

function bindConvertForm() {
  const dialog = document.querySelector("#convert-dialog");
  const form = document.querySelector("#convert-form");
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submit = form.querySelector("button[type='submit']");
    const formData = new FormData(form);
    submit.disabled = true;
    try {
      await ejecutarAccion(dialog.dataset.solicitudId, "convertir", {
        titulo: String(formData.get("titulo")).trim(),
        prioridad: String(formData.get("prioridad")),
        recompensa: Number(formData.get("recompensa")),
        publicar: formData.has("publicar")
      });
      dialog.close();
      await cargarDashboard();
    } catch (error) {
      window.alert(error.message || "No se pudo convertir el aviso.");
    } finally {
      submit.disabled = false;
    }
  });

  document.querySelector("#cancel-convert").addEventListener("click", () => dialog.close());
}

function bindAdminTabs() {
  const tabs = document.querySelectorAll("[data-admin-view]");
  tabs.forEach((tab) => tab.addEventListener("click", () => {
    const view = tab.dataset.adminView;
    tabs.forEach((item) => item.setAttribute("aria-pressed", String(item === tab)));
    document.querySelectorAll(".admin-view").forEach((section) => {
      section.hidden = section.id !== `view-${view}`;
    });
    if (view === "solicitudes") cargarSolicitudes();
    if (view === "incidentes") cargarIncidentes();
    if (view === "contratos") cargarContratos();
    if (EDITORES[view] || ["monederos", "informes", "usuarios", "actividad"].includes(view)) cargarModuloAdmin(view);
  }));
}

function bindLoginForm() {
  const form = document.querySelector("#login-form");
  if (!form) return;

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(form);
    const username = String(formData.get("username") || "").trim();
    const password = String(formData.get("password") || "");
    const button = form.querySelector("button[type='submit']");

    if (!username || !password) {
      setStatus("Usuario y contraseña son obligatorios.", false);
      return;
    }

    button.disabled = true;
    setStatus("Comprobando credenciales…", true);

    try {
      await api("/admin/login", {
        method: "POST",
        body: JSON.stringify({ username, password })
      });
      window.location.href = "admin.html";
    } catch (error) {
      setStatus(error.message || "No se pudo iniciar sesión.", false);
      button.disabled = false;
    }
  });
}

function bindLogout() {
  const button = document.querySelector("#logout-btn");
  if (!button) return;

  button.addEventListener("click", async () => {
    try {
      await api("/admin/logout", { method: "POST" });
      window.location.href = "admin-login.html";
    } catch {
      window.location.href = "admin-login.html";
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  bindLoginForm();
  bindLogout();
  if (document.body.dataset.page !== "admin") return;
  bindAdminTabs();
  bindConvertForm();
  bindIncidenteForm();
  bindContratoForms();
  bindContentAdmin();
  bindOperationsAdmin();
  document.querySelector("#refresh-solicitudes")?.addEventListener("click", cargarSolicitudes);
  document.querySelector("#solicitudes-filtro")?.addEventListener("change", renderSolicitudes);

  cargarDashboard();
});
