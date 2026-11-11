package com.example.backend.service;

import com.example.backend.model.DiaSemana;
import com.example.backend.model.Voluntario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VoluntarioService {

    Voluntario registrarVoluntario(Voluntario voluntario, Boolean crearCuenta, String password);

    Voluntario actualizarVoluntario(Integer idVoluntario, Voluntario detalles);

    Voluntario habilitarAcceso(Integer idVoluntario, String password);

    // Solución Error 2: Firma para permitir activar o desactivar al voluntario
    // y sincronizar el estado con el sistema de usuarios.
    Voluntario cambiarEstado(Integer idVoluntario, boolean activo);

    List<Voluntario> listarVoluntarios();

    Voluntario obtenerVoluntarioPorId(Integer idVoluntario);

    // KAN-27 Criterios 1, 2 y 3: búsqueda paginada por texto, estado y día de disponibilidad
    Page<Voluntario> buscarVoluntarios(String texto, Boolean activo, DiaSemana diaSemana, Pageable pageable);
}