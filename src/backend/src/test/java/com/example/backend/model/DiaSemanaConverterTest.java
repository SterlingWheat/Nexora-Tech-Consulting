package com.example.backend.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiaSemanaConverterTest {

    private final DiaSemanaConverter converter = new DiaSemanaConverter();

    @Test
    void guardaLunesComo1YDomingoComo7() {
        assertEquals((short) 1, converter.convertToDatabaseColumn(DiaSemana.LUNES));
        assertEquals((short) 7, converter.convertToDatabaseColumn(DiaSemana.DOMINGO));
    }

    @Test
    void ida_y_vuelta_paraTodosLosDias() {
        for (DiaSemana dia : DiaSemana.values()) {
            assertEquals(dia, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(dia)));
        }
    }

    @Test
    void nulos_seConservan() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void numeroFueraDeRango_seRechaza() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute((short) 0));
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute((short) 8));
    }
}
