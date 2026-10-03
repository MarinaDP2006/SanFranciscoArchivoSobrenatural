-- =====================================================================
--  SAN FRANCISCO ARCHIVE · Datos iniciales (ficción ambientada en SF)
--  Ejecutar después de 01_schema.sql
--
--  Contraseñas de demostración (cámbialas desde la app → Mi perfil):
--    Administradores (james, sarah, nina) ........ Archivo1906!
--    Potenciales (niebla, faro, cable, ...) ...... Potencial2026!
-- =====================================================================
USE sf_archive;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- USUARIOS (3 administradores + 10 potenciales)
-- ---------------------------------------------------------------------
INSERT INTO usuarios (id, username, email, password_hash, rol, nombre_completo, ultimo_acceso) VALUES
(1,  'james',   'james@sfarchive.org',   'pbkdf2_sha256$65536$N7rTbco/utkwbIzRCO9L6w==$Gj1+qP27pq/1jf7Tru35NriQL0JNWsDVqa5wFAKNH18=', 'ADMIN', 'James Whitaker', '2026-10-03 08:12:00'),
(2,  'sarah',   'sarah@sfarchive.org',   'pbkdf2_sha256$65536$N7rTbco/utkwbIzRCO9L6w==$Gj1+qP27pq/1jf7Tru35NriQL0JNWsDVqa5wFAKNH18=', 'ADMIN', 'Sarah Whitaker', '2026-10-02 22:47:00'),
(3,  'nina',    'nina@sfarchive.org',    'pbkdf2_sha256$65536$N7rTbco/utkwbIzRCO9L6w==$Gj1+qP27pq/1jf7Tru35NriQL0JNWsDVqa5wFAKNH18=', 'ADMIN', 'Nina',           '2026-10-03 09:30:00'),
(4,  'niebla',  'elena.vargas@sfarchive.org',  'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Elena Vargas',  NULL),
(5,  'faro',    'marcus.lee@sfarchive.org',    'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Marcus Lee',    NULL),
(6,  'cable',   'danny.oconnor@sfarchive.org', 'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Danny O''Connor', NULL),
(7,  'marea',   'isabel.reyes@sfarchive.org',  'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Isabel Reyes',  NULL),
(8,  'eco',     'theo.brooks@sfarchive.org',   'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Theo Brooks',   NULL),
(9,  'ceniza',  'grace.kim@sfarchive.org',     'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Grace Kim',     NULL),
(10, 'sombra',  'luis.navarro@sfarchive.org',  'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Luis Navarro',  NULL),
(11, 'brujula', 'amara.okafor@sfarchive.org',  'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Amara Okafor',  NULL),
(12, 'ancla',   'samuel.park@sfarchive.org',   'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Samuel Park',   NULL),
(13, 'roca',    'rosa.delgado@sfarchive.org',  'pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU=', 'POTENCIAL', 'Rosa Delgado',  NULL);

INSERT INTO administradores (usuario_id, cargo, edad, anios_experiencia, biografia) VALUES
(1, 'Director del Archivo', 62, 190,
 'Cofundador del San Francisco Archive. Sobrevivió al incendio de 1906 sin envejecer un solo día durante más de un siglo; su experiencia con lo sobrenatural supera con creces su edad aparente. Casado con Sarah.'),
(2, 'Directora de Operaciones', 53, 35,
 'Cofundadora del Archivo y esposa de James. Coordina la red de Potenciales, la asignación de contratos y la protección de sus familias.'),
(3, 'Administradora del Sistema y Archivista', NULL, NULL,
 'Responsable del sistema de gestión, de la web pública y del Archivo Restringido. Mantiene la tapadera informativa y la base de datos mundial.');

-- ---------------------------------------------------------------------
-- GRUPOS TÁCTICOS
-- ---------------------------------------------------------------------
INSERT INTO grupos_tacticos (id, nombre, pais, ciudad, zona, lat, lng, descripcion) VALUES
(1, 'Unidad Golden Gate', 'Estados Unidos', 'San Francisco', 'Presidio · Richmond · Sunset · Sausalito', 37.798900, -122.466200,
 'Cubre la costa oeste de la ciudad, el puente y la niebla del estrecho.'),
(2, 'Unidad Mission',     'Estados Unidos', 'San Francisco', 'Mission · Castro · SoMa · Dogpatch',      37.759900, -122.414800,
 'Barrios del sur y del centro. Especialistas en cultos y entidades urbanas.'),
(3, 'Unidad Chinatown',   'Estados Unidos', 'San Francisco', 'Chinatown · North Beach · Nob Hill · Tenderloin', 37.794100, -122.407800,
 'El corazón antiguo de la ciudad: túneles, cable cars y espíritus del 1906.'),
(4, 'Unidad Bahía Este',  'Estados Unidos', 'Oakland',       'Oakland · Berkeley · Alameda',            37.804400, -122.271200,
 'Apoyo al otro lado del Bay Bridge.');

