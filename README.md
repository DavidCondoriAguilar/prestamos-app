Prestamos App
Descripción

Aplicación para la gestión de préstamos rápidos, permitiendo la administración de clientes, cuentas bancarias y pagos asociados a préstamos. 
Esta API facilita operaciones como la creación de clientes, la gestión de cuentas, la creación de préstamos, pagos parciales y la consulta de datos relacionados.


Características

- Gestión de clientes (crear, leer, actualizar, eliminar).
- Creación de préstamos con montos, tasas de interés y estado (aprobado, pendiente).
- Registro de pagos asociados a los préstamos.
- Manejo de cuentas bancarias para cada cliente, con saldo disponible y número de cuenta.
- Consultas para obtener la información completa del cliente, préstamos y pagos.


Requisitos

Antes de empezar, asegúrate de tener las siguientes herramientas instaladas:

- Java 17+ (o superior).
- Maven (para la gestión de dependencias y construcción del proyecto).
- MySQL o PostgreSQL (para la base de datos).


Instalación

1. Clonar el repositorio:
```bash
git clone https://github.com/tu-usuario/prestamos-app.git
```

2. Navegar a la carpeta del proyecto:
```bash
cd prestamos-app
```

3. Configurar la base de datos
Configura los detalles de conexión a la base de datos en el archivo src/main/resources/application.properties. Aquí deberás establecer los parámetros como el host, usuario, contraseña y el tipo de base de datos que estás utilizando (MySQL o PostgreSQL).

```properties
# Ejemplo para PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/prestamos_db
spring.datasource.username=usuario
spring.datasource.password=contraseña
spring.datasource.driverClassName=org.postgresql.Driver
```

4. Ejecutar la aplicación
Para ejecutar la aplicación, usa el siguiente comando:
```bash
mvn spring-boot:run
```

5. Acceder a la API
Una vez que la aplicación esté en ejecución, puedes acceder a los endpoints de la API mediante HTTP en tu navegador o usando herramientas como Postman. La aplicación estará corriendo en el puerto 8080 de forma predeterminada.


Estructura de la Base de Datos

La base de datos contiene varias tablas interrelacionadas que permiten gestionar clientes, cuentas, préstamos y pagos. A continuación, se presentan algunos ejemplos de los datos que se manejan en la API.

Cliente
Representa un cliente del sistema de préstamos.

Campo         | Tipo  | Descripción
--------------|-------|------------------------------------------------------
id            | int   | Identificador único del cliente
nombre        | string| Nombre completo del cliente
correo        | string| Correo electrónico del cliente

Cuenta
Representa una cuenta bancaria asociada a un cliente.

Campo         | Tipo  | Descripción
--------------|-------|------------------------------------------------------
id            | int   | Identificador único de la cuenta
numeroCuenta  | string| Número de cuenta bancaria
saldo         | double| Saldo disponible en la cuenta

Prestamo
Representa un préstamo otorgado a un cliente.

Campo         | Tipo  | Descripción
--------------|-------|------------------------------------------------------
id            | int   | Identificador único del préstamo
monto         | double| Monto del préstamo
interes       | double| Interés aplicable al préstamo
estado        | string| Estado del préstamo (Aprobado/Pendiente)
fechaCreacion | date  | Fecha de creación del préstamo

Pago
Representa un pago realizado por el cliente a un préstamo.

Campo         | Tipo  | Descripción
--------------|-------|------------------------------------------------------
id            | int   | Identificador único del pago
montoPago     | double| Monto pagado
fecha         | date  | Fecha del pago


Relaciones

- Un Cliente puede tener varias Cuentas y Préstamos.
- Un Préstamo puede tener múltiples Pagos asociados.
- Cada Pago está vinculado a un Préstamo.


Ejemplos de Endpoints

1. Crear un Cliente
```
POST /clientes
```

Cuerpo de la solicitud:

```json
{
  "nombre": "Fabio Test",
  "correo": "fab.test@example.com"
}
```

2. Crear un Préstamo
```
POST /prestamos
```

Cuerpo de la solicitud:

```json
{
  "monto": 7000.00,
  "interes": 115.00,
  "clienteId": 1
}
```

3. Registrar un Pago
```
POST /pagos
```

Cuerpo de la solicitud:

```json
{
  "montoPago": 3550.00,
  "fecha": "2024-12-30",
  "prestamoId": 3
}
```



Contribución

Si deseas contribuir al proyecto, por favor sigue estos pasos:

1. Haz un fork del repositorio.
2. Crea una rama para tu cambio:
```bash
git checkout -b feature/nueva-funcionalidad
```
3. Realiza los cambios necesarios.
4. Haz un commit:
```bash
git commit -am 'Agregada nueva funcionalidad'
```
5. Haz un push a tu rama:
```bash
git push origin feature/nueva-funcionalidad
```
6. Crea un pull request.
