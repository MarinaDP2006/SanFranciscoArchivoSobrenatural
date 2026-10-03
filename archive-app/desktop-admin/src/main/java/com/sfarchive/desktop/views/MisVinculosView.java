package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.VinculoDao;
import com.sfarchive.core.model.Vinculo;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Pantalla Mis vínculos (solo potenciales): su familia y allegados.
 * Solo puede ver y tocar los suyos (todas las consultas filtran por su id).
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class MisVinculosView implements Vista {

    // --- Acceso a datos ---
    private final VinculoDao dao = new VinculoDao();
    private VBox raiz;
    private final TableView<Vinculo> tabla = Ui.tabla("No has registrado vínculos");

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
                Ui.col("Nombre", 160, Vinculo::nombre),
                Ui.col("Parentesco", 100, Vinculo::parentesco),
                Ui.col("Edad", 50, Vinculo::edad),
                Ui.col("Ciudad", 120, Vinculo::ciudad),
                Ui.col("Conoce el secreto", 120, Vinculo::conoceSecreto),
                Ui.colBadge("Riesgo", 90, v -> v.enRiesgo() ? "EN RIESGO" : ""),
                Ui.col("Notas", 260, Vinculo::notas)));
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Mis vínculos",
                "Registra a tu familia y allegados. Si alguno está en riesgo, el Archivo organizará su protección.",
                Ui.fila(Ui.primario("+ Añadir", () -> editar(null)),
                        Ui.boton("Editar", () -> editar(tabla.getSelectionModel().getSelectedItem())),
                        Ui.peligro("Eliminar", this::borrar)),
                tabla);
        return raiz;
    }

    /** Vuelve a leer los datos de MySQL y actualiza la tabla. Se llama cada vez que entras en la pantalla. */
    @Override
    public void refrescar() {
        Ui.cargar(tabla, dao.porPotencial(Sesion.potencialId()));
    }

    /** Abre el diálogo para añadir (v = null) o editar un vínculo. */
    private void editar(Vinculo v) {
        VinculoDialog.mostrar(Sesion.potencialId(), v).ifPresent(n -> { dao.guardar(n); refrescar(); });
    }

    /** Borra el vínculo seleccionado (pide confirmación). */
    private void borrar() {
        Vinculo v = tabla.getSelectionModel().getSelectedItem();
        if (v == null || !Ui.confirmar("¿Eliminar a " + v.nombre() + "?")) return;
        dao.borrar(v.id(), Sesion.potencialId());
        refrescar();
    }
}
