import {
	cerrarSesion,
	crearContrato,
	getContratosReporter,
	getIncidentesPublicos,
	obtenerSesion,
} from "./api.js";

const sesion = obtenerSesion();
if (!sesion || sesion.rol !== "REPORTER") {
	window.location.replace("access-denied.html");
}

const formulario = document.getElementById("contract-form");
const estadoFormulario = document.getElementById("contract-status");
const estadoContratos = document.getElementById("contracts-status");
const listaContratos = document.getElementById("contracts-list");
const seleccionarIncidente = document.getElementById("incident-id");
const botonEnviar = document.getElementById("contract-submit");
const botonCerrarSesion = document.getElementById("logout-button");
const incidentesPorId = new Map();

document.getElementById("reporter-name").textContent = sesion?.username ?? "";
botonCerrarSesion.addEventListener("click", cerrarSesionActual);
formulario.addEventListener("submit", enviarContrato);
await iniciarPanel();

/**
 * Carga incidentes públicos para asociarlos a contratos y consulta los contratos propios.
 */
async function iniciarPanel() {
	if (!sesion) {
		return;
	}
	try {
		const [incidentes] = await Promise.all([
			getIncidentesPublicos(),
			cargarContratos(),
		]);
		prepararIncidentes(incidentes);
	} catch (error) {
		mostrarEstado(estadoContratos, error.message, "error");
		mostrarOpcionVacia("No se pudo conectar con el archivo.");
	}
}

/**
 * Llena el selector con los incidentes públicos disponibles.
 *
 * @param {Array<object>} incidentes registros devueltos por la API
 */
function prepararIncidentes(incidentes) {
	seleccionarIncidente.replaceChildren();
	const opcionInicial = document.createElement("option");
	opcionInicial.value = "";
	opcionInicial.textContent = incidentes.length
		? "SELECCIONA UN INCIDENTE"
		: "NO HAY INCIDENTES PÚBLICOS";
	seleccionarIncidente.append(opcionInicial);

	incidentes.forEach((incidente) => {
		const opcion = document.createElement("option");
		opcion.value = String(incidente.idIncidente);
		opcion.textContent = [incidente.tipo, incidente.ciudad, incidente.pais]
			.filter(Boolean).join(" · ");
		incidentesPorId.set(opcion.value, incidente);
		seleccionarIncidente.append(opcion);
	});
	seleccionarIncidente.disabled = incidentes.length === 0;
}

/**
 * Envía un contrato vinculado al incidente y al reporter autenticado.
 *
 * @param {SubmitEvent} evento evento de envío del formulario
 */
async function enviarContrato(evento) {
	evento.preventDefault();
	const incidente = incidentesPorId.get(seleccionarIncidente.value);
	if (!incidente) {
		mostrarEstado(estadoFormulario, "Selecciona un incidente público.", "error");
		return;
	}

	const datos = new FormData(formulario);
	const valorDescripcion = datos.get("descripcionDetallada");
	const payload = {
		idIncidente: incidente.idIncidente,
		descripcionDetallada: typeof valorDescripcion === "string" ? valorDescripcion.trim() : "",
		ciudad: incidente.ciudad ?? "",
		pais: incidente.pais ?? "",
		zonaId: 0,
		nivelPeligro: Number(datos.get("nivelPeligro")),
		nivelDificultad: Number(datos.get("nivelDificultad")),
	};

	botonEnviar.disabled = true;
	mostrarEstado(estadoFormulario, "ENVIANDO SOLICITUD...", "");
	try {
		await crearContrato(payload, sesion.token);
		formulario.reset();
		mostrarEstado(estadoFormulario, "Contrato registrado correctamente.", "success");
		await cargarContratos();
	} catch (error) {
		mostrarEstado(estadoFormulario, error.message, "error");
	} finally {
		botonEnviar.disabled = false;
	}
}

/**
 * Recupera y renderiza los contratos del reporter actual.
 *
 * @returns {Promise<void>} finalización de la consulta
 */
async function cargarContratos() {
	const contratos = await getContratosReporter(sesion.token);
	listaContratos.replaceChildren();
	estadoContratos.textContent = `${contratos.length} CONTRATOS REGISTRADOS`;
	estadoContratos.dataset.kind = "";

	if (contratos.length === 0) {
		mostrarOpcionVacia("Todavía no tienes contratos registrados.");
		return;
	}

	contratos.forEach((contrato) => {
		const tarjeta = document.createElement("article");
		tarjeta.className = "contract-item";
		const titulo = document.createElement("strong");
		titulo.textContent = `CONTRATO #${contrato.idContrato}`;
		const estado = document.createElement("span");
		estado.className = "contract-state";
		estado.textContent = String(contrato.estado ?? "SOLICITADO").replaceAll("_", " ");
		const descripcion = document.createElement("span");
		descripcion.className = "contract-description";
		descripcion.textContent = [contrato.ciudad, contrato.pais].filter(Boolean).join(", ")
			|| "Ubicación no indicada";
		const recompensa = document.createElement("span");
		recompensa.textContent = `${Number(contrato.recompensaDinero ?? 0).toLocaleString("es")} $`;
		tarjeta.append(titulo, estado, descripcion, recompensa);
		listaContratos.append(tarjeta);
	});
}

/**
 * Presenta un estado vacío en el selector o en la lista de contratos.
 *
 * @param {string} mensaje mensaje que se muestra
 */
function mostrarOpcionVacia(mensaje) {
	const vacio = document.createElement("p");
	vacio.className = "empty-state";
	vacio.textContent = mensaje;
	listaContratos.replaceChildren(vacio);
}

/**
 * Actualiza el texto y el tipo de estado de un elemento.
 *
 * @param {HTMLElement} elemento elemento de estado
 * @param {string} mensaje mensaje para el usuario
 * @param {string} tipo tipo visual del estado
 */
function mostrarEstado(elemento, mensaje, tipo) {
	elemento.textContent = mensaje;
	elemento.dataset.kind = tipo;
}

/**
 * Invalida la sesión local y vuelve al feed público.
 */
async function cerrarSesionActual() {
	botonCerrarSesion.disabled = true;
	try {
		await cerrarSesion();
	} catch {
		localStorage.removeItem("sanfrancisco-archive.session");
	}
	window.location.assign("index.html");
}
