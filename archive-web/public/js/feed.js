import { getFeedPublico } from "./api.js";
import { CONFIG } from "./config.js";

/**
 * Conecta controles, descarga el feed y publica sus datos para el mapa.
 *
 * @param {(incidentes: Array<object>) => void} alActualizar callback para refrescar el mapa
 */
export function iniciarFeed(alActualizar) {
  const lista = document.getElementById("feed-list");
  const estado = document.getElementById("feed-status");
  const contador = document.getElementById("feed-count");
  const busqueda = document.getElementById("feed-search");
  const filtro = document.getElementById("feed-filter");
  const botonActualizar = document.getElementById("feed-refresh");
  let incidentes = [];

  /**
   * Aplica texto y país seleccionados a los incidentes cargados.
   */
  function filtrarFeed() {
    const consulta = normalizar(busqueda.value.trim());
    const paisSeleccionado = filtro.value;
    const filtrados = incidentes.filter((incidente) => {
      return (paisSeleccionado === "TODOS" || incidente.pais === paisSeleccionado)
        && normalizar(incidente.pais).includes(consulta);
    });
    const paises = new Set(filtrados.map((incidente) => incidente.pais)).size;
    contador.textContent = `${filtrados.length} EXPEDIENTES · ${paises} PAÍSES`;
    renderizarLista(lista, filtrados);
    alActualizar(filtrados);
  }

  function cargarPaises() {
    const seleccion = filtro.value;
    const paises = [...new Set(incidentes.map((incidente) => incidente.pais))]
      .filter(Boolean)
      .sort((a, b) => a.localeCompare(b, "es"));
    filtro.replaceChildren(new Option("TODOS LOS PAÍSES", "TODOS"));
    paises.forEach((pais) => filtro.add(new Option(pais, pais)));
    if (paises.includes(seleccion)) {
      filtro.value = seleccion;
    }
  }

  /**
   * Actualiza feed y mapa con una respuesta reciente de la API.
   */
  async function actualizar() {
    botonActualizar.disabled = true;
    estado.textContent = "CONSULTANDO ARCHIVO...";
    estado.classList.remove("status-error");
    try {
      incidentes = await getFeedPublico();
      cargarPaises();
      estado.textContent = CONFIG.API_BASE
        ? "CONEXIÓN API ACTIVA"
        : "SIMULACIÓN · CASOS FICTICIOS PARA DEMOSTRACIÓN";
      filtrarFeed();
    } catch (error) {
      estado.textContent = error.message;
      estado.classList.add("status-error");
      if (incidentes.length === 0) {
        contador.textContent = "SIN DATOS";
        renderizarVacio(lista, "No se pudieron cargar los registros.");
        document.getElementById("map-status").textContent = "API NO DISPONIBLE";
      }
    } finally {
      botonActualizar.disabled = false;
    }
  }

  busqueda.addEventListener("input", filtrarFeed);
  filtro.addEventListener("change", filtrarFeed);
  botonActualizar.addEventListener("click", actualizar);
  actualizar();
  if (CONFIG.API_BASE) {
    window.setInterval(actualizar, CONFIG.MAP_REFRESH_MS);
  }
}

/**
 * Renderiza las tarjetas del feed usando nodos de texto seguros.
 *
 * @param {HTMLElement} lista contenedor de tarjetas
 * @param {Array<object>} incidentes incidentes filtrados
 */
