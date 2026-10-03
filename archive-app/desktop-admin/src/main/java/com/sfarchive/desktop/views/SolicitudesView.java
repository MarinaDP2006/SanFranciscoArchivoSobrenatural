package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.SolicitudDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.EstadoSolicitud;
import com.sfarchive.core.model.Prioridad;
import com.sfarchive.core.model.SolicitudAyuda;
import com.sfarchive.core.service.SolicitudService;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

/** Avisos anónimos enviados por los ciudadanos desde el formulario "Pedir ayuda" de la web. */
public class SolicitudesView implements Vista {

    private final SolicitudDao dao = new SolicitudDao();
    private final SolicitudService servicio = new SolicitudService();

    private VBox raiz;
    private final TableView<SolicitudAyuda> tabla = Ui.tabla("No hay avisos ciudadanos");
    private SolicitudAyuda actual;

    private final Label info = new Label("Selecciona un aviso");
    private final TextArea texto = Formulario.area(6);
    private final TextField titular = new TextField();
    private final ComboBox<Prioridad> prioridad = Ui.combo(Arrays.asList(Prioridad.values()));
    private final TextField recompensa = new TextField("3000");
    private final CheckBox publicar = new CheckBox("Publicar el incidente en la web");
    private final TextField respuesta = new TextField();
    private VBox acciones;

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tabla.getColumns().addAll(List.of(
                Ui.col("Código", 95, SolicitudAyuda::codigo),
                Ui.colBadge("Estado", 110, s -> s.estado().name()),
                Ui.colBadge("Tipo", 110, s -> s.tipo().name()),
                Ui.col("Barrio", 120, SolicitudAyuda::barrio),
                Ui.col("Recibido", 125, SolicitudAyuda::recibidaEn),
                Ui.col("Descripción", 300, SolicitudAyuda::descripcion)));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> mostrar(n));
        texto.setEditable(false);
        info.setWrapText(true);
        prioridad.setValue(Prioridad.MEDIA);
        publicar.setSelected(true);
        respuesta.setPromptText("Mensaje que verá el ciudadano al consultar su código");

        acciones = new VBox(10,
                Ui.seccion("Convertir en incidente + contrato"),
                new Formulario().campo("Titular público", titular).campo("Prioridad", prioridad)
                        .campo("Recompensa USD", recompensa).campo("", publicar),
                Ui.fila(Ui.primario("Convertir y crear contrato", this::convertir),
                        Ui.boton("Marcar en revisión", this::enRevision)),
                Ui.seccion("O descartar"),
                new Formulario().campo("Respuesta", respuesta),
                Ui.fila(Ui.peligro("Descartar aviso", this::descartar)));

        VBox detalle = new VBox(10, Ui.seccion("Aviso ciudadano"), info, texto, acciones);
        detalle.getStyleClass().add("panel");
        SplitPane split = new SplitPane(tabla, detalle);
        split.setDividerPositions(0.58);
        VBox.setVgrow(split, Priority.ALWAYS);
        raiz = Ui.pantalla("Avisos ciudadanos",
                "Llegan de forma anónima desde la web. Al convertirlos se crea un incidente y un contrato SOLICITADO; el ciudadano verá la respuesta con su código.",
                Ui.fila(Ui.espacio(), Ui.boton("↻ Actualizar", this::refrescar)), split);
        mostrar(null);
        return raiz;
    }

    @Override
    public void refrescar() {
        Ui.cargar(tabla, dao.listar());
    }

    private void mostrar(SolicitudAyuda s) {
        actual = s;
        if (s == null) {
            info.setText("Selecciona un aviso de la lista.");
            texto.clear();
            acciones.setDisable(true);
            return;
        }
        info.setText(s.codigo() + " · " + s.tipo().etiqueta() + " · " + Ui.nvl(s.barrio()) + " · " + Ui.nvl(s.ubicacion())
                + "\nRecibido: " + Ui.fecha(s.recibidaEn())
                + (s.contacto() != null ? "\nContacto: " + s.contacto() : "\nSin datos de contacto")
                + (s.lat() != null ? "\nCoordenadas: " + s.lat() + ", " + s.lng() : "")
                + (s.incidenteId() != null ? "\nIncidente vinculado: #" + s.incidenteId() : ""));
        texto.setText(s.descripcion());
        boolean abierta = s.estado() == EstadoSolicitud.PENDIENTE || s.estado() == EstadoSolicitud.EN_REVISION;
        acciones.setDisable(!abierta);
        titular.setText(switch (s.tipo()) {
            case SECUESTRO -> "Denuncian un secuestro en " + Ui.nvl(s.barrio());
            case ASESINATO -> "Investigan una muerte en " + Ui.nvl(s.barrio());
            case DESAPARICION -> "Buscan a una persona desaparecida en " + Ui.nvl(s.barrio());
        });
        respuesta.clear();
    }

    private SolicitudAyuda seleccion() {
        if (actual == null) throw new DataException("Selecciona un aviso.");
        return actual;
    }

    private void convertir() {
        int contrato = servicio.convertir(seleccion().id(), titular.getText(), publicar.isSelected(), prioridad.getValue(),
                Formulario.dinero(recompensa), Sesion.id());
        refrescar();
        Ui.info("Aviso convertido. Se ha creado el contrato #" + contrato + " en estado SOLICITADO.");
    }

    private void enRevision() {
        servicio.marcarEnRevision(seleccion().id(), Sesion.id());
        refrescar();
    }

    private void descartar() {
        if (!Ui.confirmar("¿Descartar el aviso " + seleccion().codigo() + "?")) return;
        servicio.descartar(actual.id(), respuesta.getText(), Sesion.id());
        refrescar();
    }
}
