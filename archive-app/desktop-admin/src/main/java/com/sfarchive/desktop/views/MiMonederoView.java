package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.MonederoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.model.Transaccion;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pantalla Mi monedero (solo potenciales): saldo y movimientos (solo lectura).
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class MiMonederoView implements Vista {

    // --- Acceso a datos ---
    private final MonederoDao monedero = new MonederoDao();
    private final PotencialDao potenciales = new PotencialDao();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final HBox kpis = new HBox(10);
    private final TableView<Transaccion> tabla = Ui.tabla("Sin movimientos");

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
                Ui.col("Fecha", 125, Transaccion::fecha),
                Ui.colBadge("Tipo", 110, t -> t.tipo().name()),
                Ui.col("Concepto", 300, Transaccion::concepto),
                Ui.col("Contrato", 100, Transaccion::contratoCodigo),
                Ui.colDinero("Importe", 100, Transaccion::importe),
                Ui.colDinero("Saldo", 100, Transaccion::saldoResultante)));
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Mi monedero", "Tus recompensas, gastos de transporte y ajustes. Los gestiona el Archivo.", kpis, tabla);
        return raiz;
    }

    /** Recarga el historial y calcula ingresos y gastos totales con streams. */
    @Override
    public void refrescar() {
        int pid = Sesion.potencialId();
        List<Transaccion> tx = monedero.historial(pid);
        // Sumamos los importes positivos (ingresos) y, en la línea siguiente, los negativos (gastos)
        BigDecimal ingresos = tx.stream().map(Transaccion::importe).filter(b -> b.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gastos = tx.stream().map(Transaccion::importe).filter(b -> b.signum() < 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.getChildren().setAll(
                Ui.tarjeta("Saldo actual", Ui.dinero(potenciales.porId(pid).orElseThrow().saldo()), "k-verde"),
                Ui.tarjeta("Ingresos", Ui.dinero(ingresos), "k-oro"),
                Ui.tarjeta("Gastos", Ui.dinero(gastos), "k-rojo"),
                Ui.tarjeta("Movimientos", "" + tx.size(), "k-blanco"));
        Ui.cargar(tabla, tx);
    }
}
