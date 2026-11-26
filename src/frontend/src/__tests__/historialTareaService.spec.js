import { describe, it, expect, vi } from 'vitest';
import { historialTareaService } from '../services/historialTareaService';
import { request } from '../services/apiClient';

// Mock de la dependencia apiClient
vi.mock('../services/apiClient', () => ({
  request: vi.fn()
}));

describe('historialTareaService', () => {
  it('obtenerHistorial debe llamar a request con el endpoint correcto', async () => {
    const mockResponse = [
      { id: 1, tipoCambio: 'CREACION', descripcion: 'Test' }
    ];
    
    request.mockResolvedValue(mockResponse);
    
    const idTarea = 42;
    const resultado = await historialTareaService.obtenerHistorial(idTarea);
    
    expect(request).toHaveBeenCalledTimes(1);
    expect(request).toHaveBeenCalledWith(`/tareas/${idTarea}/historial`, {
      mensajeError: 'Error al obtener el historial de la tarea.'
    });
    expect(resultado).toEqual(mockResponse);
  });
});