package com.archive.desktop.service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para las operaciones usadas por el escritorio administrativo.
 */
public class ApiClient {
	private static final String URL_API_PREDETERMINADA = "http://localhost:8080/api";

	private final String urlBase;
	private final HttpClient clienteHttp;
	private final Gson gson;

	/**
	 * Crea el cliente con la URL local de la API.
	 */
	public ApiClient() {
		this(URL_API_PREDETERMINADA);
	}

	/**
	 * Crea un cliente que utiliza la URL indicada.
	 *
	 * @param urlBase URL base de la API, sin barra final
	 */
	public ApiClient(String urlBase) {
		if (urlBase == null || urlBase.isBlank()) {
			throw new IllegalArgumentException("La URL base de la API es obligatoria.");
		}
		this.urlBase = quitarBarraFinal(urlBase.trim());
		this.clienteHttp = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		this.gson = new Gson();
	}

	/**
	 * Inicia sesión y devuelve el token generado por la API.
	 *
	 * @param username nombre de usuario
	 * @param password contraseña
	 * @return sesión autenticada
	 * @throws IOException si la API responde con error o no se puede alcanzar
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	public Sesion iniciarSesion(String username, String password)
			throws IOException, InterruptedException {
		String respuesta = enviar("POST", "/auth/login", null,
				Map.of("username", username, "password", password));
		try {
			Sesion sesion = gson.fromJson(respuesta, Sesion.class);
			if (sesion == null || sesion.token() == null || sesion.token().isBlank()) {
				throw new IOException("La API no devolvió un token de sesión.");
			}
			return sesion;
		} catch (JsonParseException error) {
			throw new IOException("La respuesta de inicio de sesión no es válida.", error);
		}
	}

	/**
	 * Cierra la sesión asociada al token.
	 *
	 * @param token token Bearer de la sesión actual
	 * @throws IOException si la API responde con error o no se puede alcanzar
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	public void cerrarSesion(String token) throws IOException, InterruptedException {
		enviar("POST", "/auth/logout", token, null);
	}

	/**
	 * Recupera el feed público de incidentes.
	 *
	 * @return incidentes visibles sin autenticación
	 * @throws IOException si la API responde con error o no se puede alcanzar
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	public List<Incidente> obtenerIncidentesPublicos() throws IOException, InterruptedException {
		return obtenerLista("/incidentes/publicos", null, Incidente[].class);
	}

	/**
	 * Recupera todos los incidentes para el panel administrativo.
	 *
	 * @param token token Bearer del administrador
	 * @return incidentes registrados
	 * @throws IOException si la API responde con error o no se puede alcanzar
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	public List<Incidente> obtenerIncidentes(String token)
			throws IOException, InterruptedException {
		return obtenerLista("/incidentes", token, Incidente[].class);
	}

	/**
	 * Recupera los contratos para el panel administrativo.
	 *
	 * @param token token Bearer del administrador
	 * @return contratos registrados
	 * @throws IOException si la API responde con error o no se puede alcanzar
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	public List<Contrato> obtenerContratos(String token) throws IOException, InterruptedException {
		return obtenerLista("/contratos", token, Contrato[].class);
	}

	/**
	 * Recupera los potenciales para el panel administrativo.
	 *
	 * @param token token Bearer del administrador
	 * @return potenciales registrados
	 * @throws IOException si la API responde con error o no se puede alcanzar
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	public List<Potencial> obtenerPotenciales(String token)
			throws IOException, InterruptedException {
		return obtenerLista("/potenciales", token, Potencial[].class);
	}

	/**
	 * Solicita la finalización de un contrato como administrador.
	 *
	 * @param idContrato identificador del contrato
	 * @param token token Bearer del administrador
	 * @throws IOException si la API responde con error o no se puede alcanzar
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	public void completarContrato(int idContrato, String token)
			throws IOException, InterruptedException {
		enviar("POST", "/contratos/" + idContrato + "/completar", token, Map.of());
	}

	/**
	 * Recupera una lista JSON y la convierte al tipo de elemento solicitado.
	 *
	 * @param ruta ruta relativa a la URL base
	 * @param token token Bearer opcional
	 * @param tipoArray clase del array JSON que se espera
	 * @param <T> tipo de elemento de la respuesta
	 * @return lista de elementos; vacía si la respuesta es null
	 * @throws IOException si falla la solicitud o el formato de la respuesta
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	private <T> List<T> obtenerLista(String ruta, String token, Class<T[]> tipoArray)
			throws IOException, InterruptedException {
		String respuesta = enviar("GET", ruta, token, null);
		try {
			T[] elementos = gson.fromJson(respuesta, tipoArray);
			return elementos == null ? List.of() : List.of(elementos);
		} catch (JsonParseException error) {
			throw new IOException("La respuesta de la API no contiene una lista válida.", error);
		}
	}

	/**
	 * Envía una solicitud HTTP y convierte respuestas no exitosas en IOException.
	 *
	 * @param metodo método HTTP
	 * @param ruta ruta relativa a la API
	 * @param token token Bearer opcional
	 * @param cuerpo objeto que se serializa como JSON, o null si no hay cuerpo
	 * @return cuerpo de respuesta, que puede estar vacío en respuestas 204
	 * @throws IOException si falla la comunicación o el servidor devuelve error
	 * @throws InterruptedException si se interrumpe la solicitud
	 */
	private String enviar(String metodo, String ruta, String token, Object cuerpo)
			throws IOException, InterruptedException {
		HttpRequest.Builder solicitud = HttpRequest.newBuilder()
				.uri(URI.create(urlBase + ruta))
				.timeout(Duration.ofSeconds(12))
				.header("Accept", "application/json");

		if (token != null && !token.isBlank()) {
			solicitud.header("Authorization", "Bearer " + token);
		}
		if (cuerpo == null) {
			solicitud.method(metodo, HttpRequest.BodyPublishers.noBody());
		} else {
			solicitud.header("Content-Type", "application/json; charset=utf-8")
					.method(metodo, HttpRequest.BodyPublishers.ofString(gson.toJson(cuerpo)));
		}

		HttpResponse<String> respuesta = clienteHttp.send(
				solicitud.build(), HttpResponse.BodyHandlers.ofString());
		int estado = respuesta.statusCode();
		if (estado < 200 || estado >= 300) {
			throw new IOException("API HTTP " + estado + ": " + extraerError(respuesta.body()));
		}
		return respuesta.body();
	}

