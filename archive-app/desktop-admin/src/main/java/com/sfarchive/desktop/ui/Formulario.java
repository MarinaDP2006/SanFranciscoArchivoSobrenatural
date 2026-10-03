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

/**
 * Formulario de dos columnas (etiqueta | campo) basado en un GridPane.
 * <p>
 * Uso: {@code new Formulario().campo("Nombre", nombre).campo("País", pais)}
 * (cada {@code campo} añade una fila y devuelve el propio formulario para encadenar llamadas).
 * <p>
 * También tiene métodos static para leer los campos de forma segura (texto vacío → null,
 * números mal escritos → DataException con un mensaje claro).
 */
public class Formulario extends GridPane {

    /** Siguiente fila libre de la rejilla. */
    private int fila = 0;

    /** Configura las dos columnas: etiquetas alineadas a la derecha y campos que ocupan el resto. */
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

    /** Añade una fila: etiqueta a la izquierda y control a la derecha. */
    public Formulario campo(String etiqueta, Node control) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("etiqueta");
        add(l, 0, fila);
        add(control, 1, fila);
        if (control instanceof javafx.scene.layout.Region r) r.setMaxWidth(Double.MAX_VALUE);
        fila++;
        return this;
    }

    /** Añade un control que ocupa las dos columnas. */
    public Formulario ancho(Node control) {
        add(control, 0, fila, 2, 1);
        fila++;
        return this;
    }

    // ---- Lectura/validación de campos ----
    /** TextArea (texto de varias líneas) con ajuste de línea. */
    public static TextArea area(int filas) {
        TextArea t = new TextArea();
        t.setWrapText(true);
        t.setPrefRowCount(filas);
        return t;
    }

    /** Texto del campo sin espacios; null si está vacío. */
    public static String texto(TextField t) {
        String s = t.getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Texto del área sin espacios; null si está vacía. */
    public static String texto(TextArea t) {
        String s = t.getText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Lee un número entero; null si está vacío; error claro si no es un número. */
    public static Integer entero(TextField t) {
        String s = texto(t);
        if (s == null) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new com.sfarchive.core.db.DataException("\"" + s + "\" no es un número entero válido.");
        }
    }

    /** Lee un decimal (acepta coma o punto); null si está vacío. */
    public static Double decimal(TextField t) {
        String s = texto(t);
        if (s == null) return null;
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new com.sfarchive.core.db.DataException("\"" + s + "\" no es un número válido.");
        }
    }

    /** Lee un importe de dinero (acepta "$", coma o punto); 0 si está vacío. */
    public static BigDecimal dinero(TextField t) {
        String s = texto(t);
        if (s == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.replace(",", ".").replace("$", "").trim());
        } catch (NumberFormatException e) {
            throw new com.sfarchive.core.db.DataException("\"" + s + "\" no es un importe válido.");
        }
    }

    /** Para rellenar campos: null → "". */
    public static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