-- ---------------------------------------------------------------------
-- POTENCIALES (10). Saldo = suma de su historial de monedero.
-- ---------------------------------------------------------------------
INSERT INTO potenciales (id, usuario_id, alias, nombre_real, edad, habilidad, descripcion, nivel, estado, barrio, ciudad, lat, lng, grupo_id, saldo, fecha_reclutamiento) VALUES
(1,  4,  'Niebla',  'Elena Vargas',    29, 'Tránsito por la niebla',        'Puede desplazarse varios cientos de metros a través de cualquier banco de niebla.', 4, 'EN_MISION',  'Mission',        'San Francisco', 37.759900, -122.414800, 2,  458.81, '2022-03-11'),
(2,  5,  'Faro',    'Marcus Lee',      34, 'Lectura de rastros residuales', 'Ve la huella emocional que dejan las personas y las entidades en los lugares.',   5, 'DISPONIBLE', 'Chinatown',      'San Francisco', 37.794100, -122.407800, 3, 7218.05, '2019-09-02'),
(3,  6,  'Cable',   'Danny O''Connor', 41, 'Tecnopatía eléctrica',          'Siente y controla la corriente de cables, farolas y redes eléctricas.',          3, 'EN_MISION',  'Nob Hill',       'San Francisco', 37.793000, -122.416100, 3,  474.19, '2021-01-20'),
(4,  7,  'Marea',   'Isabel Reyes',    26, 'Hidroquinesis',                 'Manipula el agua de la bahía; puede abrir caminos en la marea.',                  4, 'DISPONIBLE', 'Sausalito',      'Sausalito',     37.859100, -122.485300, 1, 4967.08, '2023-06-15'),
(5,  8,  'Eco',     'Theo Brooks',     22, 'Psicometría auditiva',          'Escucha las conversaciones que han quedado atrapadas en objetos y paredes.',      3, 'DISPONIBLE', 'Haight-Ashbury', 'San Francisco', 37.769200, -122.448100, 1,  500.00, '2025-11-04'),
(6,  9,  'Ceniza',  'Grace Kim',       38, 'Pirocinesis controlada',        'Genera y absorbe fuego. Herida en entrenamiento; en recuperación.',              4, 'HERIDO',     'Richmond',       'San Francisco', 37.780600, -122.464400, 1,  800.00, '2020-05-30'),
(7,  10, 'Sombra',  'Luis Navarro',    31, 'Ocultación en sombras',         'Se vuelve invisible mientras permanezca en zonas sin luz directa.',               3, 'EN_MISION',  'Tenderloin',     'San Francisco', 37.784700, -122.414100, 3, 4437.19, '2021-10-31'),
(8,  11, 'Brújula', 'Amara Okafor',    45, 'Localización de personas',      'Sabe en qué dirección está cualquier persona de la que tenga un objeto personal.', 5, 'DISPONIBLE', 'Downtown',       'Oakland',       37.804400, -122.271200, 4,  261.34, '2018-02-14'),
(9,  12, 'Ancla',   'Samuel Park',     50, 'Sellado de entidades',          'Encierra entidades en objetos inertes mediante sellos caligráficos.',             4, 'DISPONIBLE', 'Southside',      'Berkeley',      37.871500, -122.273000, 4, 3961.09, '2017-08-08'),
(10, 13, 'Roca',    'Rosa Delgado',    36, 'Inmunidad a la posesión',       'Ninguna entidad puede poseerla; resiste ataques psíquicos.',                      3, 'DISPONIBLE', 'North Beach',    'San Francisco', 37.806100, -122.410300, NULL, 500.00, '2026-07-01');

-- ---------------------------------------------------------------------
-- VÍNCULOS FAMILIARES
-- ---------------------------------------------------------------------
INSERT INTO vinculos_familiares (potencial_id, nombre, parentesco, edad, ciudad, conoce_secreto, en_riesgo, notas) VALUES
(1, 'Carmen Vargas',   'Madre',     58, 'San Francisco', 0, 0, 'Tiene una panadería en la calle 24.'),
(1, 'Diego Vargas',    'Hermano',   24, 'San José',      1, 0, NULL),
(2, 'Mei Lee',         'Abuela',    81, 'San Francisco', 1, 1, 'Vive sobre la tienda de té de Ross Alley. Reconoce a los huli jing.'),
(3, 'Kathleen O''Connor','Esposa',  39, 'San Francisco', 1, 0, NULL),
(3, 'Liam O''Connor',  'Hijo',      9,  'San Francisco', 0, 0, 'Colegio en Nob Hill.'),
(4, 'Tomás Reyes',     'Padre',     61, 'Sausalito',     0, 0, 'Pescador. Lleva años hablando de "la mujer foca".'),
(5, 'Jordan Brooks',   'Hermana',   19, 'Berkeley',      0, 0, 'Estudiante en UC Berkeley.'),
(6, 'Daniel Kim',      'Hijo',      12, 'San Francisco', 0, 1, 'Marcado tras el incidente de entrenamiento: vigilancia preventiva.'),
(7, 'Ana Navarro',     'Hermana',   28, 'Los Ángeles',   1, 0, NULL),
(8, 'Chidi Okafor',    'Esposo',    47, 'Oakland',       1, 0, 'Médico en Highland Hospital; atiende heridas sin preguntar.'),
(9, 'Hana Park',       'Hija',      22, 'Seúl',          0, 0, NULL),
(10,'Marco Delgado',   'Padre',     70, 'San Francisco', 0, 1, 'Exguarda de Alcatraz. Sufre pesadillas recurrentes.');

-- ---------------------------------------------------------------------
-- INCIDENTES (lo público es la "noticia"; lo clasificado es la anomalía)
-- ---------------------------------------------------------------------
INSERT INTO incidentes (id, codigo, titulo, tipo, descripcion_publica, barrio, direccion, ciudad, lat, lng, fecha_incidente, estado, publicado, anomalia_clasificada, nivel_amenaza, origen, creado_por) VALUES
(1,  'SFA-2026-001', 'Desaparece un turista en la niebla del Golden Gate', 'DESAPARICION',
 'Un turista de 34 años fue visto por última vez caminando por la pasarela peatonal del Golden Gate durante un banco de niebla especialmente denso. Las cámaras no registraron su salida del puente.',
 'Presidio', 'Golden Gate Bridge, pasarela este', 'San Francisco', 37.819900, -122.478300, '2026-08-14 06:40:00', 'RESUELTO', 1,
 'La niebla del estrecho actúa como umbral hacia la "Bahía Gris". Entidad: el Barquero de Niebla, que cobra un pasajero por cada niebla de más de 6 horas.', 4, 'ARCHIVO', 1),
