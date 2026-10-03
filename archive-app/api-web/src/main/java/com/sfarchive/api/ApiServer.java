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

    /** Máximo de avisos que puede enviar una misma IP... */
    private static final int MAX_AVISOS = 5;
    /** ...en esta ventana de tiempo (10 minutos, en milisegundos). */
    private static final long VENTANA_MS = 10 * 60 * 1000L;
    /**
     * Para cada IP, las horas de sus últimos avisos (anti-spam). ConcurrentHashMap: seguro con varios hilos.
     */
    private static final Map<String, Deque<Long>> AVISOS_POR_IP = new ConcurrentHashMap<>();

    /** Clase de utilidades: no se crean objetos de ella. */
    private ApiServer() { }

    /**
     * Arranca la API. Por defecto escucha en http://localhost:8080
     * (en Railway/Render el puerto llega en la variable de entorno PORT).
     */
    public static void main(String[] args) {
        // La API se conecta con el usuario MySQL de permisos mínimos (si no se indica otro).
        // La API se conecta con el usuario MySQL de permisos mínimos (solo vistas públicas)
        // salvo que se indique otro con variables de entorno
        if (System.getenv("SFA_DB_USER") == null && System.getProperty("sfa.db.user") == null) {
            System.setProperty("sfa.db.user", Config.get("api.db.user", "sfa_web"));
            System.setProperty("sfa.db.password", Config.get("api.db.password", ""));
        }
        String portEnv = System.getenv("PORT"); // Railway / Render asignan el puerto aquí
        int port = portEnv != null ? Integer.parseInt(portEnv) : Config.getInt("api.port", 8080);
        crear().start(port);
    }

    /**
     * Crea el servidor Javalin con todas sus rutas. Cada ruta es:
     * {@code app.get("/api/ruta", ctx -> ...)}, donde {@code ctx} (Context) tiene la petición
     * (parámetros, cuerpo...) y sirve para responder ({@code ctx.json(objeto)} → JSON).
     */
    public static Javalin crear() {
        WebDao dao = new WebDao();
        SolicitudDao solicitudes = new SolicitudDao();
        // Jackson convierte objetos Java ↔ JSON. JavaTimeModule: para las fechas (LocalDateTime)
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Path webDir = buscarWeb();

        // Configuración del servidor
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, true));
            config.showJavalinBanner = false;
            config.http.defaultContentType = "application/json; charset=utf-8";
            // CORS: permite que la web llame a la API aunque esté en otro puerto o dominio
            config.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));
            if (webDir != null) {
                // Si existe la carpeta de la web, la servimos como archivos estáticos (index.html, css, js...)
                config.staticFiles.add(s -> {
                    s.directory = webDir.toString();
                    s.location = Location.EXTERNAL;
                    s.hostedPath = "/";
                });
            }
        });

        // Antes de cada respuesta de la API: que el navegador no la guarde en caché (siempre datos frescos)
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
            // Si el campo trampa viene relleno es un bot: le decimos que todo fue bien pero no guardamos nada
            if (a.website() != null && !a.website().isBlank()) { // honeypot anti-bots
                ctx.status(HttpStatus.CREATED).json(Map.of("codigo", "SF-000000"));
                return;
            }
            limitarFrecuencia(ctx.ip());
            TipoIncidente tipo;
            try {
                // Convertimos el texto del tipo en enum (si no es válido → error 400)
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
            // Si solo llega una de las dos coordenadas, no guardamos ninguna
            if (lat == null || lng == null) { lat = null; lng = null; }
            // Guardamos el aviso y devolvemos el código de seguimiento (201 = creado)
            String codigo = solicitudes.crear(tipo, descripcion, limpiar(a.barrio(), 80), limpiar(a.ubicacion(), 160),
                    lat, lng, limpiar(a.contacto(), 120));
            ctx.status(HttpStatus.CREATED).json(Map.of(
                    "codigo", codigo,
                    "mensaje", "Aviso recibido. Guarda tu código para consultar el estado."));
        });
        app.get("/api/ayuda/{codigo}", ctx -> ctx.json(dao.estadoAyuda(ctx.pathParam("codigo").trim().toUpperCase())
                .orElseThrow(() -> new NotFoundResponse("No existe ningún aviso con ese código"))));

        // ---------- Errores ----------
        // --- Errores: siempre respondemos JSON {"error": "mensaje"} con el código HTTP adecuado ---
        // 400 = petición incorrecta, 404 = no encontrado, 500 = error del servidor
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

    /**
     * Cuerpo JSON del formulario "pedir ayuda" (Jackson lo convierte en este record).
     * {@code website} es un campo trampa invisible para personas: si viene relleno, es un bot.
     */
    public record AvisoCiudadano(String tipo, String descripcion, String barrio, String ubicacion,
                                 Double lat, Double lng, String contacto, String website) { }

    /**
     * Localiza la carpeta archive-web/public (se arranque desde archive-app, api-web o la raíz).
     * Si la encuentra, la API sirve también la web en http://localhost:8080.
     */
    private static Path buscarWeb() {
        for (String candidato : new String[]{Config.get("web.dir", "../archive-web/public"),
                "../archive-web/public", "../../archive-web/public", "archive-web/public"}) {
            Path p = Path.of(candidato).toAbsolutePath().normalize();
            if (Files.isRegularFile(p.resolve("index.html"))) return p;
        }
        return null;
    }

    /** Lee el {id} de la URL como número (si no es un número → error 400). */
    private static int idParam(Context ctx) {
        return Integer.parseInt(ctx.pathParam("id"));
    }

    /**
     * Limpia un texto del formulario: quita espacios y caracteres de control y lo corta a {@code max}.
     * Devuelve null si queda vacío.
     */
    private static String limpiar(String s, int max) {
        if (s == null) return null;
        String t = s.strip().replaceAll("[\\p{Cntrl}&&[^\n]]", "");
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    /** Anti-spam: máximo 5 avisos cada 10 minutos por IP. Si se supera, lanza un error. */
    private static void limitarFrecuencia(String ip) {
        long ahora = System.currentTimeMillis();
        Deque<Long> marcas = AVISOS_POR_IP.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (marcas) {
            // Quitamos los avisos que ya tienen más de 10 minutos
            while (!marcas.isEmpty() && ahora - marcas.peekFirst() > VENTANA_MS) marcas.pollFirst();
            if (marcas.size() >= MAX_AVISOS)
                throw new DataException("Has enviado demasiados avisos. Inténtalo de nuevo en unos minutos o llama al 911.");
            marcas.addLast(ahora);
        }
    }
}
