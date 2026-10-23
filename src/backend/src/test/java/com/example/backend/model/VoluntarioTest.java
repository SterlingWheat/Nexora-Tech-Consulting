package com.example.backend.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoluntarioTest {

    // Criterio 3
    @Test
    void preUpdate_registraFechaDeModificacion() {
        Voluntario v = new Voluntario();
        assertNull(v.getFechaModificacion());

        v.onUpdate();

        assertNotNull(v.getFechaModificacion());
    }

    // Criterio 1
    @Test
    void prePersist_dejaActivoYFechaCreacion() {
        Voluntario v = new Voluntario();
        v.setActivo(null);

        v.onCreate();

        assertTrue(v.getActivo());
        assertNotNull(v.getFechaCreacion());
    }
}
