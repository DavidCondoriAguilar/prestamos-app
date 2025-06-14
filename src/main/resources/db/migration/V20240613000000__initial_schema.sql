-- =============================================
-- SEQUENCE: prestamo_sequence
-- =============================================
CREATE SEQUENCE IF NOT EXISTS prestamo_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- =============================================
-- TABLA: clientes
-- =============================================
CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    creado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Comentarios para la tabla clientes
COMMENT ON TABLE clientes IS 'Tabla que almacena la información de los clientes';
COMMENT ON COLUMN clientes.nombre IS 'Nombre completo del cliente';
COMMENT ON COLUMN clientes.correo IS 'Correo electrónico único del cliente';

-- =============================================
-- TABLA: cuentas
-- =============================================
CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(50) NOT NULL UNIQUE,
    saldo NUMERIC(19,2) NOT NULL,
    cliente_id BIGINT NOT NULL,
    creado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cuenta_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
);

-- Comentarios para la tabla cuentas
COMMENT ON TABLE cuentas IS 'Tabla que almacena las cuentas de los clientes';
COMMENT ON COLUMN cuentas.numero_cuenta IS 'Número único de la cuenta';
COMMENT ON COLUMN cuentas.saldo IS 'Saldo actual de la cuenta';

-- =============================================
-- TABLA: prestamos
-- =============================================
CREATE TABLE IF NOT EXISTS prestamos (
    id BIGSERIAL PRIMARY KEY,
    deuda_restante NUMERIC(19,2) NOT NULL DEFAULT 0,
    monto NUMERIC(19,2) NOT NULL,
    interes NUMERIC(5,2) NOT NULL,
    interes_moratorio NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    interes_moratorio_aplicado BOOLEAN NOT NULL DEFAULT false,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_vencimiento DATE,
    estado VARCHAR(50) NOT NULL,
    cliente_id BIGINT NOT NULL,
    fecha_ultimo_interes DATE,
    saldo_moratorio NUMERIC(15,2) DEFAULT 0,
    mora_aplicada BOOLEAN NOT NULL DEFAULT false,
    fecha_ultima_mora DATE,
    dias_mora INTEGER NOT NULL DEFAULT 0,
    mora_acumulada NUMERIC(19,2) NOT NULL DEFAULT 0,
    fecha_ultimo_calculo_mora DATE,
    fecha_creacion_auditoria TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion_auditoria TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prestamo_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
);

-- Comentarios para la tabla prestamos
COMMENT ON TABLE prestamos IS 'Tabla que almacena los préstamos realizados por los clientes';
COMMENT ON COLUMN prestamos.monto IS 'Monto total del préstamo';
COMMENT ON COLUMN prestamos.interes IS 'Tasa de interés anual del préstamo';
COMMENT ON COLUMN prestamos.interes_moratorio IS 'Tasa de interés por mora';
COMMENT ON COLUMN prestamos.estado IS 'Estado actual del préstamo';
COMMENT ON COLUMN prestamos.dias_mora IS 'Días de mora acumulados';
COMMENT ON COLUMN prestamos.fecha_creacion_auditoria IS 'Fecha de creación del registro (auditoría)';
COMMENT ON COLUMN prestamos.fecha_modificacion_auditoria IS 'Fecha de última modificación del registro (auditoría)';

-- =============================================
-- TABLA: pagos
-- =============================================
CREATE TABLE IF NOT EXISTS pagos (
    id BIGSERIAL PRIMARY KEY,
    monto NUMERIC(19,2) NOT NULL,
    fecha_pago DATE,
    prestamo_id BIGINT NOT NULL,
    creado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos(id) ON DELETE CASCADE
);

-- Comentarios para la tabla pagos
COMMENT ON TABLE pagos IS 'Registro de pagos realizados a los préstamos';
COMMENT ON COLUMN pagos.monto IS 'Monto del pago realizado';
COMMENT ON COLUMN pagos.fecha_pago IS 'Fecha en que se realizó el pago';

-- =============================================
-- ÍNDICES
-- =============================================
-- Índices para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_prestamo_cliente ON prestamos(cliente_id);
CREATE INDEX IF NOT EXISTS idx_prestamo_estado ON prestamos(estado);
CREATE INDEX IF NOT EXISTS idx_prestamo_vencimiento ON prestamos(fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_pago_prestamo ON pagos(prestamo_id);
CREATE INDEX IF NOT EXISTS idx_pago_fecha ON pagos(fecha_pago);

-- =============================================
-- FUNCIONES DE AUDITORÍA
-- =============================================
-- Función para actualizar automáticamente el campo actualizado_el
CREATE OR REPLACE FUNCTION actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_el = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers para actualizar automáticamente el campo actualizado_el
CREATE TRIGGER actualizar_clientes_timestamp
BEFORE UPDATE ON clientes
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER actualizar_cuentas_timestamp
BEFORE UPDATE ON cuentas
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER actualizar_prestamos_timestamp
BEFORE UPDATE ON prestamos
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER actualizar_pagos_timestamp
BEFORE UPDATE ON pagos
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- =============================================
-- DATOS INICIALES (OPCIONAL)
-- =============================================
-- Insertar un cliente de ejemplo (opcional, para pruebas)
INSERT INTO clientes (nombre, correo) 
VALUES ('Cliente de Prueba', 'prueba@example.com')
ON CONFLICT (correo) DO NOTHING;

-- Insertar una cuenta de ejemplo
INSERT INTO cuentas (numero_cuenta, saldo, cliente_id)
SELECT '1234567890', 10000.00, id 
FROM clientes 
WHERE correo = 'prueba@example.com'
ON CONFLICT (numero_cuenta) DO NOTHING;
