package com.sfarchive.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sfarchive.core.dao.SolicitudDao;
import com.sfarchive.core.db.Config;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Database;
import com.sfarchive.core.model.TipoIncidente;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API pública del San Francisco Archive.
 * <p>
 * La web es SOLO informativa: consulta incidentes, noticias, zonas seguras y estadísticas,
 * y permite a cualquier ciudadano (sin registrarse) enviar una solicitud de ayuda anónima.
 * Toda la gestión se hace desde la aplicación de escritorio, que escribe en la misma base de datos:
 * cualquier cambio publicado desde la app aparece en la web en el siguiente refresco (30 s).
 */
public final class ApiServer {

    private static final int MAX_AVISOS = 5;
    private static final long VENTANA_MS = 10 * 60 * 1000L;
    private static final Map<String, Deque<Long>> AVISOS_POR_IP = new ConcurrentHashMap<>();

    private ApiServer() { }

    public static void main(String[] args) {
        // La API se conecta con el usuario MySQL de permisos mínimos (si no se indica otro).
        if (System.getenv("SFA_DB_USER") == null && System.getProperty("sfa.db.user") == null) {
            System.setProperty("sfa.db.user", Config.get("api.db.user", "sfa_web"));
            System.setProperty("sfa.db.password", Config.get("api.db.password", ""));
        }
        String portEnv = System.getenv("PORT"); // Railway / Render asignan el puerto aquí
        int port = portEnv != null ? Integer.parseInt(portEnv) : Config.getInt("api.port", 8080);
        crear().start(port);
    }

