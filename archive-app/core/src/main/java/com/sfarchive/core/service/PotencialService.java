package com.sfarchive.core.service;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.MonederoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.dao.UsuarioDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.core.model.Rol;
import com.sfarchive.core.model.TipoTransaccion;
import com.sfarchive.core.security.PasswordHasher;

import java.math.BigDecimal;

public class PotencialService {

    private final PotencialDao potenciales = new PotencialDao();
    private final UsuarioDao usuarios = new UsuarioDao();
    private final MonederoDao monedero = new MonederoDao();
    private final ActividadDao actividad = new ActividadDao();

    /** Recluta un potencial nuevo creando también su cuenta de acceso a la app. */
    public int reclutar(Potencial p, String username, String email, String password, BigDecimal fondoInicial, int adminId) {
        validar(p);
        if (username == null || !username.matches("[a-z0-9._-]{3,40}"))
            throw new DataException("Usuario: 3-40 caracteres en minúscula (letras, números, punto o guion).");
        if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new DataException("Email no válido.");
        String err = PasswordHasher.validarFortaleza(password);
        if (err != null) throw new DataException(err);
        return Jdbc.inTransaction(c -> {
            int userId = usuarios.crear(c, username, email.trim(), PasswordHasher.hash(password), Rol.POTENCIAL, p.nombreReal());
            Potencial conUsuario = new Potencial(0, userId, username, p.alias(), p.nombreReal(), p.edad(), p.habilidad(),
                    p.descripcion(), p.nivel(), p.estado(), p.barrio(), p.ciudad(), p.pais(), p.lat(), p.lng(),
                    p.grupoId(), null, BigDecimal.ZERO, p.fechaReclutamiento());
            int id = potenciales.crear(c, conUsuario);
            if (fondoInicial != null && fondoInicial.signum() > 0) {
                monedero.registrar(c, id, null, TipoTransaccion.AJUSTE, fondoInicial, "Fondo inicial de la red", adminId);
            }
            actividad.registrar(c, adminId, "RECLUTAR", "POTENCIAL", id, p.alias());
            return id;
        });
    }

    public void actualizar(Potencial p, int adminId) {
        validar(p);
        potenciales.actualizar(p);
        actividad.registrar(adminId, "EDITAR", "POTENCIAL", p.id(), p.alias());
    }

    private static void validar(Potencial p) {
        if (p.alias() == null || p.alias().isBlank()) throw new DataException("El alias es obligatorio.");
        if (p.nombreReal() == null || p.nombreReal().isBlank()) throw new DataException("El nombre real es obligatorio.");
        if (p.habilidad() == null || p.habilidad().isBlank()) throw new DataException("Describe la habilidad.");
        if (p.nivel() < 1 || p.nivel() > 5) throw new DataException("El nivel debe estar entre 1 y 5.");
    }
}