(2,  'SFA-2026-002', 'Hallan sin vida a un vigilante nocturno en Alcatraz', 'ASESINATO',
 'El vigilante de seguridad de la isla fue encontrado sin vida en el bloque D de la antigua prisión. La policía del parque nacional investiga el suceso.',
 'Alcatraz', 'Alcatraz Island, Cellhouse bloque D', 'San Francisco', 37.826700, -122.423000, '2026-08-22 23:10:00', 'RESUELTO', 1,
 'Eco residual con capacidad física de un recluso muerto en la "Batalla de Alcatraz" (1946). Se manifiesta en el aniversario de cada motín.', 5, 'ARCHIVO', 2),
(3,  'SFA-2026-003', 'Secuestran a una niña en Chinatown durante el Festival de la Luna', 'SECUESTRO',
 'Una niña de 7 años desapareció entre la multitud del Festival de Medio Otoño, cerca de Ross Alley. Los testigos hablan de un hombre con una lámpara de papel.',
 'Chinatown', 'Ross Alley', 'San Francisco', 37.795500, -122.407100, '2026-09-06 21:15:00', 'RESUELTO', 1,
 'Huli jing (espíritu zorro) que se oculta en los farolillos de papel y sustituye a los niños por réplicas de arcilla.', 4, 'ARCHIVO', 2),
(4,  'SFA-2026-004', 'Tres jóvenes desaparecen en las ruinas de Sutro Baths', 'DESAPARICION',
 'Tres estudiantes que exploraban las ruinas de los antiguos baños Sutro al atardecer no regresaron. Guardacostas y bomberos rastrean la zona de Lands End.',
 'Lands End', 'Sutro Baths, Point Lobos Ave', 'San Francisco', 37.780400, -122.513800, '2026-09-12 19:30:00', 'EN_INVESTIGACION', 1,
 'Las ruinas conservan un bucle temporal anclado al día de la inauguración (1896). Las víctimas siguen allí, nadando en una piscina que ya no existe.', 4, 'ARCHIVO', 1),
(5,  'SFA-2026-005', 'Muere un conductor de cable car en Powell Street', 'ASESINATO',
 'Un conductor del cable car de la línea Powell-Mason murió electrocutado de madrugada. La empresa municipal asegura que el sistema no tenía averías.',
 'Nob Hill', 'Powell St & Washington St', 'San Francisco', 37.794600, -122.411400, '2026-09-15 07:05:00', 'EN_INVESTIGACION', 1,
 'Poltergeist eléctrico que se alimenta de la red de cables subterráneos instalada en 1873.', 3, 'ARCHIVO', 3),
(6,  'SFA-2026-006', 'Desaparición en el túnel de Stockton', 'DESAPARICION',
 'Un repartidor en bicicleta desapareció mientras cruzaba el túnel de Stockton Street a las 2 de la madrugada. Su bicicleta apareció intacta en la salida norte.',
 'Chinatown', 'Stockton Tunnel', 'San Francisco', 37.791000, -122.406900, '2026-09-18 02:20:00', 'VERIFICADO', 1,
 'Bruja de túnel que se alimenta de viajeros solitarios entre las 2:00 y las 3:00.', 3, 'ARCHIVO', 3),
(7,  'SFA-2026-007', 'Secuestran a un músico tras un concierto en Haight-Ashbury', 'SECUESTRO',
 'Un guitarrista local fue obligado a subir a una furgoneta pintada a mano tras su actuación en un bar de Haight Street.',
 'Haight-Ashbury', 'Haight St & Ashbury St', 'San Francisco', 37.769900, -122.446900, '2026-09-20 01:45:00', 'VERIFICADO', 1,
 'Culto psicodélico que intenta invocar al "Ente del Verano del Amor" (1967) con músicos como ofrenda.', 3, 'ARCHIVO', 2),
(8,  'SFA-2026-008', 'Aparece un cuerpo en el lago Stow del Golden Gate Park', 'ASESINATO',
 'Corredores madrugadores encontraron el cuerpo de un hombre en la orilla de Stow Lake. La policía no descarta ninguna hipótesis.',
 'Golden Gate Park', 'Stow Lake Dr', 'San Francisco', 37.770000, -122.477000, '2026-09-23 05:50:00', 'EN_INVESTIGACION', 1,
 'La Dama Blanca de Stow Lake: espectro que busca a su bebé y ahoga a quien no responde a su pregunta.', 5, 'ARCHIVO', 1),
(9,  'SFA-2026-009', 'Desaparece una enfermera del turno de noche en el Presidio', 'DESAPARICION',
 'Una enfermera de 41 años no llegó a su casa tras terminar el turno en un centro médico del Presidio. Su coche seguía en el aparcamiento.',
 'Presidio', 'Letterman Dr', 'San Francisco', 37.798900, -122.466200, '2026-09-25 03:30:00', 'VERIFICADO', 1,
 'Soldados fantasma del antiguo hospital militar Letterman "reclutan" personal sanitario para una guerra que terminó hace 80 años.', 4, 'ARCHIVO', 2),
