package com.archive.model;

import java.time.LocalDateTime;

public class IncidentePublico {
  private int idIncidente;
  private String tipo; // SECUESTRO, ASESINATO, DESAPARICION
  private String descripcionCorta;
  private String ciudad;
  private String pais;
  private double lat;
  private double lon;
  private LocalDateTime fechaReporte;
  private String estadoVerificacion; // NO_VERIFICADO, VERIFICADO
  private Integer idUsuarioReporter; // puede ser null
  private boolean esVisibleSinLogin = true;

  public IncidentePublico() {}

  public IncidentePublico(int idIncidente, String tipo, String descripcionCorta,
                          String ciudad, String pais, double lat, double lon,
                          LocalDateTime fechaReporte, String estadoVerificacion,
                          Integer idUsuarioReporter, boolean esVisibleSinLogin) {
    this.idIncidente = idIncidente;
    this.tipo = tipo;
    this.descripcionCorta = descripcionCorta;
    this.ciudad = ciudad;
    this.pais = pais;
    this.lat = lat;
    this.lon = lon;
    this.fechaReporte = fechaReporte;
    this.estadoVerificacion = estadoVerificacion;
    this.idUsuarioReporter = idUsuarioReporter;
    this.esVisibleSinLogin = esVisibleSinLogin;
  }

  public int getIdIncidente() { return idIncidente; }
  public void setIdIncidente(int idIncidente) { this.idIncidente = idIncidente; }

  public String getTipo() { return tipo; }
  public void setTipo(String tipo) { this.tipo = tipo; }

  public String getDescripcionCorta() { return descripcionCorta; }
  public void setDescripcionCorta(String descripcionCorta) { this.descripcionCorta = descripcionCorta; }

  public String getCiudad() { return ciudad; }
  public void setCiudad(String ciudad) { this.ciudad = ciudad; }

  public String getPais() { return pais; }
  public void setPais(String pais) { this.pais = pais; }

  public double getLat() { return lat; }
  public void setLat(double lat) { this.lat = lat; }

  public double getLon() { return lon; }
  public void setLon(double lon) { this.lon = lon; }

  public LocalDateTime getFechaReporte() { return fechaReporte; }
  public void setFechaReporte(LocalDateTime fechaReporte) { this.fechaReporte = fechaReporte; }

  public String getEstadoVerificacion() { return estadoVerificacion; }
  public void setEstadoVerificacion(String estadoVerificacion) { this.estadoVerificacion = estadoVerificacion; }

  public Integer getIdUsuarioReporter() { return idUsuarioReporter; }
  public void setIdUsuarioReporter(Integer idUsuarioReporter) { this.idUsuarioReporter = idUsuarioReporter; }

  public boolean isEsVisibleSinLogin() { return esVisibleSinLogin; }
  public void setEsVisibleSinLogin(boolean esVisibleSinLogin) { this.esVisibleSinLogin = esVisibleSinLogin; }
}
