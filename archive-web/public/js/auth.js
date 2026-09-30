import { iniciarSesion, obtenerSesion } from "./api.js";

const formulario = document.getElementById("login-form");
const estado = document.getElementById("login-status");
const boton = document.getElementById("login-submit");

/**
 * Envía las credenciales a la API y dirige al usuario según su rol.
 *
 * @param {SubmitEvent} evento evento de envío del formulario
 */
formulario.addEventListener("submit", async (evento) => {
	evento.preventDefault();
	const datos = new FormData(formulario);
	const valorUsername = datos.get("username");
	const valorPassword = datos.get("password");
	const username = typeof valorUsername === "string" ? valorUsername.trim() : "";
	const password = typeof valorPassword === "string" ? valorPassword : "";
	if (!username || !password) {
		mostrarEstado("Completa el usuario y la contraseña.", "error");
		return;
	}

	boton.disabled = true;
	boton.textContent = "VALIDANDO...";
	mostrarEstado("Conectando con la API local...", "");
	try {
		const sesion = await iniciarSesion(username, password);
		if (sesion.rol !== "REPORTER") {
			localStorage.removeItem("sanfrancisco-archive.session");
			window.location.assign("access-denied.html");
			return;
		}
		window.location.assign("reporter-dashboard.html");
	} catch (error) {
		mostrarEstado(error.message, "error");
		boton.disabled = false;
		boton.textContent = "INICIAR SESIÓN";
	}
});

/**
 * Presenta el estado del inicio de sesión con un nivel accesible.
 *
 * @param {string} mensaje texto que se muestra
 * @param {string} tipo categoría de estado
 */
function mostrarEstado(mensaje, tipo) {
	estado.textContent = mensaje;
	estado.dataset.kind = tipo;
}

const sesionExistente = obtenerSesion();
if (sesionExistente?.rol === "REPORTER") {
	window.location.replace("reporter-dashboard.html");
}
