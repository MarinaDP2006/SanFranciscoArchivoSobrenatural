package com.archive.api;

import com.archive.dao.IncidentePublicoDAO;
import com.archive.model.IncidentePublico;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * Gestiona las rutas REST de incidentes públicos.
 */
public class IncidenteController {
	private static final Set<String> TIPOS_VALIDOS =
			Set.of("SECUESTRO", "ASESINATO", "DESAPARICION");

	private final IncidentePublicoDAO incidenteDAO = new IncidentePublicoDAO();
	private final Gson gson = new Gson();
	private final AuthController authController;

	/**
	 * Construye el controlador de incidentes.
	 *
	 * @param authController controlador que valida las sesiones
	 */
	public IncidenteController(AuthController authController) {
		this.authController = authController;
	}

	/**
	 * Registra las rutas REST de incidentes.
	 *
	 * @param aplicacion aplicación Javalin donde se registran las rutas
	 */
	public void registrarRutas(Javalin aplicacion) {
		aplicacion.get("/api/incidentes/publicos", this::listarPublicos);
		aplicacion.get("/api/incidentes", this::listarTodos);
		aplicacion.get("/api/incidentes/{id}", this::obtenerPorId);
		aplicacion.post("/api/incidentes", this::crear);
		aplicacion.patch("/api/incidentes/{id}/verificacion", this::actualizarVerificacion);
	}

	/**
	 * Devuelve los incidentes visibles sin iniciar sesión.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la consulta
	 */
	private void listarPublicos(Context contexto) throws SQLException {
		contexto.json(incidenteDAO.listarVisiblesSinLogin());
	}

	/**
	 * Devuelve todos los incidentes a un administrador.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la consulta
	 */
	private void listarTodos(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}
		contexto.json(incidenteDAO.listar());
	}

	/**
	 * Devuelve un incidente público o permite a un administrador consultar cualquiera.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la consulta
	 */
	private void obtenerPorId(Context contexto) throws SQLException {
		int idIncidente = Integer.parseInt(contexto.pathParam("id"));
		IncidentePublico incidente = incidenteDAO.obtenerPorId(idIncidente);
		if (incidente == null) {
			contexto.status(404).json(Map.of("error", "Incidente no encontrado."));
			return;
		}
		if (!incidente.isEsVisibleSinLogin()
				&& !authController.requiereRol(contexto, "ADMIN")) {
			return;
		}
		contexto.json(incidente);
	}

	/**
	 * Crea un incidente asociado al reporter autenticado.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la inserción
	 */
	private void crear(Context contexto) throws SQLException {
		AuthController.Sesion sesion = authController.obtenerSesion(contexto);
		if (sesion == null) {
			return;
		}
		if (!"REPORTER".equalsIgnoreCase(sesion.rol())) {
			contexto.status(403).json(Map.of("error", "ACCESS DENIED"));
			return;
		}

		IncidentePublico incidente;
		try {
			incidente = gson.fromJson(contexto.body(), IncidentePublico.class);
		} catch (JsonSyntaxException error) {
			contexto.status(400).json(Map.of("error", "Cuerpo JSON no válido."));
			return;
		}

		if (incidente == null || incidente.getTipo() == null
				|| !TIPOS_VALIDOS.contains(incidente.getTipo())) {
			contexto.status(400).json(Map.of("error", "El tipo de incidente no es válido."));
			return;
		}

		incidente.setIdIncidente(0);
		incidente.setIdUsuarioReporter(sesion.idUsuario());
		incidente.setEstadoVerificacion("NO_VERIFICADO");
		incidente.setEsVisibleSinLogin(true);
		incidenteDAO.crear(incidente);
		contexto.status(201).json(incidente);
	}

	/**
	 * Actualiza el estado de verificación de un incidente como administrador.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la actualización
	 */
	private void actualizarVerificacion(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}

		SolicitudVerificacion solicitud;
		try {
			solicitud = gson.fromJson(contexto.body(), SolicitudVerificacion.class);
		} catch (JsonSyntaxException error) {
			contexto.status(400).json(Map.of("error", "Cuerpo JSON no válido."));
			return;
		}

		if (solicitud == null || solicitud.estado == null
				|| !Set.of("NO_VERIFICADO", "VERIFICADO").contains(solicitud.estado)) {
			contexto.status(400).json(Map.of("error", "El estado de verificación no es válido."));
			return;
		}

		int idIncidente = Integer.parseInt(contexto.pathParam("id"));
		if (!incidenteDAO.actualizarEstadoVerificacion(idIncidente, solicitud.estado)) {
			contexto.status(404).json(Map.of("error", "Incidente no encontrado."));
			return;
		}
		contexto.status(204);
	}

	/**
	 * Representa el estado solicitado para un incidente.
	 */
	private static final class SolicitudVerificacion {
		private String estado;
	}
}
