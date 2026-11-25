-- 1. Asegurar que la columna id de habilidad tenga secuencia y restricción UNIQUE/PK para la FK
CREATE SEQUENCE IF NOT EXISTS habilidad_id_seq OWNED BY habilidad.id;
ALTER TABLE habilidad ALTER COLUMN id SET DEFAULT nextval('habilidad_id_seq');
UPDATE habilidad SET id = nextval('habilidad_id_seq') WHERE id IS NULL;
SELECT setval('habilidad_id_seq', COALESCE((SELECT MAX(id) FROM habilidad), 1));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'habilidad'::regclass
          AND contype IN ('p', 'u')
          AND conkey = ARRAY[(
              SELECT attnum FROM pg_attribute
              WHERE attrelid = 'habilidad'::regclass AND attname = 'id'
          )]
    ) THEN
        ALTER TABLE habilidad ADD CONSTRAINT uq_habilidad_id UNIQUE (id);
    END IF;
END $$;

-- 2. Crear la tabla intermedia voluntario_habilidad con los tipos exactos (INTEGER y BIGINT)
CREATE TABLE IF NOT EXISTS voluntario_habilidad (
    voluntario_id INTEGER NOT NULL,
    habilidad_id BIGINT NOT NULL,
    PRIMARY KEY (voluntario_id, habilidad_id),
    CONSTRAINT fk_voluntario_habilidad_voluntario
        FOREIGN KEY (voluntario_id) REFERENCES voluntario (id_voluntario) ON DELETE CASCADE,
    CONSTRAINT fk_voluntario_habilidad_habilidad
        FOREIGN KEY (habilidad_id) REFERENCES habilidad (id) ON DELETE CASCADE
);