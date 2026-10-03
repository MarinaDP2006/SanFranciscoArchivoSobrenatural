package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.ContratoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.Contrato;
import com.sfarchive.core.model.EstadoContrato;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.core.service.ContratoService;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.MapaFx;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla Mis misiones (solo potenciales): sus contratos, iniciar misión y enviar el informe
 * de campo para que un administrador la cierre. Incluye un mapa con sus misiones abiertas.
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class MisMisionesView implements Vista {

    // --- Acceso a datos y reglas ---
    private final ContratoDao contratos = new ContratoDao();
    private final PotencialDao potenciales = new PotencialDao();
    private final ContratoService servicio = new ContratoService();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final HBox kpis = new HBox(10);
    private final TableView<Contrato> tabla = Ui.tabla("No tienes misiones asignadas");
    private final Label detalle = new Label("Selecciona una misión.");
    private final TextArea informe = Formulario.area(5);
    private final MapaFx mapa = new MapaFx();
    private Contrato actual;

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
                Ui.colBadge("Estado", 150, c -> c.estado().name()),
                Ui.colBadge("Prioridad", 90, c -> c.prioridad().name()),
                Ui.col("Incidente", 260, Contrato::incidenteTitulo),
                Ui.col("Barrio", 110, Contrato::barrio),
                Ui.colDinero("Recompensa", 100, Contrato::recompensa)));
        // Cuando el usuario selecciona una fila, rellenamos el formulario con sus datos
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> mostrar(n));
        detalle.setWrapText(true);
        informe.setPromptText("Informe de campo: qué encontraste y cómo lo resolviste…");

        VBox panel = new VBox(10, Ui.seccion("Misión"), detalle, informe,
                Ui.fila(Ui.boton("Iniciar misión", this::iniciar), Ui.primario("Reportar finalización", this::reportar)),
                Ui.subtitulo("Al reportar, un administrador revisa el caso y te paga la recompensa al cerrarlo."), mapa);
        VBox.setVgrow(mapa, Priority.ALWAYS);
        panel.getStyleClass().add("panel");
        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(tabla, panel);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.55);
        VBox.setVgrow(split, Priority.ALWAYS);
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Mis misiones", "Contratos que el Archivo te ha asignado.", kpis, split);
        return raiz;
    }

    /** Recarga sus datos (tarjetas), sus contratos y el mapa. */
    @Override
    public void refrescar() {
        // Id de la ficha del potencial que ha iniciado sesión
        Integer pid = Sesion.potencialId();
        if (pid == null) throw new DataException("Tu cuenta no está vinculada a ninguna ficha de potencial.");
        Potencial yo = potenciales.porId(pid).orElseThrow();
        List<Contrato> mias = contratos.porPotencial(pid);
        long activas = mias.stream().filter(c -> c.estado() == EstadoContrato.ASIGNADO || c.estado() == EstadoContrato.EN_CURSO).count();
        long completadas = mias.stream().filter(c -> c.estado() == EstadoContrato.COMPLETADO).count();
        kpis.getChildren().setAll(
                Ui.tarjeta("Alias", yo.alias(), "k-oro"),
                Ui.tarjeta("Estado", yo.estado().etiqueta(), "k-turquesa"),
                Ui.tarjeta("Grupo", yo.grupoNombre() == null ? "Sin grupo" : yo.grupoNombre(), "k-blanco"),
                Ui.tarjeta("Misiones activas", "" + activas, "k-ambar"),
                Ui.tarjeta("Completadas", "" + completadas, "k-verde"),
                Ui.tarjeta("Saldo", Ui.dinero(yo.saldo()), "k-verde"));
        Ui.cargar(tabla, mias);

        List<MapaFx.Pin> pins = new ArrayList<>();
        mias.stream().filter(c -> c.lat() != null && c.estado() != EstadoContrato.COMPLETADO && c.estado() != EstadoContrato.CANCELADO)
                .forEach(c -> pins.add(new MapaFx.Pin(c.lat(), c.lng(), MapaFx.colorTipo(c.tipo().name()), c.codigo(), c.incidenteTitulo(), 8)));
        if (yo.lat() != null) pins.add(new MapaFx.Pin(yo.lat(), yo.lng(), "#3ddc84", "TÚ · " + yo.alias(), Ui.nvl(yo.barrio()), 7));
        mapa.mostrar(pins, yo.lat() != null ? yo.lat() : 37.77, yo.lng() != null ? yo.lng() : -122.42, 12);
    }

    /** Enseña el detalle de la misión. El informe solo se puede escribir si está en curso. */
    private void mostrar(Contrato c) {
        actual = c;
        if (c == null) { detalle.setText("Selecciona una misión."); informe.clear(); return; }
        detalle.setText(c.codigo() + " · " + c.estado().etiqueta() + " · prioridad " + c.prioridad().etiqueta()
                + "\n" + c.incidenteCodigo() + " — " + c.incidenteTitulo() + " (" + Ui.nvl(c.barrio()) + ")"
                + "\nTipo: " + c.tipo().etiqueta() + " · Recompensa: " + Ui.dinero(c.recompensa())
                + (c.costeTransporte() != null ? " · Transporte: " + Ui.dinero(c.costeTransporte()) + " (" + c.distanciaKm() + " km)" : "")
                + "\nAsignada: " + Ui.fecha(c.fechaAsignacion()));
        informe.setText(Ui.nvl(c.notasCampo()));
        boolean abierta = c.estado() == EstadoContrato.ASIGNADO || c.estado() == EstadoContrato.EN_CURSO;
        informe.setEditable(abierta);
    }

    /** Devuelve la misión seleccionada o lanza error. */
    private Contrato seleccion() {
        if (actual == null) throw new DataException("Selecciona una misión.");
        return actual;
    }

    /** Botón Iniciar misión. */
    private void iniciar() {
        servicio.iniciar(seleccion().id(), Sesion.potencialId(), Sesion.id());
        refrescar();
    }

    /** Botón Reportar finalización (envía el informe de campo). */
    private void reportar() {
        Contrato c = seleccion();
        if (!Ui.confirmar("¿Enviar el informe de " + c.codigo() + " para que un administrador cierre la misión?")) return;
        servicio.reportarFinalizacion(c.id(), Sesion.potencialId(), Sesion.id(), informe.getText());
        Ui.info("Informe enviado. La misión queda pendiente de revisión.");
        refrescar();
    }
}
