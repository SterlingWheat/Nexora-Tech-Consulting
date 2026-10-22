package com.example.backend.service.impl;

import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import com.example.backend.repository.CategoriaTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.service.TareaService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TareaServiceImpl implements TareaService {

    private final TareaRepository tareaRepository;
    private final CategoriaTareaRepository categoriaTareaRepository;

    //@Autowired
    public TareaServiceImpl(TareaRepository tareaRepository, CategoriaTareaRepository categoriaTareaRepository) {
        this.tareaRepository = tareaRepository;
        this.categoriaTareaRepository = categoriaTareaRepository;
    }

    @Override
    @Transactional
    public Tarea crearTarea(Tarea tarea) {
        // Criterio 1: La tarea se crea en estado Pendiente (esto también está respaldado por el @PrePersist en el modelo)
        tarea.setEstado("Pendiente"); /*[cite: 2]*/
        tarea.setAvance((short) 0); /*[cite: 2]*/
        
        // Validar que la categoría exista
        CategoriaTarea categoria = categoriaTareaRepository.findById(tarea.getCategoria().getIdCategoria())
                .orElseThrow(() -> new IllegalArgumentException("La categoría seleccionada no existe."));
        
        tarea.setCategoria(categoria);
        return tareaRepository.save(tarea);
    }

    @Override
    @Transactional
    public Tarea editarTarea(Integer idTarea, Tarea tareaDetalles) {
        Tarea tareaExistente = tareaRepository.findById(idTarea)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con ID: " + idTarea));

        // Criterio 3: Dada una tarea Finalizada o Cancelada, el sistema impide su edición.
        if ("Finalizada".equalsIgnoreCase(tareaExistente.getEstado()) || 
            "Cancelada".equalsIgnoreCase(tareaExistente.getEstado())) {
            throw new IllegalStateException("No se puede editar la tarea porque se encuentra " + tareaExistente.getEstado());
        }

        // Criterio 2: Se guardan los cambios. Al usar JPA, las asignaciones (si existieran en la relación) 
        // se mantienen intactas porque solo actualizamos los campos básicos de la tarea.
        tareaExistente.setTitulo(tareaDetalles.getTitulo());
        tareaExistente.setDescripcion(tareaDetalles.getDescripcion());
        
        // Validar y actualizar categoría (Criterio 4)
        if (tareaDetalles.getCategoria() != null && tareaDetalles.getCategoria().getIdCategoria() != null) {
            CategoriaTarea categoria = categoriaTareaRepository.findById(tareaDetalles.getCategoria().getIdCategoria())
                    .orElseThrow(() -> new IllegalArgumentException("La categoría seleccionada no existe."));
            tareaExistente.setCategoria(categoria);
        }

        return tareaRepository.save(tareaExistente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tarea> listarTareas() {
        return tareaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Tarea obtenerTareaPorId(Integer idTarea) {
        return tareaRepository.findById(idTarea)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con ID: " + idTarea));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaTarea> listarCategorias() {
        // Criterio 4: Las categorías se seleccionan de un catálogo.
        return categoriaTareaRepository.findAll();
    }
}