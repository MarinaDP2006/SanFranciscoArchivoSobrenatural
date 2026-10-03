package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.ContratoDao;
import com.sfarchive.core.dao.GrupoDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.Contrato;
import com.sfarchive.core.model.EstadoContrato;
import com.sfarchive.core.model.EstadoPotencial;
import com.sfarchive.core.model.GrupoTactico;
import com.sfarchive.core.model.Incidente;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.core.model.Prioridad;
import com.sfarchive.core.service.ContratoService;
import com.sfarchive.core.service.Geo;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Gestión de contratos: lista de SOLICITADOS, selector de grupo táctico + potencial y
 * botón ASIGNAR (genera el gasto de transporte automático). También cierra contratos.
 */
public class ContratosView implements Vista {

    private final ContratoDao dao = new ContratoDao();
    private final GrupoDao grupos = new GrupoDao();
    private final PotencialDao potenciales = new PotencialDao();
    private final IncidenteDao incidentes = new IncidenteDao();
    private final ContratoService servicio = new ContratoService();

    private VBox raiz;
    private final TableView<Contrato> tabla = Ui.tabla("No hay contratos");
    private final ComboBox<String> filtro = Ui.combo(new ArrayList<>());
    private List<Contrato> todos = List.of();
    private Contrato actual;

    private final Label detalle = new Label();
    private final TextArea notas = Formulario.area(4);
    private final ComboBox<GrupoTactico> grupo = Ui.combo(List.of(), g -> g.nombre() + " (" + g.miembros() + "/6)");
    private final ComboBox<Potencial> potencial = Ui.combo(List.of(),
            p -> p.alias() + " · nv " + p.nivel() + " · " + p.habilidad());
    private final Label transporte = new Label("—");
    private final ComboBox<Prioridad> prioridad = Ui.combo(Arrays.asList(Prioridad.values()));
    private final TextField recompensa = new TextField();
    private VBox panelAsignar;
    private VBox panelCierre;

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        List<String> estados = new ArrayList<>(List.of("Todos", "Abiertos"));
        Arrays.stream(EstadoContrato.values()).forEach(e -> estados.add(e.name()));
        filtro.setItems(FXCollections.observableArrayList(estados));
        filtro.setValue("SOLICITADO");
        filtro.setMaxWidth(220);
        filtro.setOnAction(e -> filtrar());

