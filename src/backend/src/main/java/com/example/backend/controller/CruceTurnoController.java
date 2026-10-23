package com.example.backend.controller;

import com.example.backend.dto.ValidacionCruceDTO;
import com.example.backend.service.CruceTurnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voluntarios/{idVoluntario}/turnos/{idTurno}")
@CrossOrigin(origins = "*")
public class CruceTurnoController {

    private final CruceTurnoService cruceTurnoService;

    public CruceTurnoController(CruceTurnoService cruceTurnoService) {
        this.cruceTurnoService = cruceTurnoService;
    }

    /** 200 si se puede asignar; 409 con el turno que choca si hay cruce; 404 si el voluntario o el turno no existen. */
    @GetMapping("/validacion-cruce")
    public ResponseEntity<ValidacionCruceDTO> validar(@PathVariable Integer idVoluntario, @PathVariable Integer idTurno) {
        cruceTurnoService.validarSinCruces(idVoluntario, idTurno);
        return ResponseEntity.ok(new ValidacionCruceDTO(idVoluntario, idTurno, true));
    }
}
