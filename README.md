markdown
# Sistema de Gestión de Préstamos Rápidos

## Descripción General
Sistema de gestión de préstamos rápidos desarrollado con Spring Boot que proporciona una solución robusta para la administración de préstamos personales y comerciales.

## Arquitectura y Diseño

### Stack Tecnológico
- **Backend**: Spring Boot 3.4.1
- **Base de Datos**: PostgreSQL 15
- **Lenguaje**: Java 21
- **Gestión de Dependencias**: Maven 3.8.6
- **Patrones de Diseño**: 
  - Repository
  - Service
  - Controller
  - Builder Pattern
  - Stream API
  - DTO Pattern
- **Logging**: SLF4J con Lombok
- **Validación**: Bean Validation (Jakarta)
- **Seguridad**: Spring Security

### Estructura de Carpetas

```
prestamos-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.prestamosrapidos.prestamos_app/
│   │   │       ├── config/         # Configuración global
│   │   │       ├── controller/     # Controladores REST
│   │   │       ├── entity/         # Entidades JPA
│   │   │       ├── exception/      # Manejo de excepciones
│   │   │       ├── mapper/         # Mapeo entre DTOs y Entidades
│   │   │       ├── model/          # DTOs y modelos de negocio
│   │   │       ├── repository/     # Repositorios JPA
│   │   │       ├── scheduler/      # Tareas programadas
│   │   │       ├── service/        # Servicios de negocio
│   │   │       ├── util/          # Utilidades y helpers
│   │   │       └── validation/     # Validaciones personalizadas
│   │   └── resources/
│   │       ├── application.properties  # Configuración de Spring
│   │       └── data/              # Scripts de inicialización
└── pom.xml
```

# Lógica de Negocio Actualizada

## Ejemplo de Transacción de Préstamo

A continuación se presenta un ejemplo detallado de un préstamo procesado por el sistema, mostrando todos los cálculos y transacciones relacionadas:

```json
{
    "id": 5,
    "monto": 5000.00,
    "interes": 15.00,
    "interesMoratorio": 10.00,
    "deudaRestante": 0.00,
    "fechas": {
        "creacion": "2025-06-14",
        "vencimiento": "2025-06-15",
        "diasMora": 0
    },
    "estado": "PAGADO",
    "clienteId": 1,
    "desglosePago": {
        "capital": 5000.00,
        "interesOrdinario": 750.00,
        "moraAcumulada": 0.00,
        "totalDeuda": 5750.00
    },
    "pagoDiario": {
        "moraDiaria": 16.67,
        "proximoVencimiento": "2025-06-15"
    },
    "pagos": [
        {
            "id": 1,
            "montoPago": 4000.00,
            "fecha": "2025-06-14"
        },
        {
            "id": 2,
            "montoPago": 1750.00,
            "fecha": "2025-06-14"
        }
    ]
}
```

### Análisis de la Transacción

1. **Datos Generales**
   - **Monto del Préstamo**: $5,000.00
   - **Tasa de Interés Anual**: 15%
   - **Tasa de Interés Moratorio Anual**: 10%
   - **Plazo**: 1 día (del 14 al 15 de junio de 2025)

2. **Cálculo de Intereses**
   - **Interés Ordinario**: $5,000.00 × 15% = $750.00
   - **Interés Moratorio Diario**: ($5,000.00 × 10%) ÷ 365 = $1.37/día
   - **Mora Acumulada**: $0.00 (el préstamo se pagó a tiempo)

3. **Desglose del Pago**
   - **Capital**: $5,000.00 (100% del monto prestado)
   - **Interés Ordinario**: $750.00 (15% del capital)
   - **Total a Pagar**: $5,750.00

4. **Pagos Realizados**
   - **Primer Pago (ID: 1)**: $4,000.00
   - **Segundo Pago (ID: 2)**: $1,750.00
   - **Total Pagado**: $5,750.00

5. **Estado Final**
   - **Deuda Restante**: $0.00
   - **Estado del Préstamo**: PAGADO
   - **Días de Mora**: 0

Este ejemplo demuestra el proceso completo de un préstamo, desde su creación hasta su liquidación, incluyendo todos los cálculos financieros y transacciones asociadas.

---

## 1. Visión General del Proceso de Préstamos

