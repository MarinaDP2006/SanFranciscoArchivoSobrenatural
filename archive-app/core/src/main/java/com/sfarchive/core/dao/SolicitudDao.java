package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.EstadoSolicitud;
import com.sfarchive.core.model.SolicitudAyuda;
import com.sfarchive.core.model.TipoIncidente;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code solicitudes_ayuda} (avisos anónimos de la web).
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class SolicitudDao {

    /** Letras y números para los códigos. Sin 0/O ni 1/I para que no se confundan al copiarlos. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    /** Generador aleatorio seguro (los códigos no deben poder adivinarse). */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Convierte la fila actual del ResultSet en un objeto SolicitudAyuda.
     * Se usa como "mapper": {@code Jdbc.query(sql, SolicitudDao::map)}.
     */
    static SolicitudAyuda map(ResultSet rs) throws SQLException {
        return new SolicitudAyuda(rs.getInt("id"), rs.getString("codigo_seguimiento"),
                TipoIncidente.valueOf(rs.getString("tipo")), rs.getString("descripcion"), rs.getString("barrio"),
                rs.getString("ubicacion"), Jdbc.doubleOrNull(rs, "lat"), Jdbc.doubleOrNull(rs, "lng"),
                rs.getString("contacto"), EstadoSolicitud.valueOf(rs.getString("estado")),
                Jdbc.intOrNull(rs, "incidente_id"), rs.getString("respuesta_publica"),
                Jdbc.dateTime(rs, "recibida_en"));
    }

    /** Todos los avisos: primero los pendientes, luego en revisión, atendidos y descartados. */
    public List<SolicitudAyuda> listar() {
        return Jdbc.query("""
                SELECT * FROM solicitudes_ayuda
                ORDER BY FIELD(estado,'PENDIENTE','EN_REVISION','ATENDIDA','DESCARTADA'), recibida_en DESC""",
                SolicitudDao::map);
    }

    /** Busca un aviso por id. */
    public Optional<SolicitudAyuda> porId(int id) {
        return Jdbc.one("SELECT * FROM solicitudes_ayuda WHERE id = ?", SolicitudDao::map, id);
    }

    /** Busca un aviso por su código de seguimiento (SF-XXXXXX). */
    public Optional<SolicitudAyuda> porCodigo(String codigo) {
        return Jdbc.one("SELECT * FROM solicitudes_ayuda WHERE codigo_seguimiento = ?", SolicitudDao::map, codigo);
    }

    /**
     * Guarda un aviso anónimo (lo llama la API cuando un ciudadano envía el formulario)
     * y devuelve su código de seguimiento, por ejemplo SF-7K2Q9M.
     */
    public String crear(TipoIncidente tipo, String descripcion, String barrio, String ubicacion,
                        Double lat, Double lng, String contacto) {
        String codigo = nuevoCodigo();
        Jdbc.insert("""
                INSERT INTO solicitudes_ayuda (codigo_seguimiento, tipo, descripcion, barrio, ubicacion, lat, lng, contacto)
                VALUES (?,?,?,?,?,?,?,?)""", codigo, tipo, descripcion, barrio, ubicacion, lat, lng, contacto);
        return codigo;
    }

    /**
     * Cambia el estado del aviso, lo enlaza con un incidente (si se indica) y guarda
     * la respuesta que verá el ciudadano y qué admin lo revisó.
     */
    public void actualizarEstado(Connection c, int id, EstadoSolicitud estado, Integer incidenteId,
                                 String respuesta, int adminId) throws SQLException {
        Jdbc.update(c, """
                UPDATE solicitudes_ayuda SET estado = ?, incidente_id = COALESCE(?, incidente_id),
                       respuesta_publica = ?, revisada_por = ?, revisada_en = ?
                WHERE id = ?""", estado, incidenteId, respuesta, adminId, LocalDateTime.now(), id);
    }

    /** Cuántos avisos hay sin revisar (el número que sale en el menú). */
    public long pendientes() {
        return Jdbc.count("SELECT COUNT(*) FROM solicitudes_ayuda WHERE estado = 'PENDIENTE'");
    }

    /** Genera un código aleatorio "SF-" + 6 caracteres. */
    private static String nuevoCodigo() {
        StringBuilder sb = new StringBuilder("SF-");
        for (int i = 0; i < 6; i++) sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        return sb.toString();
    }
}
