package com.archive.desktop;

import com.archive.desktop.controller.DashboardController;
import com.archive.desktop.service.ApiClient;
import com.archive.desktop.service.ApiClient.Sesion;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación administrativa JavaFX.
 */
public final class MainApp extends Application {
	private static final String TITULO = "San Francisco Archive | Terminal administrativa";

	private final ApiClient apiClient = new ApiClient();
	private Stage escenarioPrincipal;

	/**
	 * Presenta el formulario de inicio de sesión.
	 *
	 * @param escenario escenario principal de JavaFX
	 */
	@Override
	public void start(Stage escenario) {
		escenarioPrincipal = escenario;
		escenario.setTitle(TITULO);
		escenario.setMinWidth(420);
		escenario.setMinHeight(380);
		mostrarLogin();
		escenario.show();
	}

	/**
	 * Construye y presenta el formulario de autenticación.
	 */
	private void mostrarLogin() {
		Label marca = new Label("SAN FRANCISCO ARCHIVE");
		marca.getStyleClass().add("brand-title");
		Label subtitulo = new Label("TERMINAL ADMINISTRATIVA  /  ACCESO RESTRINGIDO");
		subtitulo.getStyleClass().add("muted-label");

		TextField usuario = new TextField();
		usuario.setPromptText("Nombre de usuario");
		usuario.setId("usernameField");
		PasswordField contrasena = new PasswordField();
		contrasena.setPromptText("Contraseña");
		contrasena.setId("passwordField");

		Label estado = new Label("Introduce tus credenciales de administrador.");
		estado.setWrapText(true);
		estado.getStyleClass().add("status-label");

		Button entrar = new Button("INICIAR SESIÓN");
		entrar.getStyleClass().add("primary-button");
		entrar.setMaxWidth(Double.MAX_VALUE);
		entrar.setDefaultButton(true);
		entrar.setOnAction(event -> iniciarSesion(usuario.getText(), contrasena.getText(), entrar, estado));

		VBox formulario = new VBox(12, marca, subtitulo, usuario, contrasena, entrar, estado);
		formulario.getStyleClass().add("login-panel");
		formulario.setMaxWidth(460);
		formulario.setMinWidth(320);
		formulario.setFillWidth(true);

		VBox contenedor = new VBox(formulario);
		contenedor.getStyleClass().add("login-screen");
		contenedor.setFillWidth(true);
		VBox.setVgrow(formulario, javafx.scene.layout.Priority.NEVER);

		Scene escena = new Scene(contenedor, 520, 420);
		aplicarEstilos(escena);
		escenarioPrincipal.setScene(escena);
	}

	/**
	 * Valida las credenciales en segundo plano para no bloquear JavaFX.
	 *
	 * @param username nombre de usuario introducido
	 * @param password contraseña introducida
	 * @param boton botón que se deshabilita mientras se consulta la API
	 * @param estado etiqueta donde se presenta el resultado
	 */
	private void iniciarSesion(String username, String password, Button boton, Label estado) {
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			estado.setText("Faltan el nombre de usuario o la contraseña.");
			return;
		}

		boton.setDisable(true);
		estado.setText("Conectando con la API...");
		Task<Sesion> tarea = new Task<>() {
			@Override
			protected Sesion call() throws IOException, InterruptedException {
				return apiClient.iniciarSesion(username.trim(), password);
			}
		};

		tarea.setOnSucceeded(event -> abrirDashboard(tarea.getValue(), estado));
		tarea.setOnFailed(event -> {
			boton.setDisable(false);
			Throwable error = tarea.getException();
			estado.setText(error instanceof JsonSyntaxException
					? "La API devolvió una respuesta no válida."
					: "No se pudo iniciar sesión: " + mensajeError(error));
		});
		Thread hilo = new Thread(tarea, "archive-login");
		hilo.setDaemon(true);
		hilo.start();
	}

	/**
	 * Carga el dashboard y le entrega la sesión autenticada.
	 *
	 * @param sesion sesión devuelta por la API
	 * @param estado etiqueta usada para mostrar fallos al cargar la vista
	 */
	private void abrirDashboard(Sesion sesion, Label estado) {
		try {
			FXMLLoader cargador = new FXMLLoader(
					MainApp.class.getResource("/fxml/dashboard.fxml"));
			Parent raiz = cargador.load();
			DashboardController controlador = cargador.getController();
			controlador.configurar(apiClient, sesion);

			Scene escena = new Scene(raiz, 1500, 900);
			aplicarEstilos(escena);
			escenarioPrincipal.setMinWidth(1180);
			escenarioPrincipal.setMinHeight(700);
			escenarioPrincipal.setScene(escena);
			escenarioPrincipal.centerOnScreen();
		} catch (IOException | RuntimeException error) {
			estado.setText("No se pudo abrir el dashboard: " + mensajeError(error));
		}
	}

	/**
	 * Añade la hoja de estilos terminal a una escena.
	 *
	 * @param escena escena JavaFX que recibe el estilo
	 */
	private void aplicarEstilos(Scene escena) {
		var hojaEstilos = MainApp.class.getResource("/css/terminal.css");
		if (hojaEstilos != null) {
			escena.getStylesheets().add(hojaEstilos.toExternalForm());
		}
	}

	/**
	 * Devuelve un mensaje breve para una excepción.
	 *
	 * @param error excepción que se presenta en pantalla
	 * @return detalle de la excepción o un texto genérico
	 */
	private String mensajeError(Throwable error) {
		if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
			return "error de comunicación";
		}
		return error.getMessage();
	}

	/**
	 * Lanza la aplicación JavaFX.
	 *
	 * @param argumentos argumentos de inicio
	 */
	public static void main(String[] argumentos) {
		launch(argumentos);
	}
}
