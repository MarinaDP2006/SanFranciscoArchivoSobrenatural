package com.archive.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.archive.dao.AdminDAO;
import com.archive.dao.UsuarioReporterDAO;
import com.archive.model.Admin;
import com.archive.model.UsuarioReporter;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona el inicio y cierre de sesión de administradores y reporters.
 */
public class AuthController {
  private static final String CLAVE_ERROR = "error";
  private static final String PREFIJO_BEARER = "Bearer ";

  private final AdminDAO adminDAO = new AdminDAO();
  private final UsuarioReporterDAO reporterDAO = new UsuarioReporterDAO();
  private final Gson gson = new Gson();
  private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

  /**
   * Registra las rutas de autenticación.
   *
   * @param aplicacion aplicación Javalin donde se registran las rutas
   */
  public void registrarRutas(Javalin aplicacion) {
    aplicacion.post("/api/auth/login", this::iniciarSesion);
    aplicacion.post("/api/auth/logout", this::cerrarSesion);
  }

  /**
   * Comprueba las credenciales y genera un token de sesión.
   * El valor recibido se compara literalmente con el almacenado en password_hash.
   *
   * @param contexto contexto HTTP de la solicitud
   * @throws SQLException si falla una consulta de base de datos
   */
  private void iniciarSesion(Context contexto) throws SQLException {
    SolicitudLogin solicitud;
    try {
      solicitud = gson.fromJson(contexto.body(), SolicitudLogin.class);
    } catch (JsonSyntaxException error) {
      contexto.status(400).json(Map.of(CLAVE_ERROR, "Cuerpo JSON no válido."));
      return;
    }

    if (solicitud == null || solicitud.username == null || solicitud.password == null
        || solicitud.username.isBlank() || solicitud.password.isBlank()) {
      contexto.status(400).json(Map.of(CLAVE_ERROR, "Se requieren username y password."));
      return;
    }

    Admin admin = adminDAO.obtenerPorUsername(solicitud.username);
    if (admin != null && coincide(solicitud.password, admin.getPasswordHash())) {
      responderConSesion(contexto, admin.getIdAdmin(), admin.getUsername(), "ADMIN");
      return;
    }

    UsuarioReporter reporter = reporterDAO.obtenerPorUsername(solicitud.username);
    if (reporter != null && coincide(solicitud.password, reporter.getPasswordHash())) {
      responderConSesion(contexto, reporter.getIdUsuario(), reporter.getUsername(), "REPORTER");
      return;
    }

    contexto.status(401).json(Map.of(CLAVE_ERROR, "Credenciales no válidas."));
  }

  /**
   * Invalida el token de la sesión actual.
   *
   * @param contexto contexto HTTP de la solicitud
   */
  private void cerrarSesion(Context contexto) {
    String token = extraerToken(contexto);
    if (token == null || sesiones.remove(token) == null) {
      contexto.status(401).json(Map.of(CLAVE_ERROR, "Se requiere autenticación."));
      return;
    }
    contexto.status(204);
  }

  /**
   * Obtiene la sesión asociada a la cabecera Bearer.
   *
   * @param contexto contexto HTTP de la solicitud
   * @return sesión válida o null si falta o no es válida el token
   */
  public Sesion obtenerSesion(Context contexto) {
    String token = extraerToken(contexto);
    if (token == null || token.isBlank()) {
      contexto.status(401).json(Map.of(CLAVE_ERROR, "Se requiere autenticación."));
      return null;
    }

    Sesion sesion = sesiones.get(token);
    if (sesion == null) {
      contexto.status(401).json(Map.of(CLAVE_ERROR, "Token no válido."));
    }
    return sesion;
  }

  /**
   * Comprueba que la sesión pertenece al rol solicitado.
   *
   * @param contexto contexto HTTP de la solicitud
   * @param rolRequerido rol necesario para acceder a la ruta
   * @return true si la sesión tiene el rol requerido
   */
  public boolean requiereRol(Context contexto, String rolRequerido) {
    Sesion sesion = obtenerSesion(contexto);
    if (sesion == null) {
      return false;
    }
    if (!rolRequerido.equalsIgnoreCase(sesion.rol())) {
      contexto.status(403).json(Map.of(CLAVE_ERROR, "ACCESS DENIED"));
      return false;
    }
    return true;
  }

  /**
   * Extrae el token Bearer de una cabecera de autorización válida.
   *
   * @param contexto contexto HTTP que contiene la cabecera
   * @return token sin prefijo o null si la cabecera falta o no es Bearer
   */
  private String extraerToken(Context contexto) {
    String autorizacion = contexto.header("Authorization");
    if (autorizacion == null) {
      return null;
    }
    if (!autorizacion.startsWith(PREFIJO_BEARER)) {
      return null;
    }
    return autorizacion.substring(PREFIJO_BEARER.length()).trim();
  }

  /**
   * Crea una sesión y devuelve su token al cliente.
   *
   * @param contexto contexto HTTP de la solicitud
   * @param idUsuario identificador del usuario autenticado
   * @param username nombre de usuario autenticado
   * @param rol rol asignado a la sesión
   */
  private void responderConSesion(
      Context contexto, int idUsuario, String username, String rol) {
    String token = UUID.randomUUID().toString();
    sesiones.put(token, new Sesion(idUsuario, username, rol));
    contexto.json(Map.of(
        "token", token,
        "idUsuario", idUsuario,
        "username", username,
        "rol", rol
    ));
  }

  /**
   * Compara el valor recibido con el guardado en la columna password_hash.
   *
   * @param recibido contraseña recibida en la solicitud
   * @param almacenado valor persistido en la base de datos
   * @return true si ambos valores coinciden
   */
  private boolean coincide(String recibido, String almacenado) {
    if (recibido == null || almacenado == null) {
      return false;
    }
    return MessageDigest.isEqual(
        recibido.getBytes(StandardCharsets.UTF_8),
        almacenado.getBytes(StandardCharsets.UTF_8)
    );
  }

  /**
   * Representa la identidad asociada a un token de sesión.
   *
   * @param idUsuario identificador del administrador o reporter
   * @param username nombre de usuario
   * @param rol rol de la sesión
   */
  public record Sesion(int idUsuario, String username, String rol) {
  }

  /**
   * Representa las credenciales recibidas en el inicio de sesión.
   */
  private static final class SolicitudLogin {
    private String username;
    private String password;
  }
}