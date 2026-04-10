# Modelo de Datos (Supabase / Postgres)

Este documento describe la estructura de base de datos extraída del proyecto usando Spring Boot, adaptada para su despliegue en Supabase.

## Tablas Principales

### 1. `users`
Almacena toda la información de los usuarios del sistema, integrando distintos roles como clientes, proveedores de servicios y administradores.

| Columna        | Tipo de Dato | Restricciones                        | Descripción |
| -------------- | ------------ | -------------------------------------| ----------- |
| `id`           | BIGINT       | PRIMARY KEY, IDENTITY                | Identificador único del usuario. |
| `first_name`   | VARCHAR(100) | NOT NULL                             | Nombre(s) del usuario. |
| `last_name`    | VARCHAR(100) | NOT NULL                             | Apellido(s) del usuario. |
| `email`        | VARCHAR(150) | NOT NULL, UNIQUE                     | Correo electrónico único para inicio de sesión. |
| `password`     | VARCHAR(255) | NOT NULL                             | Contraseña hasheada. |
| `phone`        | VARCHAR(20)  | NOT NULL                             | Número de contacto. |
| `service_type` | VARCHAR(100) | NULL                                 | Tipo de servicio ofrecido (Aplica solo a *PROVEEDORES*). |
| `service_description` | TEXT   | NULL                                 | Descripción detallada del servicio (*PROVEEDORES*). |
| `role`         | ENUM         | NOT NULL                             | Rol del usuario: `CLIENTE`, `PROVEEDOR`, o `ADMINISTRADOR`. |
| `active`       | BOOLEAN      | NOT NULL, DEFAULT true               | Estado de la cuenta. Permite bloquear accesos (`false`). |

### 2. `provider_codes`
Almacena los códigos pregenerados que permiten a un usuario registrarse con el rol `PROVEEDOR`. Estos códigos son de un solo uso.

| Columna        | Tipo de Dato | Restricciones                        | Descripción |
| -------------- | ------------ | -------------------------------------| ----------- |
| `id`           | BIGINT       | PRIMARY KEY, IDENTITY                | Identificador único. |
| `code`         | VARCHAR(50)  | NOT NULL, UNIQUE                     | El código promocional o de acceso. |
| `used`         | BOOLEAN      | NOT NULL, DEFAULT false              | Indica si el código ya fue canjeado por un proveedor. |

## Consideraciones para Supabase
* **Row Level Security (RLS)**: En Supabase, será recomendable en el futuro activar políticas de RLS (Row Level Security) para estas tablas si se accede directamente desde el Frontend, limitando la visualización/modificación de los usuarios únicamente a sus propios perfiles o permitiendo control integral solo a los administradores.
* **Manejo de Enums**: El enum `user_role` de PostgreSQL traduce a base de datos el enumerador en Java.
