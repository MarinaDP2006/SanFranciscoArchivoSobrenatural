import { CONFIG } from "./config.js";

const CLAVE_SESION = "sanfrancisco-archive.session";

/**
 * Recupera el feed de incidentes públicos.
 *
 * @returns {Promise<Array<object>>} incidentes visibles públicamente
 */
export function getFeedPublico() {
  if (!CONFIG.API_BASE) {
    return fetch(new URL("../data/mock-incidents.json", import.meta.url))
      .then((respuesta) => {
        if (!respuesta.ok) {
          throw new Error(`No se pudieron cargar los registros de ejemplo (HTTP ${respuesta.status}).`);
        }
        return respuesta.json();
      });
  }
  return solicitar("/incidentes/publicos");
}

/**
 * Inicia sesión y guarda el token de sesión en el navegador.
 *
 * @param {string} username nombre de usuario
 * @param {string} password contraseña
 * @returns {Promise<object>} sesión devuelta por la API
 */
export async function iniciarSesion(username, password) {
  const sesion = await solicitar("/auth/login", {
    method: "POST",
    body: { username, password },
  });
  if (!sesion?.token) {
    throw new Error("La API no devolvió un token de sesión.");
  }
  localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
  return sesion;
}

/**
 * Devuelve la sesión local si existe y contiene un token.
 *
 * @returns {object|null} sesión almacenada o null
 */
export function obtenerSesion() {
  try {
    const sesion = JSON.parse(localStorage.getItem(CLAVE_SESION) ?? "null");
    return sesion?.token ? sesion : null;
  } catch {
    localStorage.removeItem(CLAVE_SESION);
    return null;
  }
}

/**
 * Invalida la sesión en la API y elimina la copia local.
 *
 * @returns {Promise<void>} finalización del cierre de sesión
 */
export async function cerrarSesion() {
  const sesion = obtenerSesion();
  try {
    if (sesion) {
      await solicitar("/auth/logout", { method: "POST", token: sesion.token });
    }
  } finally {
    localStorage.removeItem(CLAVE_SESION);
  }
}

/**
 * Recupera los contratos del reporter autenticado.
 *
 * @param {string} token token Bearer de la sesión
 * @returns {Promise<Array<object>>} contratos del usuario
 */
export function getContratosReporter(token) {
  return solicitar("/contratos/mios", { token });
}

/**
 * Recupera los incidentes públicos que pueden asociarse a un contrato.
 *
 * @returns {Promise<Array<object>>} incidentes visibles
 */
export function getIncidentesPublicos() {
  return getFeedPublico();
}

/**
 * Crea un contrato para el reporter autenticado.
 *
 * @param {object} payload datos del contrato
 * @param {string} token token Bearer de la sesión
 * @returns {Promise<object>} contrato creado
 */
export function crearContrato(payload, token) {
  return solicitar("/contratos", { method: "POST", token, body: payload });
}

/**
 * Envía una solicitud HTTP a la API local y procesa su respuesta.
 *
 * @param {string} ruta ruta relativa al prefijo /api
 * @param {{method?: string, token?: string, body?: object}} opciones opciones de la solicitud
 * @returns {Promise<any>} objeto JSON de respuesta, o null si no hay cuerpo
 */
async function solicitar(ruta, opciones = {}) {
  if (!CONFIG.API_BASE) {
    throw new Error("La API no está configurada. El acceso reporter requiere un servidor API.");
  }

  const cabeceras = { Accept: "application/json" };
  if (opciones.body !== undefined) {
    cabeceras["Content-Type"] = "application/json";
  }
  if (opciones.token) {
    cabeceras.Authorization = `Bearer ${opciones.token}`;
  }

  let respuesta;
  try {
    respuesta = await fetch(`${CONFIG.API_BASE}${ruta}`, {
      method: opciones.method ?? "GET",
      headers: cabeceras,
      ...(opciones.body === undefined ? {} : { body: JSON.stringify(opciones.body) }),
    });
  } catch (error) {
    throw new Error("No se pudo conectar con la API configurada.", { cause: error });
  }

  if (respuesta.status === 204) {
    return null;
  }

  const tipoContenido = respuesta.headers.get("content-type") ?? "";
  const cuerpo = tipoContenido.includes("application/json")
    ? await respuesta.json()
    : await respuesta.text();

  if (!respuesta.ok) {
    const detalle = typeof cuerpo === "object" ? cuerpo?.error : cuerpo;
    throw new Error(detalle || `La API respondió HTTP ${respuesta.status}.`);
  }
  return cuerpo;
}
