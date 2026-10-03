package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.SolicitudDao;
import com.sfarchive.desktop.ArchiveApp;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ventana principal tras el login: menú lateral a la izquierda y la pantalla elegida en el centro.
 * <p>
 * El menú cambia según el rol: los administradores ven todas las pantallas de gestión y los
 * potenciales solo las suyas. Las pantallas se crean la primera vez que se abren y se guardan
 * en una caché para no reconstruirlas cada vez.
 */
public class MainView {

    // BorderPane: left = menú, center = pantalla actual
    private final BorderPane raiz = new BorderPane();
    // Pantallas ya creadas, por nombre (para reutilizarlas)
    private final Map<String, Vista> cache = new LinkedHashMap<>();
    // Botones del menú (para marcar cuál está activo)
    private final List<Button> botones = new ArrayList<>();

    /** Construye el menú lateral (según el rol) y abre la primera pantalla. */
    public Parent vista() {
        VBox menu = new VBox();
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(240);

        Label marca = new Label("SF ARCHIVE");
        marca.getStyleClass().add("marca");
        Label sub = new Label("San Francisco · Gestión de anomalías");
        sub.getStyleClass().add("marca-sub");
        menu.getChildren().addAll(marca, sub);

        // Menú de ADMINISTRADOR
        if (Sesion.esAdmin()) {
            long pendientes = new SolicitudDao().pendientes();
            grupo(menu, "OPERACIONES");
            item(menu, "Dashboard mundial", DashboardView::new);
            item(menu, "Incidentes", IncidentesView::new);
            item(menu, "Avisos ciudadanos" + (pendientes > 0 ? "  (" + pendientes + ")" : ""), SolicitudesView::new);
            item(menu, "Contratos", ContratosView::new);
            grupo(menu, "RED DE POTENCIALES");
            item(menu, "Potenciales", PotencialesView::new);
            item(menu, "Grupos tácticos", GruposView::new);
            item(menu, "Monederos", MonederosView::new);
            grupo(menu, "CLASIFICADO");
            item(menu, "Archivo restringido", ArchivoRestringidoView::new);
            grupo(menu, "WEB PÚBLICA");
            item(menu, "Noticias", NoticiasView::new);
            item(menu, "Zonas seguras", ZonasView::new);
            grupo(menu, "SISTEMA");
            item(menu, "Usuarios y actividad", UsuariosView::new);
        } else {
            // Menú de POTENCIAL
            grupo(menu, "MI TRABAJO");
            item(menu, "Mis misiones", MisMisionesView::new);
            item(menu, "Mi monedero", MiMonederoView::new);
            item(menu, "Mis vínculos", MisVinculosView::new);
            item(menu, "Reportar incidente", ReportarView::new);
        }
        grupo(menu, "CUENTA");
        item(menu, "Mi perfil", PerfilView::new);

        Region hueco = new Region();
        VBox.setVgrow(hueco, Priority.ALWAYS);
        Label nombre = new Label(Sesion.usuario().nombreCompleto());
        nombre.getStyleClass().add("usuario-nombre");
        Label rol = new Label(Sesion.esAdmin() ? "ADMINISTRADOR" : "POTENCIAL · @" + Sesion.usuario().username());
        rol.getStyleClass().add("usuario-rol");
        Button salir = Ui.boton("Cerrar sesión", ArchiveApp::mostrarLogin);
        salir.setMaxWidth(Double.MAX_VALUE);
        VBox usuario = new VBox(4, nombre, rol, salir);
        usuario.getStyleClass().add("usuario-box");
        menu.getChildren().addAll(hueco, usuario);

        ScrollPane scrollMenu = new ScrollPane(menu);
        scrollMenu.setFitToWidth(true);
        scrollMenu.setFitToHeight(true);
        scrollMenu.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        raiz.setLeft(scrollMenu);
        // Simulamos un clic en el primer botón para abrir la primera pantalla
        botones.get(0).fire();
        return raiz;
    }

    /** Añade al menú un título de sección (OPERACIONES, RED DE POTENCIALES...). */
    private void grupo(VBox menu, String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("menu-grupo");
        menu.getChildren().add(l);
    }

    /**
     * Añade al menú un botón que abre una pantalla.
     * @param fabrica cómo crear la pantalla (p. ej. {@code ContratosView::new}); solo se usa la primera vez
     */
    private void item(VBox menu, String texto, Supplier<Vista> fabrica) {
        Button b = new Button(texto);
        b.getStyleClass().add("menu-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> Ui.ejecutar(() -> {
            botones.forEach(x -> x.getStyleClass().remove("activo"));
            b.getStyleClass().add("activo");
            String clave = texto.replaceAll("\\s+\\(\\d+\\)$", "");
            // Si la pantalla no existe en la caché la crea con la fábrica; si existe, la reutiliza
            Vista v = cache.computeIfAbsent(clave, k -> fabrica.get());
            Node contenido = v.vista();
            v.refrescar();
            raiz.setCenter(contenido);
        }));
        botones.add(b);
        menu.getChildren().add(b);
    }
}
