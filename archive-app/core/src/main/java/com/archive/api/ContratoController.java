package com.archive.api;

import com.archive.dao.ContratoAnomaliaDAO;
import com.archive.model.ContratoAnomalia;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.SQLException;
import java.util.Map;

/**
 * Gestiona las rutas REST de contratos de anomalía.
 */
public class ContratoController {
	private final ContratoAnomaliaDAO contratoDAO = new ContratoAnomaliaDAO();
	private final Gson gson = new Gson();
	private final AuthController authController;

	/**
	 * Construye el controlador de contratos.
	 *
	 * @param authController controlador que valida las sesiones
	 */
	public ContratoController(AuthController authController) {
		this.authController = authController;
	}

	/**
	 * Registra las rutas REST de contratos.
	 *
	 * @param aplicacion aplicación Javalin donde se registran las rutas
	 */
	public void registrarRutas(Javalin aplicacion) {
		aplicacion.get("/api/contratos/mios", this::listarMios);
		aplicacion.get("/api/contratos", this::listarTodos);
		aplicacion.get("/api/contratos/{id}", this::obtenerPorId);
		aplicacion.post("/api/contratos", this::crear);
		aplicacion.post("/api/contratos/{id}/completar", this::completar);
	}

	/**
	 * Devuelve los contratos del reporter autenticado.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la consulta
	 */
	private void listarMios(Context contexto) throws SQLException {
		AuthController.Sesion sesion = authController.obtenerSesion(contexto);
		if (sesion == null) {
			return;
		}
		if (!"REPORTER".equalsIgnoreCase(sesion.rol())) {
			contexto.status(403).json(Map.of("error", "ACCESS DENIED"));
			return;
		}
		contexto.json(contratoDAO.listarPorSolicitante(sesion.idUsuario()));
	}

	/**
	 * Devuelve todos los contratos a un administrador.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la consulta
	 */
	private void listarTodos(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}
		contexto.json(contratoDAO.listar());
	}

	/**
	 * Devuelve un contrato al administrador o al reporter solicitante.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la consulta
	 */
	private void obtenerPorId(Context contexto) throws SQLException {
		AuthController.Sesion sesion = authController.obtenerSesion(contexto);
		if (sesion == null) {
			return;
		}

		int idContrato = Integer.parseInt(contexto.pathParam("id"));
		ContratoAnomalia contrato = contratoDAO.obtenerPorId(idContrato);
		if (contrato == null) {
			contexto.status(404).json(Map.of("error", "Contrato no encontrado."));
			return;
		}

		boolean esAdmin = "ADMIN".equalsIgnoreCase(sesion.rol());
		boolean esSolicitante = "REPORTER".equalsIgnoreCase(sesion.rol())
				&& contrato.getIdUsuarioSolicitante() == sesion.idUsuario();
		if (!esAdmin && !esSolicitante) {
			contexto.status(403).json(Map.of("error", "ACCESS DENIED"));
			return;
		}
		contexto.json(contrato);
	}

	/**
	 * Crea un contrato asociado al reporter autenticado.
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

		ContratoAnomalia contrato;
		try {
			contrato = gson.fromJson(contexto.body(), ContratoAnomalia.class);
		} catch (JsonSyntaxException error) {
			contexto.status(400).json(Map.of("error", "Cuerpo JSON no válido."));
			return;
		}

		if (contrato == null || contrato.getIdIncidente() <= 0
				|| contrato.getNivelPeligro() < 1 || contrato.getNivelPeligro() > 5
				|| contrato.getNivelDificultad() < 1 || contrato.getNivelDificultad() > 5) {
			contexto.status(400).json(Map.of("error", "Los datos del contrato no son válidos."));
			return;
		}

		contrato.setIdContrato(0);
		contrato.setIdUsuarioSolicitante(sesion.idUsuario());
		contrato.setIdPotencialAsignada(null);
		contrato.setEstado("SOLICITADO");
		contrato.calcularRecompensa();
		contratoDAO.crear(contrato);
		contexto.status(201).json(contrato);
	}

	/**
	 * Completa un contrato y registra su recompensa en el monedero.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la transacción
	 */
	private void completar(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}

		int idContrato = Integer.parseInt(contexto.pathParam("id"));
		contratoDAO.completarContrato(idContrato);
		contexto.json(Map.of("mensaje", "Contrato completado y recompensa abonada."));
	}
}
