package com.archive.model;

public class Potencial {
  private int idPotencial;
  private String nombre;
  private int edad;
  private String ciudadOrigen;
  private String paisActual;
  private int fuerza;
  private int velocidad;
  private String estado; // EN_BASE, DISPONIBLE, EN_MISION, HERIDA, FALLECIDA
  private int idAdminAsignado;
  private int idGrupo;
  private double saldoCalculado;

  public Potencial() {}

  public Potencial(int idPotencial, String nombre, int edad, String ciudadOrigen,
                   String paisActual, int fuerza, int velocidad, String estado,
                   int idAdminAsignado, int idGrupo, double saldoCalculado) {
    this.idPotencial = idPotencial;
    this.nombre = nombre;
    this.edad = edad;
    this.ciudadOrigen = ciudadOrigen;
    this.paisActual = paisActual;
    this.fuerza = fuerza;
    this.velocidad = velocidad;
    this.estado = estado;
    this.idAdminAsignado = idAdminAsignado;
    this.idGrupo = idGrupo;
    this.saldoCalculado = saldoCalculado;
  }

  public int getIdPotencial() { return idPotencial; }
  public void setIdPotencial(int idPotencial) { this.idPotencial = idPotencial; }

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public int getEdad() { return edad; }
  public void setEdad(int edad) { this.edad = edad; }

  public String getCiudadOrigen() { return ciudadOrigen; }
  public void setCiudadOrigen(String ciudadOrigen) { this.ciudadOrigen = ciudadOrigen; }

  public String getPaisActual() { return paisActual; }
  public void setPaisActual(String paisActual) { this.paisActual = paisActual; }

  public int getFuerza() { return fuerza; }
  public void setFuerza(int fuerza) { this.fuerza = fuerza; }

  public int getVelocidad() { return velocidad; }
  public void setVelocidad(int velocidad) { this.velocidad = velocidad; }

  public String getEstado() { return estado; }
  public void setEstado(String estado) { this.estado = estado; }

  public int getIdAdminAsignado() { return idAdminAsignado; }
  public void setIdAdminAsignado(int idAdminAsignado) { this.idAdminAsignado = idAdminAsignado; }

  public int getIdGrupo() { return idGrupo; }
  public void setIdGrupo(int idGrupo) { this.idGrupo = idGrupo; }

  public double getSaldoCalculado() { return saldoCalculado; }
  public void setSaldoCalculado(double saldoCalculado) { this.saldoCalculado = saldoCalculado; }

  @Override
  public String toString() {
    return "Potencial{" + nombre + " [" + estado + "] grupo=" + idGrupo + "}";
  }
}