function renderizarLista(lista, incidentes) {
  lista.replaceChildren();
  if (incidentes.length === 0) {
    renderizarVacio(lista, "No hay registros que coincidan con el filtro.");
    return;
  }

  incidentes.forEach((incidente, indice) => {
    const tarjeta = document.createElement("article");
    tarjeta.className = "incident-entry";
    tarjeta.style.setProperty("--entry-index", indice);

    const tipo = document.createElement("span");
    tipo.className = `incident-type type-${String(incidente.tipo ?? "otro").toLowerCase()}`;
    tipo.textContent = String(incidente.tipo ?? "ANOMALÍA");

    const numero = document.createElement("span");
    numero.className = "incident-number";
    numero.textContent = incidente.folio ?? `#${String(incidente.idIncidente ?? incidente.id ?? "—").padStart(4, "0")}`;

    const ubicacion = document.createElement("h2");
    ubicacion.className = "incident-location";
    ubicacion.textContent = `${incidente.ciudad ?? "Ubicación no indicada"}, ${incidente.pais ?? "País no indicado"}`;

    const descripcion = document.createElement("p");
    descripcion.className = "incident-description";
    descripcion.textContent = incidente.descripcionCorta ?? "Sin descripción pública.";

    const detalle = document.createElement("details");
    detalle.className = "incident-details";
    const resumen = document.createElement("summary");
    resumen.textContent = "ABRIR EXPEDIENTE COMPLETO";
    const contenido = document.createElement("div");
    contenido.className = "incident-detail-content";
    const descripcionCompleta = document.createElement("p");
    descripcionCompleta.textContent = incidente.descripcionDetallada ?? incidente.descripcionCorta ?? "Sin descripción.";
    const datos = document.createElement("dl");
    agregarDato(datos, "REPORTE", `${formatearFecha(incidente.fechaReporte ?? incidente.fecha)} · ${incidente.horaLocal ?? "HORA NO REGISTRADA"}`);
    agregarDato(datos, "RIESGO", `${incidente.nivelPeligro ?? "—"} / 5`);
    agregarDato(datos, "ESTADO", incidente.estadoVerificacion ?? "SIMULADO");
    agregarDato(datos, "COORDENADAS", incidente.coordenadasTexto ?? `${incidente.lat}, ${incidente.lon}`);
    const listaEvidencias = document.createElement("ul");
    (incidente.evidencias ?? []).forEach((evidencia) => {
      const item = document.createElement("li");
      item.textContent = evidencia;
      listaEvidencias.append(item);
    });
    const respuesta = document.createElement("p");
    respuesta.textContent = `RESPUESTA · ${incidente.respuestaOficial ?? "Sin respuesta registrada."}`;
    contenido.append(descripcionCompleta, datos, listaEvidencias, respuesta);
    detalle.append(resumen, contenido);

    const pie = document.createElement("div");
    pie.className = "incident-meta";
    const fecha = document.createElement("time");
    fecha.textContent = formatearFecha(incidente.fechaReporte ?? incidente.fecha);
    const verificacion = document.createElement("span");
    verificacion.textContent = incidente.estadoVerificacion ?? "SIMULADO";
    pie.append(fecha, verificacion);
    tarjeta.append(tipo, numero, ubicacion, descripcion, detalle, pie);
    lista.append(tarjeta);
  });
}

function agregarDato(lista, etiqueta, valor) {
  const termino = document.createElement("dt");
  termino.textContent = etiqueta;
  const definicion = document.createElement("dd");
  definicion.textContent = valor;
  lista.append(termino, definicion);
}

function normalizar(texto) {
  return String(texto ?? "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("es");
}

/**
 * Muestra un estado vacío en el feed.
 *
 * @param {HTMLElement} lista contenedor del feed
 * @param {string} mensaje texto del estado vacío
 */
function renderizarVacio(lista, mensaje) {
  const estado = document.createElement("p");
  estado.className = "empty-state";
  estado.textContent = mensaje;
  lista.replaceChildren(estado);
}

/**
 * Formatea la fecha recibida desde MySQL o el conjunto mock local.
 *
 * @param {string|null|undefined} fecha fecha en formato ISO o SQL
 * @returns {string} fecha legible o marcador de dato ausente
 */
function formatearFecha(fecha) {
  if (!fecha) {
    return "FECHA NO INDICADA";
  }
  const coincidencia = String(fecha).match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!coincidencia) {
    return String(fecha);
  }
  const [, anio, mes, dia] = coincidencia;
  const fechaParseada = new Date(Date.UTC(Number(anio), Number(mes) - 1, Number(dia)));
  return new Intl.DateTimeFormat("es", { dateStyle: "medium", timeZone: "UTC" })
    .format(fechaParseada).toUpperCase();
}