        tabla.getColumns().addAll(List.of(
                Ui.col("Contrato", 105, Contrato::codigo),
                Ui.colBadge("Estado", 150, c -> c.estado().name()),
                Ui.colBadge("Prioridad", 90, c -> c.prioridad().name()),
                Ui.col("Incidente", 230, c -> c.incidenteCodigo() + " · " + c.incidenteTitulo()),
                Ui.col("Grupo", 130, Contrato::grupoNombre),
                Ui.col("Potencial", 90, Contrato::potencialAlias),
                Ui.colDinero("Recompensa", 100, Contrato::recompensa),
                Ui.colDinero("Transporte", 90, c -> c.costeTransporte() == null ? null : c.costeTransporte().negate())));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> mostrar(n));

        grupo.setOnAction(e -> cargarPotenciales());
        potencial.setOnAction(e -> calcularTransporte());
        notas.setEditable(false);
        detalle.setWrapText(true);
        transporte.getStyleClass().add("negativo");

        panelAsignar = new VBox(10,
                Ui.seccion("Asignar contrato"),
                new Formulario().campo("Grupo táctico", grupo).campo("Potencial", potencial).campo("Transporte", transporte),
                Ui.fila(Ui.primario("ASIGNAR", this::asignar)),
                Ui.seccion("Datos del contrato"),
                new Formulario().campo("Prioridad", prioridad).campo("Recompensa USD", recompensa),
                Ui.fila(Ui.boton("Guardar cambios", this::guardarDatos), Ui.espacio(), Ui.peligro("Cancelar contrato", this::cancelar)));
        Label ayudaCierre = new Label("Completar paga la recompensa al potencial, lo deja DISPONIBLE y marca el incidente como RESUELTO (CERRADO en la web).");
        ayudaCierre.setWrapText(true);
        ayudaCierre.getStyleClass().add("subtitulo");
        panelCierre = new VBox(10, Ui.seccion("Cierre del contrato"), ayudaCierre,
                Ui.fila(Ui.primario("Completar y pagar", this::completar), Ui.boton("Marcar fallido", this::fallido),
                        Ui.espacio(), Ui.peligro("Cancelar", this::cancelar)));

        VBox panel = new VBox(12, Ui.seccion("Detalle"), detalle, new Label("Notas de campo:"), notas, panelAsignar, panelCierre);
        panel.getStyleClass().add("panel");
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);

        VBox izquierda = new VBox(8, Ui.fila(new Label("Estado:"), filtro, Ui.espacio(),
                Ui.boton("+ Nuevo contrato", this::nuevo), Ui.boton("↻", this::refrescar)), tabla);
        SplitPane split = new SplitPane(izquierda, scroll);
        split.setDividerPositions(0.6);
        VBox.setVgrow(split, Priority.ALWAYS);
        raiz = Ui.pantalla("Gestión de contratos",
                String.format("Selecciona un contrato SOLICITADO, elige grupo táctico y potencial disponible y pulsa ASIGNAR. "
                        + "Transporte automático: %s USD + %s USD/km desde la posición del potencial.", Geo.TARIFA_BASE, Geo.TARIFA_KM),
                split);
        mostrar(null);
        return raiz;
    }

    @Override
    public void refrescar() {
        todos = dao.listar();
        filtrar();
        grupo.setItems(FXCollections.observableArrayList(grupos.listar().stream().filter(GrupoTactico::activo).toList()));
        if (actual != null) todos.stream().filter(c -> c.id() == actual.id()).findFirst()
                .ifPresentOrElse(c -> tabla.getSelectionModel().select(c), () -> mostrar(null));
    }

    private void filtrar() {
        String f = filtro.getValue();
        Ui.cargar(tabla, todos.stream().filter(c -> switch (f) {
            case "Todos" -> true;
            case "Abiertos" -> c.estado() == EstadoContrato.ASIGNADO || c.estado() == EstadoContrato.EN_CURSO
                    || c.estado() == EstadoContrato.PENDIENTE_REVISION;
            default -> c.estado().name().equals(f);
        }).toList());
    }

    private void mostrar(Contrato c) {
        actual = c;
        if (c == null) {
            detalle.setText("Selecciona un contrato.");
            notas.clear();
            panelAsignar.setVisible(false); panelAsignar.setManaged(false);
            panelCierre.setVisible(false); panelCierre.setManaged(false);
            return;
        }
        detalle.setText(c.codigo() + " · " + c.estado().etiqueta() + " · prioridad " + c.prioridad().etiqueta()
                + "\nIncidente: " + c.incidenteCodigo() + " — " + c.incidenteTitulo() + " (" + Ui.nvl(c.barrio()) + ")"
                + "\nRecompensa: " + Ui.dinero(c.recompensa())
                + (c.potencialAlias() != null ? "\nAsignado a: " + c.potencialAlias() + " · " + c.grupoNombre()
                + " · transporte " + Ui.dinero(c.costeTransporte()) + " (" + c.distanciaKm() + " km)" : "")
                + "\nSolicitado: " + Ui.fecha(c.fechaSolicitud())
                + (c.fechaAsignacion() != null ? " · Asignado: " + Ui.fecha(c.fechaAsignacion()) : "")
                + (c.fechaCierre() != null ? " · Cerrado: " + Ui.fecha(c.fechaCierre()) : "")
                + (c.tieneInforme() ? "\n✔ Tiene informe en el Archivo Restringido" : ""));
        notas.setText(Ui.nvl(c.notasCampo()));
        boolean solicitado = c.estado() == EstadoContrato.SOLICITADO;
        boolean abierto = c.estado() == EstadoContrato.ASIGNADO || c.estado() == EstadoContrato.EN_CURSO
                || c.estado() == EstadoContrato.PENDIENTE_REVISION;
        panelAsignar.setVisible(solicitado); panelAsignar.setManaged(solicitado);
        panelCierre.setVisible(abierto); panelCierre.setManaged(abierto);
        prioridad.setValue(c.prioridad());
        recompensa.setText(c.recompensa().toPlainString());
        grupo.setValue(null);
        potencial.getItems().clear();
        transporte.setText("—");
    }

    private void cargarPotenciales() {
        GrupoTactico g = grupo.getValue();
        potencial.getItems().setAll(g == null ? List.of() : potenciales.porGrupo(g.id()).stream()
                .filter(p -> p.estado() == EstadoPotencial.DISPONIBLE).toList());
        transporte.setText(g != null && potencial.getItems().isEmpty() ? "No hay potenciales disponibles en este grupo" : "—");
    }

    private void calcularTransporte() {
        if (actual == null || potencial.getValue() == null) { transporte.setText("—"); return; }
        var t = servicio.calcularTransporte(actual, potencial.getValue());
        transporte.setText(Ui.dinero(t.coste()) + "  (" + t.distanciaKm() + " km) — se descontará del monedero");
    }

    private Contrato seleccion() {
        if (actual == null) throw new DataException("Selecciona un contrato.");
        return actual;
    }

    private void asignar() {
        Contrato c = seleccion();
        if (grupo.getValue() == null || potencial.getValue() == null)
            throw new DataException("Elige un grupo táctico y un potencial disponible.");
        var t = servicio.asignar(c.id(), grupo.getValue().id(), potencial.getValue().id(), Sesion.id());
        Ui.info(c.codigo() + " asignado a " + potencial.getValue().alias() + ".\nGasto de transporte generado: "
                + Ui.dinero(t.coste()) + " (" + t.distanciaKm() + " km).");
        filtro.setValue("Abiertos");
        refrescar();
    }

    private void guardarDatos() {
        Contrato c = seleccion();
        dao.actualizarDatos(c.id(), prioridad.getValue(), Formulario.dinero(recompensa));
        refrescar();
    }

    private void completar() {
        Contrato c = seleccion();
        if (!Ui.confirmar("¿Completar " + c.codigo() + " y pagar " + Ui.dinero(c.recompensa()) + " a " + c.potencialAlias() + "?")) return;
        servicio.completar(c.id(), Sesion.id());
        Ui.info("Contrato completado. Recuerda redactar el Informe Final en el Archivo Restringido.");
        refrescar();
    }

    private void fallido() {
        Contrato c = seleccion();
        String motivo = Ui.pedirTexto("Contrato fallido", "Motivo del fallo:", "").orElse(null);
        if (motivo == null) return;
        servicio.marcarFallido(c.id(), Sesion.id(), motivo);
        refrescar();
    }

    private void cancelar() {
        Contrato c = seleccion();
        if (!Ui.confirmar("¿Cancelar " + c.codigo() + "?" + (c.potencialId() != null ? " Se reembolsará el transporte." : ""))) return;
        servicio.cancelar(c.id(), Sesion.id());
        refrescar();
    }

    private void nuevo() {
        List<Incidente> libres = incidentes.sinContratoActivo();
        if (libres.isEmpty()) throw new DataException("Todos los incidentes abiertos ya tienen un contrato activo.");
        ChoiceDialog<Incidente> d = new ChoiceDialog<>(libres.get(0), libres) {
            { setTitle("Nuevo contrato"); setHeaderText(null); setContentText("Incidente:"); }
        };
        d.getItems().setAll(libres);
        Ui.prepararDialogo(d.getDialogPane());
        // Mostrar texto legible en el combo del diálogo
        d.getDialogPane().lookupAll(".combo-box").forEach(n -> {
            @SuppressWarnings("unchecked") ComboBox<Incidente> cb = (ComboBox<Incidente>) n;
            cb.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Incidente i) { return i == null ? "" : i.codigo() + " · " + i.titulo(); }
                @Override public Incidente fromString(String s) { return null; }
            });
        });
        d.showAndWait().ifPresent(i -> {
            String r = Ui.pedirTexto("Recompensa", "Recompensa (USD):", "3000").orElse(null);
            if (r == null) return;
            try {
                servicio.crear(i.id(), Prioridad.MEDIA, new java.math.BigDecimal(r.trim().replace(",", ".")), Sesion.id());
            } catch (NumberFormatException e) {
                throw new DataException("Importe no válido.");
            }
            filtro.setValue("SOLICITADO");
            refrescar();
        });
    }
}
