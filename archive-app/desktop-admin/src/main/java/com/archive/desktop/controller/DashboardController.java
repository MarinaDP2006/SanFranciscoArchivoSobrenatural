package com.archive.desktop.controller;

import com.archive.desktop.service.ApiClient;
import com.archive.desktop.service.ApiClient.Contrato;
import com.archive.desktop.service.ApiClient.Incidente;
import com.archive.desktop.service.ApiClient.Potencial;
import com.archive.desktop.service.ApiClient.Sesion;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

/**
 * Presenta indicadores globales, mapa de coordenadas y gestión de contratos.
 */
public class DashboardController {
	private static final String COLOR_VERDE = "#5dff91";
	private static final String COLOR_VERDE_SUAVE = "#2aa75b";
	private static final String COLOR_FONDO_MAPA = "#07100b";

	@FXML private Label sesionLabel;
	@FXML private Label estadoLabel;
	@FXML private Label incidentesTotalLabel;
	@FXML private Label potencialesTotalLabel;
	@FXML private Label contratosTotalLabel;
	@FXML private Label mapaResumenLabel;
	@FXML private TableView<Incidente> incidentesTabla;
	@FXML private TableColumn<Incidente, String> incidenteIdColumna;
	@FXML private TableColumn<Incidente, String> incidenteTipoColumna;
	@FXML private TableColumn<Incidente, String> incidenteCiudadColumna;
	@FXML private TableColumn<Incidente, String> incidenteEstadoColumna;
	@FXML private TableView<Contrato> contratosTabla;
	@FXML private TableColumn<Contrato, String> contratoIdColumna;
	@FXML private TableColumn<Contrato, String> contratoCiudadColumna;
	@FXML private TableColumn<Contrato, String> contratoEstadoColumna;
	@FXML private TableColumn<Contrato, String> contratoRecompensaColumna;
	@FXML private Canvas mapaCanvas;
	@FXML private Button completarButton;

	private ApiClient apiClient;
	private Sesion sesion;

	/**
	 * Inicializa las columnas y el estado visual del dashboard.
	 */
	@FXML
	private void initialize() {
		configurarColumnas();
		dibujarMapa(List.of());
		completarButton.setDisable(true);
		contratosTabla.getSelectionModel().selectedItemProperty().addListener(
				(observable, anterior, seleccionado) ->
						completarButton.setDisable(seleccionado == null
								|| !"ASIGNADO".equalsIgnoreCase(seleccionado.estado()))
		);
	}

	/**
	 * Conecta el dashboard con la API y la sesión del administrador.
	 *
	 * @param cliente cliente HTTP configurado
	 * @param sesion sesión autenticada
	 */
	public void configurar(ApiClient cliente, Sesion sesion) {
		this.apiClient = cliente;
		this.sesion = sesion;
		sesionLabel.setText(sesion.username() + "  /  " + sesion.rol());
		recargarDatos();
	}

	/**
	 * Actualiza las tablas y los contadores sin bloquear el hilo de JavaFX.
	 */
	@FXML
	private void recargarDatos() {
		if (apiClient == null || sesion == null) {
			return;
		}

		estadoLabel.setText("CONSULTANDO API...");
		Task<DatosDashboard> tarea = new Task<>() {
			@Override
			protected DatosDashboard call() throws IOException, InterruptedException {
				String token = sesion.token();
				return new DatosDashboard(
						apiClient.obtenerIncidentes(token),
						apiClient.obtenerPotenciales(token),
						apiClient.obtenerContratos(token)
				);
			}
		};

		tarea.setOnSucceeded(evento -> mostrarDatos(tarea.getValue()));
		tarea.setOnFailed(evento -> {
			Throwable error = tarea.getException();
			estadoLabel.setText("ERROR API  /  " + mensajeError(error));
		});
		iniciarTarea(tarea, "archive-dashboard-refresh");
	}

	/**
	 * Completa el contrato seleccionado y solicita la recompensa a la API.
	 */
	@FXML
	private void completarContratoSeleccionado() {
		Contrato contrato = contratosTabla.getSelectionModel().getSelectedItem();
		if (contrato == null || apiClient == null || sesion == null) {
			estadoLabel.setText("SELECCIONA UN CONTRATO ASIGNADO.");
			return;
		}
		if (!"ASIGNADO".equalsIgnoreCase(contrato.estado())) {
			estadoLabel.setText("EL CONTRATO DEBE ESTAR ASIGNADO PARA COMPLETARSE.");
			return;
		}

		completarButton.setDisable(true);
		estadoLabel.setText("COMPLETANDO CONTRATO " + contrato.idContrato() + "...");
		Task<Void> tarea = new Task<>() {
			@Override
			protected Void call() throws IOException, InterruptedException {
				apiClient.completarContrato(contrato.idContrato(), sesion.token());
				return null;
			}
		};
		tarea.setOnSucceeded(evento -> {
			estadoLabel.setText("CONTRATO COMPLETADO  /  RECOMPENSA ABONADA");
			recargarDatos();
		});
		tarea.setOnFailed(evento -> {
			completarButton.setDisable(false);
			estadoLabel.setText("NO SE PUDO COMPLETAR  /  " + mensajeError(tarea.getException()));
		});
		iniciarTarea(tarea, "archive-contract-complete");
	}

