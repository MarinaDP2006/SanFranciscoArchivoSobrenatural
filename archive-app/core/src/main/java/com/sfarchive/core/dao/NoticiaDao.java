package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.CategoriaNoticia;
import com.sfarchive.core.model.Noticia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Acceso a la tabla {@code noticias} (lo que se publica en la web).
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class NoticiaDao {

    /** Consulta base con JOIN para traer el nombre del autor. */
    private static final String SELECT = """
            SELECT n.*, u.nombre_completo AS autor_nombre
            FROM noticias n LEFT JOIN usuarios u ON u.id = n.autor_id
            """;

    /**
     * Convierte la fila actual del ResultSet en un objeto Noticia.
     * Se usa como "mapper": {@code Jdbc.query(sql, NoticiaDao::map)}.
     */
    static Noticia map(ResultSet rs) throws SQLException {
        return new Noticia(rs.getInt("id"), rs.getString("slug"), rs.getString("titulo"), rs.getString("resumen"),
                rs.getString("contenido"), CategoriaNoticia.valueOf(rs.getString("categoria")),
                rs.getString("imagen_url"), rs.getString("barrio"), Jdbc.intOrNull(rs, "incidente_id"),
                rs.getBoolean("publicada"), rs.getBoolean("destacada"), Jdbc.dateTime(rs, "fecha_publicacion"),
                Jdbc.intOrNull(rs, "autor_id"), rs.getString("autor_nombre"));
    }

    /** Todas las noticias (publicadas y borradores), de la más nueva a la más antigua. */
    public List<Noticia> listar() {
        return Jdbc.query(SELECT + " ORDER BY n.fecha_publicacion DESC", NoticiaDao::map);
    }

    /** Crea (id 0) o actualiza una noticia. Al crearla genera su slug a partir del título. */
    public int guardar(Noticia n) {
        if (n.id() == 0) {
            String slug = slugUnico(slugify(n.titulo()));
            return Jdbc.insert("""
                    INSERT INTO noticias (slug, titulo, resumen, contenido, categoria, imagen_url, barrio, incidente_id,
                                          publicada, destacada, fecha_publicacion, autor_id)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""",
                    slug, n.titulo(), n.resumen(), n.contenido(), n.categoria(), n.imagenUrl(), n.barrio(),
                    n.incidenteId(), n.publicada(), n.destacada(), n.fechaPublicacion(), n.autorId());
        }
        Jdbc.update("""
                UPDATE noticias SET titulo = ?, resumen = ?, contenido = ?, categoria = ?, imagen_url = ?, barrio = ?,
                       incidente_id = ?, publicada = ?, destacada = ?, fecha_publicacion = ?
                WHERE id = ?""",
                n.titulo(), n.resumen(), n.contenido(), n.categoria(), n.imagenUrl(), n.barrio(), n.incidenteId(),
                n.publicada(), n.destacada(), n.fechaPublicacion(), n.id());
        return n.id();
    }

    /** Publica o retira una noticia de la web. */
    public void setPublicada(int id, boolean publicada) {
        Jdbc.update("UPDATE noticias SET publicada = ? WHERE id = ?", publicada, id);
    }

    /** Borra una noticia. */
    public void borrar(int id) {
        Jdbc.update("DELETE FROM noticias WHERE id = ?", id);
    }

    /** Si el slug ya existe, le añade -2, -3... hasta que sea único. */
    private String slugUnico(String base) {
        String slug = base;
        int i = 2;
        while (Jdbc.count("SELECT COUNT(*) FROM noticias WHERE slug = ?", slug) > 0) slug = base + "-" + i++;
        return slug;
    }

    /**
     * Convierte un título en un texto apto para URL.
     * Ejemplo: "¡Niebla récord en el Golden Gate!" → "niebla-record-en-el-golden-gate".
     */
    public static String slugify(String texto) {
        String s = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (s.length() > 80) s = s.substring(0, 80).replaceAll("-$", "");
        return s.isEmpty() ? "noticia" : s;
    }
}
