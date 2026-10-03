package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.GrupoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.dao.VinculoDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.EstadoPotencial;
import com.sfarchive.core.model.GrupoTactico;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.core.model.Vinculo;
import com.sfarchive.core.service.PotencialService;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Fichas de los potenciales: datos, habilidad, estado, cuenta de acceso y vínculos familiares. */
public class PotencialesView implements Vista {

    private final PotencialDao dao = new PotencialDao();
    private final GrupoDao grupos = new GrupoDao();
    private final VinculoDao vinculos = new VinculoDao();
    private final PotencialService servicio = new PotencialService();

    private VBox raiz;
    private final TableView<Potencial> tabla = Ui.tabla("Sin potenciales");
    private final TableView<Vinculo> tablaVinculos = Ui.tabla("Sin vínculos registrados");
    private Potencial actual;

    private final Label cabecera = new Label();
    private final TextField alias = new TextField();
    private final TextField nombre = new TextField();
    private final TextField edad = new TextField();
    private final TextField habilidad = new TextField();
    private final TextArea descripcion = Formulario.area(3);
    private final Spinner<Integer> nivel = new Spinner<>(1, 5, 1);
    private final ComboBox<EstadoPotencial> estado = Ui.combo(Arrays.asList(EstadoPotencial.values()));
    private final ComboBox<GrupoTactico> grupo = Ui.combo(List.of(), g -> g == null ? "" : g.nombre());
    private final TextField barrio = new TextField();
    private final TextField ciudad = new TextField();
    private final TextField pais = new TextField();
    private final TextField lat = new TextField();
    private final TextField lng = new TextField();
    private final DatePicker reclutamiento = new DatePicker();
    // Cuenta (solo al reclutar)
    private final TextField username = new TextField();
    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();
    private final TextField fondo = new TextField("500");
    private VBox cuenta;

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tabla.getColumns().addAll(List.of(
                Ui.col("Alias", 90, Potencial::alias),
                Ui.col("Nombre real", 130, Potencial::nombreReal),
                Ui.col("Habilidad", 190, Potencial::habilidad),
                Ui.col("Nv", 35, Potencial::nivel),
                Ui.colBadge("Estado", 110, p -> p.estado().name()),
                Ui.col("Grupo", 130, p -> p.grupoNombre() == null ? "— sin grupo —" : p.grupoNombre()),
                Ui.col("Ciudad", 100, Potencial::ciudad),
                Ui.colDinero("Saldo", 95, Potencial::saldo)));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) mostrar(n); });

        tablaVinculos.getColumns().addAll(List.of(
                Ui.col("Nombre", 140, Vinculo::nombre),
                Ui.col("Parentesco", 90, Vinculo::parentesco),
                Ui.col("Edad", 50, Vinculo::edad),
                Ui.col("Ciudad", 100, Vinculo::ciudad),
                Ui.col("Conoce el secreto", 110, Vinculo::conoceSecreto),
                Ui.colBadge("Riesgo", 80, v -> v.enRiesgo() ? "EN RIESGO" : ""),
                Ui.col("Notas", 200, Vinculo::notas)));
        tablaVinculos.setPrefHeight(220);

        username.setPromptText("p. ej. niebla");
        email.setPromptText("alias@sfarchive.org");
        password.setPromptText("mín. 8 caracteres, letras y números");
        cuenta = new VBox(8, Ui.seccion("Cuenta de acceso a la app (nuevo potencial)"),
                new Formulario().campo("Usuario", username).campo("Email", email).campo("Contraseña", password)
                        .campo("Fondo inicial USD", fondo));

        Formulario f = new Formulario()
                .campo("Alias *", alias).campo("Nombre real *", nombre).campo("Edad", edad)
                .campo("Habilidad *", habilidad).campo("Descripción", descripcion)
                .campo("Nivel (1-5)", nivel).campo("Estado", estado).campo("Grupo táctico", grupo)
                .campo("Barrio", barrio).campo("Ciudad / país", Ui.fila(ciudad, pais))
                .campo("Lat / Lng", Ui.fila(lat, lng)).campo("Reclutamiento", reclutamiento);
        VBox ficha = new VBox(10, cabecera, f, cuenta,
                Ui.fila(Ui.primario("Guardar", this::guardar), Ui.boton("Reclutar nuevo", this::limpiar)));
        ficha.getStyleClass().add("panel");
        ScrollPane scrollFicha = new ScrollPane(ficha);
        scrollFicha.setFitToWidth(true);

        VBox panelVinculos = new VBox(10, Ui.seccion("Vínculos familiares del potencial seleccionado"), tablaVinculos,
                Ui.fila(Ui.boton("+ Añadir vínculo", () -> editarVinculo(null)),
                        Ui.boton("Editar", () -> editarVinculo(tablaVinculos.getSelectionModel().getSelectedItem())),
                        Ui.peligro("Eliminar", this::borrarVinculo)));
        panelVinculos.getStyleClass().add("panel");

        TabPane tabs = new TabPane(new Tab("Ficha", scrollFicha), new Tab("Vínculos familiares", panelVinculos));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        SplitPane split = new SplitPane(tabla, tabs);
        split.setDividerPositions(0.58);
        VBox.setVgrow(split, Priority.ALWAYS);
        raiz = Ui.pantalla("Potenciales",
                "La red de agentes del Archivo. Cada potencial tiene su propia cuenta en esta aplicación para ver sus misiones, su monedero y sus vínculos.",
                split);
        limpiar();
        return raiz;
    }

    @Override
    public void refrescar() {
        List<GrupoTactico> gs = new ArrayList<>();
        gs.add(null);
        gs.addAll(grupos.listar());
        grupo.setItems(FXCollections.observableArrayList(gs));
        Ui.cargar(tabla, dao.listar());
        if (actual != null) dao.porId(actual.id()).ifPresent(this::mostrar);
    }

    private void mostrar(Potencial p) {
        actual = p;
        cabecera.setText(p.alias().toUpperCase() + " · @" + Ui.nvl(p.username()) + " · saldo " + Ui.dinero(p.saldo()));
        cabecera.getStyleClass().setAll("label", "seccion");
        alias.setText(p.alias());
        nombre.setText(p.nombreReal());
        edad.setText(Formulario.str(p.edad()));
        habilidad.setText(p.habilidad());
        descripcion.setText(Formulario.str(p.descripcion()));
        nivel.getValueFactory().setValue(p.nivel());
        estado.setValue(p.estado());
        grupo.setValue(grupo.getItems().stream().filter(g -> g != null && p.grupoId() != null && g.id() == p.grupoId()).findFirst().orElse(null));
        barrio.setText(Formulario.str(p.barrio()));
        ciudad.setText(p.ciudad());
        pais.setText(p.pais());
        lat.setText(Formulario.str(p.lat()));
        lng.setText(Formulario.str(p.lng()));
        reclutamiento.setValue(p.fechaReclutamiento());
        cuenta.setVisible(false);
        cuenta.setManaged(false);
        Ui.cargar(tablaVinculos, vinculos.porPotencial(p.id()));
    }

    private void limpiar() {
        actual = null;
        tabla.getSelectionModel().clearSelection();
        cabecera.setText("NUEVO POTENCIAL");
        cabecera.getStyleClass().setAll("label", "seccion");
        for (TextField t : List.of(alias, nombre, edad, habilidad, barrio, lat, lng, username, email)) t.clear();
        password.clear();
        descripcion.clear();
        ciudad.setText("San Francisco");
        pais.setText("Estados Unidos");
        nivel.getValueFactory().setValue(1);
        estado.setValue(EstadoPotencial.DISPONIBLE);
        grupo.setValue(null);
        reclutamiento.setValue(LocalDate.now());
        cuenta.setVisible(true);
        cuenta.setManaged(true);
        tablaVinculos.getItems().clear();
    }

    private void guardar() {
        Potencial p = new Potencial(actual == null ? 0 : actual.id(), actual == null ? null : actual.usuarioId(), null,
                Formulario.texto(alias), Formulario.texto(nombre), Formulario.entero(edad), Formulario.texto(habilidad),
                Formulario.texto(descripcion), nivel.getValue(), estado.getValue(), Formulario.texto(barrio),
                Formulario.texto(ciudad) == null ? "San Francisco" : Formulario.texto(ciudad),
                Formulario.texto(pais) == null ? "Estados Unidos" : Formulario.texto(pais),
                Formulario.decimal(lat), Formulario.decimal(lng), grupo.getValue() == null ? null : grupo.getValue().id(),
                null, actual == null ? BigDecimal.ZERO : actual.saldo(), reclutamiento.getValue());
        if (actual == null) {
            if (p.grupoId() != null && grupo.getValue().miembros() >= GrupoDao.MAX_MIEMBROS)
                throw new DataException("Ese grupo ya tiene 6 potenciales.");
            int id = servicio.reclutar(p, Formulario.texto(username), Formulario.texto(email), password.getText(),
                    Formulario.dinero(fondo), Sesion.id());
            Ui.info(p.alias() + " reclutado. Ya puede iniciar sesión con el usuario \"" + username.getText().trim() + "\".");
            actual = dao.porId(id).orElse(null);
        } else {
            servicio.actualizar(p, Sesion.id());
        }
        refrescar();
    }

    private void editarVinculo(Vinculo v) {
        if (actual == null) throw new DataException("Selecciona primero un potencial.");
        VinculoDialog.mostrar(actual.id(), v).ifPresent(nuevo -> {
            vinculos.guardar(nuevo);
            Ui.cargar(tablaVinculos, vinculos.porPotencial(actual.id()));
        });
    }

    private void borrarVinculo() {
        Vinculo v = tablaVinculos.getSelectionModel().getSelectedItem();
        if (v == null || !Ui.confirmar("¿Eliminar el vínculo con " + v.nombre() + "?")) return;
        vinculos.borrar(v.id(), v.potencialId());
        Ui.cargar(tablaVinculos, vinculos.porPotencial(v.potencialId()));
    }
}
