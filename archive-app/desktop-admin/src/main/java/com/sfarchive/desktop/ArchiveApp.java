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
 * Aplicación de gestión del San Francisco Archive (JavaFX).
 * Solo pueden entrar los 3 administradores (James, Sarah y Nina) y los potenciales.
 * Todo lo que se publica desde aquí aparece en la web pública.
 */
public class ArchiveApp extends Application {

    private static Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("San Francisco Archive · Gestión");
        var icono = getClass().getResourceAsStream("icono.png");
        if (icono != null) stage.getIcons().add(new Image(icono));
        stage.setScene(new Scene(new LoginView().vista(), 1280, 800));
        aplicarTema(stage.getScene());
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void mostrarPrincipal() {
        cambiar(new MainView().vista());
    }

    public static void mostrarLogin() {
        Sesion.cerrar();
        cambiar(new LoginView().vista());
    }

    private static void cambiar(Parent raiz) {
        stage.getScene().setRoot(raiz);
    }

    public static Stage stage() {
        return stage;
    }

    public static void aplicarTema(Scene scene) {
        scene.getStylesheets().add(ArchiveApp.class.getResource("tema.css").toExternalForm());
    }

    @Override
    public void stop() {
        Database.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
