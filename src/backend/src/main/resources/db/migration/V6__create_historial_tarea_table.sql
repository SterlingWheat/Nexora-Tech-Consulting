-- V6: Añadir columnas de trazabilidad operativa a historial_tarea sin perder datos existentes
ALTER TABLE public.historial_tarea
    ADD COLUMN IF NOT EXISTS tipo_cambio VARCHAR(50) NOT NULL DEFAULT 'EDICION',
    ADD COLUMN IF NOT EXISTS valor_anterior TEXT,
    ADD COLUMN IF NOT EXISTS valor_nuevo TEXT;

-- Clasificar registros históricos existentes según el texto de detalle
UPDATE public.historial_tarea
SET tipo_cambio = 'ESTADO'
WHERE detalle ILIKE 'El estado cambió%';

UPDATE public.historial_tarea
SET tipo_cambio = 'ASIGNACION'
WHERE detalle ILIKE '%volunt%' OR detalle ILIKE '%asign%' OR detalle ILIKE '%sustituy%';

CREATE INDEX IF NOT EXISTS idx_historial_tarea_id_tarea_fecha
    ON public.historial_tarea (id_tarea, fecha_registro DESC);