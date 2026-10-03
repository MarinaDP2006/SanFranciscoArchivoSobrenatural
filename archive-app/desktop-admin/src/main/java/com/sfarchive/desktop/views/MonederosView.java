package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.MonederoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.core.model.TipoTransaccion;
import com.sfarchive.core.model.Transaccion;
import com.sfarchive.core.service.MonederoService;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Pantalla Monederos (solo administradores): saldo de cada potencial, su historial de
 * movimientos y ajustes manuales (ajuste, bonus o penalización).
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class MonederosView implements Vista {

    // --- Acceso a datos y reglas ---
    private final PotencialDao potenciales = new PotencialDao();
    private final MonederoDao monedero = new MonederoDao();
    private final MonederoService servicio = new MonederoService();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final TableView<Potencial> tabla = Ui.tabla("Sin potenciales");
    private final TableView<Transaccion> historial = Ui.tabla("Sin movimientos");
    private final Label total = new Label();
    private final Label cabecera = Ui.seccion("Historial");
    private Potencial actual;

    // --- Formulario de ajuste: solo tipos manuales (no recompensa ni transporte) ---
    private final ComboBox<TipoTransaccion> tipo = Ui.combo(List.of(TipoTransaccion.AJUSTE, TipoTransaccion.BONUS, TipoTransaccion.PENALIZACION));
    private final TextField importe = new TextField();
    private final TextField concepto = new TextField();

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
                Ui.col("Alias", 100, Potencial::alias),
                Ui.col("Nombre", 140, Potencial::nombreReal),
                Ui.colBadge("Estado", 110, p -> p.estado().name()),
                Ui.colDinero("Saldo", 120, Potencial::saldo)));
        // Cuando el usuario selecciona una fila, rellenamos el formulario con sus datos
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) mostrar(n); });

        historial.getColumns().addAll(List.of(
                Ui.col("Fecha", 125, Transaccion::fecha),
                Ui.colBadge("Tipo", 110, t -> t.tipo().name()),
                Ui.col("Concepto", 260, Transaccion::concepto),
                Ui.col("Contrato", 100, Transaccion::contratoCodigo),
                Ui.colDinero("Importe", 100, Transaccion::importe),
                Ui.colDinero("Saldo", 100, Transaccion::saldoResultante),
                Ui.col("Por", 110, Transaccion::realizadoPor)));

        tipo.setValue(TipoTransaccion.AJUSTE);
        importe.setPromptText("p. ej. 250 o -100");
        concepto.setPromptText("Motivo del movimiento");
        VBox ajuste = new VBox(8, Ui.seccion("Ajustar saldo"),
                new Formulario().campo("Tipo", tipo).campo("Importe USD", importe).campo("Concepto", concepto),
                Ui.fila(Ui.primario("Registrar movimiento", this::ajustar)),
                Ui.subtitulo("BONUS suma, PENALIZACIÓN resta y AJUSTE respeta el signo. Recompensas y transporte se generan solos desde los contratos."));
        ajuste.getStyleClass().add("panel");

        VBox derecha = new VBox(10, cabecera, historial, ajuste);
        VBox.setVgrow(historial, Priority.ALWAYS);
        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(new VBox(8, total, tabla), derecha);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.38);
        VBox.setVgrow(split, Priority.ALWAYS);
        total.getStyleClass().add("seccion");
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Monederos", "Saldo de cada potencial (USD) y su historial completo de movimientos.", split);
        return raiz;
    }

    /** Recarga saldos, el total en circulación y el historial del potencial seleccionado. */
    @Override
    public void refrescar() {
        Ui.cargar(tabla, potenciales.listar());
        total.setText("TOTAL EN CIRCULACIÓN: " + Ui.dinero(monedero.totalEnCirculacion()));
        if (actual != null) potenciales.porId(actual.id()).ifPresent(this::mostrar);
    }

    /** Carga el historial del potencial seleccionado. */
    private void mostrar(Potencial p) {
        actual = p;
        cabecera.setText(("Historial de " + p.alias() + " · saldo " + Ui.dinero(p.saldo())).toUpperCase());
        Ui.cargar(historial, monedero.historial(p.id()));
    }

    /** Botón Registrar movimiento (ver MonederoService.ajustar). */
    private void ajustar() {
        if (actual == null) throw new DataException("Selecciona un potencial.");
        var saldo = servicio.ajustar(actual.id(), tipo.getValue(), Formulario.dinero(importe), concepto.getText(), Sesion.id());
        importe.clear();
        concepto.clear();
        refrescar();
        Ui.info("Movimiento registrado. Nuevo saldo de " + actual.alias() + ": " + Ui.dinero(saldo));
    }
}