	/**
	 * Configura celdas que muestran campos de los DTO recibidos por la API.
	 */
	private void configurarColumnas() {
		incidenteIdColumna.setCellValueFactory(celda ->
				new SimpleStringProperty("#" + celda.getValue().idIncidente()));
		incidenteTipoColumna.setCellValueFactory(celda ->
				new SimpleStringProperty(valor(celda.getValue().tipo())));
		incidenteCiudadColumna.setCellValueFactory(celda ->
				new SimpleStringProperty(valor(celda.getValue().ciudad())));
		incidenteEstadoColumna.setCellValueFactory(celda ->
				new SimpleStringProperty(valor(celda.getValue().estadoVerificacion())));

		contratoIdColumna.setCellValueFactory(celda ->
				new SimpleStringProperty("#" + celda.getValue().idContrato()));
		contratoCiudadColumna.setCellValueFactory(celda ->
				new SimpleStringProperty(valor(celda.getValue().ciudad())));
		contratoEstadoColumna.setCellValueFactory(celda ->
				new SimpleStringProperty(valor(celda.getValue().estado())));
		contratoRecompensaColumna.setCellValueFactory(celda ->
				new SimpleStringProperty(String.format(
						Locale.ROOT, "%,.0f $", celda.getValue().recompensaDinero())));
	}

	/**
	 * Publica los resultados de las solicitudes en los controles JavaFX.
	 *
	 * @param datos colecciones recibidas de la API
	 */
	private void mostrarDatos(DatosDashboard datos) {
		List<Incidente> incidentes = datos.incidentes();
		incidentesTabla.getItems().setAll(incidentes);
		contratosTabla.getItems().setAll(datos.contratos());
		incidentesTotalLabel.setText(Integer.toString(incidentes.size()));
		potencialesTotalLabel.setText(Integer.toString(datos.potenciales().size()));
		contratosTotalLabel.setText(Integer.toString(datos.contratos().size()));
		dibujarMapa(incidentes);
		estadoLabel.setText("API CONECTADA  /  DATOS ACTUALIZADOS");
	}

	/**
	 * Dibuja una proyección mundial sencilla con la latitud y longitud de los incidentes.
	 *
	 * @param puntos incidentes representados en el lienzo
	 */
	private void dibujarMapa(List<Incidente> puntos) {
		GraphicsContext grafico = mapaCanvas.getGraphicsContext2D();
		double ancho = mapaCanvas.getWidth();
		double alto = mapaCanvas.getHeight();
		grafico.setFill(Color.web(COLOR_FONDO_MAPA));
		grafico.fillRect(0, 0, ancho, alto);
		grafico.setStroke(Color.web("#183b27"));
		grafico.setLineWidth(1);

		for (int columna = 1; columna < 6; columna++) {
			double x = ancho * columna / 6.0;
			grafico.strokeLine(x, 0, x, alto);
		}
		for (int fila = 1; fila < 4; fila++) {
			double y = alto * fila / 4.0;
			grafico.strokeLine(0, y, ancho, y);
		}

		grafico.setFill(Color.web("#42684c"));
		grafico.fillText("90 N", 8, 16);
		grafico.fillText("0", 8, alto / 2.0 - 5);
		grafico.fillText("90 S", 8, alto - 8);
		grafico.fillText("180 W", 8, alto - 8);
		grafico.fillText("180 E", ancho - 55, alto - 8);

		int representados = 0;
		for (Incidente incidente : puntos) {
			double latitud = incidente.lat();
			double longitud = incidente.lon();
			if (!Double.isFinite(latitud) || !Double.isFinite(longitud)
					|| latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
				continue;
			}
			double x = (longitud + 180.0) / 360.0 * ancho;
			double y = (90.0 - latitud) / 180.0 * alto;
			grafico.setFill(Color.web(COLOR_VERDE));
			grafico.fillOval(x - 4, y - 4, 8, 8);
			grafico.setStroke(Color.web(COLOR_VERDE_SUAVE));
			grafico.strokeOval(x - 7, y - 7, 14, 14);
			representados++;
		}
		mapaResumenLabel.setText(representados + " INCIDENTES CON COORDENADAS");
	}

	/**
	 * Inicia una tarea de fondo y configura su hilo como daemon.
	 *
	 * @param tarea tarea de red ejecutada fuera del hilo gráfico
	 * @param nombreHilo nombre asignado al hilo
	 */
	private void iniciarTarea(Task<?> tarea, String nombreHilo) {
		Thread hilo = new Thread(tarea, nombreHilo);
		hilo.setDaemon(true);
		hilo.start();
	}

	/**
	 * Sustituye valores nulos por un guion para mostrarlos en una tabla.
	 *
	 * @param texto valor procedente de la respuesta JSON
	 * @return texto original o marcador de valor ausente
	 */
	private String valor(String texto) {
		return texto == null || texto.isBlank() ? "-" : texto;
	}

	/**
	 * Reduce el detalle de un error para presentarlo en la barra de estado.
	 *
	 * @param error excepción de red o servidor
	 * @return mensaje legible
	 */
	private String mensajeError(Throwable error) {
		if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
			return "error de comunicación";
		}
		return error.getMessage();
	}

	/**
	 * Agrupa las respuestas remotas necesarias para refrescar el dashboard.
	 *
	 * @param incidentes incidentes globales
	 * @param potenciales potenciales registrados
	 * @param contratos contratos de anomalía
	 */
	private record DatosDashboard(
			List<Incidente> incidentes, List<Potencial> potenciales, List<Contrato> contratos) {
	}
}
