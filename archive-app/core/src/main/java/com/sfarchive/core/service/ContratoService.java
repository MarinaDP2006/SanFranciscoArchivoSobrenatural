package com.sfarchive.core.service;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.ContratoDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.dao.MonederoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.Contrato;
import com.sfarchive.core.model.EstadoContrato;
import com.sfarchive.core.model.EstadoIncidente;
import com.sfarchive.core.model.EstadoPotencial;
import com.sfarchive.core.model.Potencial;
import com.sfarchive.core.model.Prioridad;
import com.sfarchive.core.model.TipoTransaccion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de un contrato:
 * SOLICITADO → ASIGNADO → EN_CURSO → PENDIENTE_REVISION → COMPLETADO / FALLIDO (o CANCELADO).
 */
public class ContratoService {

    /** Resultado del cálculo de transporte. */
    public record Transporte(BigDecimal distanciaKm, BigDecimal coste) { }

    private static final Set<EstadoContrato> ABIERTOS =
            EnumSet.of(EstadoContrato.ASIGNADO, EstadoContrato.EN_CURSO, EstadoContrato.PENDIENTE_REVISION);

    private final ContratoDao contratos = new ContratoDao();
    private final PotencialDao potenciales = new PotencialDao();
    private final IncidenteDao incidentes = new IncidenteDao();
    private final MonederoDao monedero = new MonederoDao();
    private final ActividadDao actividad = new ActividadDao();

    public int crear(int incidenteId, Prioridad prioridad, BigDecimal recompensa, int adminId) {
        if (recompensa == null || recompensa.signum() < 0) throw new DataException("La recompensa no puede ser negativa.");
        return Jdbc.inTransaction(c -> {
            int id = contratos.crear(c, incidenteId, null, prioridad, recompensa);
            actividad.registrar(c, adminId, "CREAR", "CONTRATO", id, "Nuevo contrato para el incidente " + incidenteId);
            return id;
        });
    }

    /** Calcula distancia y coste de transporte entre el potencial y el incidente. */
    public Transporte calcularTransporte(Contrato contrato, Potencial potencial) {
        if (contrato.lat() == null || contrato.lng() == null || potencial.lat() == null || potencial.lng() == null) {
            return new Transporte(BigDecimal.ZERO.setScale(2), Geo.TARIFA_BASE);
        }
        double km = Geo.distanciaKm(potencial.lat(), potencial.lng(), contrato.lat(), contrato.lng());
        return new Transporte(Geo.redondearKm(km), Geo.costeTransporte(km));
    }

    /**
     * Asigna un contrato SOLICITADO a un grupo táctico y a un potencial disponible de ese grupo.
     * Genera automáticamente el gasto de transporte en el monedero del potencial.
     */
    public Transporte asignar(int contratoId, int grupoId, int potencialId, int adminId) {
        return Jdbc.inTransaction(c -> {
            Contrato ct = contratos.porId(c, contratoId).orElseThrow(() -> new DataException("Contrato no encontrado."));
            if (ct.estado() != EstadoContrato.SOLICITADO)
                throw new DataException("Solo se pueden asignar contratos en estado SOLICITADO.");
            Potencial p = potenciales.porId(c, potencialId).orElseThrow(() -> new DataException("Potencial no encontrado."));
            if (p.grupoId() == null || p.grupoId() != grupoId)
                throw new DataException(p.alias() + " no pertenece al grupo táctico seleccionado.");
            if (p.estado() != EstadoPotencial.DISPONIBLE)
                throw new DataException(p.alias() + " no está disponible (" + p.estado().etiqueta() + ").");

            Transporte t = calcularTransporte(ct, p);
            Jdbc.update(c, """
                    UPDATE contratos SET estado = 'ASIGNADO', grupo_id = ?, potencial_id = ?, coste_transporte = ?,
                           distancia_km = ?, fecha_asignacion = ?, asignado_por = ?
                    WHERE id = ?""", grupoId, potencialId, t.coste(), t.distanciaKm(), LocalDateTime.now(), adminId, contratoId);
            potenciales.cambiarEstado(c, potencialId, EstadoPotencial.EN_MISION);
            monedero.registrar(c, potencialId, contratoId, TipoTransaccion.TRANSPORTE, t.coste().negate(),
                    "Transporte " + ct.codigo() + " (" + t.distanciaKm() + " km)", adminId);
            Jdbc.update(c, "UPDATE incidentes SET estado = 'EN_INVESTIGACION' WHERE id = ? AND estado IN ('NO_VERIFICADO','VERIFICADO')",
                    ct.incidenteId());
            actividad.registrar(c, adminId, "ASIGNAR", "CONTRATO", contratoId,
                    ct.codigo() + " → " + p.alias() + " (transporte " + t.coste() + " USD)");
            return t;
        });
    }

    /** El potencial confirma que empieza la misión. */
    public void iniciar(int contratoId, int potencialId, int usuarioId) {
        Jdbc.inTransaction(c -> {
            Contrato ct = propio(c, contratoId, potencialId);
            if (ct.estado() != EstadoContrato.ASIGNADO) throw new DataException("La misión ya está iniciada o cerrada.");
            Jdbc.update(c, "UPDATE contratos SET estado = 'EN_CURSO' WHERE id = ?", contratoId);
            actividad.registrar(c, usuarioId, "INICIAR", "CONTRATO", contratoId, ct.codigo());
            return null;
        });
    }

