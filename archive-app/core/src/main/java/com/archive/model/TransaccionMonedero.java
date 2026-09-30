package com.archive.model;

import java.time.LocalDateTime;

public class TransaccionMonedero {
  private int idTransaccion;
  private int idPotencial;
  private Integer idContrato; // null si es gasto medico
  private String tipo;        // INGRESO_CONTRATO, GASTO_TRANSPORTE, GASTO_MEDICO
  private double cantidad;
  private LocalDateTime fecha;
  private String descripcion;

  public TransaccionMonedero() {}

  public TransaccionMonedero(int idTransaccion, int idPotencial, Integer idContrato,
                             String tipo, double cantidad, LocalDateTime fecha,
                             String descripcion) {
    this.idTransaccion = idTransaccion;
    this.idPotencial = idPotencial;
    this.idContrato = idContrato;
    this.tipo = tipo;
    this.cantidad = cantidad;
    this.fecha = fecha;
    this.descripcion = descripcion;
  }

  public int getIdTransaccion() { return idTransaccion; }
  public void setIdTransaccion(int idTransaccion) { this.idTransaccion = idTransaccion; }

  public int getIdPotencial() { return idPotencial; }
  public void setIdPotencial(int idPotencial) { this.idPotencial = idPotencial; }

  public Integer getIdContrato() { return idContrato; }
  public void setIdContrato(Integer idContrato) { this.idContrato = idContrato; }

  public String getTipo() { return tipo; }
  public void setTipo(String tipo) { this.tipo = tipo; }

  public double getCantidad() { return cantidad; }
  public void setCantidad(double cantidad) { this.cantidad = cantidad; }

  public LocalDateTime getFecha() { return fecha; }
  public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
