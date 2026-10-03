package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.GrupoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.GrupoTactico;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.core.service.GrupoService;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Grupos tácticos por país/zona, máximo 6 potenciales. Los miembros se mueven arrastrando. */
public class GruposView implements Vista {

    private static final DataFormat POTENCIAL = new DataFormat("application/x-sfa-potencial");

    private final GrupoDao dao = new GrupoDao();
    private final PotencialDao potenciales = new PotencialDao();
    private final GrupoService servicio = new GrupoService();

    private VBox raiz;
    private final TableView<GrupoTactico> tabla = Ui.tabla("Sin grupos tácticos");
    private final ListView<Potencial> miembros = new ListView<>();
    private final ListView<Potencial> sinGrupo = new ListView<>();
    private final Label tituloMiembros = Ui.seccion("Miembros");
    private GrupoTactico actual;

    private final TextField nombre = new TextField();
    private final TextField pais = new TextField();
    private final TextField ciudad = new TextField();
    private final TextField zona = new TextField();
    private final TextField lat = new TextField();
    private final TextField lng = new TextField();
    private final TextArea descripcion = Formulario.area(2);
    private final CheckBox activo = new CheckBox("Activo");

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tabla.getColumns().addAll(List.of(
                Ui.col("Grupo", 150, GrupoTactico::nombre),
                Ui.col("País", 110, GrupoTactico::pais),
                Ui.col("Ciudad", 100, GrupoTactico::ciudad),
                Ui.col("Zona", 220, GrupoTactico::zona),
                Ui.col("Miembros", 75, g -> g.miembros() + " / " + GrupoDao.MAX_MIEMBROS),
                Ui.colBadge("Activo", 70, g -> g.activo() ? "ACTIVO" : "INACTIVO")));
        tabla.setPrefHeight(230);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) mostrar(n); });

        configurarLista(miembros, true);
        configurarLista(sinGrupo, false);
        VBox colMiembros = new VBox(6, tituloMiembros, miembros);
        VBox colLibres = new VBox(6, Ui.seccion("Potenciales sin grupo"), sinGrupo);
        VBox.setVgrow(miembros, Priority.ALWAYS);
        VBox.setVgrow(sinGrupo, Priority.ALWAYS);
        HBox.setHgrow(colMiembros, Priority.ALWAYS);
        HBox.setHgrow(colLibres, Priority.ALWAYS);
        Label ayuda = Ui.subtitulo("Arrastra potenciales entre las dos listas para asignarlos o sacarlos del grupo seleccionado.");
        HBox listas = new HBox(14, colMiembros, colLibres);
        VBox.setVgrow(listas, Priority.ALWAYS);
        VBox izquierda = new VBox(10, tabla, ayuda, listas);

        Formulario f = new Formulario().campo("Nombre *", nombre).campo("País *", pais).campo("Ciudad", ciudad)
                .campo("Zona", zona).campo("Base lat/lng", Ui.fila(lat, lng)).campo("Descripción", descripcion).campo("", activo);
        VBox ficha = new VBox(10, Ui.seccion("Ficha del grupo"), f,
                Ui.fila(Ui.primario("Guardar", this::guardar), Ui.boton("Nuevo grupo", this::limpiar), Ui.espacio(),
                        Ui.peligro("Eliminar", this::eliminar)));
        ficha.getStyleClass().add("panel");
        ScrollPane scroll = new ScrollPane(ficha);
        scroll.setFitToWidth(true);

        SplitPane split = new SplitPane(izquierda, scroll);
        split.setDividerPositions(0.62);
        VBox.setVgrow(split, Priority.ALWAYS);
        raiz = Ui.pantalla("Grupos tácticos", "Crea y edita grupos por país y zona. Máximo " + GrupoDao.MAX_MIEMBROS
                + " potenciales por grupo (también lo impide un trigger en MySQL).", split);
        limpiar();
        return raiz;
    }

    private void configurarLista(ListView<Potencial> lista, boolean esGrupo) {
        lista.setCellFactory(lv -> {
            ListCell<Potencial> celda = new ListCell<>() {
                @Override
                protected void updateItem(Potencial p, boolean empty) {
                    super.updateItem(p, empty);
                    setText(empty || p == null ? null
                            : "⠿ " + p.alias() + "  ·  nv " + p.nivel() + "  ·  " + p.habilidad() + "  ·  " + p.estado().etiqueta());
                }
            };
            celda.setOnDragDetected(e -> {
                if (celda.getItem() == null) return;
                var db = celda.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.put(POTENCIAL, celda.getItem().id());
                db.setContent(cc);
                e.consume();
            });
            return celda;
        });
        lista.setOnDragOver(e -> {
            if (e.getDragboard().hasContent(POTENCIAL) && e.getGestureSource() != lista
                    && !lista.getItems().stream().anyMatch(p -> p.id() == (Integer) e.getDragboard().getContent(POTENCIAL))) {
                e.acceptTransferModes(TransferMode.MOVE);
                if (!lista.getStyleClass().contains("drop-ok")) lista.getStyleClass().add("drop-ok");
            }
            e.consume();
        });
        lista.setOnDragExited(e -> lista.getStyleClass().remove("drop-ok"));
        lista.setOnDragDropped(e -> {
            boolean ok = false;
            if (e.getDragboard().hasContent(POTENCIAL)) {
                int id = (Integer) e.getDragboard().getContent(POTENCIAL);
                Ui.ejecutar(() -> {
                    if (esGrupo && actual == null) throw new DataException("Selecciona primero un grupo.");
                    servicio.mover(id, esGrupo ? actual.id() : null, Sesion.id());
                });
                ok = true;
            }
            e.setDropCompleted(ok);
            e.consume();
            refrescar();
        });
        lista.setPlaceholder(new Label(esGrupo ? "Arrastra aquí potenciales" : "Todos los potenciales tienen grupo"));
    }

    @Override
    public void refrescar() {
        List<GrupoTactico> gs = dao.listar();
        Ui.cargar(tabla, gs);
        if (actual != null) gs.stream().filter(g -> g.id() == actual.id()).findFirst().ifPresent(g -> {
            tabla.getSelectionModel().select(g);
            mostrar(g);
        });
        sinGrupo.setItems(FXCollections.observableArrayList(potenciales.porGrupo(null)));
    }

    private void mostrar(GrupoTactico g) {
        actual = g;
        nombre.setText(g.nombre());
        pais.setText(g.pais());
        ciudad.setText(g.ciudad());
        zona.setText(Formulario.str(g.zona()));
        lat.setText(Formulario.str(g.lat()));
        lng.setText(Formulario.str(g.lng()));
        descripcion.setText(Formulario.str(g.descripcion()));
        activo.setSelected(g.activo());
        tituloMiembros.setText(("Miembros de " + g.nombre() + " (" + g.miembros() + "/" + GrupoDao.MAX_MIEMBROS + ")").toUpperCase());
        miembros.setItems(FXCollections.observableArrayList(potenciales.porGrupo(g.id())));
    }

    private void limpiar() {
        actual = null;
        tabla.getSelectionModel().clearSelection();
        for (TextField t : List.of(nombre, zona, lat, lng)) t.clear();
        pais.setText("Estados Unidos");
        ciudad.setText("San Francisco");
        descripcion.clear();
        activo.setSelected(true);
        tituloMiembros.setText("MIEMBROS (selecciona un grupo)");
        miembros.getItems().clear();
    }

    private void guardar() {
        GrupoTactico g = new GrupoTactico(actual == null ? 0 : actual.id(), Formulario.texto(nombre), Formulario.texto(pais),
                Formulario.texto(ciudad) == null ? "" : Formulario.texto(ciudad), Formulario.texto(zona),
                Formulario.decimal(lat), Formulario.decimal(lng), Formulario.texto(descripcion), activo.isSelected(), 0);
        int id = servicio.guardar(g, Sesion.id());
        actual = dao.porId(id).orElse(null);
        refrescar();
    }

    private void eliminar() {
        if (actual == null) return;
        if (!Ui.confirmar("¿Eliminar " + actual.nombre() + "? Sus miembros quedarán sin grupo.")) return;
        dao.borrar(actual.id());
        limpiar();
        refrescar();
    }
}
