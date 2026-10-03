package com.sfarchive.core;

import com.sfarchive.core.dao.ContratoDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.dao.MonederoDao;
import com.sfarchive.core.dao.PotencialDao;
import com.sfarchive.core.dao.SolicitudDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Database;
import com.sfarchive.core.model.EstadoContrato;
import com.sfarchive.core.model.EstadoIncidente;
import com.sfarchive.core.model.EstadoPotencial;
import com.sfarchive.core.model.Prioridad;
import com.sfarchive.core.model.TipoIncidente;
import com.sfarchive.core.service.AuthService;
import com.sfarchive.core.service.ContratoService;
import com.sfarchive.core.service.GrupoService;
import com.sfarchive.core.service.SolicitudService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración contra MySQL con los datos de database/02_datos.sql.
 * MODIFICA LA BASE DE DATOS: ejecútala solo con  mvn test -Dsfa.it=true  y recarga después los scripts.
 */
class FlujoContratoIT {

    @BeforeAll
    static void requisitos() {
        Assumptions.assumeTrue(Boolean.getBoolean("sfa.it"), "Integración desactivada (usa -Dsfa.it=true)");
        Assumptions.assumeTrue(Database.ping(), "MySQL no disponible");
    }

    @Test
    void loginSoloConCredencialesValidas() {
        AuthService auth = new AuthService();
        assertEquals("nina", auth.login("Nina", "Archivo1906!").username());
        assertThrows(DataException.class, () -> auth.login("nina", "mal"));
        assertThrows(DataException.class, () -> auth.login("ciudadano", "x"));
    }

    @Test
    void consultasDeContratos() {
        ContratoDao dao = new ContratoDao();
        assertFalse(dao.porPotencial(7).isEmpty(), "Sombra tiene misiones");
        assertTrue(dao.porEstado(EstadoContrato.SOLICITADO).stream().allMatch(c -> c.estado() == EstadoContrato.SOLICITADO));
        assertFalse(dao.listar().isEmpty());
        assertFalse(dao.cerrados().isEmpty());
    }

    @Test
    void cicloCompletoDeUnContrato() {
        ContratoService servicio = new ContratoService();
        ContratoDao contratos = new ContratoDao();
        PotencialDao potenciales = new PotencialDao();

        // CTR-2026-009 (SOLICITADO) → Faro (id 2, Unidad Chinatown, grupo 3)
        BigDecimal saldoAntes = potenciales.porId(2).orElseThrow().saldo();
        var t = servicio.asignar(9, 3, 2, 1);
        assertTrue(t.coste().compareTo(new BigDecimal("25.00")) >= 0);
        var c = contratos.porId(9).orElseThrow();
        assertEquals(EstadoContrato.ASIGNADO, c.estado());
        assertEquals(EstadoPotencial.EN_MISION, potenciales.porId(2).orElseThrow().estado());
        assertEquals(saldoAntes.subtract(t.coste()), potenciales.porId(2).orElseThrow().saldo());

        // No se puede asignar dos veces ni a alguien de otro grupo
        assertThrows(DataException.class, () -> servicio.asignar(9, 3, 2, 1));
        assertThrows(DataException.class, () -> servicio.asignar(10, 3, 4, 1));

        servicio.iniciar(9, 2, 5);
        servicio.reportarFinalizacion(9, 2, 5, "Bruja del túnel sellada en un azulejo.");
        servicio.completar(9, 1);
        assertEquals(EstadoContrato.COMPLETADO, contratos.porId(9).orElseThrow().estado());
        assertEquals(EstadoIncidente.RESUELTO, new IncidenteDao().porId(6).orElseThrow().estado());
        assertEquals(saldoAntes.subtract(t.coste()).add(new BigDecimal("3500.00")), potenciales.porId(2).orElseThrow().saldo());
        // El historial cuadra con el saldo
        BigDecimal suma = new MonederoDao().historial(2).stream().map(x -> x.importe()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, suma.compareTo(potenciales.porId(2).orElseThrow().saldo()));
    }

    @Test
    void avisoCiudadanoSeConvierteEnContrato() {
        String codigo = new SolicitudDao().crear(TipoIncidente.SECUESTRO, "Prueba de integración: se llevan a alguien en Castro",
                "Castro", "Castro St", 37.7609, -122.435, null);
        var s = new SolicitudDao().porCodigo(codigo).orElseThrow();
        int contrato = new SolicitudService().convertir(s.id(), "Secuestro en Castro", true, Prioridad.ALTA, new BigDecimal("2000"), 3);
        assertEquals(EstadoContrato.SOLICITADO, new ContratoDao().porId(contrato).orElseThrow().estado());
        assertNotNull(new SolicitudDao().porCodigo(codigo).orElseThrow().incidenteId());
    }

    @Test
    void grupoNoAdmiteMasDeSeis() {
        GrupoService g = new GrupoService();
        // Unidad Mission (2) tiene 1 miembro: añadimos hasta 6 y el séptimo falla
        int[] ids = {10, 5, 6, 4, 9};
        for (int id : ids) g.mover(id, 2, 1);
        assertThrows(DataException.class, () -> g.mover(8, 2, 1));
    }
}
