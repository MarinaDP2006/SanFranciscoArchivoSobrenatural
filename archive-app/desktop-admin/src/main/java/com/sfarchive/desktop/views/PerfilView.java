package com.sfarchive.desktop.views;

import com.sfarchive.core.db.DataException;
import com.sfarchive.core.service.AuthService;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/** Datos de la cuenta y cambio de contraseña. */
public class PerfilView implements Vista {

    private final AuthService auth = new AuthService();
    private VBox raiz;
    private final PasswordField actual = new PasswordField();
    private final PasswordField nueva = new PasswordField();
    private final PasswordField repetir = new PasswordField();

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        var u = Sesion.usuario();
        VBox datos = new VBox(6, Ui.seccion("Cuenta"),
                new Label("Usuario: " + u.username()), new Label("Nombre: " + u.nombreCompleto()),
                new Label("Email: " + u.email()), new Label("Rol: " + u.rol().etiqueta()),
                new Label("Último acceso: " + Ui.fecha(u.ultimoAcceso())));
        datos.getStyleClass().add("panel");
        datos.setMaxWidth(560);
        VBox pass = new VBox(10, Ui.seccion("Cambiar contraseña"),
                new Formulario().campo("Actual", actual).campo("Nueva", nueva).campo("Repetir", repetir),
                Ui.fila(Ui.primario("Cambiar contraseña", this::cambiar)));
        pass.getStyleClass().add("panel");
        pass.setMaxWidth(560);
        raiz = Ui.pantalla("Mi perfil", "Cambia la contraseña de demostración la primera vez que entres.", datos, pass);
        return raiz;
    }

    private void cambiar() {
        if (!nueva.getText().equals(repetir.getText())) throw new DataException("Las contraseñas nuevas no coinciden.");
        auth.cambiarPassword(Sesion.id(), actual.getText(), nueva.getText());
        actual.clear(); nueva.clear(); repetir.clear();
        Ui.info("Contraseña actualizada.");
    }
}
