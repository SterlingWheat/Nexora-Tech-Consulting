package com.example.backend.service;

import com.example.backend.model.Voluntario;
import java.util.List;

public interface VoluntarioService {
    
    // Firma modificada para recibir bandera y password opcionales
    Voluntario registrarVoluntario(Voluntario voluntario, Boolean crearCuenta, String password);
    
    Voluntario actualizarVoluntario(Integer idVoluntario, Voluntario detalles);
    
    // Método para el botón "Habilitar Acceso" en voluntarios existentes
    Voluntario habilitarAcceso(Integer idVoluntario, String password);
    
    List<Voluntario> listarVoluntarios();
    Voluntario obtenerVoluntarioPorId(Integer idVoluntario);
}