package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.ContratoDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.dao.InformeDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.Clasificacion;
import com.sfarchive.core.model.Contrato;
import com.sfarchive.core.model.Informe;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

/**
 * Pantalla Archivo Restringido (solo administradores): escribir y guardar el Informe Final
 * Clasificado de cada contrato cerrado (completado o fallido). Nada de esto sale a la web.
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class ArchivoRestringidoView implements Vista {

    // --- Acceso a datos ---
    private final ContratoDao contratos = new ContratoDao();
    private final InformeDao informes = new InformeDao();
    private final IncidenteDao incidentes = new IncidenteDao();
    private final ActividadDao actividad = new ActividadDao();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final TableView<Contrato> tabla = Ui.tabla("Todavía no hay contratos cerrados");
    private Contrato actual;

    // --- Editor del informe ---
    private final Label cabecera = Ui.seccion("Selecciona un contrato cerrado");
    private final Label anomalia = new Label();
    private final TextField titulo = new TextField();
    private final TextField entidad = new TextField();
    private final ComboBox<Clasificacion> clasificacion = Ui.combo(Arrays.asList(Clasificacion.values()));
    private final Spinner<Integer> bajas = new Spinner<>(0, 999, 0);
    private final TextField resumen = new TextField();
    private final TextArea contenido = Formulario.area(16);
    private final Label meta = new Label();
    private VBox editor;

    /**
     * Construye la pantalla (solo la primera vez; después devuelve la misma).
     * Aquí se crean las columnas de la tabla, el formulario, los botones y la distribución.
     */
    @Override
    public Node vista() {
        // Si ya la habíamos construido, la reutilizamos (así no se pierde lo que estaba seleccionado)
        if (raiz != null) return raiz;
        // Columnas de la tabla: título, ancho en píxeles y qué dato del objeto mostrar en cada una
        tabla.getColumns().addAll(List.of(
                Ui.col("Contrato", 105, Contrato::codigo),
                Ui.colBadge("Resultado", 110, c -> c.estado().name()),
                Ui.col("Incidente", 220, Contrato::incidenteTitulo),
                Ui.col("Potencial", 85, Contrato::potencialAlias),
                Ui.col("Cierre", 120, Contrato::fechaCierre),
                Ui.colBadge("Informe", 90, c -> c.tieneInforme() ? "COMPLETADO" : "PENDIENTE")));
        // Cuando el usuario selecciona una fila, rellenamos el formulario con sus datos
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) mostrar(n); });

        anomalia.setWrapText(true);
        anomalia.getStyleClass().add("aviso");
        meta.getStyleClass().add("subtitulo");
        bajas.setEditable(true);
        editor = new VBox(10, cabecera, anomalia,
                new Formulario().campo("Título *", titulo).campo("Entidad anómala", entidad)
                        .campo("Clasificación", clasificacion).campo("Bajas civiles", bajas).campo("Resumen", resumen),
                new Label("Informe final:"), contenido, meta,
                Ui.fila(Ui.primario("Guardar informe", this::guardar), Ui.espacio(), Ui.peligro("Eliminar informe", this::eliminar)));
        VBox.setVgrow(contenido, Priority.ALWAYS);
        editor.getStyleClass().add("panel");

        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(tabla, editor);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.45);
        VBox.setVgrow(split, Priority.ALWAYS);
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Archivo Restringido",
                "Solo administradores. Aquí queda la verdad de cada caso: la entidad, lo ocurrido y su resolución. Nada de esto sale a la web.",
                split);
        editor.setDisable(true);
        return raiz;
    }

    /** Recarga la lista de contratos cerrados. */
    @Override
    public void refrescar() {
        Ui.cargar(tabla, contratos.cerrados());
    }

    /** Carga el informe del contrato (o prepara uno nuevo) y enseña la anomalía registrada. */
    private void mostrar(Contrato c) {
        actual = c;
        editor.setDisable(false);
        cabecera.setText((c.codigo() + " · " + c.incidenteCodigo() + " · " + c.potencialAlias()).toUpperCase());
        anomalia.setText("Anomalía registrada: " + incidentes.porId(c.incidenteId()).map(i -> Ui.nvl(i.anomalia())).orElse("—")
                + (c.notasCampo() != null ? "\nNotas de campo: " + c.notasCampo() : ""));
        Informe f = informes.porContrato(c.id()).orElse(null);
        titulo.setText(f == null ? c.incidenteTitulo() : f.titulo());
        entidad.setText(f == null ? "" : Formulario.str(f.entidad()));
        clasificacion.setValue(f == null ? Clasificacion.SECRETO : f.clasificacion());
        bajas.getValueFactory().setValue(f == null ? 0 : f.bajasCiviles());
        resumen.setText(f == null ? "" : Formulario.str(f.resumen()));
        contenido.setText(f == null ? "" : f.contenido());
        meta.setText(f == null ? "Informe pendiente de redactar."
                : "Autor: " + Ui.nvl(f.autorNombre()) + " · creado " + Ui.fecha(f.fechaCreacion()) + " · modificado " + Ui.fecha(f.fechaModificacion()));
    }

    /** Botón Guardar informe (crea o actualiza; uno por contrato). */
    private void guardar() {
        if (actual == null) return;
        if (Formulario.texto(titulo) == null) throw new DataException("El informe necesita un título.");
        if (Formulario.texto(contenido) == null) throw new DataException("Escribe el contenido del informe.");
        informes.guardar(new Informe(0, actual.id(), actual.codigo(), Sesion.id(), null, Formulario.texto(titulo),
                Formulario.texto(entidad), clasificacion.getValue(), Formulario.texto(resumen), contenido.getText().trim(),
                bajas.getValue(), null, null));
        actividad.registrar(Sesion.id(), "INFORME", "CONTRATO", actual.id(), "Informe final " + actual.codigo());
        refrescar();
        mostrar(actual);
        Ui.info("Informe clasificado guardado.");
    }

    /** Botón Eliminar informe. */
    private void eliminar() {
        if (actual == null || !Ui.confirmar("¿Eliminar el informe de " + actual.codigo() + "?")) return;
        informes.borrar(actual.id());
        refrescar();
        mostrar(actual);
    }
}