### 1.1 Flujo del Préstamo
1. **Solicitud**: Cliente solicita un préstamo con monto y plazo específicos
2. **Aprobación**: El sistema valida y aprueba el préstamo
3. **Desembolso**: El monto se transfiere a la cuenta del cliente
4. **Pagos**: Cliente realiza pagos periódicos
5. **Cierre**: El préstamo se cierra una vez pagado en su totalidad

### 1.2 Estados del Préstamo
- **APROBADO**: Préstamo activo con pagos al día
- **EN MORA**: Préstamo con pagos atrasados
- **VENCIDO**: Préstamo que superó la fecha de vencimiento
- **PAGADO**: Préstamo cancelado en su totalidad

## 2. Cálculos Financieros

### 2.1 Interés Ordinario
Se calcula sobre el monto total del préstamo utilizando la tasa de interés anual.

```
Interés Ordinario = Monto del Préstamo × (Tasa de Interés Anual / 100)
```

**Ejemplo Práctico**:
- Monto del préstamo: $10,000
- Tasa de interés: 12% anual
- Interés Ordinario = 10,000 × 0.12 = $1,200

### 2.2 Cálculo de la Mora

#### 2.2.1 Mora Diaria
Se calcula diariamente sobre el capital pendiente cuando el préstamo está vencido.

```
Tasa de Mora Diaria = (Tasa de Mora Anual / 365) / 100
Mora Diaria = Capital Pendiente × Tasa de Mora Diaria
```

**Ejemplo**:
- Capital pendiente: $8,000
- Tasa de mora anual: 10%
- Tasa diaria = (10 / 365) / 100 = 0.00027397
- Mora diaria = 8,000 × 0.00027397 = $2.19

#### 2.2.2 Mora Acumulada
```
Mora Acumulada = Mora Diaria × Días de Mora
```
- Días de mora: 7
- Mora acumulada = 2.19 × 7 = $15.33

### 2.3 Pago Diario Recomendado
Para ayudar a los clientes a planificar sus pagos, calculamos un monto de pago diario sugerido.

```
Pago Diario = (Monto del Préstamo + Interés Total) / Plazo en Días
```

**Ejemplo**:
- Monto: $10,000
- Interés total: $1,200
- Plazo: 90 días
- Pago diario = (10,000 + 1,200) / 90 = $124.44

### 2.4 Desglose de Pagos
Cada préstamo incluye un desglose detallado:

1. **Capital**: Monto original del préstamo
2. **Interés Ordinario**: Cargos por el préstamo
3. **Mora Acumulada**: Cargos por pagos atrasados
4. **Total a Pagar**: Suma de todos los conceptos

### 2.5 Ejemplo Completo de Cálculo

**Parámetros del Préstamo**:
- Monto: $10,000
- Tasa de interés: 12% anual
- Tasa de mora: 10% anual
- Plazo: 3 meses (90 días)
- Días de mora: 7

**Cálculos**:
1. Interés Ordinario = 10,000 × 0.12 = $1,200
2. Pago Diario = (10,000 + 1,200) / 90 = $124.44
3. Mora Diaria = 10,000 × (0.10/365) = $2.74
4. Mora Acumulada (7 días) = 2.74 × 7 = $19.18
5. **Total a Pagar** = 10,000 + 1,200 + 19.18 = **$11,219.18**

## 3. Beneficios para el Negocio

### 3.1 Para la Empresa
- **Mayor control financiero**: Cálculos precisos en tiempo real
- **Reducción de morosidad**: Sistema automático de seguimiento de mora
- **Mejor experiencia del cliente**: Transparencia en los cargos
- **Toma de decisiones informada**: Reportes financieros detallados

