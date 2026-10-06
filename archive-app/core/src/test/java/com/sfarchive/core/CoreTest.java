package com.sfarchive.core;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sfarchive.core.dao.NoticiaDao;
import com.sfarchive.core.security.PasswordHasher;
import com.sfarchive.core.service.Geo;

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
            void hashesDeLasCuatroCuentasDemo() {
            assertTrue(PasswordHasher.verify("240101",
                "pbkdf2_sha256$65536$EZ9qqaSuoQKdoqnkwjNqaQ==$1sW0Ruqe6oVpuyAj1D70inWeta/4mI0yoIAXShlEcvs="));
            assertTrue(PasswordHasher.verify("240102",
                "pbkdf2_sha256$65536$nF7eBfYqdApf8CmIkQzdJA==$KftKm0MwABzr2O0KoKx5VHutezIj3dABK5z3KWwxlQA="));
            assertTrue(PasswordHasher.verify("240103",
                "pbkdf2_sha256$65536$K0Cs7SWJUiTCQ0un6imuwQ==$vmoijqarWuCGqmUyKTwjVNdYaNFRZd0ALbS6oh+gXRk="));
            assertTrue(PasswordHasher.verify("240104",
                "pbkdf2_sha256$65536$uCLw39nGv/OJ/wjW1LK0XQ==$jGmxdUCZxPVVDv/KS/CnfVhRQfVJsasX/XiIlM16LZU="));
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
        assertNull(PasswordHasher.validarFortaleza("240103"));
        assertNotNull(PasswordHasher.validarFortaleza("12345"));
        assertNotNull(PasswordHasher.validarFortaleza("1234567"));
    }
}
