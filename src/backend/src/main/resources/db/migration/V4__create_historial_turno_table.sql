CREATE TABLE IF NOT EXISTS historial_turno (
    id_historial BIGSERIAL PRIMARY KEY,
    id_turno INTEGER NOT NULL REFERENCES turno(id_turno) ON DELETE CASCADE,
    id_usuario INTEGER NOT NULL REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    detalle TEXT NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_historial_turno_id_turno 
    ON historial_turno(id_turno);