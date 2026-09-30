package com.archive.model;

public class Admin {
  private int idAdmin;
  private String nombre;
  private String username;
  private String passwordHash;
  private int edadReal;
  private int edadSobrenatural;
  private String ciudadResidencia = "San Francisco";

  public Admin() {}
  public Admin(int idAdmin, String nombre, String username, String passwordHash,
               int edadReal, int edadSobrenatural, String ciudadResidencia) {
    this.idAdmin = idAdmin;
    this.nombre = nombre;
    this.username = username;
    this.passwordHash = passwordHash;
    this.edadReal = edadReal;
    this.edadSobrenatural = edadSobrenatural;
    this.ciudadResidencia = ciudadResidencia;
  }
  // getters y setters
  public int getIdAdmin() { return idAdmin; }
  public void setIdAdmin(int idAdmin) { this.idAdmin = idAdmin; }
  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public int getEdadReal() { return edadReal; }
  public void setEdadReal(int edadReal) { this.edadReal = edadReal; }
  public int getEdadSobrenatural() { return edadSobrenatural; }
  public void setEdadSobrenatural(int edadSobrenatural) { this.edadSobrenatural = edadSobrenatural; }
  public String getCiudadResidencia() { return ciudadResidencia; }
  public void setCiudadResidencia(String c) { this.ciudadResidencia = c; }
}