	/**
	 * Extrae el mensaje de error JSON o devuelve el cuerpo sin procesar.
	 *
	 * @param cuerpo cuerpo devuelto por el servidor
	 * @return mensaje breve de error
	 */
	private String extraerError(String cuerpo) {
		if (cuerpo == null || cuerpo.isBlank()) {
			return "sin detalle";
		}
		try {
			MensajeError mensaje = gson.fromJson(cuerpo, MensajeError.class);
			return mensaje == null || mensaje.error() == null ? cuerpo : mensaje.error();
		} catch (JsonParseException error) {
			return cuerpo;
		}
	}

	/**
	 * Elimina barras finales de una URL base para construir rutas uniformes.
	 *
	 * @param url URL que se normaliza
	 * @return URL sin barras finales
	 */
	private String quitarBarraFinal(String url) {
		int fin = url.length();
		while (fin > 0 && url.charAt(fin - 1) == '/') {
			fin--;
		}
		return url.substring(0, fin);
	}

	/**
	 * Datos de sesión devueltos por el endpoint de login.
	 *
	 * @param token token Bearer de autenticación
	 * @param idUsuario identificador de la cuenta
	 * @param username nombre de usuario
	 * @param rol rol asignado por la API
	 */
	public record Sesion(String token, int idUsuario, String username, String rol) {
	}

	/**
	 * Datos de incidente usados por el dashboard.
	 *
	 * @param idIncidente identificador del incidente
	 * @param tipo categoría del incidente
	 * @param descripcionCorta descripción pública
	 * @param ciudad ciudad donde ocurrió
	 * @param pais país donde ocurrió
	 * @param lat latitud del incidente
	 * @param lon longitud del incidente
	 * @param estadoVerificacion estado de verificación
	 */
	public record Incidente(int idIncidente, String tipo, String descripcionCorta,
													String ciudad, String pais, double lat, double lon,
													String estadoVerificacion) {
	}

	/**
	 * Datos de contrato usados por el dashboard.
	 *
	 * @param idContrato identificador del contrato
	 * @param idIncidente identificador del incidente asociado
	 * @param ciudad ciudad del contrato
	 * @param pais país del contrato
	 * @param nivelPeligro nivel de peligro
	 * @param recompensaDinero recompensa asignada
	 * @param estado estado actual del contrato
	 */
	public record Contrato(int idContrato, int idIncidente, String ciudad, String pais,
												 int nivelPeligro, double recompensaDinero, String estado) {
	}

	/**
	 * Datos de potencial usados por el dashboard.
	 *
	 * @param idPotencial identificador del potencial
	 * @param nombre nombre del potencial
	 * @param estado estado operativo
	 * @param ciudadOrigen ciudad de origen
	 * @param paisActual país actual
	 */
	public record Potencial(int idPotencial, String nombre, String estado,
													String ciudadOrigen, String paisActual) {
	}

	/**
	 * Representa el mensaje de error habitual de la API.
	 *
	 * @param error detalle de error devuelto por el backend
	 */
	private record MensajeError(String error) {
	}
}
