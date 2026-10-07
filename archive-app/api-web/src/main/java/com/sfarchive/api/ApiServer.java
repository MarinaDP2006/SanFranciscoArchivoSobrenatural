package com.sfarchive.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.ContratoDao;
import com.sfarchive.core.dao.GrupoDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.dao.InformeDao;
import com.sfarchive.core.dao.MonederoDao;
import com.sfarchive.core.dao.NoticiaDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.dao.SolicitudDao;
import com.sfarchive.core.dao.UsuarioDao;
import com.sfarchive.core.dao.ZonaSeguraDao;
import com.sfarchive.core.db.Config;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Database;
import com.sfarchive.core.model.CategoriaNoticia;
import com.sfarchive.core.model.Clasificacion;
import com.sfarchive.core.model.EstadoContrato;
import com.sfarchive.core.model.EstadoIncidente;
import com.sfarchive.core.model.EstadoPotencial;
import com.sfarchive.core.model.GrupoTactico;
import com.sfarchive.core.model.Incidente;
import com.sfarchive.core.model.Informe;
import com.sfarchive.core.model.Noticia;
import com.sfarchive.core.model.OrigenIncidente;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.core.model.Prioridad;
import com.sfarchive.core.model.Rol;
import com.sfarchive.core.model.TipoIncidente;
import com.sfarchive.core.model.TipoTransaccion;
import com.sfarchive.core.model.TipoZona;
import com.sfarchive.core.model.ZonaSegura;
import com.sfarchive.core.service.AuthService;
import com.sfarchive.core.service.ContratoService;
import com.sfarchive.core.service.GrupoService;
import com.sfarchive.core.service.MonederoService;
import com.sfarchive.core.service.PotencialService;
import com.sfarchive.core.service.SolicitudService;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;

