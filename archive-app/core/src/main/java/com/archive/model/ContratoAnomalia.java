package com.archive.model;

public class ContratoAnomalia {
  private int idContrato;
  private int idIncidente;
  private String descripcionDetallada;
  private int zonaId;
  private String ciudad;
  private String pais;
  private int nivelPeligro;      // 1 - 5
  private int nivelDificultad;   // 1 - 5
  private double recompensaDinero;
  private String estado;         // SOLICITADO, ASIGNADO, COMPLETADO
  private int idUsuarioSolicitante;
  private Integer idPotencialAsignada; // null mientras no se asigne

  public ContratoAnomalia() {}

  public ContratoAnomalia(int idContrato, int idIncidente, String descripcionDetallada,
                          int zonaId, String ciudad, String pais, int nivelPeligro,
                          int nivelDificultad, double recompensaDinero, String estado,
                          int idUsuarioSolicitante, Integer idPotencialAsignada) {
    this.idContrato = idContrato;
    this.idIncidente = idIncidente;
    this.descripcionDetallada = descripcionDetallada;
    this.zonaId = zonaId;
    this.ciudad = ciudad;
    this.pais = pais;
    this.nivelPeligro = nivelPeligro;
    this.nivelDificultad = nivelDificultad;
    this.recompensaDinero = recompensaDinero;
    this.estado = estado;
    this.idUsuarioSolicitante = idUsuarioSolicitante;
    this.idPotencialAsignada = idPotencialAsignada;
  }

  /**
   * Calcula la recompensa segun peligro y dificultad.
   * Formula simple: base 500 + (peligro * 300) + (dificultad * 200)
   */
  public void calcularRecompensa() {
    this.recompensaDinero = 500 + (this.nivelPeligro * 300) + (this.nivelDificultad * 200);
  }

  public int getIdContrato() { return idContrato; }
  public void setIdContrato(int idContrato) { this.idContrato = idContrato; }

  public int getIdIncidente() { return idIncidente; }
  public void setIdIncidente(int idIncidente) { this.idIncidente = idIncidente; }

  public String getDescripcionDetallada() { return descripcionDetallada; }
  public void setDescripcionDetallada(String d) { this.descripcionDetallada = d; }

  public int getZonaId() { return zonaId; }
  public void setZonaId(int zonaId) { this.zonaId = zonaId; }

  public String getCiudad() { return ciudad; }
  public void setCiudad(String ciudad) { this.ciudad = ciudad; }

  public String getPais() { return pais; }
  public void setPais(String pais) { this.pais = pais; }

  public int getNivelPeligro() { return nivelPeligro; }
  public void setNivelPeligro(int nivelPeligro) { this.nivelPeligro = nivelPeligro; }

  public int getNivelDificultad() { return nivelDificultad; }
  public void setNivelDificultad(int nivelDificultad) { this.nivelDificultad = nivelDificultad; }

  public double getRecompensaDinero() { return recompensaDinero; }
  public void setRecompensaDinero(double recompensaDinero) { this.recompensaDinero = recompensaDinero; }

  public String getEstado() { return estado; }
  public void setEstado(String estado) { this.estado = estado; }

  public int getIdUsuarioSolicitante() { return idUsuarioSolicitante; }
  public void setIdUsuarioSolicitante(int id) { this.idUsuarioSolicitante = id; }

  public Integer getIdPotencialAsignada() { return idPotencialAsignada; }
  public void setIdPotencialAsignada(Integer id) { this.idPotencialAsignada = id; }
}