    /** El potencial informa del resultado y pide el cierre a los administradores. */
    public void reportarFinalizacion(int contratoId, int potencialId, int usuarioId, String notas) {
        if (notas == null || notas.isBlank()) throw new DataException("Escribe un breve informe de campo.");
        Jdbc.inTransaction(c -> {
            Contrato ct = propio(c, contratoId, potencialId);
            if (ct.estado() != EstadoContrato.ASIGNADO && ct.estado() != EstadoContrato.EN_CURSO)
                throw new DataException("Esta misión no está en curso.");
            Jdbc.update(c, "UPDATE contratos SET estado = 'PENDIENTE_REVISION', notas_campo = ? WHERE id = ?", notas.trim(), contratoId);
            actividad.registrar(c, usuarioId, "REPORTAR", "CONTRATO", contratoId, ct.codigo() + " pendiente de revisión");
            return null;
        });
    }

    /** Cierra el contrato con éxito: paga la recompensa y resuelve el incidente. */
    public void completar(int contratoId, int adminId) {
        Jdbc.inTransaction(c -> {
            Contrato ct = abierto(c, contratoId);
            Jdbc.update(c, "UPDATE contratos SET estado = 'COMPLETADO', fecha_cierre = ? WHERE id = ?", LocalDateTime.now(), contratoId);
            if (ct.recompensa().signum() > 0) {
                monedero.registrar(c, ct.potencialId(), contratoId, TipoTransaccion.RECOMPENSA, ct.recompensa(),
                        "Recompensa " + ct.codigo(), adminId);
            }
            potenciales.cambiarEstado(c, ct.potencialId(), EstadoPotencial.DISPONIBLE);
            incidentes.setEstado(c, ct.incidenteId(), EstadoIncidente.RESUELTO);
            actividad.registrar(c, adminId, "COMPLETAR", "CONTRATO", contratoId, ct.codigo() + " completado por " + ct.potencialAlias());
            return null;
        });
    }

    /** Cierra el contrato como fallido: sin recompensa; el incidente sigue abierto. */
    public void marcarFallido(int contratoId, int adminId, String motivo) {
        Jdbc.inTransaction(c -> {
            Contrato ct = abierto(c, contratoId);
            Jdbc.update(c, "UPDATE contratos SET estado = 'FALLIDO', fecha_cierre = ?, notas_campo = CONCAT(COALESCE(notas_campo,''), ?) WHERE id = ?",
                    LocalDateTime.now(), motivo == null || motivo.isBlank() ? "" : "\n[FALLIDO] " + motivo, contratoId);
            potenciales.cambiarEstado(c, ct.potencialId(), EstadoPotencial.DISPONIBLE);
            actividad.registrar(c, adminId, "FALLIDO", "CONTRATO", contratoId, ct.codigo());
            return null;
        });
    }

    /** Cancela un contrato. Si ya estaba asignado se reembolsa el transporte al potencial. */
    public void cancelar(int contratoId, int adminId) {
        Jdbc.inTransaction(c -> {
            Contrato ct = contratos.porId(c, contratoId).orElseThrow(() -> new DataException("Contrato no encontrado."));
            if (ct.estado() != EstadoContrato.SOLICITADO && !ABIERTOS.contains(ct.estado()))
                throw new DataException("El contrato ya está cerrado.");
            Jdbc.update(c, "UPDATE contratos SET estado = 'CANCELADO', fecha_cierre = ? WHERE id = ?", LocalDateTime.now(), contratoId);
            if (ct.potencialId() != null) {
                potenciales.cambiarEstado(c, ct.potencialId(), EstadoPotencial.DISPONIBLE);
                if (ct.costeTransporte() != null && ct.costeTransporte().signum() > 0) {
                    monedero.registrar(c, ct.potencialId(), contratoId, TipoTransaccion.AJUSTE, ct.costeTransporte(),
                            "Reembolso de transporte " + ct.codigo() + " (cancelado)", adminId);
                }
            }
            actividad.registrar(c, adminId, "CANCELAR", "CONTRATO", contratoId, ct.codigo());
            return null;
        });
    }

    private Contrato abierto(Connection c, int contratoId) throws SQLException {
        Contrato ct = contratos.porId(c, contratoId).orElseThrow(() -> new DataException("Contrato no encontrado."));
        if (!ABIERTOS.contains(ct.estado()) || ct.potencialId() == null)
            throw new DataException("El contrato debe estar asignado para poder cerrarlo.");
        return ct;
    }

    private Contrato propio(Connection c, int contratoId, int potencialId) throws SQLException {
        Contrato ct = contratos.porId(c, contratoId).orElseThrow(() -> new DataException("Contrato no encontrado."));
        if (ct.potencialId() == null || ct.potencialId() != potencialId)
            throw new DataException("Esta misión no está asignada a ti.");
        return ct;
    }
}
