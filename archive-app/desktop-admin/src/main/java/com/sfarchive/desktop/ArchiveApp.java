package com.sfarchive.desktop;

import com.sfarchive.core.db.Database;
import com.sfarchive.desktop.views.LoginView;
import com.sfarchive.desktop.views.MainView;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Aplicación de gestión del San Francisco Archive (JavaFX). Es el punto de arranque de la app.
 * <p>
 * En JavaFX la clase principal extiende {@link Application}: JavaFX llama a {@link #start(Stage)}
 * con la ventana (Stage) ya creada. Aquí ponemos la escena con la pantalla de login.
 * <p>
 * Para cambiar de pantalla no se abren ventanas nuevas: se cambia la raíz de la escena
 * ({@code scene.setRoot(...)}).
 * <p>
 * Solo pueden entrar los 3 administradores (James, Sarah y Nina) y los potenciales.
 * Todo lo que se publica desde aquí aparece en la web pública.
 */
public class ArchiveApp extends Application {

    /** La ventana principal (se guarda para poder cambiar su contenido desde cualquier sitio). */
    private static Stage stage;

    /** JavaFX lo llama al arrancar: configura la ventana y enseña el login. */
    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("San Francisco Archive · Gestión");
        var icono = getClass().getResourceAsStream("icono.png");
        if (icono != null) stage.getIcons().add(new Image(icono));
        // Escena inicial: la pantalla de login, con tamaño 1280 x 800
        stage.setScene(new Scene(new LoginView().vista(), 1280, 800));
        aplicarTema(stage.getScene());
        // Tamaño mínimo de la ventana para que las tablas quepan
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    /** Tras un login correcto: muestra la ventana principal con el menú. */
    public static void mostrarPrincipal() {
        cambiar(new MainView().vista());
    }

    /** Cerrar sesión: olvida el usuario y vuelve al login. */
    public static void mostrarLogin() {
        Sesion.cerrar();
        cambiar(new LoginView().vista());
    }

    /** Sustituye todo el contenido de la ventana. */
    private static void cambiar(Parent raiz) {
        stage.getScene().setRoot(raiz);
    }

    /** Devuelve la ventana principal (la usan los diálogos para colocarse encima). */
    public static Stage stage() {
        return stage;
    }

    /** Aplica el CSS del tema oscuro (tema.css) a una escena. */
    public static void aplicarTema(Scene scene) {
        scene.getStylesheets().add(ArchiveApp.class.getResource("tema.css").toExternalForm());
    }

    /** JavaFX lo llama al cerrar la ventana: cerramos las conexiones a MySQL. */
    @Override
    public void stop() {
        Database.close();
    }

    /** main "normal" de Java: arranca JavaFX (que acabará llamando a start). */
    public static void main(String[] args) {
        launch(args);
    }
}
