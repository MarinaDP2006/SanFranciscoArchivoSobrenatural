package com.sfarchive.desktop.views;

import javafx.scene.Node;

/** Una pantalla de la aplicación. */
public interface Vista {
    /** Construye (una sola vez) y devuelve el contenido de la pantalla. */
    Node vista();

    /** Vuelve a leer los datos de MySQL. Por defecto no hace nada. */
    default void refrescar() { }
}