/**
 * API pública del San Francisco Archive.
 * <p>
 * La web es informativa y la gestión principal se hace desde la misma web con login de administración,
 * sin depender de la app de escritorio JavaFX. Los cambios publicados desde la gestión web aparecen en
 * la web pública con el siguiente refresco (30 s).
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
    private static final Map<String, Map<String, Object>> ADMIN_SESIONES = new ConcurrentHashMap<>();
    private static final String ADMIN_SESSION_COOKIE = "sfa_admin_session";

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
            System.setProperty("sfa.db.user", Config.get("api.db.user", "sfa_admin"));
            System.setProperty("sfa.db.password", Config.get("api.db.password", "sfa_admin_2026"));
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
        SolicitudService gestionSolicitudes = new SolicitudService();
        IncidenteDao incidentes = new IncidenteDao();
        ContratoDao contratos = new ContratoDao();
        GrupoDao grupos = new GrupoDao();
        PotencialDao potenciales = new PotencialDao();
        UsuarioDao usuarios = new UsuarioDao();
        ActividadDao actividad = new ActividadDao();
        MonederoDao monedero = new MonederoDao();
        InformeDao informes = new InformeDao();
        NoticiaDao noticiasAdmin = new NoticiaDao();
        ZonaSeguraDao zonasAdmin = new ZonaSeguraDao();
        ContratoService gestionContratos = new ContratoService();
        GrupoService gestionGrupos = new GrupoService();
        PotencialService gestionPotenciales = new PotencialService();
        MonederoService gestionMonedero = new MonederoService();
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
            } else {
                config.staticFiles.add(s -> {
                    s.directory = "/public";
                    s.location = Location.CLASSPATH;
                    s.hostedPath = "/";
                });
            }
        });

        // Antes de cada respuesta de la API: que el navegador no la guarde en caché (siempre datos frescos)
        app.before("/api/*", ctx -> ctx.header("Cache-Control", "no-store"));

        // ---------- Login web de administración ----------
        app.post("/api/admin/login", ctx -> {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            if (request == null || request.username() == null || request.password() == null) {
                throw new DataException("Usuario y contraseña son obligatorios.");
            }
            var auth = new AuthService();
            var usuario = auth.login(request.username(), request.password());
            if (usuario.rol() != Rol.ADMIN) {
                throw new DataException("Este acceso es exclusivo para administradores.");
            }
            String token = UUID.randomUUID().toString();
            Map<String, Object> info = Map.of(
                    "id", usuario.id(),
                    "username", usuario.username(),
                    "email", usuario.email(),
                    "nombre", usuario.nombreCompleto(),
                    "rol", usuario.rol().name(),
                    "activo", usuario.activo());
            ADMIN_SESIONES.put(token, info);
            ctx.header("Set-Cookie", ADMIN_SESSION_COOKIE + "=" + token + "; Path=/; Max-Age=604800; HttpOnly; SameSite=Lax");
            ctx.json(Map.of("ok", true, "usuario", info));
        });

        app.get("/api/admin/session", ctx -> {
            String token = leerCookie(ctx, ADMIN_SESSION_COOKIE);
            if (token == null || !ADMIN_SESIONES.containsKey(token)) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "No autenticado."));
                return;
            }
            ctx.json(Map.of("ok", true, "usuario", ADMIN_SESIONES.get(token)));
        });

        app.post("/api/admin/logout", ctx -> {
            String token = leerCookie(ctx, ADMIN_SESSION_COOKIE);
            if (token != null) ADMIN_SESIONES.remove(token);
            ctx.header("Set-Cookie", ADMIN_SESSION_COOKIE + "=; Path=/; Max-Age=0; SameSite=Lax");
            ctx.json(Map.of("ok", true));
        });

        app.get("/api/admin/solicitudes", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(solicitudes.listar());
        });
        app.post("/api/admin/solicitudes/{id}/revision", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            gestionSolicitudes.marcarEnRevision(idParam(ctx), adminId);
            ctx.json(Map.of("ok", true));
        });
        app.post("/api/admin/solicitudes/{id}/descartar", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            RespuestaSolicitudRequest request = ctx.bodyAsClass(RespuestaSolicitudRequest.class);
            gestionSolicitudes.descartar(idParam(ctx), request.respuesta(), adminId);
            ctx.json(Map.of("ok", true));
        });
        app.post("/api/admin/solicitudes/{id}/convertir", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            ConvertirSolicitudRequest request = ctx.bodyAsClass(ConvertirSolicitudRequest.class);
            if (request == null || request.recompensa() == null || request.recompensa().signum() < 0) {
                throw new DataException("Indica una recompensa válida, igual o superior a cero.");
            }
            Prioridad prioridad;
            try {
                prioridad = Prioridad.valueOf(request.prioridad());
            } catch (RuntimeException e) {
                throw new DataException("Prioridad no válida.");
            }
            int contratoId = gestionSolicitudes.convertir(idParam(ctx), request.titulo(), request.publicar(),
                    prioridad, request.recompensa(), adminId);
            ctx.json(Map.of("ok", true, "contratoId", contratoId));
        });
        app.get("/api/admin/incidentes", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(incidentes.listar());
        });
        app.post("/api/admin/incidentes", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            IncidenteRequest request = ctx.bodyAsClass(IncidenteRequest.class);
            if (request.titulo() == null || request.titulo().isBlank()
                    || request.descripcionPublica() == null || request.descripcionPublica().isBlank()
                    || request.fecha() == null || request.nivelAmenaza() < 1 || request.nivelAmenaza() > 5) {
                throw new DataException("Completa el titular, descripción, fecha y nivel de amenaza (1-5).");
            }
            TipoIncidente tipo;
            EstadoIncidente estado;
            try {
                tipo = TipoIncidente.valueOf(request.tipo());
                estado = EstadoIncidente.valueOf(request.estado());
            } catch (RuntimeException e) {
                throw new DataException("Tipo o estado de incidente no válido.");
            }
            Incidente incidente = new Incidente(request.id() == null ? 0 : request.id(), null,
                    request.titulo().trim(), tipo, request.descripcionPublica().trim(), limpiar(request.barrio(), 80),
                    limpiar(request.direccion(), 160), limpiar(request.ciudad(), 100), limpiar(request.pais(), 100),
                    request.lat(), request.lng(), request.fecha(), estado, request.publicado(),
                    limpiar(request.anomalia(), 2000), request.nivelAmenaza(),
                    request.id() == null ? OrigenIncidente.ARCHIVO : incidentes.porId(request.id())
                            .orElseThrow(() -> new DataException("Incidente no encontrado.")).origen(),
                    request.id() == null ? adminId : incidentes.porId(request.id()).orElseThrow().creadoPor());
            if (request.id() == null) {
                ctx.status(HttpStatus.CREATED).json(Map.of("id", incidentes.crear(incidente)));
            } else {
                incidentes.actualizar(incidente);
                ctx.json(Map.of("id", incidente.id()));
            }
        });
        app.post("/api/admin/incidentes/{id}/contrato", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            CrearContratoRequest request = ctx.bodyAsClass(CrearContratoRequest.class);
            if (request == null || request.recompensa() == null || request.recompensa().signum() < 0)
                throw new DataException("Indica una recompensa válida, igual o superior a cero.");
            Prioridad prioridad;
            try {
                prioridad = Prioridad.valueOf(request.prioridad());
            } catch (RuntimeException e) {
                throw new DataException("Prioridad no válida.");
            }
            int contratoId = gestionContratos.crear(idParam(ctx), prioridad, request.recompensa(), adminId);
            ctx.status(HttpStatus.CREATED).json(Map.of("id", contratoId));
        });
        app.get("/api/admin/contratos", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(contratos.listar());
        });
        app.get("/api/admin/grupos", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(grupos.listar());
        });
        app.get("/api/admin/potenciales-disponibles", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(potenciales.listar().stream().filter(p -> p.estado().name().equals("DISPONIBLE")).toList());
        });
        app.post("/api/admin/contratos/{id}/asignar", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            AsignarContratoRequest request = ctx.bodyAsClass(AsignarContratoRequest.class);
            if (request.grupoId() == null || request.potencialId() == null)
                throw new DataException("Selecciona un grupo y un potencial.");
            var transporte = gestionContratos.asignar(idParam(ctx), request.grupoId(), request.potencialId(), adminId);
            ctx.json(Map.of("ok", true, "distanciaKm", transporte.distanciaKm(), "coste", transporte.coste()));
        });
        app.post("/api/admin/contratos/{id}/completar", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            gestionContratos.completar(idParam(ctx), adminId);
            ctx.json(Map.of("ok", true));
        });
        app.post("/api/admin/contratos/{id}/fallido", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            RespuestaSolicitudRequest request = ctx.bodyAsClass(RespuestaSolicitudRequest.class);
            gestionContratos.marcarFallido(idParam(ctx), adminId, request.respuesta());
            ctx.json(Map.of("ok", true));
        });
        app.post("/api/admin/contratos/{id}/cancelar", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            gestionContratos.cancelar(idParam(ctx), adminId);
            ctx.json(Map.of("ok", true));
        });
        app.get("/api/admin/potenciales", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(potenciales.listar());
        });
        app.post("/api/admin/potenciales", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            PotencialRequest request = ctx.bodyAsClass(PotencialRequest.class);
            EstadoPotencial estado;
            try {
                estado = EstadoPotencial.valueOf(request.estado());
            } catch (RuntimeException e) {
                throw new DataException("Estado de potencial no válido.");
            }
            Potencial potencial = new Potencial(request.id() == null ? 0 : request.id(), null, null,
                    request.alias(), request.nombreReal(), request.edad(), request.habilidad(), request.descripcion(),
                    request.nivel(), estado, request.barrio(), request.ciudad(), request.pais(), request.lat(),
                    request.lng(), request.grupoId(), null, null,
                    request.fechaReclutamiento() == null ? java.time.LocalDate.now() : request.fechaReclutamiento());
            if (request.id() == null) {
                if (request.username() == null || request.email() == null || request.password() == null)
                    throw new DataException("Usuario, email y contraseña inicial son obligatorios al reclutar.");
                int id = gestionPotenciales.reclutar(potencial, request.username(), request.email(), request.password(),
                        request.fondoInicial(), adminId);
                ctx.status(HttpStatus.CREATED).json(Map.of("id", id));
            } else {
                Potencial actual = potenciales.porId(request.id())
                        .orElseThrow(() -> new DataException("Potencial no encontrado."));
                potencial = new Potencial(potencial.id(), actual.usuarioId(), actual.username(), potencial.alias(),
                        potencial.nombreReal(), potencial.edad(), potencial.habilidad(), potencial.descripcion(),
                        potencial.nivel(), potencial.estado(), potencial.barrio(), potencial.ciudad(), potencial.pais(),
                        potencial.lat(), potencial.lng(), potencial.grupoId(), actual.grupoNombre(), actual.saldo(),
                        potencial.fechaReclutamiento());
                gestionPotenciales.actualizar(potencial, adminId);
                ctx.json(Map.of("id", potencial.id()));
            }
        });
        app.post("/api/admin/potenciales/{id}/grupo", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            MoverPotencialRequest request = ctx.bodyAsClass(MoverPotencialRequest.class);
            gestionGrupos.mover(idParam(ctx), request.grupoId(), adminId);
            ctx.json(Map.of("ok", true));
        });
        app.post("/api/admin/grupos", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            GrupoRequest request = ctx.bodyAsClass(GrupoRequest.class);
            GrupoTactico grupo = new GrupoTactico(request.id() == null ? 0 : request.id(), request.nombre(),
                    request.pais(), request.ciudad(), request.zona(), request.lat(), request.lng(),
                    request.descripcion(), request.activo(), 0);
            int id = gestionGrupos.guardar(grupo, adminId);
            ctx.json(Map.of("id", id));
        });
        app.get("/api/admin/noticias", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(noticiasAdmin.listar());
        });
        app.post("/api/admin/noticias", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            NoticiaRequest request = ctx.bodyAsClass(NoticiaRequest.class);
            CategoriaNoticia categoria;
            try {
                categoria = CategoriaNoticia.valueOf(request.categoria());
            } catch (RuntimeException e) {
                throw new DataException("Categoría de noticia no válida.");
            }
            if (request.titulo() == null || request.titulo().isBlank()
                    || request.resumen() == null || request.resumen().isBlank())
                throw new DataException("El titular y el resumen son obligatorios.");
            Noticia noticia = new Noticia(request.id() == null ? 0 : request.id(), null, request.titulo().trim(),
                    request.resumen().trim(), request.contenido(), categoria, request.imagenUrl(), request.barrio(),
                    request.incidenteId(), request.publicada(), request.destacada(), request.fechaPublicacion(), adminId, null);
            ctx.json(Map.of("id", noticiasAdmin.guardar(noticia)));
        });
        app.get("/api/admin/zonas-seguras", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(zonasAdmin.listar());
        });
        app.post("/api/admin/zonas-seguras", ctx -> {
            if (adminId(ctx) == null) return;
            ZonaRequest request = ctx.bodyAsClass(ZonaRequest.class);
            TipoZona tipo;
            try {
                tipo = TipoZona.valueOf(request.tipo());
            } catch (RuntimeException e) {
                throw new DataException("Tipo de zona no válido.");
            }
            if (request.nombre() == null || request.nombre().isBlank() || request.lat() < -90 || request.lat() > 90
                    || request.lng() < -180 || request.lng() > 180)
                throw new DataException("Nombre o coordenadas de la zona no válidos.");
            int id = zonasAdmin.guardar(new ZonaSegura(request.id() == null ? 0 : request.id(), request.nombre().trim(),
                    tipo, request.direccion(), request.barrio(), request.lat(), request.lng(), request.telefono(),
                    request.horario(), request.activa()));
            ctx.json(Map.of("id", id));
        });
        app.get("/api/admin/monederos", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(Map.of("potenciales", potenciales.listar(), "total", monedero.totalEnCirculacion(),
                    "movimientos", monedero.recientes(200)));
        });
        app.post("/api/admin/monederos/{id}/ajustes", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            AjusteMonederoRequest request = ctx.bodyAsClass(AjusteMonederoRequest.class);
            TipoTransaccion tipo;
            try {
                tipo = TipoTransaccion.valueOf(request.tipo());
            } catch (RuntimeException e) {
                throw new DataException("Tipo de movimiento no válido.");
            }
            var saldo = gestionMonedero.ajustar(idParam(ctx), tipo, request.importe(), request.concepto(), adminId);
            ctx.json(Map.of("ok", true, "saldo", saldo));
        });
        app.get("/api/admin/usuarios", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(usuarios.listar());
        });
        app.post("/api/admin/usuarios/{id}", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            int userId = idParam(ctx);
            UsuarioRequest request = ctx.bodyAsClass(UsuarioRequest.class);
            if (userId == adminId && !request.activo())
                throw new DataException("No puedes desactivar tu propia cuenta desde esta sesión.");
            if (usuarios.porId(userId).isEmpty()) throw new DataException("Usuario no encontrado.");
            usuarios.actualizar(userId, request.email(), request.nombre(), request.activo());
            actividad.registrar(adminId, "EDITAR", "USUARIO", userId, "Cuenta actualizada desde gestión web");
            ctx.json(Map.of("ok", true));
        });
        app.post("/api/admin/usuarios/{id}/password", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            PasswordResetRequest request = ctx.bodyAsClass(PasswordResetRequest.class);
            new AuthService().restablecerPassword(adminId, idParam(ctx), request.password());
            ctx.json(Map.of("ok", true));
        });
        app.get("/api/admin/actividad", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(actividad.recientes(200));
        });
        app.get("/api/admin/informes", ctx -> {
            if (adminId(ctx) == null) return;
            ctx.json(informes.listar());
        });
        app.post("/api/admin/informes", ctx -> {
            Integer adminId = adminId(ctx);
            if (adminId == null) return;
            InformeRequest request = ctx.bodyAsClass(InformeRequest.class);
            var contrato = contratos.porId(request.contratoId())
                    .orElseThrow(() -> new DataException("Contrato no encontrado."));
            if (contrato.estado() != EstadoContrato.COMPLETADO && contrato.estado() != EstadoContrato.FALLIDO)
                throw new DataException("El informe solo se puede guardar en un contrato cerrado.");
            Clasificacion clasificacion;
            try {
                clasificacion = Clasificacion.valueOf(request.clasificacion());
            } catch (RuntimeException e) {
                throw new DataException("Clasificación no válida.");
            }
            if (request.titulo() == null || request.titulo().isBlank() || request.resumen() == null
                    || request.resumen().isBlank() || request.bajasCiviles() < 0)
                throw new DataException("Completa titular, resumen y un número válido de bajas.");
            informes.guardar(new Informe(0, request.contratoId(), contrato.codigo(), adminId, null,
                    request.titulo().trim(), request.entidad(), clasificacion, request.resumen().trim(),
                    request.contenido(), request.bajasCiviles(), null, null));
            ctx.json(Map.of("ok", true));
        });

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

    public record LoginRequest(String username, String password) { }

    public record RespuestaSolicitudRequest(String respuesta) { }

    public record ConvertirSolicitudRequest(String titulo, boolean publicar, String prioridad,
                                            java.math.BigDecimal recompensa) { }

    public record IncidenteRequest(Integer id, String titulo, String tipo, String descripcionPublica,
                                   String barrio, String direccion, String ciudad, String pais,
                                   Double lat, Double lng, LocalDateTime fecha, String estado,
                                   boolean publicado, String anomalia, int nivelAmenaza) { }

    public record AsignarContratoRequest(Integer grupoId, Integer potencialId) { }

    public record CrearContratoRequest(String prioridad, java.math.BigDecimal recompensa) { }

    public record PotencialRequest(Integer id, String alias, String nombreReal, Integer edad, String habilidad,
                                   String descripcion, int nivel, String estado, String barrio, String ciudad,
                                   String pais, Double lat, Double lng, Integer grupoId,
                                   java.time.LocalDate fechaReclutamiento, String username, String email,
                                   String password, java.math.BigDecimal fondoInicial) { }

    public record GrupoRequest(Integer id, String nombre, String pais, String ciudad, String zona, Double lat,
                               Double lng, String descripcion, boolean activo) { }

    public record MoverPotencialRequest(Integer grupoId) { }

    public record NoticiaRequest(Integer id, String titulo, String resumen, String contenido, String categoria,
                                 String imagenUrl, String barrio, Integer incidenteId, boolean publicada,
                                 boolean destacada, LocalDateTime fechaPublicacion) { }

    public record ZonaRequest(Integer id, String nombre, String tipo, String direccion, String barrio, double lat,
                              double lng, String telefono, String horario, boolean activa) { }

    public record AjusteMonederoRequest(String tipo, java.math.BigDecimal importe, String concepto) { }

    public record UsuarioRequest(String email, String nombre, boolean activo) { }

    public record PasswordResetRequest(String password) { }

    public record InformeRequest(int contratoId, String titulo, String entidad, String clasificacion,
                                String resumen, String contenido, int bajasCiviles) { }

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

    private static String leerCookie(Context ctx, String nombre) {
        String header = ctx.header("Cookie");
        if (header == null || header.isBlank()) return null;
        for (String parte : header.split(";")) {
            String[] trozos = parte.trim().split("=", 2);
            if (trozos.length == 2 && nombre.equals(trozos[0])) return trozos[1];
        }
        return null;
    }

    private static Integer adminId(Context ctx) {
        String token = leerCookie(ctx, ADMIN_SESSION_COOKIE);
        Map<String, Object> usuario = token == null ? null : ADMIN_SESIONES.get(token);
        if (usuario == null || !Rol.ADMIN.name().equals(usuario.get("rol"))) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Inicia sesión como administrador."));
            return null;
        }
        return (Integer) usuario.get("id");
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
