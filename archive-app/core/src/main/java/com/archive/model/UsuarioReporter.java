package com.archive.model;

public class UsuarioReporter {
  private int idUsuario;
  private String username;
  private String passwordHash;
  private String email;

  public UsuarioReporter() {}

  public UsuarioReporter(int idUsuario, String username, String passwordHash, String email) {
    this.idUsuario = idUsuario;
    this.username = username;
    this.passwordHash = passwordHash;
    this.email = email;
  }

  public int getIdUsuario() { return idUsuario; }
  public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
}
