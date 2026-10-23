package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionCruceDTO {
    private Integer idVoluntario;
    private Integer idTurno;
    // true = el voluntario puede ser asignado a ese turno sin cruzarse con otro
    private boolean permitido;
}
