package com.sfarchive.desktop.views;

import javafx.scene.Node;

/** Una pantalla de la aplicación. */
public interface Vista {
    /** Construye (una vez) y devuelve el contenido. */
    Node vista();

    /** Vuelve a leer los datos de la base de datos. */
    default void refrescar() { }
}
