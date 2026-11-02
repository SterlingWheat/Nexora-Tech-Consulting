ALTER TABLE turno
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

ALTER TABLE asignacion_turno
    DROP CONSTRAINT IF EXISTS asignacion_turno_id_turno_id_voluntario_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_asignacion_turno_vigente
    ON asignacion_turno (id_turno, id_voluntario)
    WHERE estado IS NULL OR estado NOT IN ('Cancelada', 'Rechazada');
