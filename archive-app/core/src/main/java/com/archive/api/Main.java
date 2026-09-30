package com.archive.api;

/**
 * Punto de entrada de la API REST de San Francisco Archive.
 */
public final class Main {

  /**
   * Impide crear instancias de esta clase de entrada.
   */
  private Main() {
  }

  /**
   * Inicia el servidor Javalin en el puerto 8080.
   *
   * @param argumentos argumentos recibidos al iniciar la aplicación
   */
  public static void main(String[] argumentos) {
    Router.crearAplicacion().start(8080);
  }
}