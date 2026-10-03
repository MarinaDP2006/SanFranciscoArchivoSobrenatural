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
 * Reglas de negocio de los contratos: es el "corazón" de la aplicación.
 * <p>
 * Ciclo de vida de un contrato:
 * <pre>
 * SOLICITADO → ASIGNADO → EN_CURSO → PENDIENTE_REVISION → COMPLETADO
 *                    └──────────────┴────────────────────→ FALLIDO / CANCELADO
 * </pre>
 * Cada paso toca varias tablas a la vez (contrato, potencial, monedero, incidente, auditoría),
 * por eso todos los métodos usan {@code Jdbc.inTransaction}: o se hace todo, o nada.
 * <p>
 * Si una regla no se cumple se lanza {@link DataException} con un mensaje para el usuario;
 * la pantalla lo muestra en un diálogo.
 */
public class ContratoService {

    /**
     * Resultado del cálculo de transporte.
     *
     * @param distanciaKm kilómetros entre el potencial y el incidente
     * @param coste       dinero que se descuenta del monedero (USD)
     */
    public record Transporte(BigDecimal distanciaKm, BigDecimal coste) { }

    /** Estados en los que el contrato tiene a alguien trabajando (se puede completar, fallar o cancelar). */
    private static final Set<EstadoContrato> ABIERTOS =
            EnumSet.of(EstadoContrato.ASIGNADO, EstadoContrato.EN_CURSO, EstadoContrato.PENDIENTE_REVISION);

    // DAOs que necesita el servicio (cada uno habla con su tabla)
    private final ContratoDao contratos = new ContratoDao();
    private final PotencialDao potenciales = new PotencialDao();
    private final IncidenteDao incidentes = new IncidenteDao();
    private final MonederoDao monedero = new MonederoDao();
    private final ActividadDao actividad = new ActividadDao();

    /**
     * Crea un contrato nuevo en estado SOLICITADO para un incidente.
     *
     * @return id del contrato creado
     */
    public int crear(int incidenteId, Prioridad prioridad, BigDecimal recompensa, int adminId) {
        if (recompensa == null || recompensa.signum() < 0) throw new DataException("La recompensa no puede ser negativa.");
        return Jdbc.inTransaction(c -> {
            int id = contratos.crear(c, incidenteId, null, prioridad, recompensa);
            actividad.registrar(c, adminId, "CREAR", "CONTRATO", id, "Nuevo contrato para el incidente " + incidenteId);
            return id;
        });
    }

    /**
     * Calcula distancia y coste de transporte entre el potencial y el incidente.
     * No guarda nada: la pantalla de Contratos lo usa para enseñar el coste ANTES de asignar.
     */
    public Transporte calcularTransporte(Contrato contrato, Potencial potencial) {
        // Si falta alguna coordenada no podemos medir la distancia: se cobra solo la tarifa base
        if (contrato.lat() == null || contrato.lng() == null || potencial.lat() == null || potencial.lng() == null) {
            return new Transporte(BigDecimal.ZERO.setScale(2), Geo.TARIFA_BASE);
        }
        // Distancia en línea recta entre la base del potencial y el lugar del incidente
        double km = Geo.distanciaKm(potencial.lat(), potencial.lng(), contrato.lat(), contrato.lng());
        return new Transporte(Geo.redondearKm(km), Geo.costeTransporte(km));
    }

