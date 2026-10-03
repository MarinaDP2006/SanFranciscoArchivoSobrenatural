package com.sfarchive.desktop.ui;

import com.sfarchive.core.db.DataException;
import com.sfarchive.desktop.ArchiveApp;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Utilidades de interfaz reutilizadas por todas las pantallas. */
public final class Ui {

    private static final DateTimeFormatter FH = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);

    private Ui() { }

    // ---------- Formatos ----------
    public static String fecha(LocalDateTime d) { return d == null ? "—" : FH.format(d); }
    public static String fecha(LocalDate d) { return d == null ? "—" : F.format(d); }
    public static String dinero(BigDecimal b) { return b == null ? "—" : USD.format(b); }
    public static String nvl(Object o) { return o == null ? "" : String.valueOf(o); }

    // ---------- Tablas ----------
    public static <T> TableColumn<T, String> col(String titulo, double ancho, Function<T, Object> valor) {
        TableColumn<T, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(texto(valor.apply(cd.getValue()))));
        c.setPrefWidth(ancho);
        return c;
    }

    /** Columna con "chip" de color según el valor (estados, tipos...). */
    public static <T> TableColumn<T, String> colBadge(String titulo, double ancho, Function<T, Object> valor) {
        TableColumn<T, String> c = col(titulo, ancho, valor);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) { setGraphic(null); setText(null); return; }
                setText(null);
                setGraphic(badge(item));
            }
        });
        return c;
    }

    /** Columna de dinero alineada a la derecha y coloreada (verde/rojo). */
    public static <T> TableColumn<T, BigDecimal> colDinero(String titulo, double ancho, Function<T, BigDecimal> valor) {
        TableColumn<T, BigDecimal> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(valor.apply(cd.getValue())));
        c.setPrefWidth(ancho);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("positivo", "negativo");
                if (empty || item == null) { setText(null); return; }
                setText(dinero(item));
                setAlignment(Pos.CENTER_RIGHT);
                getStyleClass().add(item.signum() < 0 ? "negativo" : "positivo");
            }
        });
        return c;
    }

    private static String texto(Object o) {
        if (o == null) return "";
        if (o instanceof LocalDateTime d) return fecha(d);
        if (o instanceof LocalDate d) return fecha(d);
        if (o instanceof BigDecimal b) return dinero(b);
        if (o instanceof Boolean b) return b ? "Sí" : "No";
        return String.valueOf(o);
    }

    public static <T> TableView<T> tabla(String vacio) {
        TableView<T> t = new TableView<>();
        t.setPlaceholder(new Label(vacio));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(t, Priority.ALWAYS);
        return t;
    }

    public static <T> void cargar(TableView<T> t, List<T> datos) {
        T sel = t.getSelectionModel().getSelectedItem();
        t.setItems(FXCollections.observableArrayList(datos));
        if (sel != null) {
            for (T d : datos) if (d.equals(sel)) { t.getSelectionModel().select(d); break; }
        }
    }

    // ---------- Componentes ----------
    public static Label badge(String texto) {
        Label l = new Label(texto.toUpperCase().replace('_', ' '));
        l.getStyleClass().addAll("badge", "badge-" + texto.toLowerCase().replace(' ', '_').replace("á", "a")
                .replace("ó", "o").replace("í", "i"));
        return l;
    }

    public static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo");
        return l;
    }

    public static Label subtitulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("subtitulo");
        l.setWrapText(true);
        return l;
    }

    public static Label seccion(String texto) {
        Label l = new Label(texto.toUpperCase());
        l.getStyleClass().add("seccion");
        return l;
    }

    public static Button boton(String texto, Runnable accion) {
        Button b = new Button(texto);
        b.setOnAction(e -> ejecutar(accion));
        return b;
    }

    public static Button primario(String texto, Runnable accion) {
        Button b = boton(texto, accion);
        b.getStyleClass().add("primario");
        return b;
    }

    public static Button peligro(String texto, Runnable accion) {
        Button b = boton(texto, accion);
        b.getStyleClass().add("peligro");
        return b;
    }

    public static HBox fila(Node... nodos) {
        HBox h = new HBox(8, nodos);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public static Region espacio() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static VBox tarjeta(String etiqueta, String valor, String clase) {
        Label v = new Label(valor);
        v.getStyleClass().addAll("kpi-valor", clase);
        Label e = new Label(etiqueta.toUpperCase());
        e.getStyleClass().add("kpi-etiqueta");
        VBox box = new VBox(2, v, e);
        box.getStyleClass().add("kpi");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMinWidth(Region.USE_PREF_SIZE); // que el valor no se corte con "..."
        return box;
    }

    /** Contenedor estándar de una pantalla: cabecera + contenido. */
    public static VBox pantalla(String titulo, String subtitulo, Node... contenido) {
        VBox v = new VBox(14);
        v.setPadding(new Insets(22, 26, 22, 26));
        v.getChildren().addAll(titulo(titulo), subtitulo(subtitulo));
        v.getChildren().addAll(contenido);
        v.getStyleClass().add("pantalla");
        return v;
    }

    public static <E> ComboBox<E> combo(List<E> valores) {
        ComboBox<E> c = new ComboBox<>(FXCollections.observableArrayList(valores));
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    /** ComboBox que muestra cada elemento con el texto indicado (útil para records). */
    public static <E> ComboBox<E> combo(List<E> valores, Function<E, String> texto) {
        ComboBox<E> c = combo(valores);
        c.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(E e) { return e == null ? "" : texto.apply(e); }
            @Override public E fromString(String s) { return null; }
        });
        return c;
    }

    // ---------- Diálogos ----------
    /** Ejecuta una acción mostrando los errores de negocio/BD en un diálogo. */
    public static void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (DataException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            error("Error inesperado: " + e.getMessage());
        }
    }

    public static void info(String msg) {
        alerta(Alert.AlertType.INFORMATION, "San Francisco Archive", msg).showAndWait();
    }

    public static void error(String msg) {
        alerta(Alert.AlertType.ERROR, "No se pudo completar", msg).showAndWait();
    }

    public static boolean confirmar(String msg) {
        Alert a = alerta(Alert.AlertType.CONFIRMATION, "Confirmar", msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    public static Optional<String> pedirTexto(String titulo, String mensaje, String valor) {
        TextInputDialog d = new TextInputDialog(valor);
        d.setTitle(titulo);
        d.setHeaderText(null);
        d.setContentText(mensaje);
        prepararDialogo(d.getDialogPane());
        return d.showAndWait();
    }

    private static Alert alerta(Alert.AlertType tipo, String titulo, String msg) {
        Alert a = new Alert(tipo, msg);
        a.setTitle(titulo);
        a.setHeaderText(null);
        prepararDialogo(a.getDialogPane());
        return a;
    }

    public static void prepararDialogo(javafx.scene.control.DialogPane p) {
        p.getStylesheets().add(ArchiveApp.class.getResource("tema.css").toExternalForm());
        if (ArchiveApp.stage() != null && p.getScene() != null && p.getScene().getWindow() instanceof javafx.stage.Stage s
                && s.getOwner() == null) {
            s.initOwner(ArchiveApp.stage());
        }
    }
}
