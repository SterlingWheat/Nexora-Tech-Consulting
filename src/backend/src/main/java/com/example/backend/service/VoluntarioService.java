package com.example.backend.service;

import com.example.backend.model.Voluntario;
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
}