package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.UsuarioDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.Actividad;
import com.sfarchive.core.model.Usuario;
import com.sfarchive.core.service.AuthService;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Pantalla Usuarios y actividad (solo administradores): cuentas de acceso (activar/desactivar,
 * restablecer contraseña) y el registro de auditoría de todo lo que se hace en la app.
 * <p>
 * Como todas las pantallas, implementa {@link Vista}: {@code vista()} construye los controles
 * una sola vez y {@code refrescar()} vuelve a leer los datos de MySQL cada vez que se abre.
 */
public class UsuariosView implements Vista {

    // --- Acceso a datos y reglas ---
    private final UsuarioDao dao = new UsuarioDao();
    private final ActividadDao actividad = new ActividadDao();
    private final AuthService auth = new AuthService();

    // --- Componentes de la pantalla ---
    private VBox raiz;
    private final TableView<Usuario> tabla = Ui.tabla("Sin usuarios");
    private final TableView<Actividad> log = Ui.tabla("Sin actividad");

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
                Ui.col("Usuario", 90, Usuario::username),
                Ui.col("Nombre", 140, Usuario::nombreCompleto),
                Ui.colBadge("Rol", 90, u -> u.rol().name()),
                Ui.col("Email", 200, Usuario::email),
                Ui.colBadge("Estado", 80, u -> u.activo() ? "ACTIVO" : "INACTIVO"),
                Ui.col("Último acceso", 125, Usuario::ultimoAcceso)));
        log.getColumns().addAll(List.of(
                Ui.col("Fecha", 125, Actividad::fecha),
                Ui.col("Usuario", 80, Actividad::usuario),
                Ui.col("Acción", 100, Actividad::accion),
                Ui.col("Entidad", 90, Actividad::entidad),
                Ui.col("Detalle", 300, Actividad::detalle)));
        VBox arriba = new VBox(8, Ui.fila(Ui.seccion("Cuentas"), Ui.espacio(),
                Ui.boton("Activar / desactivar", this::alternar), Ui.boton("Restablecer contraseña", this::reset)), tabla);
        VBox abajo = new VBox(8, Ui.fila(Ui.seccion("Registro de actividad (últimos 200)"), Ui.espacio(), Ui.boton("↻", this::refrescar)), log);
        // Dos paneles con una separación que se puede arrastrar (izquierda | derecha)
        SplitPane split = new SplitPane(arriba, abajo);
        // VERTICAL: un panel arriba y otro abajo
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        // Posición inicial de la separación (0.5 = mitad)
        split.setDividerPositions(0.5);
        VBox.setVgrow(split, Priority.ALWAYS);
        // Montamos la pantalla: título + descripción + contenido
        raiz = Ui.pantalla("Usuarios y actividad",
                "Solo existen cuentas para los administradores y los potenciales. Los ciudadanos nunca se registran.", split);
        return raiz;
    }

    /** Recarga cuentas y actividad. */
    @Override
    public void refrescar() {
        Ui.cargar(tabla, dao.listar());
        Ui.cargar(log, actividad.recientes(200));
    }

    /** Devuelve la cuenta seleccionada o lanza error. */
    private Usuario seleccion() {
        Usuario u = tabla.getSelectionModel().getSelectedItem();
        if (u == null) throw new DataException("Selecciona una cuenta.");
        return u;
    }

    /** Botón Activar / desactivar (no puedes desactivarte a ti misma). */
    private void alternar() {
        Usuario u = seleccion();
        if (u.id() == Sesion.id()) throw new DataException("No puedes desactivar tu propia cuenta.");
        dao.actualizar(u.id(), u.email(), u.nombreCompleto(), !u.activo());
        actividad.registrar(Sesion.id(), u.activo() ? "DESACTIVAR" : "ACTIVAR", "USUARIO", u.id(), u.username());
        refrescar();
    }

    /** Botón Restablecer contraseña: pide la nueva en un diálogo. */
    private void reset() {
        Usuario u = seleccion();
        Ui.pedirTexto("Restablecer contraseña", "Nueva contraseña para " + u.username() + ":", "").ifPresent(p -> {
            auth.restablecerPassword(Sesion.id(), u.id(), p);
            Ui.info("Contraseña actualizada.");
            refrescar();
        });
    }
}
