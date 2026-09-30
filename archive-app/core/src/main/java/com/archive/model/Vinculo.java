package com.archive.model;

public class Vinculo {
  private int idVinculo;
  private int idPotencial;
  private String tipoVinculo; // HIJO, MARIDO, VIUDA, HUERFANA, OTRO
  private String nombrePersona;
  private String ciudadProteccion;

  public Vinculo() {}

  public Vinculo(int idVinculo, int idPotencial, String tipoVinculo,
                 String nombrePersona, String ciudadProteccion) {
    this.idVinculo = idVinculo;
    this.idPotencial = idPotencial;
    this.tipoVinculo = tipoVinculo;
    this.nombrePersona = nombrePersona;
    this.ciudadProteccion = ciudadProteccion;
  }

  public int getIdVinculo() { return idVinculo; }
  public void setIdVinculo(int idVinculo) { this.idVinculo = idVinculo; }

  public int getIdPotencial() { return idPotencial; }
  public void setIdPotencial(int idPotencial) { this.idPotencial = idPotencial; }

  public String getTipoVinculo() { return tipoVinculo; }
  public void setTipoVinculo(String tipoVinculo) { this.tipoVinculo = tipoVinculo; }

  public String getNombrePersona() { return nombrePersona; }
  public void setNombrePersona(String nombrePersona) { this.nombrePersona = nombrePersona; }

  public String getCiudadProteccion() { return ciudadProteccion; }
  public void setCiudadProteccion(String ciudadProteccion) { this.ciudadProteccion = ciudadProteccion; }
}
