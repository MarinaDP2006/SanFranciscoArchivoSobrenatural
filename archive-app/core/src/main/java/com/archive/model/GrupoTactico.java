package com.archive.model;

public class GrupoTactico {
  public static final int MAX_INTEGRANTES = 6;

  private int idGrupo;
  private String pais;
  private int idAdminCreador;
  private int integrantesActuales;

  public GrupoTactico() {}

  public GrupoTactico(int idGrupo, String pais, int idAdminCreador, int integrantesActuales) {
    this.idGrupo = idGrupo;
    this.pais = pais;
    this.idAdminCreador = idAdminCreador;
    this.integrantesActuales = integrantesActuales;
  }

  public boolean hayHueco() {
    return integrantesActuales < MAX_INTEGRANTES;
  }

  public int getIdGrupo() { return idGrupo; }
  public void setIdGrupo(int idGrupo) { this.idGrupo = idGrupo; }

  public String getPais() { return pais; }
  public void setPais(String pais) { this.pais = pais; }

  public int getIdAdminCreador() { return idAdminCreador; }
  public void setIdAdminCreador(int idAdminCreador) { this.idAdminCreador = idAdminCreador; }

  public int getIntegrantesActuales() { return integrantesActuales; }
  public void setIntegrantesActuales(int integrantesActuales) { this.integrantesActuales = integrantesActuales; }
}
