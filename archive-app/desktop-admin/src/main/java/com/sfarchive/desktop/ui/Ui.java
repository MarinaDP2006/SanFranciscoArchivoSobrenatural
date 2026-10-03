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

/**
 * Caja de herramientas de la interfaz: métodos static que usan TODAS las pantallas para no
 * repetir código (crear columnas de tabla, botones, tarjetas, diálogos, formatos de fecha y dinero...).
 * <p>
 * Ejemplo: {@code Ui.col("Alias", 90, Potencial::alias)} crea una columna de tabla en una línea
 * en vez de las 4-5 líneas que harían falta con JavaFX "a pelo".
 */
public final class Ui {

    /** Formato de fecha y hora: 03/10/2026 18:30 */
    private static final DateTimeFormatter FH = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Formato de solo fecha: 03/10/2026 */
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Formato de dinero en dólares: $4,437.19 */
    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);

    /** Clase de utilidades: solo métodos static. */
    private Ui() { }

    // ---------- Formatos ----------
    /** Fecha y hora con formato (o "—" si es null). */
    public static String fecha(LocalDateTime d) { return d == null ? "—" : FH.format(d); }
    /** Fecha con formato (o "—" si es null). */
    public static String fecha(LocalDate d) { return d == null ? "—" : F.format(d); }
    /** Dinero con formato de dólares (o "—" si es null). */
    public static String dinero(BigDecimal b) { return b == null ? "—" : USD.format(b); }
    /** Convierte cualquier valor a texto; null → "" (vacío). */
    public static String nvl(Object o) { return o == null ? "" : String.valueOf(o); }

    // ---------- Tablas ----------
    /**
     * Crea una columna de texto para un TableView.
     * @param titulo cabecera de la columna
     * @param ancho  ancho preferido en píxeles
     * @param valor  qué dato mostrar de cada fila, p. ej. {@code Potencial::alias}
     *              (fechas, dinero y booleanos se formatean solos)
     */
    public static <T> TableColumn<T, String> col(String titulo, double ancho, Function<T, Object> valor) {
        TableColumn<T, String> c = new TableColumn<>(titulo);
        // Para cada fila: cd.getValue() es el objeto de la fila; le aplicamos la función y lo pasamos a texto
        c.setCellValueFactory(cd -> new SimpleStringProperty(texto(valor.apply(cd.getValue()))));
        c.setPrefWidth(ancho);
        return c;
    }

    /**
     * Como {@link #col}, pero pinta el valor como una etiqueta de color (estados, tipos...).
     * El color sale del CSS: clase "badge-" + valor (p. ej. badge-completado en verde).
     */
    public static <T> TableColumn<T, String> colBadge(String titulo, double ancho, Function<T, Object> valor) {
        TableColumn<T, String> c = col(titulo, ancho, valor);
        // CellFactory = cómo se DIBUJA cada celda (aquí, como etiqueta de color en vez de texto)
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

    /** Columna de dinero alineada a la derecha: verde si es positivo, rojo si es negativo. */
    public static <T> TableColumn<T, BigDecimal> colDinero(String titulo, double ancho, Function<T, BigDecimal> valor) {
        TableColumn<T, BigDecimal> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(valor.apply(cd.getValue())));
        c.setPrefWidth(ancho);
        // Cada celda pinta el importe con formato $ y la clase CSS positivo/negativo
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

    /** Convierte el valor de una celda en texto según su tipo. */
    private static String texto(Object o) {
        if (o == null) return "";
        if (o instanceof LocalDateTime d) return fecha(d);
        if (o instanceof LocalDate d) return fecha(d);
        if (o instanceof BigDecimal b) return dinero(b);
        if (o instanceof Boolean b) return b ? "Sí" : "No";
        return String.valueOf(o);
    }

    /**
     * Crea un TableView que ocupa todo el alto disponible.
     * @param vacio texto que se ve cuando no hay filas
     */
    public static <T> TableView<T> tabla(String vacio) {
        TableView<T> t = new TableView<>();
        t.setPlaceholder(new Label(vacio));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(t, Priority.ALWAYS);
        return t;
    }

    /** Mete datos en una tabla intentando mantener seleccionada la fila que lo estaba. */
    public static <T> void cargar(TableView<T> t, List<T> datos) {
        T sel = t.getSelectionModel().getSelectedItem();
        t.setItems(FXCollections.observableArrayList(datos));
        if (sel != null) {
            for (T d : datos) if (d.equals(sel)) { t.getSelectionModel().select(d); break; }
        }
    }

    // ---------- Componentes ----------
    /** Etiqueta pequeña de color (la de los estados en las tablas). */
    public static Label badge(String texto) {
        Label l = new Label(texto.toUpperCase().replace('_', ' '));
        l.getStyleClass().addAll("badge", "badge-" + texto.toLowerCase().replace(' ', '_').replace("á", "a")
                .replace("ó", "o").replace("í", "i"));
        return l;
    }

    /** Título grande de una pantalla. */
    public static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo");
        return l;
    }

    /** Texto gris de descripción bajo el título. */
    public static Label subtitulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("subtitulo");
        l.setWrapText(true);
        return l;
    }

    /** Título pequeño en mayúsculas y color ámbar para separar zonas. */
    public static Label seccion(String texto) {
        Label l = new Label(texto.toUpperCase());
        l.getStyleClass().add("seccion");
        return l;
    }

    /** Botón normal. La acción se ejecuta dentro de {@link #ejecutar}: si falla, sale un diálogo de error. */
    public static Button boton(String texto, Runnable accion) {
        Button b = new Button(texto);
        b.setOnAction(e -> ejecutar(accion));
        return b;
    }

    /** Botón principal (amarillo) para la acción más importante de la pantalla. */
    public static Button primario(String texto, Runnable accion) {
        Button b = boton(texto, accion);
        b.getStyleClass().add("primario");
        return b;
    }

    /** Botón rojo para acciones destructivas (eliminar, cancelar). */
    public static Button peligro(String texto, Runnable accion) {
        Button b = boton(texto, accion);
        b.getStyleClass().add("peligro");
        return b;
    }

    /** Coloca varios controles en fila con 8 px de separación. */
    public static HBox fila(Node... nodos) {
        HBox h = new HBox(8, nodos);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Hueco elástico: empuja lo que va detrás hacia la derecha. */
    public static Region espacio() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /**
     * Tarjeta KPI del dashboard: un número grande y una etiqueta debajo.
     * @param clase clase CSS del color del número (k-verde, k-rojo...)
     */
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

    /** Contenedor estándar de una pantalla: título, subtítulo y el contenido debajo. */
    public static VBox pantalla(String titulo, String subtitulo, Node... contenido) {
        VBox v = new VBox(14);
        v.setPadding(new Insets(22, 26, 22, 26));
        v.getChildren().addAll(titulo(titulo), subtitulo(subtitulo));
        v.getChildren().addAll(contenido);
        v.getStyleClass().add("pantalla");
        return v;
    }

    /** ComboBox con los valores indicados (se muestra su toString()). */
    public static <E> ComboBox<E> combo(List<E> valores) {
        ComboBox<E> c = new ComboBox<>(FXCollections.observableArrayList(valores));
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    /**
     * ComboBox que muestra cada elemento con el texto que digas (útil para records).
     * Ejemplo: {@code Ui.combo(grupos, g -> g.nombre())}
     */
    public static <E> ComboBox<E> combo(List<E> valores, Function<E, String> texto) {
        ComboBox<E> c = combo(valores);
        c.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(E e) { return e == null ? "" : texto.apply(e); }
            @Override public E fromString(String s) { return null; }
        });
        return c;
    }

    // ---------- Diálogos ----------
    /**
     * Ejecuta una acción y, si lanza un error, lo enseña en un diálogo en vez de romper la app.
     * Por eso las pantallas casi no tienen try/catch.
     */
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

    /** Diálogo informativo con un botón Aceptar. */
    public static void info(String msg) {
        alerta(Alert.AlertType.INFORMATION, "San Francisco Archive", msg).showAndWait();
    }

    /** Diálogo de error. */
    public static void error(String msg) {
        alerta(Alert.AlertType.ERROR, "No se pudo completar", msg).showAndWait();
    }

    /** Diálogo Aceptar/Cancelar. Devuelve true si el usuario acepta. */
    public static boolean confirmar(String msg) {
        Alert a = alerta(Alert.AlertType.CONFIRMATION, "Confirmar", msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /** Diálogo que pide un texto. Vacío si el usuario cancela. */
    public static Optional<String> pedirTexto(String titulo, String mensaje, String valor) {
        TextInputDialog d = new TextInputDialog(valor);
        d.setTitle(titulo);
        d.setHeaderText(null);
        d.setContentText(mensaje);
        prepararDialogo(d.getDialogPane());
        return d.showAndWait();
    }

    /** Crea un Alert de JavaFX con el tema oscuro aplicado. */
    private static Alert alerta(Alert.AlertType tipo, String titulo, String msg) {
        Alert a = new Alert(tipo, msg);
        a.setTitle(titulo);
        a.setHeaderText(null);
        prepararDialogo(a.getDialogPane());
        return a;
    }

    /**
     * Aplica el CSS del tema a un diálogo y lo asocia a la ventana principal (para que salga centrado encima).
     */
    public static void prepararDialogo(javafx.scene.control.DialogPane p) {
        p.getStylesheets().add(ArchiveApp.class.getResource("tema.css").toExternalForm());
        if (ArchiveApp.stage() != null && p.getScene() != null && p.getScene().getWindow() instanceof javafx.stage.Stage s
                && s.getOwner() == null) {
            s.initOwner(ArchiveApp.stage());
        }
    }
}
