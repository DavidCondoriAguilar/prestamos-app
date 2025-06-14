-- Add audit columns to prestamos table
ALTER TABLE prestamos 
    ADD COLUMN IF NOT EXISTS creado_por VARCHAR(50) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS modificado_por VARCHAR(50) NOT NULL DEFAULT 'system',
    ALTER COLUMN fecha_creacion_auditoria SET NOT NULL,
    ALTER COLUMN fecha_modificacion_auditoria SET NOT NULL;
