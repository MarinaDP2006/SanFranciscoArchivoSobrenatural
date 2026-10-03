package com.sfarchive.core.db;

/** Excepción no comprobada para errores de acceso a datos o reglas de negocio. */
public class DataException extends RuntimeException {
    public DataException(String message) {
        super(message);
    }

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
