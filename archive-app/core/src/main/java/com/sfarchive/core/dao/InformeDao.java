package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.Clasificacion;
import com.sfarchive.core.model.Informe;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code informes_clasificados} (Archivo Restringido).
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class InformeDao {

    /** Consulta base con JOIN para traer el código del contrato y el nombre del autor. */
    private static final String SELECT = """
            SELECT f.*, c.codigo AS contrato_codigo, u.nombre_completo AS autor_nombre
            FROM informes_clasificados f
            JOIN contratos c ON c.id = f.contrato_id
            LEFT JOIN usuarios u ON u.id = f.autor_id
            """;

    /**
     * Convierte la fila actual del ResultSet en un objeto Informe.
     * Se usa como "mapper": {@code Jdbc.query(sql, InformeDao::map)}.
     */
    static Informe map(ResultSet rs) throws SQLException {
        return new Informe(rs.getInt("id"), rs.getInt("contrato_id"), rs.getString("contrato_codigo"),
                Jdbc.intOrNull(rs, "autor_id"), rs.getString("autor_nombre"), rs.getString("titulo"),
                rs.getString("entidad_anomala"), Clasificacion.valueOf(rs.getString("clasificacion")),
                rs.getString("resumen"), rs.getString("contenido"), rs.getInt("bajas_civiles"),
                Jdbc.dateTime(rs, "fecha_creacion"), Jdbc.dateTime(rs, "fecha_modificacion"));
    }

    /** Todos los informes, del modificado más recientemente al más antiguo. */
    public List<Informe> listar() {
        return Jdbc.query(SELECT + " ORDER BY f.fecha_modificacion DESC", InformeDao::map);
    }

    /** El informe de un contrato (cada contrato tiene como máximo uno). */
    public Optional<Informe> porContrato(int contratoId) {
        return Jdbc.one(SELECT + " WHERE f.contrato_id = ?", InformeDao::map, contratoId);
    }

    /**
     * Crea o actualiza el informe de un contrato en una sola sentencia:
     * INSERT ... ON DUPLICATE KEY UPDATE (si ya existe uno para ese contrato, lo actualiza).
     */
    public void guardar(Informe f) {
        Jdbc.update("""
                INSERT INTO informes_clasificados (contrato_id, autor_id, titulo, entidad_anomala, clasificacion, resumen, contenido, bajas_civiles)
                VALUES (?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), entidad_anomala = VALUES(entidad_anomala),
                    clasificacion = VALUES(clasificacion), resumen = VALUES(resumen), contenido = VALUES(contenido),
                    bajas_civiles = VALUES(bajas_civiles), autor_id = VALUES(autor_id)""",
                f.contratoId(), f.autorId(), f.titulo(), f.entidad(), f.clasificacion(), f.resumen(), f.contenido(),
                f.bajasCiviles());
    }

    /** Borra el informe de un contrato. */
    public void borrar(int contratoId) {
        Jdbc.update("DELETE FROM informes_clasificados WHERE contrato_id = ?", contratoId);
    }
}
