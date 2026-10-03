package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.ZonaSeguraDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.TipoZona;
import com.sfarchive.core.model.ZonaSegura;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.MapaFx;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

/**
 * Pantalla Zonas seguras (solo administradores): hospitales, comisarías, refugios...
 * que la web pinta en verde en el mapa. Incluye un mapa para ver dónde están.
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class ZonasView implements Vista {

    // --- Acceso a datos ---
    private final ZonaSeguraDao dao = new ZonaSeguraDao();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final TableView<ZonaSegura> tabla = Ui.tabla("Sin zonas seguras");
    private final MapaFx mapa = new MapaFx();
    private ZonaSegura actual;

    // --- Campos del formulario ---
    private final TextField nombre = new TextField();
    private final ComboBox<TipoZona> tipo = Ui.combo(Arrays.asList(TipoZona.values()));
    private final TextField direccion = new TextField();
    private final TextField barrio = new TextField();
    private final TextField lat = new TextField();
    private final TextField lng = new TextField();
    private final TextField telefono = new TextField();
    private final TextField horario = new TextField();
    private final CheckBox activa = new CheckBox("Visible en la web");

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
                Ui.colBadge("Tipo", 90, z -> z.tipo().name()),
                Ui.col("Nombre", 230, ZonaSegura::nombre),
                Ui.col("Barrio", 110, ZonaSegura::barrio),
                Ui.col("Horario", 90, ZonaSegura::horario),
                Ui.col("Activa", 60, ZonaSegura::activa)));
        // Cuando el usuario selecciona una fila, rellenamos el formulario con sus datos
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) mostrar(n); });

        VBox ficha = new VBox(10, Ui.seccion("Zona segura"),
                new Formulario().campo("Nombre *", nombre).campo("Tipo", tipo).campo("Dirección", direccion)
                        .campo("Barrio", barrio).campo("Lat / Lng *", Ui.fila(lat, lng)).campo("Teléfono", telefono)
                        .campo("Horario", horario).campo("", activa),
                Ui.fila(Ui.primario("Guardar", this::guardar), Ui.boton("Nueva", this::limpiar), Ui.espacio(),
                        Ui.peligro("Eliminar", this::eliminar)));
        ficha.getStyleClass().add("panel");
        VBox derecha = new VBox(10, ficha, mapa);
        VBox.setVgrow(mapa, Priority.ALWAYS);
        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(tabla, derecha);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.5);
        VBox.setVgrow(split, Priority.ALWAYS);
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Zonas seguras", "Hospitales, comisarías, bomberos, refugios y templos que aparecen en verde en el mapa público.", split);
        limpiar();
        return raiz;
    }

    /** Recarga la tabla y vuelve a pintar el mapa. */
    @Override
    public void refrescar() {
        List<ZonaSegura> zs = dao.listar();
        Ui.cargar(tabla, zs);
        mapa.mostrar(zs.stream().map(z -> new MapaFx.Pin(z.lat(), z.lng(), z.activa() ? "#3ddc84" : "#5d6d68",
                z.tipo().etiqueta() + " · " + z.nombre(), Ui.nvl(z.direccion()), 7)).toList(), 37.77, -122.42, 12);
    }

    /** Copia la zona seleccionada al formulario. */
    private void mostrar(ZonaSegura z) {
        actual = z;
        nombre.setText(z.nombre());
        tipo.setValue(z.tipo());
        direccion.setText(Formulario.str(z.direccion()));
        barrio.setText(Formulario.str(z.barrio()));
        lat.setText(String.valueOf(z.lat()));
        lng.setText(String.valueOf(z.lng()));
        telefono.setText(Formulario.str(z.telefono()));
        horario.setText(Formulario.str(z.horario()));
        activa.setSelected(z.activa());
    }

    /** Vacía el formulario para una zona nueva. */
    private void limpiar() {
        actual = null;
        tabla.getSelectionModel().clearSelection();
        for (TextField t : List.of(nombre, direccion, barrio, lat, lng, telefono, horario)) t.clear();
        tipo.setValue(TipoZona.HOSPITAL);
        activa.setSelected(true);
    }

    /** Botón Guardar (crea o actualiza). */
    private void guardar() {
        Double la = Formulario.decimal(lat), ln = Formulario.decimal(lng);
        if (Formulario.texto(nombre) == null || la == null || ln == null) throw new DataException("Nombre y coordenadas son obligatorios.");
        dao.guardar(new ZonaSegura(actual == null ? 0 : actual.id(), Formulario.texto(nombre), tipo.getValue(),
                Formulario.texto(direccion), Formulario.texto(barrio), la, ln, Formulario.texto(telefono),
                Formulario.texto(horario), activa.isSelected()));
        limpiar();
        refrescar();
    }

    /** Botón Eliminar (pide confirmación). */
    private void eliminar() {
        if (actual == null || !Ui.confirmar("¿Eliminar " + actual.nombre() + "?")) return;
        dao.borrar(actual.id());
        limpiar();
        refrescar();
    }
}
