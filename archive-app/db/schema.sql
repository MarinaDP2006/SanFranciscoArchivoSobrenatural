CREATE DATABASE IF NOT EXISTS sanfrancisco_archive CHARACTER SET utf8mb4;
USE sanfrancisco_archive;

CREATE TABLE ADMIN (
  id_admin INT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(50) NOT NULL,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  edad_real INT,
  edad_sobrenatural INT,
  ciudad_residencia VARCHAR(80) DEFAULT 'San Francisco'
);

CREATE TABLE USUARIO_REPORTER (
  id_usuario INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(120)
);

CREATE TABLE GRUPO_TACTICO (
  id_grupo INT PRIMARY KEY AUTO_INCREMENT,
  pais VARCHAR(80) NOT NULL,
  id_admin_creador INT,
  FOREIGN KEY (id_admin_creador) REFERENCES ADMIN(id_admin)
);

CREATE TABLE POTENCIAL (
  id_potencial INT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(80),
  edad INT,
  ciudad_origen VARCHAR(80),
  pais_actual VARCHAR(80),
  fuerza INT,
  velocidad INT,
  estado ENUM('EN_BASE','DISPONIBLE','EN_MISION','HERIDA','FALLECIDA') DEFAULT 'EN_BASE',
  id_admin_asignado INT,
  id_grupo INT,
  saldo_calculado DOUBLE DEFAULT 0,
  FOREIGN KEY (id_admin_asignado) REFERENCES ADMIN(id_admin),
  FOREIGN KEY (id_grupo) REFERENCES GRUPO_TACTICO(id_grupo)
);

CREATE TABLE INCIDENTE_PUBLICO (
  id_incidente INT PRIMARY KEY AUTO_INCREMENT,
  tipo ENUM('SECUESTRO','ASESINATO','DESAPARICION') NOT NULL,
  descripcion_corta VARCHAR(255),
  ciudad VARCHAR(80),
  pais VARCHAR(80),
  lat DOUBLE,
  lon DOUBLE,
  fecha_reporte DATETIME DEFAULT CURRENT_TIMESTAMP,
  estado_verificacion ENUM('NO_VERIFICADO','VERIFICADO') DEFAULT 'NO_VERIFICADO',
  id_usuario_reporter INT NULL,
  es_visible_sin_login BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (id_usuario_reporter) REFERENCES USUARIO_REPORTER(id_usuario)
);

CREATE TABLE CONTRATO_ANOMALIA (
  id_contrato INT PRIMARY KEY AUTO_INCREMENT,
  id_incidente INT NOT NULL,
  descripcion_detallada TEXT,
  zona_id INT,
  ciudad VARCHAR(80),
  pais VARCHAR(80),
  nivel_peligro INT,
  nivel_dificultad INT,
  recompensa_dinero DOUBLE,
  estado ENUM('SOLICITADO','ASIGNADO','COMPLETADO') DEFAULT 'SOLICITADO',
  id_usuario_solicitante INT,
  id_potencial_asignada INT NULL,
  FOREIGN KEY (id_incidente) REFERENCES INCIDENTE_PUBLICO(id_incidente),
  FOREIGN KEY (id_usuario_solicitante) REFERENCES USUARIO_REPORTER(id_usuario),
  FOREIGN KEY (id_potencial_asignada) REFERENCES POTENCIAL(id_potencial)
);

CREATE TABLE TRANSACCION_MONEDERO (
  id_transaccion INT PRIMARY KEY AUTO_INCREMENT,
  id_potencial INT NOT NULL,
  id_contrato INT NULL,
  tipo ENUM('INGRESO_CONTRATO','GASTO_TRANSPORTE','GASTO_MEDICO') NOT NULL,
  cantidad DOUBLE NOT NULL,
  fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
  descripcion VARCHAR(255),
  FOREIGN KEY (id_potencial) REFERENCES POTENCIAL(id_potencial),
  FOREIGN KEY (id_contrato) REFERENCES CONTRATO_ANOMALIA(id_contrato)
);

CREATE TABLE VINCULO (
  id_vinculo INT PRIMARY KEY AUTO_INCREMENT,
  id_potencial INT NOT NULL,
  tipo_vinculo ENUM('HIJO','MARIDO','VIUDA','HUERFANA','OTRO') NOT NULL,
  nombre_persona VARCHAR(120),
  ciudad_proteccion VARCHAR(80),
  FOREIGN KEY (id_potencial) REFERENCES POTENCIAL(id_potencial)
);

CREATE TABLE INFORME_CLASIFICADO (
  id_informe INT PRIMARY KEY AUTO_INCREMENT,
  id_contrato INT NOT NULL,
  id_admin_autor INT NOT NULL,
  titulo VARCHAR(200),
  contenido_sin_censura TEXT,
  nivel_acceso VARCHAR(30) DEFAULT 'ADMIN_ONLY',
  fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_contrato) REFERENCES CONTRATO_ANOMALIA(id_contrato),
  FOREIGN KEY (id_admin_autor) REFERENCES ADMIN(id_admin)
);
