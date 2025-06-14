-- Check if the columns exist in the prestamos table
SELECT 
    column_name, 
    data_type, 
    column_default,
    is_nullable
FROM 
    information_schema.columns 
WHERE 
    table_name = 'prestamos' AND
    column_name IN ('dias_mora', 'mora_acumulada', 'fecha_ultimo_calculo_mora');

-- Check Flyway schema version
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
