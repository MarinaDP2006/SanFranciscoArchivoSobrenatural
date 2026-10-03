package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.VinculoDao;
import com.sfarchive.core.model.Vinculo;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.List;

/** Vínculos familiares del potencial: el Archivo protege a sus familias. */
public class MisVinculosView implements Vista {

    private final VinculoDao dao = new VinculoDao();
    private VBox raiz;
    private final TableView<Vinculo> tabla = Ui.tabla("No has registrado vínculos");

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tabla.getColumns().addAll(List.of(
                Ui.col("Nombre", 160, Vinculo::nombre),
                Ui.col("Parentesco", 100, Vinculo::parentesco),
                Ui.col("Edad", 50, Vinculo::edad),
                Ui.col("Ciudad", 120, Vinculo::ciudad),
                Ui.col("Conoce el secreto", 120, Vinculo::conoceSecreto),
                Ui.colBadge("Riesgo", 90, v -> v.enRiesgo() ? "EN RIESGO" : ""),
                Ui.col("Notas", 260, Vinculo::notas)));
        raiz = Ui.pantalla("Mis vínculos",
                "Registra a tu familia y allegados. Si alguno está en riesgo, el Archivo organizará su protección.",
                Ui.fila(Ui.primario("+ Añadir", () -> editar(null)),
                        Ui.boton("Editar", () -> editar(tabla.getSelectionModel().getSelectedItem())),
                        Ui.peligro("Eliminar", this::borrar)),
                tabla);
        return raiz;
    }

    @Override
    public void refrescar() {
        Ui.cargar(tabla, dao.porPotencial(Sesion.potencialId()));
    }

    private void editar(Vinculo v) {
        VinculoDialog.mostrar(Sesion.potencialId(), v).ifPresent(n -> { dao.guardar(n); refrescar(); });
    }

    private void borrar() {
        Vinculo v = tabla.getSelectionModel().getSelectedItem();
        if (v == null || !Ui.confirmar("¿Eliminar a " + v.nombre() + "?")) return;
        dao.borrar(v.id(), Sesion.potencialId());
        refrescar();
    }
}
