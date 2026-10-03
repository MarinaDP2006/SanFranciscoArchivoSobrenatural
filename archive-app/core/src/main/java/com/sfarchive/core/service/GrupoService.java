package com.sfarchive.core.service;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.GrupoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.GrupoTactico;
import com.sfarchive.core.model.Potencial;

public class GrupoService {

    private final GrupoDao grupos = new GrupoDao();
    private final PotencialDao potenciales = new PotencialDao();
    private final ActividadDao actividad = new ActividadDao();

    /** Mueve un potencial a un grupo (o lo deja sin grupo si grupoId es null). Máximo 6 por grupo. */
    public void mover(int potencialId, Integer grupoId, int adminId) {
        Potencial p = potenciales.porId(potencialId).orElseThrow(() -> new DataException("Potencial no encontrado."));
        if (grupoId != null) {
            if (grupoId.equals(p.grupoId())) return;
            GrupoTactico g = grupos.porId(grupoId).orElseThrow(() -> new DataException("Grupo no encontrado."));
            if (g.miembros() >= GrupoDao.MAX_MIEMBROS)
                throw new DataException(g.nombre() + " ya tiene " + GrupoDao.MAX_MIEMBROS + " potenciales (máximo).");
        }
        potenciales.cambiarGrupo(potencialId, grupoId);
        actividad.registrar(adminId, "MOVER", "POTENCIAL", potencialId,
                p.alias() + " → " + (grupoId == null ? "sin grupo" : "grupo " + grupoId));
    }

    public int guardar(GrupoTactico g, int adminId) {
        if (g.nombre() == null || g.nombre().isBlank()) throw new DataException("El grupo necesita un nombre.");
        if (g.pais() == null || g.pais().isBlank()) throw new DataException("Indica el país del grupo.");
        int id = grupos.guardar(g);
        actividad.registrar(adminId, g.id() == 0 ? "CREAR" : "EDITAR", "GRUPO", id, g.nombre());
        return id;
    }
}
