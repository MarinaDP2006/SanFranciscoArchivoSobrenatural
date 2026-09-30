package com.archive.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
  private static final String URL  = "jdbc:mysql://localhost:3306/sanfrancisco_archive";
  private static final String USER = "root";
  private static final String PASS = "";

  public static Connection get() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASS);
  }
}
