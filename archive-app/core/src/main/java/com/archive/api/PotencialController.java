package com.archive.api;

import com.archive.dao.PotencialDAO;
import com.archive.model.Potencial;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.SQLException;
import java.util.Map;

/**
 * Gestiona las rutas REST de administración de potenciales.
 */
public class PotencialController {
	private final PotencialDAO potencialDAO = new PotencialDAO();
	private final Gson gson = new Gson();
	private final AuthController authController;

	/**
	 * Construye el controlador de potenciales.
	 *
	 * @param authController controlador que valida las sesiones
	 */
	public PotencialController(AuthController authController) {
		this.authController = authController;
	}

	/**
	 * Registra las rutas REST de potenciales.
	 *
	 * @param aplicacion aplicación Javalin donde se registran las rutas
	 */
	public void registrarRutas(Javalin aplicacion) {
		aplicacion.get("/api/potenciales", this::listar);
		aplicacion.get("/api/potenciales/{id}", this::obtenerPorId);
		aplicacion.post("/api/potenciales", this::crear);
		aplicacion.put("/api/potenciales/{id}", this::actualizar);
		aplicacion.delete("/api/potenciales/{id}", this::eliminar);
	}

	/**
	 * Devuelve todos los potenciales a un administrador.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la consulta
	 */
	private void listar(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}
		contexto.json(potencialDAO.listar());
	}

	/**
	 * Devuelve un potencial por identificador a un administrador.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la consulta
	 */
	private void obtenerPorId(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}

		int idPotencial = Integer.parseInt(contexto.pathParam("id"));
		Potencial potencial = potencialDAO.obtenerPorId(idPotencial);
		if (potencial == null) {
			contexto.status(404).json(Map.of("error", "Potencial no encontrado."));
			return;
		}
		contexto.json(potencial);
	}

	/**
	 * Crea un potencial bajo control de un administrador.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la inserción
	 */
	private void crear(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}

		Potencial potencial;
		try {
			potencial = gson.fromJson(contexto.body(), Potencial.class);
		} catch (JsonSyntaxException error) {
			contexto.status(400).json(Map.of("error", "Cuerpo JSON no válido."));
			return;
		}

		if (potencial == null || potencial.getNombre() == null
				|| potencial.getNombre().isBlank()) {
			contexto.status(400).json(Map.of("error", "El nombre del potencial es obligatorio."));
			return;
		}

		potencial.setIdPotencial(0);
		if (potencial.getEstado() == null) {
			potencial.setEstado("EN_BASE");
		}
		potencialDAO.crear(potencial);
		contexto.status(201).json(potencial);
	}

	/**
	 * Actualiza un potencial bajo control de un administrador.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la actualización
	 */
	private void actualizar(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}

		Potencial potencial;
		try {
			potencial = gson.fromJson(contexto.body(), Potencial.class);
		} catch (JsonSyntaxException error) {
			contexto.status(400).json(Map.of("error", "Cuerpo JSON no válido."));
			return;
		}

		if (potencial == null) {
			contexto.status(400).json(Map.of("error", "Se requieren datos del potencial."));
			return;
		}
		potencial.setIdPotencial(Integer.parseInt(contexto.pathParam("id")));
		if (!potencialDAO.actualizar(potencial)) {
			contexto.status(404).json(Map.of("error", "Potencial no encontrado."));
			return;
		}
		contexto.json(potencial);
	}

	/**
	 * Elimina un potencial bajo control de un administrador.
	 *
	 * @param contexto contexto HTTP de la solicitud
	 * @throws SQLException si falla la eliminación
	 */
	private void eliminar(Context contexto) throws SQLException {
		if (!authController.requiereRol(contexto, "ADMIN")) {
			return;
		}

		int idPotencial = Integer.parseInt(contexto.pathParam("id"));
		if (!potencialDAO.eliminar(idPotencial)) {
			contexto.status(404).json(Map.of("error", "Potencial no encontrado."));
			return;
		}
		contexto.status(204);
	}
}
