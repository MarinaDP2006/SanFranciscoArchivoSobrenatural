package com.sfarchive.desktop.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Mapa oscuro de Leaflet dentro de un WebView de JavaFX.
 * Leaflet va incluido en el jar; solo las teselas del mapa base requieren conexión.
 */
public class MapaFx extends StackPane {

    private final WebView web = new WebView();

    /** Punto que se dibuja en el mapa. */
    public record Pin(double lat, double lng, String color, String titulo, String detalle, int radio) { }

    private static String leafletJs;
    private static String leafletCss;

    public MapaFx() {
        web.setContextMenuEnabled(false);
        getChildren().add(web);
        setMinHeight(260);
    }

    public void mostrar(List<Pin> pins, double latCentro, double lngCentro, int zoom) {
        StringBuilder datos = new StringBuilder("[");
        for (Pin p : pins) {
            if (datos.length() > 1) datos.append(',');
            datos.append(String.format(Locale.ROOT, "{\"lat\":%.6f,\"lng\":%.6f,\"c\":%s,\"t\":%s,\"d\":%s,\"r\":%d}",
                    p.lat(), p.lng(), json(p.color()), json(p.titulo()), json(p.detalle()), p.radio()));
        }
        datos.append(']');
        String html = """
                <!doctype html><html><head><meta charset="utf-8">
                <style>%s
                html,body,#m{margin:0;height:100%%;background:#050607;font-family:monospace}
                .leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#141b1e;color:#d9e4df}
                .leaflet-control-attribution{background:rgba(0,0,0,.6)!important;color:#5d6d68!important}
                </style><script>%s</script></head><body><div id="m"></div><script>
                var m=L.map('m',{zoomControl:true}).setView([%s,%s],%d);
                L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
                  {subdomains:'abcd',maxZoom:19,attribution:'&copy; OpenStreetMap &copy; CARTO'}).addTo(m);
                var pins=%s;
                pins.forEach(function(p){
                  var esc=function(s){var d=document.createElement('div');d.textContent=s;return d.innerHTML;};
                  L.circleMarker([p.lat,p.lng],{radius:p.r,color:p.c,fillColor:p.c,fillOpacity:.8,weight:1.5})
                   .bindPopup('<b style="color:'+p.c+'">'+esc(p.t)+'</b><br>'+esc(p.d)).addTo(m);
                });
                </script></body></html>""".formatted(leafletCss(), leafletJs(),
                String.format(Locale.ROOT, "%.5f", latCentro), String.format(Locale.ROOT, "%.5f", lngCentro), zoom, datos);
        web.getEngine().loadContent(html);
    }

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

    private static synchronized String leafletJs() {
        if (leafletJs == null) leafletJs = recurso("leaflet/leaflet.js");
        return leafletJs;
    }

    private static synchronized String leafletCss() {
        if (leafletCss == null) leafletCss = recurso("leaflet/leaflet.css");
        return leafletCss;
    }

    private static String recurso(String ruta) {
        try (InputStream in = MapaFx.class.getResourceAsStream("/com/sfarchive/desktop/" + ruta)) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    public static String colorTipo(String tipo) {
        return switch (tipo) {
            case "SECUESTRO" -> "#f5a524";
            case "ASESINATO" -> "#ff4d4d";
            default -> "#4fd1c5";
        };
    }
}
