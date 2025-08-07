# Módulo de Cálculo de Intereses Moratorios

## Descripción
Este módulo se encarga de calcular y aplicar automáticamente los intereses moratorios a los préstamos vencidos en el sistema de gestión de préstamos.

## Características Principales

- Cálculo diario de intereses moratorios
- Actualización automática del estado de los préstamos (VENCIDO, EN_MORA)
- Registro detallado de operaciones
- Manejo de errores robusto
- Configuración flexible de tasas de interés

## Flujo de Trabajo

1. **Programación**: El proceso se ejecuta automáticamente una vez al día mediante un programador de tareas.
2. **Búsqueda de Préstamos**: Localiza todos los préstamos que:
   - Estén en estado APROBADO, VENCIDO o EN_MORA
   - Tengan fecha de vencimiento anterior al día actual
3. **Cálculo de Intereses**: Para cada préstamo encontrado:
   - Calcula los días de mora
   - Aplica la tasa de interés moratorio diaria
   - Actualiza el monto total adeudado
4. **Actualización de Estados**:
   - 1 día de mora: Cambia estado a VENCIDO
   - +5 días de mora: Cambia estado a EN_MORA
5. **Registro**: Guarda todos los cambios en la base de datos

## Fórmula de Cálculo

```
Interés Moratorio Diario = (Monto del Préstamo + Interés Ordinario) * Tasa de Interés Moratorio Diario
```

Donde:
- Tasa de Interés Moratorio Diario por defecto: 0.1% (0.001)
- Se puede configurar una tasa específica por préstamo

## Endpoints

### Cálculo Manual de Mora

```
POST /scheduler/calcular-mora
```

**Parámetros Opcionales:**
- `fecha`: Fecha para el cálculo (formato: YYYY-MM-DD). Si no se especifica, usa la fecha actual.

**Respuesta Exitosa (200 OK):**
```json
{
    "status": "success",
    "fechaCalculo": "2025-08-07",
    "totalPrestamosProcesados": 5,
    "prestamosEnMora": 3,
    "prestamosVencidos": 2,
    "mensaje": "Cálculo de mora ejecutado exitosamente"
}
```

**Autenticación:**
- Requiere autenticación JWT
- Rol mínimo requerido: ROLE_ADMIN

## Configuración

### Propiedades de la Aplicación

```properties
# Habilitar/deshabilitar el cálculo automático
app.scheduler.enabled=true

# Hora programada para el cálculo diario (formato cron)
app.scheduler.cron=0 0 0 * * ?  # Ejecutar todos los días a medianoche

# Tasa de interés moratorio diaria por defecto (0.1%)
app.prestamo.tasa-interes-moratorio-diario=0.001
```

## Registro de Actividad

El sistema registra las siguientes operaciones:

- Inicio del proceso de cálculo
- Préstamos procesados
- Errores encontrados
- Resumen de la operación

Ejemplo de registro:
```
=== CÁLCULO DE INTERESES MORATORIOS COMPLETADO ===
Fecha: 2025-08-07
Préstamos procesados: 15
Monto total de intereses aplicados: 1250.75
Errores: 0
```

## Consideraciones de Rendimiento

- El proceso utiliza procesamiento por lotes para manejar grandes volúmenes de préstamos
- Las operaciones se realizan dentro de transacciones para garantizar la integridad de los datos
- Se recomienda ejecutar durante períodos de baja demanda del sistema

## Solución de Problemas

### Error: "No se encontró la tasa de interés moratorio"
Verificar que el préstamo tenga configurada una tasa de interés moratorio o que esté definida la tasa por defecto.

### Error: "No se pudo actualizar el préstamo"
Revisar los logs para identificar problemas de conexión a la base de datos o restricciones de integridad.

## Seguridad

- Todas las operaciones requieren autenticación
- Los registros contienen información sensible y deben protegerse adecuadamente
- Se recomienda implementar auditoría de cambios para seguimiento de operaciones