(10, 'SFA-2026-010', 'Secuestro exprés en el Tenderloin', 'SECUESTRO',
 'Un hombre fue retenido durante seis horas por un conductor de una aplicación de transporte. Fue liberado en Bayview sin recordar nada.',
 'Tenderloin', 'Turk St', 'San Francisco', 37.783000, -122.414000, '2026-09-27 22:40:00', 'EN_INVESTIGACION', 1,
 'Cambiaformas que suplanta a conductores de VTC para robar años de vida a sus pasajeros.', 3, 'ARCHIVO', 3),
(11, 'SFA-2026-011', 'Hallan muerto a un pescador en Fisherman''s Wharf', 'ASESINATO',
 'El cuerpo de un pescador veterano apareció enredado en sus propias redes junto al Pier 45.',
 'Fisherman''s Wharf', 'Pier 45', 'San Francisco', 37.808700, -122.418000, '2026-09-28 05:10:00', 'VERIFICADO', 1,
 'Selkie hostil de la bahía: reclama la piel que un pescador le robó hace décadas.', 4, 'ARCHIVO', 1),
(12, 'SFA-2026-012', 'Un estudiante desaparece en Twin Peaks', 'DESAPARICION',
 'Un estudiante universitario subió a Twin Peaks para ver el atardecer y no volvió. Su familia pide colaboración ciudadana.',
 'Twin Peaks', 'Twin Peaks Blvd', 'San Francisco', 37.754400, -122.447700, '2026-09-29 20:00:00', 'NO_VERIFICADO', 1,
 NULL, 2, 'CIUDADANO', 3),
(13, 'SFA-2026-013', 'Intento de secuestro en el Ferry Building', 'SECUESTRO',
 'Una mujer denunció que dos personas intentaron subirla a la fuerza a un barco privado en el muelle del Ferry Building.',
 'Embarcadero', 'Ferry Building, 1 Ferry Building', 'San Francisco', 37.795500, -122.393700, '2026-09-30 18:20:00', 'NO_VERIFICADO', 1,
 NULL, 2, 'CIUDADANO', 3),
(14, 'SFA-2026-014', 'Asesinato en un loft de SoMa', 'ASESINATO',
 'Un coleccionista de arte fue hallado muerto en su loft. No había signos de entrada forzada; todos los espejos de la casa estaban rotos menos uno.',
 'SoMa', 'Folsom St', 'San Francisco', 37.778500, -122.405600, '2026-10-01 00:30:00', 'VERIFICADO', 1,
 'Espejo maldito adquirido en una subasta de Union Square. El reflejo sale cuando no hay nadie mirando.', 3, 'ARCHIVO', 2),
(15, 'SFA-2026-015', 'Desaparecen dos hermanos en Ocean Beach', 'DESAPARICION',
 'Dos hermanos de 15 y 17 años desaparecieron mientras surfeaban en Ocean Beach. Los guardacostas mantienen la búsqueda.',
 'Outer Sunset', 'Great Highway', 'San Francisco', 37.759400, -122.510700, '2026-10-01 17:45:00', 'VERIFICADO', 1,
 'Corriente de resaca habitada por "Los que tiran": ahogados que necesitan compañía.', 4, 'ARCHIVO', 1),
(16, 'SFA-2026-016', 'Secuestran a un anciano en Japantown', 'SECUESTRO',
 'Un vecino de 82 años desapareció de la Peace Plaza a plena luz del día.',
 'Japantown', 'Peace Plaza', 'San Francisco', 37.785400, -122.429400, '2026-10-02 11:00:00', 'NO_VERIFICADO', 0,
 NULL, 2, 'CIUDADANO', 2),
(17, 'SFA-2026-017', 'Muerte inexplicable en Lake Merritt', 'ASESINATO',
 'Un remero apareció sin vida en Lake Merritt. La autopsia no determina la causa de la muerte.',
 'Lake Merritt', 'Lakeside Dr', 'Oakland', 37.803000, -122.258000, '2026-09-10 06:00:00', 'RESUELTO', 1,
 'Nix del lago: espíritu del agua que arrastra a quien rema solo antes del amanecer.', 3, 'ARCHIVO', 1),
(18, 'SFA-2026-018', 'Desaparece una bibliotecaria en Berkeley', 'DESAPARICION',
 'Una bibliotecaria de la universidad no salió del edificio tras el cierre. Las cámaras la muestran entrando en la sala de lectura.',
 'Campus', 'Doe Library', 'Berkeley', 37.871900, -122.258500, '2026-09-02 21:00:00', 'ARCHIVADO', 1,
 'Libro-portal del fondo antiguo. La víctima permanece al otro lado; el portal se cerró antes de la extracción.', 4, 'ARCHIVO', 2),
(19, 'SFA-2026-019', 'Secuestran a una mujer junto a la Misión Dolores', 'SECUESTRO',
 'Una vecina fue vista por última vez junto al cementerio de la Misión Dolores. Varios testigos oyeron llantos durante la noche.',
 'Mission', 'Dolores St & 16th St', 'San Francisco', 37.764200, -122.427100, '2026-10-02 21:30:00', 'EN_INVESTIGACION', 1,
 'La Llorona del cementerio de la misión se lleva a mujeres que pasean solas a medianoche.', 4, 'CIUDADANO', 3),
(20, 'SFA-2026-020', 'Hallazgo bajo la Coit Tower', 'ASESINATO',
 'Restos humanos hallados en los túneles de mantenimiento bajo Telegraph Hill.',
 'Telegraph Hill', 'Coit Tower, 1 Telegraph Hill Blvd', 'San Francisco', 37.802400, -122.405800, '2026-10-03 06:15:00', 'NO_VERIFICADO', 0,
 'Informe de campo de Roca: posible necrófago en los túneles de Telegraph Hill.', 3, 'POTENCIAL', 13);