    /**
     * Botón ASIGNAR: asigna un contrato SOLICITADO a un grupo táctico y a un potencial disponible
     * de ese grupo, y genera automáticamente el gasto de transporte en su monedero.
     *
     * @return el transporte calculado (para enseñárselo al administrador)
     */
    public Transporte asignar(int contratoId, int grupoId, int potencialId, int adminId) {
        return Jdbc.inTransaction(c -> {
            // 1) Comprobaciones: si alguna falla se lanza DataException y no se guarda nada
            Contrato ct = contratos.porId(c, contratoId).orElseThrow(() -> new DataException("Contrato no encontrado."));
            if (ct.estado() != EstadoContrato.SOLICITADO)
                throw new DataException("Solo se pueden asignar contratos en estado SOLICITADO.");
            Potencial p = potenciales.porId(c, potencialId).orElseThrow(() -> new DataException("Potencial no encontrado."));
            if (p.grupoId() == null || p.grupoId() != grupoId)
                throw new DataException(p.alias() + " no pertenece al grupo táctico seleccionado.");
            if (p.estado() != EstadoPotencial.DISPONIBLE)
                throw new DataException(p.alias() + " no está disponible (" + p.estado().etiqueta() + ").");

            // 2) Calculamos el transporte (25 USD + 1,80 USD por km)
            Transporte t = calcularTransporte(ct, p);
            // 3) El contrato pasa a ASIGNADO y guardamos quién, cuándo y cuánto costó el viaje
            Jdbc.update(c, """
                    UPDATE contratos SET estado = 'ASIGNADO', grupo_id = ?, potencial_id = ?, coste_transporte = ?,
                           distancia_km = ?, fecha_asignacion = ?, asignado_por = ?
                    WHERE id = ?""", grupoId, potencialId, t.coste(), t.distanciaKm(), LocalDateTime.now(), adminId, contratoId);
            // 4) El potencial ya no está libre
            potenciales.cambiarEstado(c, potencialId, EstadoPotencial.EN_MISION);
            // 5) Descontamos el transporte de su monedero (importe negativo = gasto)
            monedero.registrar(c, potencialId, contratoId, TipoTransaccion.TRANSPORTE, t.coste().negate(),
                    "Transporte " + ct.codigo() + " (" + t.distanciaKm() + " km)", adminId);
            // 6) El incidente pasa a "en investigación" (en la web sigue saliendo como ABIERTO)
            Jdbc.update(c, "UPDATE incidentes SET estado = 'EN_INVESTIGACION' WHERE id = ? AND estado IN ('NO_VERIFICADO','VERIFICADO')",
                    ct.incidenteId());
            // 7) Lo apuntamos en la auditoría
            actividad.registrar(c, adminId, "ASIGNAR", "CONTRATO", contratoId,
                    ct.codigo() + " → " + p.alias() + " (transporte " + t.coste() + " USD)");
            return t;
        });
    }

    /** El potencial pulsa "Iniciar misión": ASIGNADO → EN_CURSO. */
    public void iniciar(int contratoId, int potencialId, int usuarioId) {
        Jdbc.inTransaction(c -> {
            Contrato ct = propio(c, contratoId, potencialId); // solo puede iniciar SUS misiones
            if (ct.estado() != EstadoContrato.ASIGNADO) throw new DataException("La misión ya está iniciada o cerrada.");
            Jdbc.update(c, "UPDATE contratos SET estado = 'EN_CURSO' WHERE id = ?", contratoId);
            actividad.registrar(c, usuarioId, "INICIAR", "CONTRATO", contratoId, ct.codigo());
            return null;
        });
    }

    /**
     * El potencial envía su informe de campo y pide el cierre: pasa a PENDIENTE_REVISION.
     * Un administrador decidirá después si lo completa (y paga) o lo marca como fallido.
     */
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

    /**
     * Cierra el contrato con éxito: paga la recompensa, deja libre al potencial
     * y marca el incidente como RESUELTO (en la web aparecerá como CERRADO).
     */
    public void completar(int contratoId, int adminId) {
        Jdbc.inTransaction(c -> {
            Contrato ct = abierto(c, contratoId);
            // 1) Contrato completado con fecha de cierre
            Jdbc.update(c, "UPDATE contratos SET estado = 'COMPLETADO', fecha_cierre = ? WHERE id = ?", LocalDateTime.now(), contratoId);
            // 2) Pagamos la recompensa (importe positivo = ingreso)
            if (ct.recompensa().signum() > 0) {
                monedero.registrar(c, ct.potencialId(), contratoId, TipoTransaccion.RECOMPENSA, ct.recompensa(),
                        "Recompensa " + ct.codigo(), adminId);
            }
            // 3) El potencial vuelve a estar disponible para otra misión
            potenciales.cambiarEstado(c, ct.potencialId(), EstadoPotencial.DISPONIBLE);
            // 4) El incidente queda resuelto
            incidentes.setEstado(c, ct.incidenteId(), EstadoIncidente.RESUELTO);
            actividad.registrar(c, adminId, "COMPLETAR", "CONTRATO", contratoId, ct.codigo() + " completado por " + ct.potencialAlias());
            return null;
        });
    }

