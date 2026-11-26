package com.example.backend.service.impl;

import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.HistorialTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Usuario;
import com.example.backend.repository.HistorialTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.HistorialTareaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistorialTareaServiceImpl implements HistorialTareaService {

    private final HistorialTareaRepository historialTareaRepository;
    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialTareaServiceImpl(HistorialTareaRepository historialTareaRepository,
                                     TareaRepository tareaRepository,
                                     UsuarioRepository usuarioRepository) {
        this.historialTareaRepository = historialTareaRepository;
        this.tareaRepository = tareaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public HistorialTarea registrarAccion(Integer idTarea, Integer idUsuario, String detalle) {
        // Se mantiene por compatibilidad hacia atrás con otros flujos, asignando el tipo por defecto
        return registrarAccionDetallada(idTarea, idUsuario, "EDICION", detalle, null, null);
    }

    @Override
    @Transactional
    public HistorialTarea registrarAccionDetallada(Integer idTarea, Integer idUsuario, String tipoCambio, String detalle, String valorAnterior, String valorNuevo) {
        Tarea tarea = tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + idTarea));
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        HistorialTarea historial = HistorialTarea.builder()
                .tarea(tarea)
                .usuario(usuario)
                .tipoCambio(tipoCambio)
                .detalle(detalle)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .build();

        return historialTareaRepository.save(historial);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialTarea> obtenerHistorialPorTarea(Integer idTarea) {
        return historialTareaRepository.findByTareaIdTareaOrderByFechaRegistroDesc(idTarea);
    }
}