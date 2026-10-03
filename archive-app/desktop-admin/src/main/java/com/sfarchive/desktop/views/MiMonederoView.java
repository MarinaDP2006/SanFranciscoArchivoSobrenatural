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

/** Monedero del potencial: saldo y movimientos (solo lectura). */
public class MiMonederoView implements Vista {

    private final MonederoDao monedero = new MonederoDao();
    private final PotencialDao potenciales = new PotencialDao();

    private VBox raiz;
    private final HBox kpis = new HBox(10);
    private final TableView<Transaccion> tabla = Ui.tabla("Sin movimientos");

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tabla.getColumns().addAll(List.of(
                Ui.col("Fecha", 125, Transaccion::fecha),
                Ui.colBadge("Tipo", 110, t -> t.tipo().name()),
                Ui.col("Concepto", 300, Transaccion::concepto),
                Ui.col("Contrato", 100, Transaccion::contratoCodigo),
                Ui.colDinero("Importe", 100, Transaccion::importe),
                Ui.colDinero("Saldo", 100, Transaccion::saldoResultante)));
        raiz = Ui.pantalla("Mi monedero", "Tus recompensas, gastos de transporte y ajustes. Los gestiona el Archivo.", kpis, tabla);
        return raiz;
    }

    @Override
    public void refrescar() {
        int pid = Sesion.potencialId();
        List<Transaccion> tx = monedero.historial(pid);
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
