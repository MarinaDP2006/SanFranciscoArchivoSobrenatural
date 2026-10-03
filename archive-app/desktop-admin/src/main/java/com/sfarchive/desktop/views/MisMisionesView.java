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

/** Panel del potencial: sus misiones (contratos asignados), iniciar y reportar finalización. */
public class MisMisionesView implements Vista {

    private final ContratoDao contratos = new ContratoDao();
    private final PotencialDao potenciales = new PotencialDao();
    private final ContratoService servicio = new ContratoService();

    private VBox raiz;
    private final HBox kpis = new HBox(10);
    private final TableView<Contrato> tabla = Ui.tabla("No tienes misiones asignadas");
    private final Label detalle = new Label("Selecciona una misión.");
    private final TextArea informe = Formulario.area(5);
    private final MapaFx mapa = new MapaFx();
    private Contrato actual;

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tabla.getColumns().addAll(List.of(
                Ui.col("Contrato", 105, Contrato::codigo),
                Ui.colBadge("Estado", 150, c -> c.estado().name()),
                Ui.colBadge("Prioridad", 90, c -> c.prioridad().name()),
                Ui.col("Incidente", 260, Contrato::incidenteTitulo),
                Ui.col("Barrio", 110, Contrato::barrio),
                Ui.colDinero("Recompensa", 100, Contrato::recompensa)));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> mostrar(n));
        detalle.setWrapText(true);
        informe.setPromptText("Informe de campo: qué encontraste y cómo lo resolviste…");

        VBox panel = new VBox(10, Ui.seccion("Misión"), detalle, informe,
                Ui.fila(Ui.boton("Iniciar misión", this::iniciar), Ui.primario("Reportar finalización", this::reportar)),
                Ui.subtitulo("Al reportar, un administrador revisa el caso y te paga la recompensa al cerrarlo."), mapa);
        VBox.setVgrow(mapa, Priority.ALWAYS);
        panel.getStyleClass().add("panel");
        SplitPane split = new SplitPane(tabla, panel);
        split.setDividerPositions(0.55);
        VBox.setVgrow(split, Priority.ALWAYS);
        raiz = Ui.pantalla("Mis misiones", "Contratos que el Archivo te ha asignado.", kpis, split);
        return raiz;
    }

    @Override
    public void refrescar() {
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

    private Contrato seleccion() {
        if (actual == null) throw new DataException("Selecciona una misión.");
        return actual;
    }

    private void iniciar() {
        servicio.iniciar(seleccion().id(), Sesion.potencialId(), Sesion.id());
        refrescar();
    }

    private void reportar() {
        Contrato c = seleccion();
        if (!Ui.confirmar("¿Enviar el informe de " + c.codigo() + " para que un administrador cierre la misión?")) return;
        servicio.reportarFinalizacion(c.id(), Sesion.potencialId(), Sesion.id(), informe.getText());
        Ui.info("Informe enviado. La misión queda pendiente de revisión.");
        refrescar();
    }
}
