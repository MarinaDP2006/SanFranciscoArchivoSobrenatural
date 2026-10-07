-- Permisos adicionales para el login web en una base ya instalada.
-- No modifica ni elimina datos.
GRANT SELECT ON sf_archive.usuarios TO 'sfa_web'@'%';
GRANT SELECT ON sf_archive.potenciales TO 'sfa_web'@'%';
GRANT UPDATE (ultimo_acceso) ON sf_archive.usuarios TO 'sfa_web'@'%';
GRANT INSERT ON sf_archive.registro_actividad TO 'sfa_web'@'%';
FLUSH PRIVILEGES;
