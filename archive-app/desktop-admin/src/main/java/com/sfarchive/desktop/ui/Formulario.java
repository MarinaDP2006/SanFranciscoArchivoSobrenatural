package com.sfarchive.desktop.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;

/** Rejilla etiqueta/campo para los formularios de edición. */
public class Formulario extends GridPane {

    private int fila = 0;

    public Formulario() {
        setHgap(10);
        setVgap(8);
        setPadding(new Insets(4, 0, 4, 0));
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(110);
        c1.setHalignment(HPos.RIGHT);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setFillWidth(true);
        getColumnConstraints().addAll(c1, c2);
    }

    public Formulario campo(String etiqueta, Node control) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("etiqueta");
        add(l, 0, fila);
        add(control, 1, fila);
        if (control instanceof javafx.scene.layout.Region r) r.setMaxWidth(Double.MAX_VALUE);
        fila++;
        return this;
    }

    public Formulario ancho(Node control) {
        add(control, 0, fila, 2, 1);
        fila++;
        return this;
    }

    // ---- Lectura/validación de campos ----
    public static TextArea area(int filas) {
        TextArea t = new TextArea();
        t.setWrapText(true);
        t.setPrefRowCount(filas);
        return t;
    }

    public static String texto(TextField t) {
        String s = t.getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static String texto(TextArea t) {
        String s = t.getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static Integer entero(TextField t) {
        String s = texto(t);
        if (s == null) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new com.sfarchive.core.db.DataException("\"" + s + "\" no es un número entero válido.");
        }
    }

    public static Double decimal(TextField t) {
        String s = texto(t);
        if (s == null) return null;
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new com.sfarchive.core.db.DataException("\"" + s + "\" no es un número válido.");
        }
    }

    public static BigDecimal dinero(TextField t) {
        String s = texto(t);
        if (s == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.replace(",", ".").replace("$", "").trim());
        } catch (NumberFormatException e) {
            throw new com.sfarchive.core.db.DataException("\"" + s + "\" no es un importe válido.");
        }
    }

    public static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
