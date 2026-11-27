package com.example.backend.service;

import com.example.backend.dto.HistorialUnificadoResponseDTO;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.Voluntario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface VoluntarioService {

    // HU2: Modificados para recibir el array de IDs de habilidades
    Voluntario registrarVoluntario(Voluntario voluntario, Boolean crearCuenta, String password, List<Long> habilidadesIds);

    Voluntario actualizarVoluntario(Integer idVoluntario, Voluntario detalles, List<Long> habilidadesIds);

    // Autoservicio desde el móvil: solo el teléfono es editable por el propio voluntario;
    // nombre, apellido y correo no se pueden cambiar así para evitar que alguien se haga pasar por otra persona
    Voluntario actualizarPerfil(Integer idUsuario, String telefono);

    Voluntario habilitarAcceso(Integer idVoluntario, String password);

    Voluntario cambiarEstado(Integer idVoluntario, boolean activo);

    List<Voluntario> listarVoluntarios();

    Voluntario obtenerVoluntarioPorId(Integer idVoluntario);

    // HU2: Agregado filtro por habilidadId
    Page<Voluntario> buscarVoluntarios(String texto, Boolean activo, DiaSemana diaSemana, Long habilidadId, Pageable pageable);

    // HU1: Método base para el historial unificado de participaciones
    List<HistorialUnificadoResponseDTO> obtenerHistorialUnificado(Integer idVoluntario, LocalDate desde, LocalDate hasta);
}