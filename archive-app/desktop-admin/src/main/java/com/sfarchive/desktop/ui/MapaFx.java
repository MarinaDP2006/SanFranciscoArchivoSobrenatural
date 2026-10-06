package com.sfarchive.desktop.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

/**
 * Mapa oscuro de Leaflet dentro de la aplicación JavaFX.
 * <p>
 * JavaFX tiene un mini navegador ({@link WebView}). Le pasamos una página HTML generada aquí
 * con Leaflet (la misma librería de mapas de la web) y los puntos a dibujar.
 * Leaflet va dentro del .jar (resources/.../leaflet); solo las imágenes del mapa (teselas) vienen de internet.
 * <p>
 * Uso: {@code mapa.mostrar(listaDePins, latitud, longitud, zoom)}
 */
public class MapaFx extends StackPane {

    /** El navegador interno donde se carga el mapa. */
    private final WebView web = new WebView();

    /** Un punto del mapa: coordenadas, color, título, texto del globo y tamaño (radio en píxeles). */
    public record Pin(double lat, double lng, String color, String titulo, String detalle, int radio) { }

    /** Código de Leaflet (se lee una vez del .jar y se guarda). */
    private static String leafletJs;
    /** Estilos de Leaflet (se leen una vez del .jar y se guardan). */
    private static String leafletCss;

    /** Crea el contenedor del mapa (sin menú contextual del navegador). */
    public MapaFx() {
        web.setContextMenuEnabled(false);
        getChildren().add(web);
        setMinHeight(260);
    }

    /**
     * Dibuja el mapa con los pins indicados.
     * @param pins      puntos a dibujar
     * @param latCentro latitud del centro inicial
     * @param lngCentro longitud del centro inicial
     * @param zoom      nivel de zoom (11-12 = ciudad entera, 15 = calle)
     */
    public void mostrar(List<Pin> pins, double latCentro, double lngCentro, int zoom) {
        // 1) Convertimos la lista de pins en un array JSON que entienda JavaScript
        StringBuilder datos = new StringBuilder("[");
        for (Pin p : pins) {
            if (datos.length() > 1) datos.append(',');
            datos.append(String.format(Locale.ROOT, "{\"lat\":%.6f,\"lng\":%.6f,\"c\":%s,\"t\":%s,\"d\":%s,\"r\":%d}",
                    p.lat(), p.lng(), json(p.color()), json(p.titulo()), json(p.detalle()), p.radio()));
        }
        datos.append(']');
        // 2) Montamos la página HTML: CSS + JS de Leaflet + el mapa + un bucle que pinta cada pin.
        //    Los %s y %d se sustituyen con .formatted(...) al final
        String html = """
                <!doctype html><html><head><meta charset="utf-8">
                <style>%s
                html,body,#m{margin:0;height:100%%;background:#050607;font-family:monospace}
                .leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#141b1e;color:#d9e4df}
                .leaflet-control-attribution{background:rgba(0,0,0,.6)!important;color:#5d6d68!important}
                </style><script>%s</script></head><body><div id="m"></div><script>
                var m=L.map('m',{zoomControl:true}).setView([%s,%s],%d);
                                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                                    {maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}).addTo(m);
                var pins=%s;
                pins.forEach(function(p){
                  var esc=function(s){var d=document.createElement('div');d.textContent=s;return d.innerHTML;};
                  L.circleMarker([p.lat,p.lng],{radius:p.r,color:p.c,fillColor:p.c,fillOpacity:.8,weight:1.5})
                   .bindPopup('<b style="color:'+p.c+'">'+esc(p.t)+'</b><br>'+esc(p.d)).addTo(m);
                });
                </script></body></html>""".formatted(leafletCss(), leafletJs(),
                String.format(Locale.ROOT, "%.5f", latCentro), String.format(Locale.ROOT, "%.5f", lngCentro), zoom, datos);
        // 3) Cargamos la página en el WebView
        web.getEngine().loadContent(html);
    }

    /** Convierte un texto Java en un texto JSON seguro (escapa comillas, saltos de línea y '<'). */
    private static String json(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> { }
                case '<' -> sb.append("\\u003c");
                default -> sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }

    /** Devuelve el JavaScript de Leaflet (leído una sola vez). */
    private static synchronized String leafletJs() {
        if (leafletJs == null) leafletJs = recurso("leaflet/leaflet.js");
        return leafletJs;
    }

    /** Devuelve el CSS de Leaflet (leído una sola vez). */
    private static synchronized String leafletCss() {
        if (leafletCss == null) leafletCss = recurso("leaflet/leaflet.css");
        return leafletCss;
    }

    /** Lee un archivo de texto de resources (dentro del .jar). */
    private static String recurso(String ruta) {
        try (InputStream in = MapaFx.class.getResourceAsStream("/com/sfarchive/desktop/" + ruta)) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /** Color de cada tipo de incidente (el mismo que en la web). */
    public static String colorTipo(String tipo) {
        return switch (tipo) {
            case "SECUESTRO" -> "#f5a524";
            case "ASESINATO" -> "#ff4d4d";
            default -> "#4fd1c5";
        };
    }
}
