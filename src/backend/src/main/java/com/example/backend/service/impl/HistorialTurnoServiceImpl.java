package com.example.backend.service.impl;

import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.HistorialTurno;
import com.example.backend.model.Turno;
import com.example.backend.model.Usuario;
import com.example.backend.repository.HistorialTurnoRepository;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.HistorialTurnoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistorialTurnoServiceImpl implements HistorialTurnoService {

    private final HistorialTurnoRepository historialTurnoRepository;
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialTurnoServiceImpl(HistorialTurnoRepository historialTurnoRepository,
                                     TurnoRepository turnoRepository,
                                     UsuarioRepository usuarioRepository) {
        this.historialTurnoRepository = historialTurnoRepository;
        this.turnoRepository = turnoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public HistorialTurno registrarAccion(Integer idTurno, Integer idUsuario, String detalle) {
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrada con ID: " + idTurno));
                
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        HistorialTurno historial = HistorialTurno.builder()
                .turno(turno)
                .usuario(usuario)
                .detalle(detalle)
                .build();

        return historialTurnoRepository.save(historial);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialTurno> obtenerHistorialPorTurno(Integer idTurno) {
        return historialTurnoRepository.findByTurnoIdTurnoOrderByFechaRegistroDesc(idTurno);
    }
}