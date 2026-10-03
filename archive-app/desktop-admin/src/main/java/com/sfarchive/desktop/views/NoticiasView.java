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

/**
 * Pantalla Noticias (solo administradores): redactar, publicar, destacar o retirar las noticias
 * de la web pública. Al guardar como "Publicada" aparece en la web enseguida.
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class NoticiasView implements Vista {

    // --- Acceso a datos ---
    private final NoticiaDao dao = new NoticiaDao();
    private final IncidenteDao incidentes = new IncidenteDao();
    private final ActividadDao actividad = new ActividadDao();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final TableView<Noticia> tabla = Ui.tabla("Sin noticias");
    private Noticia actual;

    // --- Campos del formulario ---
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
                Ui.col("Fecha", 120, Noticia::fechaPublicacion),
                Ui.colBadge("Categoría", 100, n -> n.categoria().name()),
                Ui.col("Titular", 300, Noticia::titulo),
                Ui.colBadge("Estado", 100, n -> n.publicada() ? "PUBLICADO" : "BORRADOR"),
                Ui.col("Destacada", 75, Noticia::destacada),
                Ui.col("Autor", 110, Noticia::autorNombre)));
        // Cuando el usuario selecciona una fila, rellenamos el formulario con sus datos
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

        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(tabla, scroll);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.52);
        VBox.setVgrow(split, Priority.ALWAYS);
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Noticias de la web", "Las noticias son la cara pública del Archivo: informan sin revelar la anomalía.", split);
        limpiar();
        return raiz;
    }

    /** Recarga noticias e incidentes (para el ComboBox de incidente relacionado). */
    @Override
    public void refrescar() {
        List<Incidente> lista = new ArrayList<>();
        lista.add(null);
        lista.addAll(incidentes.listar());
        incidente.setItems(FXCollections.observableArrayList(lista));
        Ui.cargar(tabla, dao.listar());
    }

    /** Copia la noticia al formulario. */
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

    /** Prepara el formulario para una noticia nueva. */
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

    /** Botón Guardar: valida, conserva la hora original si no cambia el día y guarda. */
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

    /** Botón Eliminar (pide confirmación). */
    private void eliminar() {
        if (actual == null || !Ui.confirmar("¿Eliminar la noticia \"" + actual.titulo() + "\"?")) return;
        dao.borrar(actual.id());
        actividad.registrar(Sesion.id(), "BORRAR", "NOTICIA", actual.id(), actual.titulo());
        limpiar();
        refrescar();
    }
}