-- ---------------------------------------------------------------------
-- SOLICITUDES DE AYUDA (enviadas de forma anónima desde la web)
-- ---------------------------------------------------------------------
INSERT INTO solicitudes_ayuda (id, codigo_seguimiento, tipo, descripcion, barrio, ubicacion, lat, lng, contacto, estado, incidente_id, respuesta_publica, recibida_en, revisada_por, revisada_en) VALUES
(1, 'SF-7K2Q9M', 'SECUESTRO',    'Mi vecina salió a pasear al perro y no volvió. Se oían llantos en el cementerio de la Misión.', 'Mission', 'Dolores St & 16th St', 37.764200, -122.427100, NULL, 'ATENDIDA', 19, 'Tu aviso ha sido atendido. Un equipo está trabajando en el caso.', '2026-10-02 23:05:00', 3, '2026-10-02 23:40:00'),
(2, 'SF-3H8P1X', 'DESAPARICION', 'Mi compañero de piso subió a Twin Peaks y no contesta al teléfono desde ayer.', 'Twin Peaks', 'Mirador de Twin Peaks', 37.754400, -122.447700, 'compi@correo.com', 'ATENDIDA', 12, 'Hemos registrado el caso. Mantén el código para consultar novedades.', '2026-09-30 08:15:00', 3, '2026-09-30 10:00:00'),
(3, 'SF-9D4L6B', 'SECUESTRO',    'Un señor mayor desapareció de la Peace Plaza. Nadie vio cómo se iba.', 'Japantown', 'Peace Plaza', 37.785400, -122.429400, NULL, 'EN_REVISION', 16, NULL, '2026-10-02 12:30:00', 2, '2026-10-02 13:00:00'),
(4, 'SF-2W5N8R', 'ASESINATO',    'Gritos durante tres noches seguidas en una casa abandonada de Bernal Heights. Hoy huele muy mal.', 'Bernal Heights', 'Cortland Ave', 37.739000, -122.415500, NULL, 'PENDIENTE', NULL, NULL, '2026-10-03 07:50:00', NULL, NULL),
(5, 'SF-6T1J3V', 'DESAPARICION', 'Mi vecino de Dogpatch lleva cuatro días sin aparecer. Su puerta está abierta y hay escarcha dentro.', 'Dogpatch', '3rd St & 22nd St', 37.757900, -122.388300, '(415) 555-0142', 'PENDIENTE', NULL, NULL, '2026-10-03 09:05:00', NULL, NULL),
(6, 'SF-8Y7C2Z', 'DESAPARICION', 'Han desaparecido las luces del Bay Bridge, seguro que son extraterrestres.', 'Embarcadero', 'Bay Bridge', 37.798300, -122.377800, NULL, 'DESCARTADA', NULL, 'Gracias por tu aviso. No hemos encontrado indicios de un incidente.', '2026-09-26 23:59:00', 1, '2026-09-27 09:00:00');

-- ---------------------------------------------------------------------
-- CONTRATOS DE ANOMALÍA
-- Coste de transporte = 25 USD + 1,80 USD/km (distancia potencial → incidente)
-- ---------------------------------------------------------------------
INSERT INTO contratos (id, codigo, incidente_id, solicitud_id, estado, prioridad, grupo_id, potencial_id, recompensa, coste_transporte, distancia_km, notas_campo, fecha_solicitud, fecha_asignacion, fecha_cierre, asignado_por) VALUES
(1,  'CTR-2026-001', 1,  NULL, 'COMPLETADO',         'ALTA',    1, 4, 4500.00, 32.92, 4.40, 'Abrí un camino en la marea bajo el puente y saqué al turista de la Bahía Gris antes del amanecer.', '2026-08-14 08:00:00', '2026-08-14 09:00:00', '2026-08-16 04:30:00', 1),
(2,  'CTR-2026-002', 2,  NULL, 'COMPLETADO',         'CRITICA', 3, 2, 6000.00, 31.95, 3.86, 'Rastro residual seguido hasta la celda 403. Eco disipado con ayuda de un capellán.', '2026-08-23 07:00:00', '2026-08-23 08:00:00', '2026-08-25 02:10:00', 2),
(3,  'CTR-2026-003', 3,  NULL, 'COMPLETADO',         'ALTA',    3, 7, 4000.00, 27.43, 1.35, 'Niña recuperada en el sótano de una herboristería. Réplica de arcilla destruida.', '2026-09-06 21:40:00', '2026-09-06 22:00:00', '2026-09-07 05:40:00', 2),
(4,  'CTR-2026-004', 17, NULL, 'COMPLETADO',         'MEDIA',   4, 9, 3500.00, 38.91, 7.73, 'Nix sellado en una piedra de río. Entregada al Archivo.', '2026-09-10 10:00:00', '2026-09-10 12:00:00', '2026-09-12 23:00:00', 1),
(5,  'CTR-2026-005', 18, NULL, 'FALLIDO',            'MEDIA',   4, 8, 2500.00, 38.66, 7.59, 'Localicé a la víctima al otro lado del libro, pero el portal se cerró. El libro está en custodia.', '2026-09-03 09:00:00', '2026-09-03 10:00:00', '2026-09-09 18:00:00', 2),
(6,  'CTR-2026-006', 4,  NULL, 'EN_CURSO',           'ALTA',    2, 1, 5000.00, 41.19, 9.00, 'Entrada al bucle identificada en la piscina norte. Necesito una noche de niebla.', '2026-09-13 08:00:00', '2026-09-13 09:30:00', NULL, 1),
(7,  'CTR-2026-007', 8,  NULL, 'ASIGNADO',           'CRITICA', 3, 7, 7000.00, 35.38, 5.76, NULL, '2026-09-23 09:00:00', '2026-09-24 10:00:00', NULL, 1),
(8,  'CTR-2026-008', 5,  NULL, 'PENDIENTE_REVISION', 'MEDIA',   3, 3, 3000.00, 25.81, 0.45, 'Poltergeist descargado en la subestación de Mason St. Solicito cierre.', '2026-09-15 10:00:00', '2026-09-15 12:00:00', NULL, 3),
(9,  'CTR-2026-009', 6,  NULL, 'SOLICITADO',         'ALTA',    NULL, NULL, 3500.00, NULL, NULL, NULL, '2026-09-18 09:00:00', NULL, NULL, NULL),
(10, 'CTR-2026-010', 9,  NULL, 'SOLICITADO',         'ALTA',    NULL, NULL, 4500.00, NULL, NULL, NULL, '2026-09-25 09:00:00', NULL, NULL, NULL),
(11, 'CTR-2026-011', 11, NULL, 'SOLICITADO',         'MEDIA',   NULL, NULL, 4000.00, NULL, NULL, NULL, '2026-09-28 08:00:00', NULL, NULL, NULL),
(12, 'CTR-2026-012', 14, NULL, 'SOLICITADO',         'MEDIA',   NULL, NULL, 3000.00, NULL, NULL, NULL, '2026-10-01 09:00:00', NULL, NULL, NULL),
(13, 'CTR-2026-013', 15, NULL, 'SOLICITADO',         'CRITICA', NULL, NULL, 6500.00, NULL, NULL, NULL, '2026-10-01 19:00:00', NULL, NULL, NULL),
(14, 'CTR-2026-014', 19, 1,    'SOLICITADO',         'ALTA',    NULL, NULL, 5000.00, NULL, NULL, NULL, '2026-10-02 23:40:00', NULL, NULL, NULL),
(15, 'CTR-2026-015', 10, NULL, 'SOLICITADO',         'MEDIA',   NULL, NULL, 3000.00, NULL, NULL, NULL, '2026-09-28 10:00:00', NULL, NULL, NULL),
(16, 'CTR-2026-016', 7,  NULL, 'SOLICITADO',         'BAJA',    NULL, NULL, 2000.00, NULL, NULL, NULL, '2026-09-20 12:00:00', NULL, NULL, NULL);

