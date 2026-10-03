package com.sfarchive.desktop;

/**
 * Punto de entrada del .jar ejecutable (y del .exe que genera jpackage).
 * <p>
 * ¿Por qué existe? Si el main está en una clase que extiende Application (ArchiveApp) y la app
 * se lanza desde un "fat jar", Java da el error "JavaFX runtime components are missing".
 * Con una clase intermedia que NO extiende Application el problema desaparece.
 */
public final class Launcher {
    /** Simplemente llama al main de ArchiveApp. */
    public static void main(String[] args) {
        ArchiveApp.main(args);
    }
}
