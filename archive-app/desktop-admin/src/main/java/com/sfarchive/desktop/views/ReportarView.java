package com.sfarchive.desktop.views;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.EstadoIncidente;
import com.sfarchive.core.model.Incidente;
import com.sfarchive.core.model.OrigenIncidente;
import com.sfarchive.core.model.TipoIncidente;
import com.sfarchive.desktop.Sesion;
import com.sfarchive.desktop.ui.Formulario;
import com.sfarchive.desktop.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.Arrays;

/** El potencial reporta un incidente detectado en campo (queda NO VERIFICADO y sin publicar). */
public class ReportarView implements Vista {

    private final IncidenteDao dao = new IncidenteDao();
    private final ActividadDao actividad = new ActividadDao();
    private VBox raiz;

    private final TextField titulo = new TextField();
    private final ComboBox<TipoIncidente> tipo = Ui.combo(Arrays.asList(TipoIncidente.values()));
    private final Spinner<Integer> amenaza = new Spinner<>(1, 5, 2);
    private final TextField barrio = new TextField();
    private final TextField direccion = new TextField();
    private final TextField lat = new TextField();
    private final TextField lng = new TextField();
    private final TextArea publico = Formulario.area(4);
    private final TextArea anomalia = Formulario.area(4);

    @Override
    public Node vista() {
        if (raiz != null) return raiz;
        tipo.setValue(TipoIncidente.DESAPARICION);
        VBox panel = new VBox(10, new Formulario().campo("Titular *", titulo).campo("Tipo", tipo).campo("Amenaza (1-5)", amenaza)
                .campo("Barrio", barrio).campo("Dirección", direccion).campo("Lat / Lng", Ui.fila(lat, lng))
                .campo("Qué ha pasado *", publico).campo("Indicios anómalos", anomalia),
                Ui.fila(Ui.primario("Enviar reporte al Archivo", this::enviar)));
        panel.getStyleClass().add("panel");
        panel.setMaxWidth(820);
        raiz = Ui.pantalla("Reportar incidente",
                "Lo que reportes llega a los administradores como NO VERIFICADO. Ellos deciden si se publica en la web y si genera un contrato.",
                panel);
        return raiz;
    }

    private void enviar() {
        if (Formulario.texto(titulo) == null || Formulario.texto(publico) == null)
            throw new DataException("Titular y descripción son obligatorios.");
        Double la = Formulario.decimal(lat), ln = Formulario.decimal(lng);
        int id = dao.crear(new Incidente(0, null, Formulario.texto(titulo), tipo.getValue(), Formulario.texto(publico),
                Formulario.texto(barrio), Formulario.texto(direccion), "San Francisco", "Estados Unidos", la, ln,
                LocalDateTime.now().withNano(0), EstadoIncidente.NO_VERIFICADO, false,
                "Informe de campo de " + Sesion.usuario().username() + ": " + Ui.nvl(Formulario.texto(anomalia)),
                amenaza.getValue(), OrigenIncidente.POTENCIAL, Sesion.id()));
        actividad.registrar(Sesion.id(), "REPORTAR", "INCIDENTE", id, Formulario.texto(titulo));
        for (TextField t : java.util.List.of(titulo, barrio, direccion, lat, lng)) t.clear();
        publico.clear();
        anomalia.clear();
        Ui.info("Reporte enviado. Gracias: el Archivo lo revisará en breve.");
    }
}