    public static Javalin crear() {
        WebDao dao = new WebDao();
        SolicitudDao solicitudes = new SolicitudDao();
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Path webDir = buscarWeb();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, true));
            config.showJavalinBanner = false;
            config.http.defaultContentType = "application/json; charset=utf-8";
            config.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));
            if (webDir != null) {
                config.staticFiles.add(s -> {
                    s.directory = webDir.toString();
                    s.location = Location.EXTERNAL;
                    s.hostedPath = "/";
                });
            }
        });

        app.before("/api/*", ctx -> ctx.header("Cache-Control", "no-store"));

        // ---------- Salud ----------
        app.get("/api/salud", ctx -> ctx.json(Map.of(
                "servicio", "San Francisco Archive API",
                "estado", "OK",
                "baseDatos", Database.ping() ? "CONECTADA" : "SIN CONEXION",
                "hora", LocalDateTime.now())));

        // ---------- Incidentes (feed + mapa) ----------
        app.get("/api/incidentes", ctx -> ctx.json(dao.incidentes(
                ctx.queryParam("tipo"), ctx.queryParam("barrio"), ctx.queryParam("q"))));
        app.get("/api/incidentes/publicos", ctx -> ctx.json(dao.incidentes(null, null, null))); // compatibilidad con el TFG
        app.get("/api/incidentes/{id}", ctx -> {
            int id = idParam(ctx);
            var inc = dao.incidente(id).orElseThrow(() -> new NotFoundResponse("Incidente no encontrado"));
            ctx.json(Map.of("incidente", inc, "noticias", dao.noticiasDeIncidente(id)));
        });

        // ---------- Noticias ----------
        app.get("/api/noticias", ctx -> {
            int limite = Math.max(1, Math.min(50, ctx.queryParamAsClass("limite", Integer.class).getOrDefault(20)));
            ctx.json(dao.noticias(ctx.queryParam("categoria"), limite));
        });
        app.get("/api/noticias/{slug}", ctx -> ctx.json(dao.noticia(ctx.pathParam("slug"))
                .orElseThrow(() -> new NotFoundResponse("Noticia no encontrada"))));

        // ---------- Zonas seguras y estadísticas ----------
        app.get("/api/zonas-seguras", ctx -> ctx.json(dao.zonasSeguras()));
        app.get("/api/estadisticas", ctx -> ctx.json(dao.estadisticas()));

        // ---------- Pedir ayuda (anónimo, sin login) ----------
        app.post("/api/ayuda", ctx -> {
            AvisoCiudadano a = ctx.bodyAsClass(AvisoCiudadano.class);
            if (a.website() != null && !a.website().isBlank()) { // honeypot anti-bots
                ctx.status(HttpStatus.CREATED).json(Map.of("codigo", "SF-000000"));
                return;
            }
            limitarFrecuencia(ctx.ip());
            TipoIncidente tipo;
            try {
                tipo = TipoIncidente.valueOf(String.valueOf(a.tipo()).toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new DataException("Tipo de incidente no válido.");
            }
            String descripcion = limpiar(a.descripcion(), 2000);
            if (descripcion == null || descripcion.length() < 20)
                throw new DataException("Describe lo ocurrido con al menos 20 caracteres.");
            Double lat = a.lat(), lng = a.lng();
            if (lat != null && (lat < -90 || lat > 90)) lat = null;
            if (lng != null && (lng < -180 || lng > 180)) lng = null;
            if (lat == null || lng == null) { lat = null; lng = null; }
            String codigo = solicitudes.crear(tipo, descripcion, limpiar(a.barrio(), 80), limpiar(a.ubicacion(), 160),
                    lat, lng, limpiar(a.contacto(), 120));
            ctx.status(HttpStatus.CREATED).json(Map.of(
                    "codigo", codigo,
                    "mensaje", "Aviso recibido. Guarda tu código para consultar el estado."));
        });
        app.get("/api/ayuda/{codigo}", ctx -> ctx.json(dao.estadoAyuda(ctx.pathParam("codigo").trim().toUpperCase())
                .orElseThrow(() -> new NotFoundResponse("No existe ningún aviso con ese código"))));

        // ---------- Errores ----------
        app.exception(DataException.class, (e, ctx) -> ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage())));
        app.exception(NotFoundResponse.class, (e, ctx) -> ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage())));
        app.exception(NumberFormatException.class, (e, ctx) -> ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Parámetro no válido")));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", "Error interno del servidor"));
        });
        app.error(404, ctx -> {
            if (ctx.path().startsWith("/api")) {
                if (ctx.result() == null || ctx.result().isBlank()) ctx.json(Map.of("error", "Recurso no encontrado"));
            }
        });
        return app;
    }

    /** Cuerpo JSON del formulario "pedir ayuda". {@code website} es un campo trampa para bots. */
    public record AvisoCiudadano(String tipo, String descripcion, String barrio, String ubicacion,
                                 Double lat, Double lng, String contacto, String website) { }

    /** Localiza la carpeta archive-web/public (se arranque desde archive-app, api-web o la raíz). */
    private static Path buscarWeb() {
        for (String candidato : new String[]{Config.get("web.dir", "../archive-web/public"),
                "../archive-web/public", "../../archive-web/public", "archive-web/public"}) {
            Path p = Path.of(candidato).toAbsolutePath().normalize();
            if (Files.isRegularFile(p.resolve("index.html"))) return p;
        }
        return null;
    }

    private static int idParam(Context ctx) {
        return Integer.parseInt(ctx.pathParam("id"));
    }

    private static String limpiar(String s, int max) {
        if (s == null) return null;
        String t = s.strip().replaceAll("[\\p{Cntrl}&&[^\n]]", "");
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    /** Máximo 5 avisos cada 10 minutos por IP. */
    private static void limitarFrecuencia(String ip) {
        long ahora = System.currentTimeMillis();
        Deque<Long> marcas = AVISOS_POR_IP.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahora - marcas.peekFirst() > VENTANA_MS) marcas.pollFirst();
            if (marcas.size() >= MAX_AVISOS)
                throw new DataException("Has enviado demasiados avisos. Inténtalo de nuevo en unos minutos o llama al 911.");
            marcas.addLast(ahora);
        }
    }
}
