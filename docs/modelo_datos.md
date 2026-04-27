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

| Columna  | Tipo de Dato | Restricciones           | Descripción |
| -------- | ------------ | ------------------------| ----------- |
| `id`     | BIGINT       | PRIMARY KEY, IDENTITY   | Identificador único. |
| `code`   | VARCHAR(50)  | NOT NULL, UNIQUE        | El código promocional o de acceso. |
| `used`   | BOOLEAN      | NOT NULL, DEFAULT false | Indica si el código ya fue canjeado por un proveedor. |
| `active` | BOOLEAN      | NOT NULL, DEFAULT true  | Indica si el código está habilitado (admin puede desactivarlo sin que sea usado). |

### 3. `schedules` *(Sprint 2 — HU-06, HU-07)*
Almacena las franjas de disponibilidad horaria registradas por los proveedores.

| Columna           | Tipo de Dato | Restricciones                      | Descripción |
| ----------------- | ------------ | -----------------------------------| ----------- |
| `id`              | BIGINT       | PRIMARY KEY, IDENTITY              | Identificador único de la franja. |
| `provider_id`     | BIGINT       | NOT NULL, FK → users(id)           | Proveedor dueño de la franja. |
| `start_time`      | TIMESTAMP    | NOT NULL                           | Hora de inicio de la franja. |
| `end_time`        | TIMESTAMP    | NOT NULL, CHECK(end > start)       | Hora de fin de la franja. |
| `available_slots` | INTEGER      | NOT NULL, CHECK(>= 0)              | Cupos disponibles. Se decrementa por cada reserva. |
| `active`          | BOOLEAN      | NOT NULL, DEFAULT true             | Si `false`, la franja no aparece en consultas de disponibilidad. |
| `created_at`      | TIMESTAMP    | NOT NULL, DEFAULT NOW()            | Fecha/hora de creación. |

**Índices:** `provider_id`, `start_time`, `active`.

### 4. `bookings` *(Sprint 2 — HU-08)*
Registra las reservas realizadas por los clientes sobre franjas disponibles.

| Columna       | Tipo de Dato       | Restricciones             | Descripción |
| ------------- | ------------------ | --------------------------| ----------- |
| `id`          | BIGINT             | PRIMARY KEY, IDENTITY     | Identificador único de la reserva. |
| `client_id`   | BIGINT             | NOT NULL, FK → users(id)  | Cliente que realizó la reserva. |
| `schedule_id` | BIGINT             | NOT NULL, FK → schedules(id) | Franja reservada. |
| `status`      | booking_status     | NOT NULL, DEFAULT 'CONFIRMED' | Estado: `CONFIRMED` o `CANCELLED`. |
| `created_at`  | TIMESTAMP          | NOT NULL, DEFAULT NOW()   | Fecha/hora de creación. |

**Índices:** `client_id`, `schedule_id`.

---

## Consideraciones para Supabase
* **Row Level Security (RLS)**: En Supabase, se recomienda activar políticas de RLS para estas tablas si se accede directamente desde el Frontend, limitando la visualización/modificación de los usuarios únicamente a sus propios perfiles o permitiendo control integral solo a los administradores.
* **Manejo de Enums**: Los enums `user_role` y `booking_status` de PostgreSQL traducen los enumeradores Java a la base de datos.
* **Integridad referencial**: Las FK de `schedules` y `bookings` tienen `ON DELETE CASCADE` para mantener consistencia cuando se elimina un usuario.
