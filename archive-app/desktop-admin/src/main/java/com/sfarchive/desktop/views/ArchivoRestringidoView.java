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

/** Archivo Restringido: escribir y guardar el Informe Final Clasificado de cada contrato cerrado. */
public class ArchivoRestringidoView implements Vista {

    private final ContratoDao contratos = new ContratoDao();
    private final InformeDao informes = new InformeDao();
    private final IncidenteDao incidentes = new IncidenteDao();
    private final ActividadDao actividad = new ActividadDao();

    private VBox raiz;
    private final TableView<Contrato> tabla = Ui.tabla("Todavía no hay contratos cerrados");
    private Contrato actual;

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

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tabla.getColumns().addAll(List.of(
                Ui.col("Contrato", 105, Contrato::codigo),
                Ui.colBadge("Resultado", 110, c -> c.estado().name()),
                Ui.col("Incidente", 220, Contrato::incidenteTitulo),
                Ui.col("Potencial", 85, Contrato::potencialAlias),
                Ui.col("Cierre", 120, Contrato::fechaCierre),
                Ui.colBadge("Informe", 90, c -> c.tieneInforme() ? "COMPLETADO" : "PENDIENTE")));
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

        SplitPane split = new SplitPane(tabla, editor);
        split.setDividerPositions(0.45);
        VBox.setVgrow(split, Priority.ALWAYS);
        raiz = Ui.pantalla("Archivo Restringido",
                "Solo administradores. Aquí queda la verdad de cada caso: la entidad, lo ocurrido y su resolución. Nada de esto sale a la web.",
                split);
        editor.setDisable(true);
        return raiz;
    }

    @Override
    public void refrescar() {
        Ui.cargar(tabla, contratos.cerrados());
    }

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

    private void eliminar() {
        if (actual == null || !Ui.confirmar("¿Eliminar el informe de " + actual.codigo() + "?")) return;
        informes.borrar(actual.id());
        refrescar();
        mostrar(actual);
    }
}