#### Ejemplo Práctico:
El sistema detecta automáticamente préstamos en mora (como el préstamo #2 en el ejemplo) y calcula los intereses de mora en tiempo real, mejorando la gestión de cartera.

### 3.2 Para los Clientes
- **Transparencia**: Desglose claro de todos los cargos
- **Planificación**: Conocimiento exacto de los pagos diarios
- **Flexibilidad**: Opciones claras para evitar mora
- **Accesibilidad**: Consulta en línea del estado de cuenta

#### Ejemplo en Uso:
El cliente puede ver en tiempo real su estado de cuenta, incluyendo el préstamo #2 con 7 días de mora y los cargos asociados, permitiéndole tomar decisiones informadas sobre sus pagos.

## 4. Manejo de Estados del Préstamo

### 4.1 Transiciones de Estado
1. **APROBADO** → **PENDIENTE**: Cuando el préstamo es creado pero aún no vence
2. **PENDIENTE** → **EN MORA**: Al día siguiente del vencimiento sin pago
3. **EN MORA** → **PAGADO**: Cuando se liquida la deuda completa
4. **CUALQUIER ESTADO** → **VENCIDO**: Si no se paga después de 30 días de mora

### 4.2 Impacto en Cálculos
- **APROBADO/PENDIENTE**: Solo se calcula interés ordinario
- **EN MORA**: Se añade mora diaria al capital pendiente
- **VENCIDO**: Se aplican cargos adicionales según política

### 4.3 Ejemplo del Sistema
En el préstamo #2:
- Fecha de creación: 14/06/2025
- Fecha de vencimiento: 07/06/2025
- Estado: EN_MORA (7 días)
- Acción: El sistema calcula automáticamente $26.67 de mora diaria

## 5. Seguridad y Cumplimiento

- **Cumplimiento Normativo**: Todos los cálculos siguen las regulaciones financieras locales
- **Auditoría**: Registro detallado de cada transacción (ej: creación de préstamo #2 el 14/06/2025)
- **Encriptación**: Protección de datos sensibles como números de cuenta
- **Respaldo**: Copias de seguridad diarias que incluyen el historial de estados de préstamos

### 5.1 Ejemplo de Seguridad
El préstamo #2 muestra solo los últimos 4 dígitos de la cuenta asociada, protegiendo la información confidencial del cliente.

## 5. Ejemplo Práctico: Caso de Estudio

### 5.1 Datos del Cliente
```json
{
    "id": 1,
    "nombre": "Cliente de Prueba",
    "correo": "prueba@example.com",
    "cuenta": {
        "id": 1,
        "numeroCuenta": "1234567890",
        "saldo": 10000.00,
        "clienteId": 1
    }
}
```

### 5.2 Préstamo con Mora
**Detalles del Préstamo:**
- **Monto:** $8,000.00
- **Tasa de Interés:** 15% anual
- **Tasa de Mora:** 10% anual
- **Fecha de Creación:** 14/06/2025
- **Fecha de Vencimiento:** 07/06/2025 (7 días de mora)
- **Estado Actual:** EN_MORA

**Cálculos:**
1. **Interés Ordinario:** 
   ```
   $8,000 * 15% = $1,200
   ```
2. **Mora Diaria:**
   ```
   ($8,000 * 10%) / 365 = $2.19 por día
   ```
3. **Mora Acumulada (7 días):**
   ```
   $2.19 * 7 = $15.33
   ```
4. **Total a Pagar:**
   ```
   $8,000 (capital) + $1,200 (interés) + $15.33 (mora) = $9,215.33
   ```

**Respuesta del Sistema:**
```json
{
    "id": 2,
    "monto": 8000.00,
    "interes": 15.00,
    "interesMoratorio": 10.00,
    "deudaRestante": 9200.00,
    "fechas": {
        "creacion": "2025-06-14",
        "vencimiento": "2025-06-07",
        "diasMora": 7
    },
    "estado": "EN_MORA",
    "desglosePago": {
        "capital": 8000.00,
        "interesOrdinario": 1200.00,
        "moraAcumulada": 56.00,
        "totalDeuda": 9256.00
    },
    "pagoDiario": {
        "moraDiaria": 26.67,
        "proximoVencimiento": "2025-06-07"
    }
}
```

### 5.3 Análisis del Ejemplo
1. **Estado del Préstamo:** 
   - `EN_MORA` indica pagos atrasados
   - 7 días de mora acumulados

2. **Desglose de Pagos:**
   - Capital: $8,000.00 (monto original)
   - Interés Ordinario: $1,200.00 (15% del capital)
   - Mora Acumulada: $56.00 (calculada a la tasa de mora)
   - **Total Deuda:** $9,256.00

3. **Pago Diario:**
   - Mora Diaria: $26.67 (costo por día de atraso)
   - Próximo Vencimiento: 07/06/2025

## 6. Soporte y Mantenimiento

- Monitoreo 24/7 del sistema
- Actualizaciones periódicas de seguridad
- Capacitación continua del personal
- Soporte técnico dedicado

### 4. Proyección de Pagos
El sistema calcula el próximo vencimiento sumando un mes a la fecha de vencimiento original.

### Consideraciones Importantes
- La mora se calcula solo sobre el capital pendiente, no sobre los intereses.
- Los cálculos utilizan redondeo a 2 decimales con la estrategia HALF_UP.
- La mora se actualiza diariamente para préstamos vencidos.

### Patrones de Diseño Implementados

#### DDD (Domain-Driven Design)
- **Bounded Contexts**:
  - Contexto de Clientes
  - Contexto de Préstamos
  - Contexto de Pagos
  - Separación clara de responsabilidades

- **Entities**:
  - Entidades con identidad única
  - Estado y comportamiento
  - Ejemplo: ClienteEntity, PrestamoEntity

- **Value Objects**:
  - Objetos inmutables
  - Validación de datos
  - Ejemplo: Monto, TasaInteres

- **Repositories**:
  - Acceso a datos
  - Consultas específicas
  - Ejemplo: ClienteRepository, PrestamoRepository

- **Domain Events**:
  - Eventos de negocio
  - Notificación de cambios
  - Ejemplo: PréstamoAprobadoEvent, PagoRealizadoEvent

#### DDL (Data-Driven Logic)
- **Validaciones de Dominio**:
  - Reglas de negocio
  - Consistencia de datos
  - Ejemplo: Validación de montos, tasas

- **Cálculos de Dominio**:
  - Lógica financiera
  - Cálculo de intereses
  - Gestión de estados

- **Estados y Transiciones**:
  - Estados de préstamos
  - Transiciones válidas
  - Ejemplo: Pendiente -> Aprobado -> Pagado

#### Patrón Builder
- Utilizado en entidades y DTOs
- Mejora la creación de objetos complejos
- Reducción de constructores
- Código más limpio y mantenible
- Ejemplo:
```java
ClienteModel cliente = ClienteModel.builder()
    .nombre("Deiv Dev")
    .email("deiv@example.com")
    .build();
```

#### Patrón DTO
- Separación de datos de negocio y transferencia
- Optimización de transferencia de datos
- Seguridad de datos
- Flexibilidad en la evolución del sistema

#### Stream API
- Procesamiento funcional de datos
- Optimización de operaciones
- Código más legible
- Ejemplo:
```java
List<PagoModel> pagosPendientes = pagos.stream()
    .filter(p -> p.getEstado() == EstadoPago.PENDIENTE)
    .collect(Collectors.toList());
```

### Arquitectura: Entidades vs Modelos (DTOs)

#### Entidades (`entity/`)
- **Propósito**: Representan la estructura de la base de datos.
- **Características**:
  - Mapean directamente a tablas en la base de datos
  - Contienen anotaciones JPA/Hibernate
  - Incluyen relaciones entre tablas
  - Tienen lógica de negocio asociada
  - Son persistentes

#### Modelos/DTOs (`model/`)
- **Propósito**: Transferir datos entre capas de la aplicación.
- **Ventajas**:
  - Desacoplamiento entre la capa de persistencia y la capa de presentación
  - Mayor seguridad al controlar qué datos se exponen
  - Flexibilidad para transformar datos según las necesidades de la interfaz
  - Mejor rendimiento al seleccionar solo los campos necesarios

### Estructura de Entidades y Modelos

#### Entidades (`entity/`)
- **Entidades de Dominio**:
  - ClienteEntity: Manejo de clientes
  - PrestamoEntity: Gestión de préstamos
  - PagoEntity: Registro de pagos
  - Relaciones entre entidades
  - Validaciones de negocio
  - Anotaciones JPA

#### Modelos (`model/`)
- **DTOs de Dominio**:
  - ClienteModel: Transferencia de datos cliente
  - PrestamoDTO: Transferencia de datos préstamo
  - PagoRequest: Solicitud de pago
  - Validaciones específicas
  - Mapeo entre capas

#### Razonamiento de la Arquitectura

1. **Por qué DDD**:
   - **Dominio Complejo**: Gestión de préstamos con reglas específicas
   - **Modelo de Dominio**: Entidades con comportamiento
   - **Bounded Contexts**: Separación clara de responsabilidades
   - **Evolutivo**: Facilita cambios en el negocio

2. **Por qué DDL**:
   - **Reglas de Negocio**: Validaciones de dominio
   - **Cálculos Financieros**: Precisión en cálculos
   - **Estados y Transiciones**: Control de flujo
   - **Consistencia**: Validación de datos

3. **Beneficios de la Arquitectura**:
   - **Mantenibilidad**: Código limpio y organizado
   - **Escalabilidad**: Separación de responsabilidades
   - **Seguridad**: Validaciones en cada capa
   - **Flexibilidad**: Fácil evolución del sistema

### Mejores Prácticas de DDD/DDL

1. **Modelado de Dominio**:
   - Entidades con comportamiento
   - Value Objects inmutables
   - Repositorios específicos
   - Eventos de dominio

2. **Validaciones**:
   - Reglas de negocio
   - Consistencia de datos
   - Estados válidos
   - Transiciones permitidas

3. **Cálculos**:
   - Precisión financiera
   - Manejo de redondeo
   - Validación de montos
   - Cálculo de intereses

#### Stream API
- Procesamiento funcional de datos
- Optimización de operaciones
- Código más legible
- Ejemplo:
```java
List<PagoModel> pagosPendientes = pagos.stream()
    .filter(p -> p.getEstado() == EstadoPago.PENDIENTE)
    .collect(Collectors.toList());
```





## Funcionalidad de Cálculo de Mora

### Características Principales
- Cálculo automático de mora diaria basado en el monto del préstamo
- Soporte para días de gracia configurables
- Actualización automática de estados (APROBADO → VENCIDO → EN_MORA)
- Cálculo acumulativo de mora
- Registro detallado de operaciones

### Configuración
```properties
# Habilitar/deshabilitar cálculo de mora
prestamo.mora.habilitada=true

# Porcentaje de mora diario (ej: 0.1 = 0.1%)
prestamo.mora.porcentaje-diario=0.1

# Días de gracia antes de aplicar mora
prestamo.mora.dias-gracia=0
```

### Flujo de Trabajo
1. **Detección de Préstamos Vencidos**
   - Verificación diaria de préstamos APROBADOS que superan su fecha de vencimiento
   - Actualización automática a estado VENCIDO

2. **Cálculo de Mora**
   - Ejecución periódica (cada minuto en desarrollo)
   - Cálculo de días de mora considerando días de gracia
   - Acumulación de mora diaria
   - Actualización de la deuda total

3. **Actualización de Estados**
   - Transición automática a estado EN_MORA al detectar mora
   - Registro de fechas de cálculo para seguimiento

### Monitoreo
- Logs detallados para auditoría
- Seguimiento de cambios de estado
- Registro de cálculos realizados

## Mejoras Futuras

### Arquitectura y Diseño
- Implementación de CQRS
- Patrón Event Sourcing
- Microservicios
- API Gateway
- Circuit Breaker

### Características de Negocio
- Sistema de reportes avanzado
- Análisis de riesgos
- Sistema de recomendaciones
- Automatización de procesos

### Rendimiento y Escalabilidad
- Implementación de cache distribuido
- Optimización de consultas
- Manejo de carga
- Monitoreo avanzado

### Seguridad
- Implementación de OAuth2
- Tokenización de datos
- Encriptación avanzada
- Auditoría detallada

### Integraciones
- Integración con sistemas bancarios
- API de notificaciones
- Sistema de mensajería
- Integración con ERP

### Interfaces
- Frontend moderno (React/Angular)
- Mobile App
- Dashboard administrativo
- API Gateway

### Calidad
- Pruebas automatizadas
- Monitoreo de rendimiento
- Logging avanzado
- Sistema de alertas
- Documentación automática

### Estructura de Capas

#### Capa de Presentación (Controller)
- Endpoints RESTful
- Manejo de excepciones
- Validación de entrada
- Logging de operaciones
- Manejo de transacciones

#### Capa de Servicios (Service)
- Lógica de negocio
- Manejo de estados
- Cálculos financieros
- Manejo de fechas
- Validaciones de negocio

#### Capa de Datos (Repository)
- Acceso a datos
- Gestión de transacciones
- Manejo de conexiones
- Optimización de consultas

#### Capa de Dominio (Model)
- Entidades de negocio
- Validaciones
- Relaciones entre entidades
- Cálculos de negocio



### Diagrama de Componentes
```
+-------------------+
|     Controller    |
|                   |
|  ClienteController|
|  PrestamoController|
|  PagoController   |
+-------------------+
          ↑
          ↓
+-------------------+
|     Service       |
|                   |
| ClienteServiceImpl|
|PrestamoServiceImpl|
|  PagoServiceImpl  |
+-------------------+
          ↑
          ↓
+-------------------+
|    Repository     |
|                   |
| ClienteRepository |
|PrestamoRepository |
|  PagoRepository   |
+-------------------+
          ↑
          ↓
+-------------------+
|      Model        |
|                   |
|   ClienteModel    |
|  PrestamoModel    |
|    PagoModel      |
+-------------------+
```







## Requisitos del Sistema
### Requisitos Técnicos
- Java 21+
- Maven 3.8.x+
- PostgreSQL 15.x+
- Git 2.30.x+

### Requisitos de Desarrollo
- IDE compatible con Java (IntelliJ IDEA)
- Postman
- Git

## Instalación y Configuración

1. **Clonar el Repositorio**:
```bash
git clone [https://github.com/tu-usuario/prestamos-app.git](https://github.com/tu-usuario/prestamos-app.git)
cd prestamos-app
```

2. **Configuración de la Base de Datos**:
```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=prestamos
DB_USERNAME=user
DB_PASSWORD=password
```

3. **Compilación y Ejecución**:
```bash
mvn clean install
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`

## Documentación de la API

### Generación de Reportes en PDF

La aplicación incluye funcionalidad para generar reportes en formato PDF con información detallada de clientes, préstamos y pagos.

### Características

- Generación de informes detallados de clientes en formato PDF
- Incluye información personal, cuentas, préstamos y pagos
- Diseño profesional con estilos CSS
- Numeración de páginas y pie de página personalizado
- Marca de agua con el logo de la empresa
- Tablas con formato profesional para mejor legibilidad

### Uso

```java
// Ejemplo de generación de reporte de cliente
@GetMapping("/clientes/{id}/reporte")
public ResponseEntity<ByteArrayResource> generarReporteCliente(@PathVariable Long id) {
    Cliente cliente = clienteService.obtenerClientePorId(id);
    ByteArrayInputStream bis = pdfGeneratorService.generateClientReport(cliente);
    
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Disposition", "inline; filename=reporte-cliente-" + id + ".pdf");
    
    return ResponseEntity
            .ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_PDF)
            .body(new ByteArrayResource(bis.readAllBytes()));
}
```

### Personalización

Puedes personalizar los siguientes aspectos del reporte:

- **Información de la empresa**: Modifica las constantes en `PDFGeneratorService`
- **Estilos**: Ajusta colores y fuentes en los métodos correspondientes
- **Contenido**: Personaliza las secciones que se incluyen en el reporte

### Dependencias

- iText PDF 5.5.13.3 para la generación de documentos PDF

## API Endpoints Principales

#### Clientes
- `POST /api/clientes` - Crear cliente
- `GET /api/clientes` - Listar clientes
- `GET /api/clientes/{id}` - Obtener cliente
- `PUT /api/clientes/{id}` - Actualizar cliente
- `DELETE /api/clientes/{id}` - Eliminar cliente

#### Préstamos
- `POST /api/prestamos` - Crear préstamo
- `GET /api/prestamos` - Listar préstamos
- `GET /api/prestamos/{id}` - Obtener préstamo

#### Pagos
- `POST /api/pagos` - Registrar pago
- `GET /api/pagos` - Listar pagos
- `GET /api/pagos/{id}` - Obtener pago

## Contribución

1. **Fork del Repositorio**:
```bash
git clone https://github.com/tu-usuario/prestamos-app.git
cd prestamos-app
git checkout -b feature/tu-nueva-funcionalidad
```

2. **Desarrollo**:
   - Crea ramas separadas
   - Escribe pruebas
   - Documenta el código

3. **Pull Request**:
   - Detalla los cambios
   - Incluye pruebas
   - Documenta adecuadamente

## Licencia

MIT License

## Contacto

Para reportar problemas o sugerir mejoras, abre un issue en el repositorio.

## Historial de Versiones

- 1.0.0 - Versión inicial
- 1.1.0 - Sistema de pagos
- 1.2.0 - Validaciones
- 1.3.0 - Seguridad

## Roadmap

- Autenticación JWT
- Sistema de reportes
- Cache
- Optimización

## Recursos Adicionales

- [Spring Boot](https://spring.io/projects/spring-boot)
- [PostgreSQL](https://www.postgresql.org/docs/)
- [Guía de Estilo](https://google.github.io/styleguide/javaguide.html)