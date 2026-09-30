# APP — San Francisco Archive
Aplicación principal del proyecto San Francisco Archive, compuesta por un backend Java y una interfaz administrativa desktop para gestionar incidentes, contratos, potenciales y reportes sobrenaturales.

La app centraliza la operación del sistema en tres líneas principales:

- gestión de incidentes públicos y verificados,
- administración de contratos y recompensas,
- control de potenciales, grupos tácticos y estado operativo.

## Estructura del módulo
- `core/`: API REST, modelos del dominio, DAO y configuración de acceso a base de datos.
- `desktop-admin/`: cliente administrativo con JavaFX para consultar y operar sobre la API.
- `db/`: scripts SQL para crear la base de datos y sembrar datos iniciales.
- `android-potencial/`: esquema de cliente o extensión asociada al proyecto, pendiente de integración formal.

## Tecnologías
- Java 21
- Maven
- Javalin 6
- JavaFX 21
- MySQL Connector/J
- Gson

## Configuración inicial
1. Instala Java 21 y Maven.
2. Crea la base de datos MySQL.
3. Ejecuta el esquema de `db/schema.sql`.
4. Ajusta la conexión en `core/src/main/resources/application.properties`.

## Compilación
Desde la carpeta `archive-app`:

```bash
mvn clean package
```

## Ejecución del backend
La API se configura con Javalin y expone rutas bajo `/api`:

- `/api/salud` — comprobación del servicio
- `/auth/login` y `/auth/logout` — autenticación
- `/incidentes/publicos` — listado de incidentes
- `/contratos` — gestión de contratos
- `/potenciales` — consulta y administración de potenciales

## Ejecución del dashboard administrativo

El módulo desktop se ejecuta con JavaFX:

```bash
mvn -pl desktop-admin javafx:run
```

## Base de datos
El esquema incluye tablas para:

- administradores,
- reporteros,
- grupos tácticos,
- potenciales,
- incidentes públicos,
- contratos de anomalía,
- transacciones de monedero,
- vínculos familiares,
- informes clasificados.

## Estado del proyecto
La base funcional del sistema ya está organizada en módulos y la API principal está conectada a la capa de datos. La aplicación sigue en desarrollo para cerrar la integración completa entre backend, desktop y frontend web.

## Siguientes pasos
- terminar la integración de autenticación y permisos,
- revisar la API de contratos y potenciales con datos reales,
- consolidar la UI desktop con el flujo operativo final,
- validar el frontend web contra la API en entorno real.
