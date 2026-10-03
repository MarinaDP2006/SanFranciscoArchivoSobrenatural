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

/**
 * Pantalla Avisos ciudadanos (solo administradores): los avisos anónimos que envían los ciudadanos
 * desde el formulario "Pedir ayuda" de la web. Desde aquí se convierten en incidente + contrato,
 * se ponen en revisión o se descartan. El ciudadano ve el resultado con su código de seguimiento.
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class SolicitudesView implements Vista {

    // --- Acceso a datos y reglas ---
    private final SolicitudDao dao = new SolicitudDao();
    private final SolicitudService servicio = new SolicitudService();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final TableView<SolicitudAyuda> tabla = Ui.tabla("No hay avisos ciudadanos");
    private SolicitudAyuda actual;

    // --- Detalle del aviso y formulario de conversión ---
    private final Label info = new Label("Selecciona un aviso");
    private final TextArea texto = Formulario.area(6);
    private final TextField titular = new TextField();
    private final ComboBox<Prioridad> prioridad = Ui.combo(Arrays.asList(Prioridad.values()));
    private final TextField recompensa = new TextField("3000");
    private final CheckBox publicar = new CheckBox("Publicar el incidente en la web");
    private final TextField respuesta = new TextField();
    private VBox acciones;

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
                Ui.col("Código", 95, SolicitudAyuda::codigo),
                Ui.colBadge("Estado", 110, s -> s.estado().name()),
                Ui.colBadge("Tipo", 110, s -> s.tipo().name()),
                Ui.col("Barrio", 120, SolicitudAyuda::barrio),
                Ui.col("Recibido", 125, SolicitudAyuda::recibidaEn),
                Ui.col("Descripción", 300, SolicitudAyuda::descripcion)));
        // Cuando el usuario selecciona una fila, rellenamos el formulario con sus datos
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
        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(tabla, detalle);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.58);
        VBox.setVgrow(split, Priority.ALWAYS);
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Avisos ciudadanos",
                "Llegan de forma anónima desde la web. Al convertirlos se crea un incidente y un contrato SOLICITADO; el ciudadano verá la respuesta con su código.",
                Ui.fila(Ui.espacio(), Ui.boton("↻ Actualizar", this::refrescar)), split);
        mostrar(null);
        return raiz;
    }

    /** Vuelve a leer los datos de MySQL y actualiza la tabla. Se llama cada vez que entras en la pantalla. */
    @Override
    public void refrescar() {
        Ui.cargar(tabla, dao.listar());
    }

    /**
     * Enseña el aviso seleccionado y propone un titular según su tipo.
     * Los botones solo se activan si el aviso sigue abierto.
     */
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
        // Titular propuesto según el tipo (el admin puede cambiarlo antes de convertir)
        titular.setText(switch (s.tipo()) {
            case SECUESTRO -> "Denuncian un secuestro en " + Ui.nvl(s.barrio());
            case ASESINATO -> "Investigan una muerte en " + Ui.nvl(s.barrio());
            case DESAPARICION -> "Buscan a una persona desaparecida en " + Ui.nvl(s.barrio());
        });
        respuesta.clear();
    }

    /** Devuelve el aviso seleccionado o lanza error si no hay ninguno. */
    private SolicitudAyuda seleccion() {
        if (actual == null) throw new DataException("Selecciona un aviso.");
        return actual;
    }

    /** Botón Convertir: crea el incidente y el contrato (ver SolicitudService.convertir). */
    private void convertir() {
        int contrato = servicio.convertir(seleccion().id(), titular.getText(), publicar.isSelected(), prioridad.getValue(),
                Formulario.dinero(recompensa), Sesion.id());
        refrescar();
        Ui.info("Aviso convertido. Se ha creado el contrato #" + contrato + " en estado SOLICITADO.");
    }

    /** Botón Marcar en revisión. */
    private void enRevision() {
        servicio.marcarEnRevision(seleccion().id(), Sesion.id());
        refrescar();
    }

    /** Botón Descartar (pide confirmación). */
    private void descartar() {
        if (!Ui.confirmar("¿Descartar el aviso " + seleccion().codigo() + "?")) return;
        servicio.descartar(actual.id(), respuesta.getText(), Sesion.id());
        refrescar();
    }
}
