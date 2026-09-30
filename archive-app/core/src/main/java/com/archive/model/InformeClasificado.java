package com.archive.model;

import java.time.LocalDateTime;

public class InformeClasificado {
  private int idInforme;
  private int idContrato;
  private int idAdminAutor; // James o Sarah
  private String titulo;
  private String contenidoSinCensura;
  private String nivelAcceso = "ADMIN_ONLY";
  private LocalDateTime fecha;

  public InformeClasificado() {}

  public InformeClasificado(int idInforme, int idContrato, int idAdminAutor,
                            String titulo, String contenidoSinCensura,
                            String nivelAcceso, LocalDateTime fecha) {
    this.idInforme = idInforme;
    this.idContrato = idContrato;
    this.idAdminAutor = idAdminAutor;
    this.titulo = titulo;
    this.contenidoSinCensura = contenidoSinCensura;
    this.nivelAcceso = nivelAcceso;
    this.fecha = fecha;
  }

  public int getIdInforme() { return idInforme; }
  public void setIdInforme(int idInforme) { this.idInforme = idInforme; }

  public int getIdContrato() { return idContrato; }
  public void setIdContrato(int idContrato) { this.idContrato = idContrato; }

  public int getIdAdminAutor() { return idAdminAutor; }
  public void setIdAdminAutor(int idAdminAutor) { this.idAdminAutor = idAdminAutor; }

  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }

  public String getContenidoSinCensura() { return contenidoSinCensura; }
  public void setContenidoSinCensura(String c) { this.contenidoSinCensura = c; }

  public String getNivelAcceso() { return nivelAcceso; }
  public void setNivelAcceso(String nivelAcceso) { this.nivelAcceso = nivelAcceso; }

  public LocalDateTime getFecha() { return fecha; }
  public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
