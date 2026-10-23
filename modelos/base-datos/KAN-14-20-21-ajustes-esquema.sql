-- KAN-14 / KAN-20 / KAN-21: ajustes de esquema en la BD compartida (Neon / PostgreSQL)
--
-- Solo RELAJA restricciones o agrega valores por defecto: no borra tablas ni columnas, y ninguno de los cambios
-- exige datos nuevos a las historias del resto del equipo. Se ejecuta en una sola transacción.
-- Antes de aplicar: voluntario, turno y disponibilidad_horaria estaban vacías (verificar con los SELECT).
--
-- Contexto: la primera ejecución del backend con spring.jpa.hibernate.ddl-auto=update ya agregó, junto a las
-- columnas originales, estas columnas propias: voluntario(nombre, apellido, documento, email, fecha_creacion,
-- fecha_modificacion) y turno(cupos_totales, cupos_ocupados, fecha_creacion), y creó la tabla "disponibilidad".
-- El código ahora usa las columnas/tablas originales (nombres, apellidos, cupos, disponibilidad_horaria);
-- las columnas y la tabla agregadas quedan sin uso y se pueden eliminar más adelante si el equipo lo decide.

BEGIN;

SELECT 'voluntario' AS tabla, count(*) FROM voluntario
UNION ALL SELECT 'turno', count(*) FROM turno
UNION ALL SELECT 'disponibilidad_horaria', count(*) FROM disponibilidad_horaria;

-- voluntario: teléfono a 20 caracteres (la tabla está vacía, no se trunca ningún dato)
ALTER TABLE voluntario ALTER COLUMN telefono TYPE varchar(20);

-- Columnas agregadas por KAN-14: opcionales en la BD para que un INSERT con el esquema original siga funcionando
-- (el API las sigue exigiendo y mantiene su restricción UNIQUE)
ALTER TABLE voluntario ALTER COLUMN nombre DROP NOT NULL;
ALTER TABLE voluntario ALTER COLUMN apellido DROP NOT NULL;
ALTER TABLE voluntario ALTER COLUMN documento DROP NOT NULL;
ALTER TABLE voluntario ALTER COLUMN email DROP NOT NULL;
ALTER TABLE voluntario ALTER COLUMN activo SET DEFAULT true;

-- turno: KAN-21 usa la columna original "cupos" como total; las columnas propias dejan de exigir valor
ALTER TABLE turno ALTER COLUMN cupos_totales DROP NOT NULL;
ALTER TABLE turno ALTER COLUMN cupos_ocupados SET DEFAULT 0;

COMMIT;