-- ---------------------------------------------------------------------
-- TRANSACCIONES DE MONEDERO (USD). saldo_resultante encadena el historial.
-- ---------------------------------------------------------------------
INSERT INTO transacciones_monedero (potencial_id, contrato_id, tipo, importe, saldo_resultante, concepto, fecha, realizado_por) VALUES
(1,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(2,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(3,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(4,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(5,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(6,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(7,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(8,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(9,  NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(10, NULL, 'AJUSTE',       500.00,  500.00, 'Fondo inicial de la red',                       '2026-08-01 10:00:00', 1),
(4,  1,    'TRANSPORTE',   -32.92,  467.08, 'Transporte CTR-2026-001 (4.40 km)',             '2026-08-14 09:00:00', 1),
(4,  1,    'RECOMPENSA',  4500.00, 4967.08, 'Recompensa CTR-2026-001',                       '2026-08-16 04:30:00', 1),
(2,  2,    'TRANSPORTE',   -31.95,  468.05, 'Transporte CTR-2026-002 (3.86 km)',             '2026-08-23 08:00:00', 2),
(2,  2,    'RECOMPENSA',  6000.00, 6468.05, 'Recompensa CTR-2026-002',                       '2026-08-25 02:10:00', 2),
(2,  2,    'BONUS',        750.00, 7218.05, 'Bonus por contención de amenaza nivel 5',       '2026-08-26 10:00:00', 1),
(8,  5,    'TRANSPORTE',   -38.66,  461.34, 'Transporte CTR-2026-005 (7.59 km)',             '2026-09-03 10:00:00', 2),
(7,  3,    'TRANSPORTE',   -27.43,  472.57, 'Transporte CTR-2026-003 (1.35 km)',             '2026-09-06 22:00:00', 2),
(7,  3,    'RECOMPENSA',  4000.00, 4472.57, 'Recompensa CTR-2026-003',                       '2026-09-07 05:40:00', 2),
(8,  5,    'PENALIZACION',-200.00,  261.34, 'Pérdida de equipo de rastreo (contrato fallido)','2026-09-09 18:00:00', 2),
(9,  4,    'TRANSPORTE',   -38.91,  461.09, 'Transporte CTR-2026-004 (7.73 km)',             '2026-09-10 12:00:00', 1),
(9,  4,    'RECOMPENSA',  3500.00, 3961.09, 'Recompensa CTR-2026-004',                       '2026-09-12 23:00:00', 1),
(1,  6,    'TRANSPORTE',   -41.19,  458.81, 'Transporte CTR-2026-006 (9.00 km)',             '2026-09-13 09:30:00', 1),
(3,  8,    'TRANSPORTE',   -25.81,  474.19, 'Transporte CTR-2026-008 (0.45 km)',             '2026-09-15 12:00:00', 3),
(7,  7,    'TRANSPORTE',   -35.38, 4437.19, 'Transporte CTR-2026-007 (5.76 km)',             '2026-09-24 10:00:00', 1),
(6,  NULL, 'AJUSTE',       300.00,  800.00, 'Gastos médicos cubiertos por el Archivo',       '2026-09-26 11:00:00', 2);

-- ---------------------------------------------------------------------
-- ARCHIVO RESTRINGIDO: informes finales clasificados
-- ---------------------------------------------------------------------
INSERT INTO informes_clasificados (contrato_id, autor_id, titulo, entidad_anomala, clasificacion, resumen, contenido, bajas_civiles) VALUES
(1, 1, 'El Barquero de Niebla', 'Barquero de Niebla', 'ALTO_SECRETO',
 'Extracción exitosa de un civil desde la Bahía Gris.',
 'Durante la niebla del 14 de agosto (9 h 20 min de duración) el Barquero reclamó a un turista en la pasarela este.\n\nLa potencial MAREA abrió un canal en la marea bajo la torre sur y alcanzó la Bahía Gris por debajo del agua, donde la entidad no tiene jurisdicción. El civil fue devuelto con hipotermia leve y sin recuerdos del episodio.\n\nRecomendación: vigilar nieblas de más de 6 horas. Coordinar con la Unidad Golden Gate un turno de observación en Fort Point.', 0),
(2, 2, 'Eco del motín de 1946', 'Eco residual con capacidad física', 'SECRETO',
 'Eco disipado; la víctima fue la única baja.',
 'FARO siguió el rastro emocional desde el bloque D hasta la celda 403, donde el eco del recluso se reproducía cada aniversario del motín.\n\nCon la colaboración de un antiguo capellán de la prisión se realizó el rito de cierre. La celda queda marcada para revisión anual cada 2 de mayo.', 1),
(3, 2, 'Huli jing de Ross Alley', 'Huli jing (espíritu zorro)', 'SECRETO',
 'Niña recuperada sana. Réplica de arcilla neutralizada.',
 'SOMBRA se infiltró durante el festival y siguió al portador de la lámpara hasta una herboristería. La niña estaba dormida en el sótano.\n\nLa réplica de arcilla que el zorro dejó en su cama se desmoronó al amanecer. La abuela de FARO (Mei Lee) colaboró identificando el farolillo.\n\nLa entidad huyó: amenaza latente.', 0),
(4, 1, 'El Nix de Lake Merritt', 'Nix del lago', 'CONFIDENCIAL',
 'Entidad sellada en una piedra de río y almacenada.',
 'ANCLA realizó el sello caligráfico al amanecer en el embarcadero de Lakeside. La piedra se custodia en la cámara 2 del Archivo.', 0),
(5, 2, 'La sala de lectura de Doe Library', 'Libro-portal', 'OMEGA',
 'Contrato fallido. Víctima atrapada al otro lado.',
 'BRÚJULA localizó a la bibliotecaria "dentro" del volumen antiguo MS-1789. El portal se cerró antes de la extracción.\n\nEl libro está bajo custodia y NO debe abrirse sin la presencia de ANCLA y de un administrador. Se estudia un segundo intento en la próxima luna nueva.', 1);

-- ---------------------------------------------------------------------
-- NOTICIAS (portada pública de la web)
-- ---------------------------------------------------------------------
INSERT INTO noticias (slug, titulo, resumen, contenido, categoria, barrio, incidente_id, publicada, destacada, fecha_publicacion, autor_id) VALUES
('niebla-record-golden-gate', 'Niebla récord en el Golden Gate: recomiendan no cruzar a pie de madrugada',
 'Tras la desaparición de un turista en agosto, el Archivo recomienda evitar la pasarela durante las nieblas más densas.',
 'La niebla de agosto volvió a cubrir el Golden Gate durante más de nueve horas. El turista desaparecido el día 14 fue localizado dos días después en buen estado, aunque desorientado.\n\nDesde el San Francisco Archive recomendamos no cruzar el puente a pie de madrugada cuando la visibilidad sea inferior a 50 metros, y llevar siempre el teléfono cargado.',
 'CIUDAD', 'Presidio', 1, 1, 1, '2026-08-17 09:00:00', 3),
('cerrado-caso-vigilante-alcatraz', 'Cierran el caso del vigilante de Alcatraz',
 'La investigación concluye que no hubo intervención de terceros. La isla reabre las visitas nocturnas.',
 'El parque nacional ha reabierto las visitas nocturnas a la isla de Alcatraz después de que la investigación sobre la muerte del vigilante concluyera sin hallar responsables.\n\nLos visitantes deberán permanecer siempre con su grupo y no acceder al bloque D.',
 'SUCESOS', 'Alcatraz', 2, 1, 0, '2026-08-27 10:30:00', 2),
('recuperada-nina-chinatown', 'Encuentran sana y salva a la niña desaparecida en Chinatown',
 'La menor fue hallada a primera hora de la mañana a pocas calles del festival.',
 'La niña de 7 años desaparecida durante el Festival de Medio Otoño ha sido encontrada sana y salva. La familia agradece la colaboración de los vecinos de Ross Alley.\n\nSe recuerda a las familias que mantengan a los menores cerca durante los eventos multitudinarios.',
 'SUCESOS', 'Chinatown', 3, 1, 0, '2026-09-07 12:00:00', 2),
('aviso-cierre-sutro-baths', 'Aviso: cierre nocturno de las ruinas de Sutro Baths',
 'El acceso a las ruinas queda restringido desde el atardecer mientras continúa la búsqueda de tres jóvenes.',
 'Las autoridades han cerrado el acceso nocturno a las ruinas de Sutro Baths y a los senderos de Lands End.\n\nSi tienes información sobre los tres estudiantes desaparecidos el 12 de septiembre, usa el formulario anónimo de ayuda de esta web.',
 'AVISO', 'Lands End', 4, 1, 1, '2026-09-14 08:00:00', 1),
('como-pedir-ayuda-anonima', 'Cómo pedir ayuda al Archivo de forma anónima',
 'No necesitas registrarte: rellena el formulario, guarda tu código y consulta el estado de tu aviso.',
 'Cualquier ciudadano puede comunicarnos un secuestro, una desaparición o una muerte extraña sin crear una cuenta.\n\n1. Entra en "Pedir ayuda".\n2. Describe lo ocurrido y marca el lugar en el mapa.\n3. Guarda el código de seguimiento que te daremos.\n\nTus datos de contacto son opcionales. En caso de peligro inmediato llama siempre al 911.',
 'COMUNIDAD', NULL, NULL, 1, 0, '2026-09-01 09:00:00', 3),
('1906-terremoto-que-desperto-ciudad', '1906: el terremoto que despertó a la ciudad',
 'Ciento veinte años después, repasamos el seísmo y el incendio que cambiaron San Francisco para siempre.',
 'A las 5:12 de la mañana del 18 de abril de 1906 un terremoto de magnitud 7,9 sacudió San Francisco. Los incendios posteriores arrasaron más de 500 manzanas.\n\nMuchas de las historias que aún se cuentan en Chinatown, North Beach o Nob Hill nacieron aquellos días. El Archivo conserva testimonios de la época.',
 'HISTORIA', NULL, NULL, 1, 0, '2026-04-18 05:12:00', 1),
('vecinos-mission-rondas', 'Vecinos de la Misión organizan rondas nocturnas',
 'Tras el secuestro junto a la Misión Dolores, los vecinos se coordinan para no caminar solos de noche.',
 'Los vecinos del barrio de la Misión han organizado grupos para acompañarse por la noche tras la desaparición de una mujer junto al cementerio de la Misión Dolores.\n\nEl Archivo recuerda que la zona segura más cercana es la propia basílica y la comisaría de Valencia Street.',
 'COMUNIDAD', 'Mission', 19, 1, 1, '2026-10-03 08:00:00', 3),
('investigan-muerte-cable-car', 'Investigan la muerte de un conductor de cable car en Powell Street',
 'La empresa municipal descarta averías en el sistema de cables.',
 'La muerte por electrocución de un conductor de la línea Powell-Mason sigue sin explicación. La empresa insiste en que no había fallos eléctricos.\n\nLa línea funciona con normalidad.',
 'SUCESOS', 'Nob Hill', 5, 1, 0, '2026-09-16 09:00:00', 3),
('avistamientos-ocean-beach', 'Nuevos avistamientos en Ocean Beach',
 'BORRADOR — pendiente de revisión por Sarah.',
 'Borrador interno. No publicar hasta cerrar el contrato CTR-2026-013.',
 'SUCESOS', 'Outer Sunset', 15, 0, 0, '2026-10-03 10:00:00', 3);

-- ---------------------------------------------------------------------
-- ZONAS SEGURAS (pins verdes del mapa público)
-- ---------------------------------------------------------------------
INSERT INTO zonas_seguras (nombre, tipo, direccion, barrio, lat, lng, telefono, horario) VALUES
('Zuckerberg San Francisco General Hospital', 'HOSPITAL', '1001 Potrero Ave',      'Mission',        37.755700, -122.404600, '911', '24 horas'),
('UCSF Medical Center (Parnassus)',           'HOSPITAL', '505 Parnassus Ave',     'Inner Sunset',   37.763100, -122.457600, '911', '24 horas'),
('SFPD Central Station',                      'POLICIA',  '766 Vallejo St',        'North Beach',    37.798700, -122.409800, '911', '24 horas'),
('SFPD Mission Station',                      'POLICIA',  '630 Valencia St',       'Mission',        37.762700, -122.421900, '911', '24 horas'),
('SFPD Tenderloin Station',                   'POLICIA',  '301 Eddy St',           'Tenderloin',     37.783800, -122.412900, '911', '24 horas'),
('Grace Cathedral',                           'TEMPLO',   '1100 California St',   'Nob Hill',       37.791900, -122.413400, NULL,  '8:00 - 18:00'),
('Basílica de la Misión Dolores',             'TEMPLO',   '3321 16th St',          'Mission',        37.764400, -122.426900, NULL,  '9:00 - 16:30'),
('SFFD Station 1',                            'BOMBEROS', '935 Folsom St',         'SoMa',           37.779000, -122.404200, '911', '24 horas'),
('Glide Memorial (refugio)',                  'REFUGIO',  '330 Ellis St',          'Tenderloin',     37.785300, -122.411500, NULL,  '24 horas'),
('Highland Hospital',                         'HOSPITAL', '1411 E 31st St',        'Oakland',        37.799000, -122.231000, '911', '24 horas');

-- ---------------------------------------------------------------------
-- REGISTRO DE ACTIVIDAD (muestra)
-- ---------------------------------------------------------------------
INSERT INTO registro_actividad (usuario_id, accion, entidad, entidad_id, detalle, fecha) VALUES
(1, 'ASIGNAR',  'CONTRATO',  7,  'CTR-2026-007 → Sombra (Unidad Chinatown)',     '2026-09-24 10:00:00'),
(3, 'CONVERTIR','SOLICITUD', 1,  'SF-7K2Q9M → incidente SFA-2026-019',           '2026-10-02 23:40:00'),
(3, 'PUBLICAR', 'NOTICIA',   7,  'Vecinos de la Misión organizan rondas',        '2026-10-03 08:00:00'),
(13,'REPORTAR', 'INCIDENTE', 20, 'Informe de campo bajo Coit Tower',             '2026-10-03 06:40:00');
