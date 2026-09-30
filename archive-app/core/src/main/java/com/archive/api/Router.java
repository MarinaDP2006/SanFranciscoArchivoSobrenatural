package com.archive.api;

import java.sql.SQLException;
import java.util.Map;

import com.google.gson.JsonSyntaxException;

import io.javalin.Javalin;

/**
 * Configura e inicia el servidor REST de San Francisco Archive.
 */
public final class Router {
  private static final String CLAVE_ERROR = "error";

  /**
   * Impide crear instancias de esta clase de configuración.
   */
  private Router() {
  }

  /**
  * Crea la aplicación Javalin, configura errores y registra las rutas REST.
   *
   * @return aplicación Javalin configurada
   */
  public static Javalin crearAplicacion() {
    Javalin aplicacion = Javalin.create(config ->
      config.bundledPlugins.enableCors(cors -> cors.addRule(regla -> regla.anyHost()))
    );

    AuthController authController = new AuthController();
    authController.registrarRutas(aplicacion);
    new IncidenteController(authController).registrarRutas(aplicacion);
    new ContratoController(authController).registrarRutas(aplicacion);
    new PotencialController(authController).registrarRutas(aplicacion);

    aplicacion.get("/api/salud", contexto ->
        contexto.json(Map.of(
            "estado", "operativo",
            "servicio", "San Francisco Archive"
        ))
    );

    aplicacion.exception(SQLException.class, (error, contexto) -> {
      if ("28000".equals(error.getSQLState())) {
        contexto.status(403).json(Map.of(CLAVE_ERROR, "ACCESS DENIED"));
      } else if (error.getSQLState() != null && error.getSQLState().startsWith("23")) {
        contexto.status(409).json(Map.of(CLAVE_ERROR, "Conflicto con los datos existentes."));
      } else {
        contexto.status(500).json(Map.of(CLAVE_ERROR, "Error al acceder a la base de datos."));
      }
    });

    aplicacion.exception(JsonSyntaxException.class, (error, contexto) ->
        contexto.status(400).json(Map.of(CLAVE_ERROR, "Cuerpo JSON no válido."))
    );

    aplicacion.exception(NumberFormatException.class, (error, contexto) ->
        contexto.status(400).json(Map.of(CLAVE_ERROR, "El identificador debe ser numérico."))
    );

    return aplicacion;
  }
}