    /** Cierra el contrato como fallido: sin recompensa; el incidente sigue abierto. */
    public void marcarFallido(int contratoId, int adminId, String motivo) {
        Jdbc.inTransaction(c -> {
            Contrato ct = abierto(c, contratoId);
            // Añadimos el motivo al final de las notas de campo (CONCAT) sin borrar lo que había
            Jdbc.update(c, "UPDATE contratos SET estado = 'FALLIDO', fecha_cierre = ?, notas_campo = CONCAT(COALESCE(notas_campo,''), ?) WHERE id = ?",
                    LocalDateTime.now(), motivo == null || motivo.isBlank() ? "" : "\n[FALLIDO] " + motivo, contratoId);
            potenciales.cambiarEstado(c, ct.potencialId(), EstadoPotencial.DISPONIBLE);
            actividad.registrar(c, adminId, "FALLIDO", "CONTRATO", contratoId, ct.codigo());
            return null;
        });
    }

    /** Cancela un contrato. Si ya estaba asignado, se reembolsa el transporte al potencial. */
    public void cancelar(int contratoId, int adminId) {
        Jdbc.inTransaction(c -> {
            Contrato ct = contratos.porId(c, contratoId).orElseThrow(() -> new DataException("Contrato no encontrado."));
            if (ct.estado() != EstadoContrato.SOLICITADO && !ABIERTOS.contains(ct.estado()))
                throw new DataException("El contrato ya está cerrado.");
            Jdbc.update(c, "UPDATE contratos SET estado = 'CANCELADO', fecha_cierre = ? WHERE id = ?", LocalDateTime.now(), contratoId);
            if (ct.potencialId() != null) {
                // Ya tenía potencial: lo liberamos...
                potenciales.cambiarEstado(c, ct.potencialId(), EstadoPotencial.DISPONIBLE);
                // ...y le devolvemos lo que pagó de transporte
                if (ct.costeTransporte() != null && ct.costeTransporte().signum() > 0) {
                    monedero.registrar(c, ct.potencialId(), contratoId, TipoTransaccion.AJUSTE, ct.costeTransporte(),
                            "Reembolso de transporte " + ct.codigo() + " (cancelado)", adminId);
                }
            }
            actividad.registrar(c, adminId, "CANCELAR", "CONTRATO", contratoId, ct.codigo());
            return null;
        });
    }

    /** Carga el contrato y comprueba que está "abierto" (con potencial asignado); si no, lanza error. */
    private Contrato abierto(Connection c, int contratoId) throws SQLException {
        Contrato ct = contratos.porId(c, contratoId).orElseThrow(() -> new DataException("Contrato no encontrado."));
        if (!ABIERTOS.contains(ct.estado()) || ct.potencialId() == null)
            throw new DataException("El contrato debe estar asignado para poder cerrarlo.");
        return ct;
    }

    /** Carga el contrato y comprueba que pertenece a ese potencial (seguridad: nadie toca misiones ajenas). */
    private Contrato propio(Connection c, int contratoId, int potencialId) throws SQLException {
        Contrato ct = contratos.porId(c, contratoId).orElseThrow(() -> new DataException("Contrato no encontrado."));
        if (ct.potencialId() == null || ct.potencialId() != potencialId)
            throw new DataException("Esta misión no está asignada a ti.");
        return ct;
    }
}
