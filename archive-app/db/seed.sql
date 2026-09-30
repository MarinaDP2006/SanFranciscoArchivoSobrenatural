USE sanfrancisco_archive;

INSERT INTO ADMIN (nombre, username, password_hash, edad_real, edad_sobrenatural, ciudad_residencia) VALUES
('James', 'james.admin', 'HASH_AQUI', 62, 190, 'San Francisco'),
('Sarah', 'sarah.admin', 'HASH_AQUI', 53, 192, 'San Francisco'),
('Marin', 'marin', 'HASH_AQUI', 20, NULL, 'San Francisco');

INSERT INTO USUARIO_REPORTER (username, password_hash, email) VALUES
('reporter1', 'HASH_AQUI', 'reporter1@archive.local'),
('reporter2', 'HASH_AQUI', 'reporter2@archive.local');
