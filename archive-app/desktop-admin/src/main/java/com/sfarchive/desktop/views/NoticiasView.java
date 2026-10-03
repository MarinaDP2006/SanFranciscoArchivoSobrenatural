package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.dao.NoticiaDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.CategoriaNoticia;
import com.sfarchive.core.model.Incidente;
import com.sfarchive.core.model.Noticia;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Noticias de la web pública: redactar, publicar, destacar o retirar. */
public class NoticiasView implements Vista {

    private final NoticiaDao dao = new NoticiaDao();
    private final IncidenteDao incidentes = new IncidenteDao();
    private final ActividadDao actividad = new ActividadDao();

    private VBox raiz;
    private final TableView<Noticia> tabla = Ui.tabla("Sin noticias");
    private Noticia actual;

    private final TextField titulo = new TextField();
    private final TextField resumen = new TextField();
    private final TextArea contenido = Formulario.area(10);
    private final ComboBox<CategoriaNoticia> categoria = Ui.combo(Arrays.asList(CategoriaNoticia.values()));
    private final TextField barrio = new TextField();
    private final TextField imagen = new TextField();
    private final ComboBox<Incidente> incidente = Ui.combo(List.of(), i -> i == null ? "— ninguno —" : i.codigo() + " · " + i.titulo());
    private final DatePicker fecha = new DatePicker();
    private final CheckBox publicada = new CheckBox("Publicada en la web");
    private final CheckBox destacada = new CheckBox("Destacada en portada");

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tabla.getColumns().addAll(List.of(
                Ui.col("Fecha", 120, Noticia::fechaPublicacion),
                Ui.colBadge("Categoría", 100, n -> n.categoria().name()),
                Ui.col("Titular", 300, Noticia::titulo),
                Ui.colBadge("Estado", 100, n -> n.publicada() ? "PUBLICADO" : "BORRADOR"),
                Ui.col("Destacada", 75, Noticia::destacada),
                Ui.col("Autor", 110, Noticia::autorNombre)));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) mostrar(n); });
        imagen.setPromptText("URL de imagen (opcional)");

        VBox ficha = new VBox(10, Ui.seccion("Noticia"),
                new Formulario().campo("Titular *", titulo).campo("Entradilla *", resumen).campo("Texto *", contenido)
                        .campo("Categoría", categoria).campo("Barrio", barrio).campo("Imagen", imagen)
                        .campo("Incidente", incidente).campo("Fecha", fecha).campo("", publicada).campo("", destacada),
                Ui.fila(Ui.primario("Guardar", this::guardar), Ui.boton("Nueva", this::limpiar), Ui.espacio(),
                        Ui.peligro("Eliminar", this::eliminar)),
                Ui.subtitulo("Separa los párrafos con una línea en blanco. Al guardar como publicada aparece en la web inmediatamente."));
        ficha.getStyleClass().add("panel");
        ScrollPane scroll = new ScrollPane(ficha);
        scroll.setFitToWidth(true);

        SplitPane split = new SplitPane(tabla, scroll);
        split.setDividerPositions(0.52);
        VBox.setVgrow(split, Priority.ALWAYS);
        raiz = Ui.pantalla("Noticias de la web", "Las noticias son la cara pública del Archivo: informan sin revelar la anomalía.", split);
        limpiar();
        return raiz;
    }

    @Override
    public void refrescar() {
        List<Incidente> lista = new ArrayList<>();
        lista.add(null);
        lista.addAll(incidentes.listar());
        incidente.setItems(FXCollections.observableArrayList(lista));
        Ui.cargar(tabla, dao.listar());
    }

    private void mostrar(Noticia n) {
        actual = n;
        titulo.setText(n.titulo());
        resumen.setText(n.resumen());
        contenido.setText(n.contenido());
        categoria.setValue(n.categoria());
        barrio.setText(Formulario.str(n.barrio()));
        imagen.setText(Formulario.str(n.imagenUrl()));
        incidente.setValue(incidente.getItems().stream().filter(i -> i != null && n.incidenteId() != null && i.id() == n.incidenteId())
                .findFirst().orElse(null));
        fecha.setValue(n.fechaPublicacion().toLocalDate());
        publicada.setSelected(n.publicada());
        destacada.setSelected(n.destacada());
    }

    private void limpiar() {
        actual = null;
        tabla.getSelectionModel().clearSelection();
        for (TextField t : List.of(titulo, resumen, barrio, imagen)) t.clear();
        contenido.clear();
        categoria.setValue(CategoriaNoticia.SUCESOS);
        incidente.setValue(null);
        fecha.setValue(LocalDate.now());
        publicada.setSelected(false);
        destacada.setSelected(false);
    }

    private void guardar() {
        if (Formulario.texto(titulo) == null || Formulario.texto(resumen) == null || Formulario.texto(contenido) == null)
            throw new DataException("Titular, entradilla y texto son obligatorios.");
        String img = Formulario.texto(imagen);
        if (img != null && !img.matches("https?://\\S+")) throw new DataException("La imagen debe ser una URL http(s).");
        LocalDateTime cuando = actual != null && actual.fechaPublicacion().toLocalDate().equals(fecha.getValue())
                ? actual.fechaPublicacion()
                : LocalDateTime.of(fecha.getValue() == null ? LocalDate.now() : fecha.getValue(),
                fecha.getValue() == null || fecha.getValue().equals(LocalDate.now()) ? LocalTime.now().withNano(0) : LocalTime.of(9, 0));
        Noticia n = new Noticia(actual == null ? 0 : actual.id(), null, Formulario.texto(titulo), Formulario.texto(resumen),
                contenido.getText().trim(), categoria.getValue(), img, Formulario.texto(barrio),
                incidente.getValue() == null ? null : incidente.getValue().id(), publicada.isSelected(), destacada.isSelected(),
                cuando, Sesion.id(), null);
        int id = dao.guardar(n);
        actividad.registrar(Sesion.id(), n.publicada() ? "PUBLICAR" : "EDITAR", "NOTICIA", id, n.titulo());
        refrescar();
        dao.listar().stream().filter(x -> x.id() == id).findFirst().ifPresent(x -> { tabla.getSelectionModel().select(x); mostrar(x); });
        Ui.info(n.publicada() ? "Noticia publicada en la web." : "Noticia guardada como borrador.");
    }

    private void eliminar() {
        if (actual == null || !Ui.confirmar("¿Eliminar la noticia \"" + actual.titulo() + "\"?")) return;
        dao.borrar(actual.id());
        actividad.registrar(Sesion.id(), "BORRAR", "NOTICIA", actual.id(), actual.titulo());
        limpiar();
        refrescar();
    }
}
