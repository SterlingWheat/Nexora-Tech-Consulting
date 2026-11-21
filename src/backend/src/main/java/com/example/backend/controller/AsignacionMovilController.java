package com.example.backend.controller;

import com.example.backend.dto.AsignacionTareaMovilDTO;
import com.example.backend.dto.AsignacionTurnoMovilDTO;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.TurnoAssignmentMobileRecord;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.AsignacionTareaService;
import com.example.backend.service.AsignacionTurnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movil/asignaciones")
public class AsignacionMovilController {

    private final AsignacionTareaService tareaService;
    private final AsignacionTurnoService turnoService;
    private final UsuarioRepository usuarioRepository;
    private final VoluntarioRepository voluntarioRepository;

    public AsignacionMovilController(
            AsignacionTareaService tareaService,
            AsignacionTurnoService turnoService,
            UsuarioRepository usuarioRepository,
            VoluntarioRepository voluntarioRepository) {
        this.tareaService = tareaService;
        this.turnoService = turnoService;
        this.usuarioRepository = usuarioRepository;
        this.voluntarioRepository = voluntarioRepository;
    }

    @GetMapping("/tareas")
    public ResponseEntity<List<AsignacionTareaMovilDTO>> listarTareas(Authentication authentication) {
        IdentidadVoluntario identidad = identidadVoluntario(authentication);
        return ResponseEntity.ok(
                tareaService.obtenerAsignacionesDelVoluntario(identidad.voluntario().getIdVoluntario()));
    }

    @GetMapping("/turnos")
    public ResponseEntity<List<AsignacionTurnoMovilDTO>> listarTurnos(Authentication authentication) {
        IdentidadVoluntario identidad = identidadVoluntario(authentication);
        List<AsignacionTurnoMovilDTO> asignaciones = turnoService
                .obtenerAsignacionesDelVoluntario(identidad.voluntario().getIdVoluntario())
                .stream()
                .map(this::aDto)
                .toList();
        return ResponseEntity.ok(asignaciones);
    }

    @PatchMapping("/tareas/{idTarea}/{idAsignacion}/confirmar")
    public ResponseEntity<Void> confirmarTarea(
            @PathVariable Integer idTarea,
            @PathVariable Integer idAsignacion,
            Authentication authentication) {
        IdentidadVoluntario identidad = identidadVoluntario(authentication);
        tareaService.responderAsignacion(
                idTarea, idAsignacion, identidad.voluntario().getIdVoluntario(),
                identidad.usuario().getIdUsuario(), true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/tareas/{idTarea}/{idAsignacion}/rechazar")
    public ResponseEntity<Void> rechazarTarea(
            @PathVariable Integer idTarea,
            @PathVariable Integer idAsignacion,
            Authentication authentication) {
        IdentidadVoluntario identidad = identidadVoluntario(authentication);
        tareaService.responderAsignacion(
                idTarea, idAsignacion, identidad.voluntario().getIdVoluntario(),
                identidad.usuario().getIdUsuario(), false);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/turnos/{idTurno}/{idAsignacion}/confirmar")
    public ResponseEntity<Void> confirmarTurno(
            @PathVariable Integer idTurno,
            @PathVariable Long idAsignacion,
            Authentication authentication) {
        IdentidadVoluntario identidad = identidadVoluntario(authentication);
        turnoService.responderAsignacion(
                idTurno, idAsignacion, identidad.voluntario().getIdVoluntario(), true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/turnos/{idTurno}/{idAsignacion}/rechazar")
    public ResponseEntity<Void> rechazarTurno(
            @PathVariable Integer idTurno,
            @PathVariable Long idAsignacion,
            Authentication authentication) {
        IdentidadVoluntario identidad = identidadVoluntario(authentication);
        turnoService.responderAsignacion(
                idTurno, idAsignacion, identidad.voluntario().getIdVoluntario(), false);
        return ResponseEntity.noContent().build();
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado en el sistema."));
    }

    private IdentidadVoluntario identidadVoluntario(Authentication authentication) {
        Usuario usuario = usuarioAutenticado(authentication);
        Voluntario voluntario = voluntarioRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el perfil de voluntario para el usuario autenticado."));
        return new IdentidadVoluntario(usuario, voluntario);
    }

    private record IdentidadVoluntario(Usuario usuario, Voluntario voluntario) {}

    private AsignacionTurnoMovilDTO aDto(TurnoAssignmentMobileRecord asignacion) {
        return new AsignacionTurnoMovilDTO(
                asignacion.idAsignacion(),
                asignacion.idTurno(),
                asignacion.actividad(),
                asignacion.fecha(),
                asignacion.horaInicio(),
                asignacion.horaFin(),
                asignacion.estado());
    }
}
