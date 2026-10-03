package com.sfarchive.desktop.views;

import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.Vinculo;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Optional;

/** Diálogo para crear/editar un vínculo familiar (lo usan administradores y potenciales). */
final class VinculoDialog {

    private VinculoDialog() { }

    static Optional<Vinculo> mostrar(int potencialId, Vinculo v) {
        TextField nombre = new TextField(v == null ? "" : v.nombre());
        TextField parentesco = new TextField(v == null ? "" : v.parentesco());
        TextField edad = new TextField(v == null ? "" : Formulario.str(v.edad()));
        TextField ciudad = new TextField(v == null ? "San Francisco" : Formulario.str(v.ciudad()));
        CheckBox secreto = new CheckBox("Conoce el secreto");
        secreto.setSelected(v != null && v.conoceSecreto());
        CheckBox riesgo = new CheckBox("En riesgo (requiere protección)");
        riesgo.setSelected(v != null && v.enRiesgo());
        TextArea notas = Formulario.area(3);
        notas.setText(v == null ? "" : Formulario.str(v.notas()));

        Dialog<Vinculo> d = new Dialog<>();
        d.setTitle(v == null ? "Nuevo vínculo" : "Editar vínculo");
        d.getDialogPane().setContent(new Formulario().campo("Nombre *", nombre).campo("Parentesco *", parentesco)
                .campo("Edad", edad).campo("Ciudad", ciudad).campo("", secreto).campo("", riesgo).campo("Notas", notas));
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().setPrefWidth(480);
        Ui.prepararDialogo(d.getDialogPane());
        d.setResultConverter(b -> {
            if (b != ButtonType.OK) return null;
            if (Formulario.texto(nombre) == null || Formulario.texto(parentesco) == null) {
                Ui.error("Nombre y parentesco son obligatorios.");
                return null;
            }
            try {
                return new Vinculo(v == null ? 0 : v.id(), potencialId, Formulario.texto(nombre), Formulario.texto(parentesco),
                        Formulario.entero(edad), Formulario.texto(ciudad), secreto.isSelected(), riesgo.isSelected(),
                        Formulario.texto(notas));
            } catch (DataException e) {
                Ui.error(e.getMessage());
                return null;
            }
        });
        return d.showAndWait();
    }
}
