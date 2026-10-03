package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.EstadisticasDao;
import com.sfarchive.core.dao.GrupoDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.model.EstadoIncidente;
import com.sfarchive.core.model.Incidente;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.desktop.ui.MapaFx;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard mundial (solo administradores): tarjetas con contadores, la base de datos completa
 * de incidentes (verificados y no verificados) y un mapa en tiempo real con todos los pins:
 * incidentes por tipo, potenciales en verde y bases de los grupos tácticos en amarillo.
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class DashboardView implements Vista {

    // --- Acceso a datos ---
    private final IncidenteDao incidentes = new IncidenteDao();
    private final PotencialDao potenciales = new PotencialDao();
    private final GrupoDao grupos = new GrupoDao();
    private final EstadisticasDao stats = new EstadisticasDao();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final HBox kpis = new HBox(10);
    private final TableView<Incidente> tabla = Ui.tabla("Sin incidentes");
    private final ComboBox<String> filtro = Ui.combo(List.of("Todos", "Verificados", "No verificados", "Abiertos", "Cerrados"));
    private final MapaFx mapa = new MapaFx();
    private List<Incidente> todos = List.of();

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
                Ui.col("Código", 105, Incidente::codigo),
                Ui.colBadge("Tipo", 110, i -> i.tipo().name()),
                Ui.col("Título", 260, Incidente::titulo),
                Ui.col("Barrio", 120, Incidente::barrio),
                Ui.col("Fecha", 125, Incidente::fecha),
                Ui.colBadge("Estado", 140, i -> i.estado().name()),
                Ui.col("Web", 50, Incidente::publicado),
                Ui.col("Amenaza", 70, i -> "★".repeat(i.nivelAmenaza()))));
        filtro.getSelectionModel().selectFirst();
        filtro.setMaxWidth(200);
        filtro.setOnAction(e -> filtrar());

        VBox izquierda = new VBox(8, Ui.fila(Ui.seccion("Base de datos mundial de incidentes"), Ui.espacio(), filtro), tabla);
        VBox derecha = new VBox(8, Ui.fila(Ui.seccion("Mapa en tiempo real"), Ui.espacio(),
                Ui.badge("incidente"), Ui.badge("potencial")), mapa);
        VBox.setVgrow(mapa, Priority.ALWAYS);
        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(izquierda, derecha);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.56);
        VBox.setVgrow(split, Priority.ALWAYS);

        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Dashboard mundial",
                "Visión general del Archivo. Pins de colores: incidentes por tipo · verde: potenciales · amarillo: bases de grupos tácticos.",
                Ui.fila(kpis, Ui.espacio(), Ui.boton("↻ Actualizar", this::refrescar)), split);
        HBox.setHgrow(kpis, Priority.ALWAYS);
        return raiz;
    }

    /** Recarga contadores, tabla y mapa desde MySQL. */
    @Override
    public void refrescar() {
        Map<String, Long> r = stats.resumen();
        kpis.getChildren().setAll(
                Ui.tarjeta("Incidentes", "" + r.get("incidentes"), "k-blanco"),
                Ui.tarjeta("Sin verificar", "" + r.get("sinVerificar"), "k-rojo"),
                Ui.tarjeta("Contratos solicitados", "" + r.get("contratosSolicitados"), "k-oro"),
                Ui.tarjeta("Contratos activos", "" + r.get("contratosActivos"), "k-turquesa"),
                Ui.tarjeta("Potenciales disponibles", r.get("potencialesDisponibles") + "/" + r.get("potenciales"), "k-verde"),
                Ui.tarjeta("Avisos pendientes", "" + r.get("solicitudesPendientes"), "k-ambar"));
        // Guardamos todos los incidentes en memoria para poder filtrarlos sin volver a MySQL
        todos = incidentes.listar();
        filtrar();

        // Preparamos los puntos del mapa: un Pin por incidente, potencial y base de grupo
        List<MapaFx.Pin> pins = new ArrayList<>();
        for (Incidente i : todos) {
            if (i.lat() == null || i.lng() == null) continue;
            pins.add(new MapaFx.Pin(i.lat(), i.lng(), MapaFx.colorTipo(i.tipo().name()),
                    i.codigo() + " · " + i.tipo().etiqueta(), i.titulo() + " [" + i.estado().etiqueta() + "]", 7));
        }
        for (Potencial p : potenciales.listar()) {
            if (p.lat() == null || p.lng() == null) continue;
            pins.add(new MapaFx.Pin(p.lat(), p.lng(), "#3ddc84", "POTENCIAL · " + p.alias(),
                    p.habilidad() + " · " + p.estado().etiqueta() + (p.grupoNombre() != null ? " · " + p.grupoNombre() : ""), 6));
        }
        grupos.listar().stream().filter(g -> g.lat() != null && g.lng() != null).forEach(g ->
                pins.add(new MapaFx.Pin(g.lat(), g.lng(), "#e8b04b", "BASE · " + g.nombre(), g.zona() + " · " + g.miembros() + "/6", 9)));
        mapa.mostrar(pins, 37.7849, -122.40, 11);
    }

    /**
     * Aplica el filtro del ComboBox (Todos, Verificados, No verificados...) sobre la lista ya cargada,
     * sin volver a consultar la base de datos.
     */
    private void filtrar() {
        String f = filtro.getValue();
        Ui.cargar(tabla, todos.stream().filter(i -> switch (f) {
            case "Verificados" -> i.estado() != EstadoIncidente.NO_VERIFICADO;
            case "No verificados" -> i.estado() == EstadoIncidente.NO_VERIFICADO;
            case "Abiertos" -> i.estado() != EstadoIncidente.RESUELTO && i.estado() != EstadoIncidente.ARCHIVADO;
            case "Cerrados" -> i.estado() == EstadoIncidente.RESUELTO || i.estado() == EstadoIncidente.ARCHIVADO;
            default -> true;
        }).toList());
    }
}
