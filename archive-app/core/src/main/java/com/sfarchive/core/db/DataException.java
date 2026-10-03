package com.sfarchive.core.db;

/**
 * Excepción para errores que el usuario debe leer: reglas de negocio
 * ("El potencial no está disponible") o problemas de base de datos traducidos
 * ("No se pudo conectar con MySQL").
 * <p>
 * Es "no comprobada" (extiende RuntimeException): no obliga a poner throws en cada método.
 * Las pantallas la capturan en {@code Ui.ejecutar(...)} y muestran el mensaje en un diálogo.
 */
public class DataException extends RuntimeException {

    /** @param message texto que verá el usuario */
    public DataException(String message) {
        super(message);
    }

    /**
     * @param message texto que verá el usuario
     * @param cause   error original (por ejemplo, la SQLException) para depurar
     */
    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
