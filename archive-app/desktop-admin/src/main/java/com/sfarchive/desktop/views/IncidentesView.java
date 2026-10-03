package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.EstadoIncidente;
import com.sfarchive.core.model.Incidente;
import com.sfarchive.core.model.OrigenIncidente;
import com.sfarchive.core.model.Prioridad;
import com.sfarchive.core.model.TipoIncidente;
import com.sfarchive.core.service.ContratoService;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Pantalla Incidentes (solo administradores): alta, edición, verificación y publicación en la web.
 * La casilla "Publicado en la web" decide si el incidente aparece en el feed y en el mapa público;
 * la anomalía clasificada nunca se publica.
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class IncidentesView implements Vista {

    // --- Acceso a datos y reglas ---
    private final IncidenteDao dao = new IncidenteDao();
    private final ContratoService contratos = new ContratoService();
    private final ActividadDao actividad = new ActividadDao();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final TableView<Incidente> tabla = Ui.tabla("Sin incidentes");
    private final TextField buscar = new TextField();
    private List<Incidente> todos = List.of();
    private Incidente actual;

    // --- Campos del formulario ---
    private final Label codigo = new Label("NUEVO");
    private final TextField titulo = new TextField();
    private final ComboBox<TipoIncidente> tipo = Ui.combo(Arrays.asList(TipoIncidente.values()));
    private final ComboBox<EstadoIncidente> estado = Ui.combo(Arrays.asList(EstadoIncidente.values()));
    private final Spinner<Integer> amenaza = new Spinner<>(1, 5, 2);
    private final TextField barrio = new TextField();
    private final TextField direccion = new TextField();
    private final TextField ciudad = new TextField("San Francisco");
    private final TextField pais = new TextField("Estados Unidos");
    private final TextField lat = new TextField();
    private final TextField lng = new TextField();
    private final DatePicker dia = new DatePicker(LocalDate.now());
    private final TextField hora = new TextField("12:00");
    private final TextArea descripcion = Formulario.area(4);
    private final TextArea anomalia = Formulario.area(3);
    private final CheckBox publicado = new CheckBox("Publicado en la web pública");

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
                Ui.col("Título", 250, Incidente::titulo),
                Ui.col("Barrio", 110, Incidente::barrio),
                Ui.colBadge("Estado", 140, i -> i.estado().name()),
                Ui.col("Origen", 90, Incidente::origen),
                Ui.colBadge("Web", 90, i -> i.publicado() ? "PUBLICADO" : "")));
        // Cuando el usuario selecciona una fila, rellenamos el formulario con sus datos
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) mostrar(n); });
        buscar.setPromptText("Buscar por título, barrio o código…");
        buscar.textProperty().addListener((o, a, n) -> filtrar());

        amenaza.setEditable(true);
        // Formulario: cada .campo("Etiqueta", control) es una fila etiqueta + campo
        Formulario f = new Formulario()
                .campo("Código", codigo)
                .campo("Titular *", titulo)
                .campo("Tipo *", tipo)
                .campo("Estado", estado)
                .campo("Amenaza (1-5)", amenaza)
                .campo("Fecha y hora", Ui.fila(dia, hora))
                .campo("Barrio", barrio)
                .campo("Dirección", direccion)
                .campo("Ciudad / país", Ui.fila(ciudad, pais))
                .campo("Lat / Lng", Ui.fila(lat, lng))
                .campo("Texto público *", descripcion)
                .campo("Anomalía (privado)", anomalia)
                .campo("", publicado);
        hora.setPrefWidth(70);

        VBox formulario = new VBox(10, Ui.seccion("Ficha del incidente"), f,
                Ui.fila(Ui.primario("Guardar", this::guardar), Ui.boton("Nuevo", this::limpiar),
                        Ui.boton("Crear contrato", this::crearContrato), Ui.espacio(), Ui.peligro("Eliminar", this::eliminar)),
                avisoWeb());
        formulario.getStyleClass().add("panel");
        ScrollPane scroll = new ScrollPane(formulario);
        scroll.setFitToWidth(true);

        VBox izquierda = new VBox(8, buscar, tabla);
        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(izquierda, scroll);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.55);
        VBox.setVgrow(split, Priority.ALWAYS);
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Incidentes",
                "Lo que el público ve como un suceso normal. Marca \"Publicado\" para que aparezca en el feed y el mapa de la web; la anomalía nunca se publica.",
                split);
        limpiar();
        return raiz;
    }

    /** Recuadro informativo que recuerda cuándo aparecen los cambios en la web. */
    private Node avisoWeb() {
        Label l = new Label("Los cambios publicados aparecen en la web en menos de 30 segundos (auto-refresco del feed).");
        l.setWrapText(true);
        l.getStyleClass().add("aviso");
        return l;
    }

    /** Vuelve a leer los datos de MySQL y actualiza la tabla. Se llama cada vez que entras en la pantalla. */
    @Override
    public void refrescar() {
        todos = dao.listar();
        filtrar();
    }

    /** Filtra la tabla con el texto del buscador (título, barrio o código). */
    private void filtrar() {
        String q = buscar.getText() == null ? "" : buscar.getText().toLowerCase();
        Ui.cargar(tabla, todos.stream().filter(i -> q.isBlank()
                || (i.titulo() + " " + i.barrio() + " " + i.codigo()).toLowerCase().contains(q)).toList());
    }

    /** Copia los datos del incidente seleccionado en los campos del formulario. */
    private void mostrar(Incidente i) {
        actual = i;
        codigo.setText(i.codigo() + " · origen " + i.origen().etiqueta());
        titulo.setText(i.titulo());
        tipo.setValue(i.tipo());
        estado.setValue(i.estado());
        amenaza.getValueFactory().setValue(i.nivelAmenaza());
        barrio.setText(Formulario.str(i.barrio()));
        direccion.setText(Formulario.str(i.direccion()));
        ciudad.setText(i.ciudad());
        pais.setText(i.pais());
        lat.setText(Formulario.str(i.lat()));
        lng.setText(Formulario.str(i.lng()));
        dia.setValue(i.fecha().toLocalDate());
        hora.setText(i.fecha().toLocalTime().toString().substring(0, 5));
        descripcion.setText(i.descripcionPublica());
        anomalia.setText(Formulario.str(i.anomalia()));
        publicado.setSelected(i.publicado());
    }

    /** Vacía el formulario para crear un incidente nuevo. */
    private void limpiar() {
        actual = null;
        tabla.getSelectionModel().clearSelection();
        codigo.setText("NUEVO (el código se genera al guardar)");
        titulo.clear(); barrio.clear(); direccion.clear(); lat.clear(); lng.clear();
        descripcion.clear(); anomalia.clear();
        ciudad.setText("San Francisco");
        pais.setText("Estados Unidos");
        tipo.setValue(TipoIncidente.DESAPARICION);
        estado.setValue(EstadoIncidente.NO_VERIFICADO);
        amenaza.getValueFactory().setValue(2);
        dia.setValue(LocalDate.now());
        hora.setText(LocalTime.now().withSecond(0).withNano(0).toString().substring(0, 5));
        publicado.setSelected(false);
    }

    /**
     * Lee y valida los campos del formulario y construye un Incidente con ellos.
     * Si algo está mal lanza DataException (se muestra en un diálogo).
     */
    private Incidente leer() {
        String t = Formulario.texto(titulo);
        String d = Formulario.texto(descripcion);
        if (t == null) throw new DataException("El titular es obligatorio.");
        if (d == null) throw new DataException("El texto público es obligatorio.");
        LocalTime h;
        try {
            h = LocalTime.parse(hora.getText().trim());
        } catch (DateTimeParseException e) {
            throw new DataException("La hora debe tener el formato HH:mm.");
        }
        if (dia.getValue() == null) throw new DataException("Indica la fecha del incidente.");
        Double la = Formulario.decimal(lat), ln = Formulario.decimal(lng);
        if ((la == null) != (ln == null)) throw new DataException("Indica latitud y longitud, o ninguna.");
        if (la != null && (la < -90 || la > 90 || ln < -180 || ln > 180)) throw new DataException("Coordenadas fuera de rango.");
        return new Incidente(actual == null ? 0 : actual.id(), actual == null ? null : actual.codigo(), t, tipo.getValue(), d,
                Formulario.texto(barrio), Formulario.texto(direccion),
                Formulario.texto(ciudad) == null ? "San Francisco" : Formulario.texto(ciudad),
                Formulario.texto(pais) == null ? "Estados Unidos" : Formulario.texto(pais), la, ln,
                LocalDateTime.of(dia.getValue(), h), estado.getValue(), publicado.isSelected(), Formulario.texto(anomalia),
                amenaza.getValue(), actual == null ? OrigenIncidente.ARCHIVO : actual.origen(),
                actual == null ? Sesion.id() : actual.creadoPor());
    }

    /** Botón Guardar: crea el incidente (si es nuevo) o lo actualiza. */
    private void guardar() {
        Incidente i = leer();
        if (i.id() == 0) {
            int id = dao.crear(i);
            actividad.registrar(Sesion.id(), "CREAR", "INCIDENTE", id, i.titulo());
            refrescar();
            todos.stream().filter(x -> x.id() == id).findFirst().ifPresent(x -> tabla.getSelectionModel().select(x));
        } else {
            dao.actualizar(i);
            actividad.registrar(Sesion.id(), i.publicado() ? "PUBLICAR" : "EDITAR", "INCIDENTE", i.id(), i.titulo());
            refrescar();
        }
        Ui.info(i.publicado() ? "Incidente guardado y visible en la web." : "Incidente guardado (no publicado en la web).");
    }

    /**
     * Botón Crear contrato: pide la recompensa y crea un contrato SOLICITADO.
     * La prioridad se deduce del nivel de amenaza.
     */
    private void crearContrato() {
        if (actual == null) throw new DataException("Selecciona primero un incidente.");
        String r = Ui.pedirTexto("Nuevo contrato", "Recompensa (USD) para " + actual.codigo() + ":", "3000").orElse(null);
        if (r == null) return;
        BigDecimal recompensa;
        try {
            recompensa = new BigDecimal(r.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new DataException("Importe no válido.");
        }
        Prioridad p = actual.nivelAmenaza() >= 5 ? Prioridad.CRITICA : actual.nivelAmenaza() >= 4 ? Prioridad.ALTA
                : actual.nivelAmenaza() >= 2 ? Prioridad.MEDIA : Prioridad.BAJA;
        contratos.crear(actual.id(), p, recompensa, Sesion.id());
        Ui.info("Contrato creado en estado SOLICITADO (prioridad " + p.etiqueta() + "). Asígnalo desde la pantalla Contratos.");
    }

    /** Botón Eliminar (pide confirmación). */
    private void eliminar() {
        if (actual == null) return;
        if (!Ui.confirmar("¿Eliminar " + actual.codigo() + "? Se borrarán también sus contratos e informes.")) return;
        dao.borrar(actual.id());
        actividad.registrar(Sesion.id(), "BORRAR", "INCIDENTE", actual.id(), actual.titulo());
        limpiar();
        refrescar();
    }
}
