package com.example.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el día como smallint 1-7, el formato del esquema existente (disponibilidad_horaria). */
@Converter
public class DiaSemanaConverter implements AttributeConverter<DiaSemana, Short> {

    @Override
    public Short convertToDatabaseColumn(DiaSemana dia) {
        return dia == null ? null : dia.getNumero();
    }

    @Override
    public DiaSemana convertToEntityAttribute(Short numero) {
        return numero == null ? null : DiaSemana.desdeNumero(numero);
    }
}
