CREATE TABLE IF NOT EXISTS habilidad (
    id_habilidad SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS voluntario_habilidad (
    id_voluntario INTEGER NOT NULL REFERENCES voluntario(id_voluntario) ON DELETE CASCADE,
    id_habilidad INTEGER NOT NULL REFERENCES habilidad(id_habilidad) ON DELETE CASCADE,
    PRIMARY KEY (id_voluntario, id_habilidad)
);

CREATE INDEX IF NOT EXISTS idx_voluntario_habilidad_id_habilidad
    ON voluntario_habilidad(id_habilidad);

INSERT INTO habilidad (nombre) VALUES
    ('Primeros auxilios'),
    ('Logística'),
    ('Traducción'),
    ('Cocina'),
    ('Conducción')
ON CONFLICT (nombre) DO NOTHING;
