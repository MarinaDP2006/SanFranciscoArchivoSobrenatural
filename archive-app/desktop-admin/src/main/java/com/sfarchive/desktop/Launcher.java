package com.sfarchive.desktop;

/**
 * Punto de entrada del jar ejecutable. Se necesita una clase que NO extienda Application
 * para que JavaFX arranque correctamente desde un "fat jar" (y desde jpackage → .exe).
 */
public final class Launcher {
    public static void main(String[] args) {
        ArchiveApp.main(args);
    }
}
