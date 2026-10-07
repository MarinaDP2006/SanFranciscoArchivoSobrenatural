-- =====================================================================
--  SAN FRANCISCO ARCHIVE · Sistema de Gestión de Anomalías
--  Capa pública  → la web solo LEE (vistas v_*) y crea solicitudes de ayuda.
--  Capa privada  → la aplicación de escritorio (JavaFX) gestiona todo.
--  Ejecutar:  mysql -u root -p < database/01_schema.sql
--             mysql -u root -p < database/02_datos.sql
-- =====================================================================

DROP DATABASE IF EXISTS sf_archive;
CREATE DATABASE sf_archive;
USE sf_archive;

-- ---------------------------------------------------------------------
-- 1. USUARIOS: solo pueden iniciar sesión los 3 administradores y los potenciales. Los ciudadanos NUNCA se registran.
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(40)  NOT NULL UNIQUE,
    email           VARCHAR(120) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    rol             ENUM('ADMIN','POTENCIAL') NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    activo          TINYINT(1)   NOT NULL DEFAULT 1,
    ultimo_acceso   DATETIME     NULL,
    creado_en       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. ADMINISTRADORES: ficha ampliada (James, Sarah y Nina)
-- ---------------------------------------------------------------------
CREATE TABLE administradores (
    usuario_id        INT PRIMARY KEY,
    cargo             VARCHAR(80)  NOT NULL,
    edad              INT          NULL,
    anios_experiencia INT          NULL,
    biografia         TEXT         NULL,
    CONSTRAINT fk_admin_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. GRUPOS TÁCTICOS: por país/zona, máximo 6 potenciales (trigger)
-- ---------------------------------------------------------------------
CREATE TABLE grupos_tacticos (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL UNIQUE,
    pais        VARCHAR(60)  NOT NULL DEFAULT 'Estados Unidos',
    ciudad      VARCHAR(80)  NOT NULL DEFAULT 'San Francisco',
    zona        VARCHAR(120) NULL,
    lat         DECIMAL(9,6) NULL,
    lng         DECIMAL(9,6) NULL,
    descripcion TEXT         NULL,
    activo      TINYINT(1)   NOT NULL DEFAULT 1,
    creado_en   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. POTENCIALES: agentes con habilidades. Su saldo es el monedero.
-- ---------------------------------------------------------------------
CREATE TABLE potenciales (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id          INT          NULL UNIQUE,
    alias               VARCHAR(40)  NOT NULL UNIQUE,
    nombre_real         VARCHAR(120) NOT NULL,
    edad                INT          NULL,
    habilidad           VARCHAR(120) NOT NULL,
    descripcion         TEXT         NULL,
    nivel               TINYINT      NOT NULL DEFAULT 1,
    estado              ENUM('DISPONIBLE','EN_MISION','HERIDO','INACTIVO') NOT NULL DEFAULT 'DISPONIBLE',
    barrio              VARCHAR(80)  NULL,
    ciudad              VARCHAR(80)  NOT NULL DEFAULT 'San Francisco',
    pais                VARCHAR(60)  NOT NULL DEFAULT 'Estados Unidos',
    lat                 DECIMAL(9,6) NULL,
    lng                 DECIMAL(9,6) NULL,
    grupo_id            INT          NULL,
    saldo               DECIMAL(12,2) NOT NULL DEFAULT 0,
    fecha_reclutamiento DATE         NULL,
    CONSTRAINT ck_pot_nivel CHECK (nivel BETWEEN 1 AND 5),
    CONSTRAINT fk_pot_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT fk_pot_grupo   FOREIGN KEY (grupo_id)   REFERENCES grupos_tacticos(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. VÍNCULOS FAMILIARES de cada potencial (protección de civiles)
-- ---------------------------------------------------------------------
CREATE TABLE vinculos_familiares (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    potencial_id   INT          NOT NULL,
    nombre         VARCHAR(120) NOT NULL,
    parentesco     VARCHAR(40)  NOT NULL,
    edad           INT          NULL,
    ciudad         VARCHAR(80)  NULL,
    conoce_secreto TINYINT(1)   NOT NULL DEFAULT 0,
    en_riesgo      TINYINT(1)   NOT NULL DEFAULT 0,
    notas          TEXT         NULL,
    CONSTRAINT fk_vinc_pot FOREIGN KEY (potencial_id) REFERENCES potenciales(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. INCIDENTES: lo que el público ve como una noticia normal
--    (SECUESTRO, ASESINATO, DESAPARICIÓN). La anomalía real es privada.
-- ---------------------------------------------------------------------
CREATE TABLE incidentes (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    codigo               VARCHAR(20)  NOT NULL UNIQUE,
    titulo               VARCHAR(160) NOT NULL,
    tipo                 ENUM('SECUESTRO','ASESINATO','DESAPARICION') NOT NULL,
    descripcion_publica  TEXT         NOT NULL,
    barrio               VARCHAR(80)  NULL,
    direccion            VARCHAR(160) NULL,
    ciudad               VARCHAR(80)  NOT NULL DEFAULT 'San Francisco',
    pais                 VARCHAR(60)  NOT NULL DEFAULT 'Estados Unidos',
    lat                  DECIMAL(9,6) NULL,
    lng                  DECIMAL(9,6) NULL,
    fecha_incidente      DATETIME     NOT NULL,
    estado               ENUM('NO_VERIFICADO','VERIFICADO','EN_INVESTIGACION','RESUELTO','ARCHIVADO') NOT NULL DEFAULT 'NO_VERIFICADO',
    publicado            TINYINT(1)   NOT NULL DEFAULT 0,
    anomalia_clasificada TEXT         NULL,
    nivel_amenaza        TINYINT      NOT NULL DEFAULT 1,
    origen               ENUM('ARCHIVO','CIUDADANO','POTENCIAL') NOT NULL DEFAULT 'ARCHIVO',
    creado_por           INT          NULL,
    creado_en            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_inc_amenaza CHECK (nivel_amenaza BETWEEN 1 AND 5),
    CONSTRAINT fk_inc_usuario FOREIGN KEY (creado_por) REFERENCES usuarios(id) ON DELETE SET NULL,
    INDEX idx_inc_publicado (publicado, fecha_incidente),
    INDEX idx_inc_tipo (tipo)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. SOLICITUDES DE AYUDA: formulario anónimo de la web ("pedir ayuda")
-- ---------------------------------------------------------------------
CREATE TABLE solicitudes_ayuda (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    codigo_seguimiento VARCHAR(12)  NOT NULL UNIQUE,
    tipo               ENUM('SECUESTRO','ASESINATO','DESAPARICION') NOT NULL,
    descripcion        TEXT         NOT NULL,
    barrio             VARCHAR(80)  NULL,
    ubicacion          VARCHAR(160) NULL,
    lat                DECIMAL(9,6) NULL,
    lng                DECIMAL(9,6) NULL,
    contacto           VARCHAR(120) NULL,
    estado             ENUM('PENDIENTE','EN_REVISION','ATENDIDA','DESCARTADA') NOT NULL DEFAULT 'PENDIENTE',
    incidente_id       INT          NULL,
    respuesta_publica  VARCHAR(255) NULL,
    recibida_en        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revisada_por       INT          NULL,
    revisada_en        DATETIME     NULL,
    CONSTRAINT fk_sol_incidente FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE SET NULL,
    CONSTRAINT fk_sol_usuario   FOREIGN KEY (revisada_por) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. CONTRATOS DE ANOMALÍA: la única forma de resolver un incidente
-- ---------------------------------------------------------------------
CREATE TABLE contratos (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    codigo           VARCHAR(20)   NOT NULL UNIQUE,
    incidente_id     INT           NOT NULL,
    solicitud_id     INT           NULL,
    estado           ENUM('SOLICITADO','ASIGNADO','EN_CURSO','PENDIENTE_REVISION','COMPLETADO','FALLIDO','CANCELADO') NOT NULL DEFAULT 'SOLICITADO',
    prioridad        ENUM('BAJA','MEDIA','ALTA','CRITICA') NOT NULL DEFAULT 'MEDIA',
    grupo_id         INT           NULL,
    potencial_id     INT           NULL,
    recompensa       DECIMAL(12,2) NOT NULL DEFAULT 0,
    coste_transporte DECIMAL(12,2) NULL,
    distancia_km     DECIMAL(8,2)  NULL,
    notas_campo      TEXT          NULL,
    fecha_solicitud  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_asignacion DATETIME      NULL,
    fecha_cierre     DATETIME      NULL,
    asignado_por     INT           NULL,
    CONSTRAINT fk_con_incidente FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
    CONSTRAINT fk_con_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes_ayuda(id) ON DELETE SET NULL,
    CONSTRAINT fk_con_grupo     FOREIGN KEY (grupo_id)     REFERENCES grupos_tacticos(id) ON DELETE SET NULL,
    CONSTRAINT fk_con_potencial FOREIGN KEY (potencial_id) REFERENCES potenciales(id) ON DELETE SET NULL,
    CONSTRAINT fk_con_admin     FOREIGN KEY (asignado_por) REFERENCES usuarios(id) ON DELETE SET NULL,
    INDEX idx_con_estado (estado)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 9. TRANSACCIONES DE MONEDERO (historial de movimientos, en USD)
-- ---------------------------------------------------------------------
CREATE TABLE transacciones_monedero (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    potencial_id     INT           NOT NULL,
    contrato_id      INT           NULL,
    tipo             ENUM('RECOMPENSA','TRANSPORTE','AJUSTE','BONUS','PENALIZACION') NOT NULL,
    importe          DECIMAL(12,2) NOT NULL,
    saldo_resultante DECIMAL(12,2) NOT NULL,
    concepto         VARCHAR(200)  NOT NULL,
    fecha            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    realizado_por    INT           NULL,
    CONSTRAINT fk_tx_pot      FOREIGN KEY (potencial_id)  REFERENCES potenciales(id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_contrato FOREIGN KEY (contrato_id)   REFERENCES contratos(id)   ON DELETE SET NULL,
    CONSTRAINT fk_tx_usuario  FOREIGN KEY (realizado_por) REFERENCES usuarios(id)    ON DELETE SET NULL,
    INDEX idx_tx_pot (potencial_id, fecha)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 10. INFORMES CLASIFICADOS: Archivo Restringido (solo administradores)
-- ---------------------------------------------------------------------
CREATE TABLE informes_clasificados (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    contrato_id        INT          NOT NULL UNIQUE,
    autor_id           INT          NULL,
    titulo             VARCHAR(160) NOT NULL,
    entidad_anomala    VARCHAR(120) NULL,
    clasificacion      ENUM('CONFIDENCIAL','SECRETO','ALTO_SECRETO','OMEGA') NOT NULL DEFAULT 'SECRETO',
    resumen            VARCHAR(400) NULL,
    contenido          MEDIUMTEXT   NOT NULL,
    bajas_civiles      INT          NOT NULL DEFAULT 0,
    fecha_creacion     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inf_contrato FOREIGN KEY (contrato_id) REFERENCES contratos(id) ON DELETE CASCADE,
    CONSTRAINT fk_inf_autor    FOREIGN KEY (autor_id)    REFERENCES usuarios(id)  ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 11. NOTICIAS: lo que publica el archivo en la web (portada de "tapadera")
-- ---------------------------------------------------------------------
CREATE TABLE noticias (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    slug              VARCHAR(160) NOT NULL UNIQUE,
    titulo            VARCHAR(200) NOT NULL,
    resumen           VARCHAR(400) NOT NULL,
    contenido         TEXT         NOT NULL,
    categoria         ENUM('SUCESOS','CIUDAD','AVISO','COMUNIDAD','HISTORIA') NOT NULL DEFAULT 'SUCESOS',
    imagen_url        VARCHAR(400) NULL,
    barrio            VARCHAR(80)  NULL,
    incidente_id      INT          NULL,
    publicada         TINYINT(1)   NOT NULL DEFAULT 0,
    destacada         TINYINT(1)   NOT NULL DEFAULT 0,
    fecha_publicacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    autor_id          INT          NULL,
    CONSTRAINT fk_not_incidente FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE SET NULL,
    CONSTRAINT fk_not_autor     FOREIGN KEY (autor_id)     REFERENCES usuarios(id)   ON DELETE SET NULL,
    INDEX idx_not_publicada (publicada, fecha_publicacion)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 12. ZONAS SEGURAS: pins verdes del mapa público (hospitales, comisarías…)
-- ---------------------------------------------------------------------
CREATE TABLE zonas_seguras (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(120) NOT NULL,
    tipo      ENUM('HOSPITAL','POLICIA','BOMBEROS','REFUGIO','TEMPLO') NOT NULL,
    direccion VARCHAR(160) NULL,
    barrio    VARCHAR(80)  NULL,
    lat       DECIMAL(9,6) NOT NULL,
    lng       DECIMAL(9,6) NOT NULL,
    telefono  VARCHAR(40)  NULL,
    horario   VARCHAR(80)  NULL,
    activa    TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 13. REGISTRO DE ACTIVIDAD (auditoría de la aplicación de gestión)
-- ---------------------------------------------------------------------
CREATE TABLE registro_actividad (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT          NULL,
    accion     VARCHAR(40)  NOT NULL,
    entidad    VARCHAR(40)  NOT NULL,
    entidad_id INT          NULL,
    detalle    VARCHAR(400) NULL,
    fecha      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    INDEX idx_log_fecha (fecha)
) ENGINE=InnoDB;

-- =====================================================================
-- TRIGGERS: un grupo táctico admite como máximo 6 potenciales
-- =====================================================================
DELIMITER $$

CREATE TRIGGER trg_pot_grupo_max_ins BEFORE INSERT ON potenciales
FOR EACH ROW
BEGIN
    IF NEW.grupo_id IS NOT NULL AND
       (SELECT COUNT(*) FROM potenciales WHERE grupo_id = NEW.grupo_id) >= 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un grupo tactico no puede tener mas de 6 potenciales';
    END IF;
END$$

CREATE TRIGGER trg_pot_grupo_max_upd BEFORE UPDATE ON potenciales
FOR EACH ROW
BEGIN
    IF NEW.grupo_id IS NOT NULL AND NOT (NEW.grupo_id <=> OLD.grupo_id) AND
       (SELECT COUNT(*) FROM potenciales WHERE grupo_id = NEW.grupo_id) >= 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un grupo tactico no puede tener mas de 6 potenciales';
    END IF;
END$$

DELIMITER ;

-- =====================================================================
-- VISTAS PÚBLICAS: lo único que la web puede ver (sin anomalías)
-- =====================================================================
CREATE VIEW v_incidentes_publicos AS
SELECT i.id, i.codigo, i.titulo, i.tipo, i.descripcion_publica, i.barrio, i.direccion,
       i.ciudad, i.pais, i.lat, i.lng, i.fecha_incidente,
       CASE WHEN i.estado IN ('RESUELTO','ARCHIVADO') THEN 'CERRADO'
            WHEN i.estado = 'NO_VERIFICADO' THEN 'SIN CONFIRMAR'
            ELSE 'ABIERTO' END AS estado_publico,
       i.actualizado_en
FROM incidentes i
WHERE i.publicado = 1;

CREATE VIEW v_noticias_publicas AS
SELECT n.id, n.slug, n.titulo, n.resumen, n.contenido, n.categoria, n.imagen_url,
       n.barrio, n.incidente_id, n.destacada, n.fecha_publicacion,
       u.nombre_completo AS autor
FROM noticias n
LEFT JOIN usuarios u ON u.id = n.autor_id
WHERE n.publicada = 1 AND n.fecha_publicacion <= NOW();

-- =====================================================================
-- USUARIO DE BASE DE DATOS para la API pública (solo lectura + ayuda) y para la aplicación de gestión. Cambia las contraseñas en producción.
-- =====================================================================
CREATE USER IF NOT EXISTS 'sfa_web'@'%'   IDENTIFIED BY 'sfa_web_2026';
CREATE USER IF NOT EXISTS 'sfa_admin'@'%' IDENTIFIED BY 'sfa_admin_2026';

GRANT SELECT ON sf_archive.v_incidentes_publicos TO 'sfa_web'@'%';
GRANT SELECT ON sf_archive.v_noticias_publicas   TO 'sfa_web'@'%';
GRANT SELECT ON sf_archive.zonas_seguras         TO 'sfa_web'@'%';
GRANT SELECT, INSERT ON sf_archive.solicitudes_ayuda TO 'sfa_web'@'%';
GRANT SELECT ON sf_archive.usuarios TO 'sfa_web'@'%';
GRANT SELECT ON sf_archive.potenciales TO 'sfa_web'@'%';
GRANT UPDATE (ultimo_acceso) ON sf_archive.usuarios TO 'sfa_web'@'%';
GRANT INSERT ON sf_archive.registro_actividad TO 'sfa_web'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE ON sf_archive.* TO 'sfa_admin'@'%';
FLUSH PRIVILEGES;
