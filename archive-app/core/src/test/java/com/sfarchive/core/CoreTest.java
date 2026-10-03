package com.sfarchive.core;

import com.sfarchive.core.dao.NoticiaDao;
import com.sfarchive.core.security.PasswordHasher;
import com.sfarchive.core.service.Geo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CoreTest {

    @Test
    void hashYVerificacion() {
        String h = PasswordHasher.hash("Secreto123");
        assertTrue(PasswordHasher.verify("Secreto123", h));
        assertFalse(PasswordHasher.verify("otra", h));
    }

    @Test
    void hashesDeLosDatosIniciales() {
        assertTrue(PasswordHasher.verify("Archivo1906!",
                "pbkdf2_sha256$65536$N7rTbco/utkwbIzRCO9L6w==$Gj1+qP27pq/1jf7Tru35NriQL0JNWsDVqa5wFAKNH18="));
        assertTrue(PasswordHasher.verify("Potencial2026!",
                "pbkdf2_sha256$65536$A4tYxRQE/1Ep2qH+JvGLpg==$INRN04O7sr2U4eQ2t7///2e8nUyxharyiDp7k5Q/FWU="));
    }

    @Test
    void costeTransporte() {
        double km = Geo.distanciaKm(37.8591, -122.4853, 37.8199, -122.4783); // Sausalito → Golden Gate
        assertEquals(4.40, km, 0.01);
        assertEquals(new BigDecimal("32.92"), Geo.costeTransporte(km));
    }

    @Test
    void slugs() {
        assertEquals("niebla-record-en-el-golden-gate", NoticiaDao.slugify("¡Niebla récord en el Golden Gate!"));
    }

    @Test
    void fortalezaPassword() {
        assertNotNull(PasswordHasher.validarFortaleza("corta"));
        assertNull(PasswordHasher.validarFortaleza("Archivo1906!"));
    }
}
