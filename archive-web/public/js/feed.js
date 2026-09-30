import { getFeedPublico } from "./api.js";
import { CONFIG } from "./config.js";

const NOMBRES_TIPO = {
  DESAPARICION: "DESAPARICIÓN",
  SECUESTRO: "SECUESTRO",
  ASESINATO: "ASESINATO",
};

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
   * Aplica texto y tipo seleccionados a los incidentes cargados.
   */
  function filtrarFeed() {
    const consulta = busqueda.value.trim().toLocaleLowerCase("es");
    const tipoSeleccionado = filtro.value;
    const filtrados = incidentes.filter((incidente) => {
      const texto = [incidente.tipo, incidente.ciudad, incidente.pais]
        .filter(Boolean).join(" ").toLocaleLowerCase("es");
      return (tipoSeleccionado === "TODOS" || incidente.tipo === tipoSeleccionado)
        && texto.includes(consulta);
    });
    contador.textContent = `${filtrados.length} REGISTROS`;
    renderizarLista(lista, filtrados);
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
      alActualizar(incidentes);
      estado.textContent = "CONEXIÓN API ACTIVA";
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
  window.setInterval(actualizar, CONFIG.MAP_REFRESH_MS);
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
    tipo.textContent = NOMBRES_TIPO[incidente.tipo] ?? String(incidente.tipo ?? "INCIDENTE");

    const numero = document.createElement("span");
    numero.className = "incident-number";
    numero.textContent = `#${String(incidente.idIncidente ?? incidente.id ?? "—").padStart(4, "0")}`;

    const ubicacion = document.createElement("h2");
    ubicacion.className = "incident-location";
    ubicacion.textContent = [incidente.ciudad, incidente.pais].filter(Boolean).join(", ")
      || "Ubicación no indicada";

    const descripcion = document.createElement("p");
    descripcion.className = "incident-description";
    descripcion.textContent = incidente.descripcionCorta ?? "Sin descripción pública.";

    const pie = document.createElement("div");
    pie.className = "incident-meta";
    const fecha = document.createElement("time");
    fecha.textContent = formatearFecha(incidente.fechaReporte ?? incidente.fecha);
    const verificacion = document.createElement("span");
    verificacion.textContent = incidente.estadoVerificacion === "VERIFICADO"
      ? "VERIFICADO" : "PENDIENTE";
    pie.append(fecha, verificacion);
    tarjeta.append(tipo, numero, ubicacion, descripcion, pie);
    lista.append(tarjeta);
  });
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
