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

### Estructura de Entidades y Modelos

#### Entidades (entity/)
- **Entidades de Dominio**:
  - ClienteEntity: Manejo de clientes
  - PrestamoEntity: Gestión de préstamos
  - PagoEntity: Registro de pagos
  - Relaciones entre entidades
  - Validaciones de negocio
  - Anotaciones JPA

#### Modelos (model/)
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