package com.sfarchive.desktop.views;

import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Database;
import com.sfarchive.core.service.AuthService;
import com.sfarchive.desktop.ArchiveApp;
import com.sfarchive.desktop.Sesion;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Pantalla de acceso: solo administradores y potenciales tienen cuenta. */
public class LoginView {

    private final AuthService auth = new AuthService();

    public Parent vista() {
        Label sello = new Label("SFA");
        sello.getStyleClass().add("sello");
        Label titulo = new Label("SAN FRANCISCO ARCHIVE");
        titulo.getStyleClass().add("titulo");
        Label sub = new Label("Sistema de Gestión de Anomalías · acceso restringido");
        sub.getStyleClass().add("subtitulo");

        TextField usuario = new TextField();
        usuario.setPromptText("Usuario (james, sarah, nina, niebla…)");
        usuario.setId("usuario");
        PasswordField password = new PasswordField();
        password.setPromptText("Contraseña");
        password.setId("password");
        Label error = new Label();
        error.getStyleClass().add("error-login");
        error.setWrapText(true);

        Button entrar = new Button("ENTRAR");
        entrar.getStyleClass().add("primario");
        entrar.setDefaultButton(true);
        entrar.setMaxWidth(Double.MAX_VALUE);
        entrar.setOnAction(e -> {
            error.setText("");
            try {
                Sesion.iniciar(auth.login(usuario.getText(), password.getText()));
                ArchiveApp.mostrarPrincipal();
            } catch (DataException ex) {
                error.setText(ex.getMessage());
                password.clear();
            } catch (RuntimeException ex) {
                error.setText(Database.ping() ? "Error: " + ex.getMessage()
                        : "No se puede conectar con MySQL. Revisa archive.properties y que el servidor esté arrancado.");
            }
        });

        Label nota = new Label("Los ciudadanos no necesitan cuenta: consultan la web pública.");
        nota.getStyleClass().add("marca-sub");

        VBox caja = new VBox(sello, titulo, sub, new Label(" "), usuario, password, entrar, error, nota);
        caja.getStyleClass().add("login-caja");
        caja.setMaxWidth(420);
        caja.setMaxHeight(VBox.USE_PREF_SIZE);
        caja.setAlignment(Pos.CENTER_LEFT);

        StackPane raiz = new StackPane(caja);
        raiz.getStyleClass().add("login-fondo");
        usuario.requestFocus();
        return raiz;
    }
}
