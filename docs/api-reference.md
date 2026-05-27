# ReserveHub — Referencia de API

**Base URL (local):** `http://localhost:8080`  
**Base URL (producción):** `https://<tu-servicio>.onrender.com`  
**Swagger UI:** `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

---

## Autenticación

La API usa **JWT Bearer Token**. Tras hacer login, incluye el token en cada petición protegida:

```
Authorization: Bearer <JWT_TOKEN>
```

**Roles disponibles:** `CLIENTE` | `PROVEEDOR` | `ADMINISTRADOR`

---

## 1. Usuarios — `/api/users`

### 1.1 Registrar Cliente
**Público — No requiere token**

```
POST /api/users/register/cliente
```

**Body:**
```json
{
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "password": "Password123",
  "phone": "3001234567"
}
```

**Respuesta 200:**
```json
{
  "id": 1,
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "phone": "3001234567",
  "role": "CLIENTE",
  "active": true
}
```

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | Email ya registrado / campos inválidos |

---

### 1.2 Registrar Proveedor
**Público — No requiere token**

```
POST /api/users/register/proveedor
```

**Body:**
```json
{
  "firstName": "María",
  "lastName": "García",
  "email": "maria@example.com",
  "password": "Password123",
  "phone": "3109876543",
  "serviceType": "Peluquería",
  "serviceDescription": "Cortes, tintes y peinados profesionales",
  "providerCode": "PROV-ABC123-XYZ"
}
```

> El campo `providerCode` debe ser generado previamente por un ADMINISTRADOR. El código se consume (marca como `used`) al registrarse.

**Respuesta 200:**
```json
{
  "id": 2,
  "firstName": "María",
  "lastName": "García",
  "email": "maria@example.com",
  "phone": "3109876543",
  "serviceType": "Peluquería",
  "serviceDescription": "Cortes, tintes y peinados profesionales",
  "role": "PROVEEDOR",
  "active": true
}
```

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | Código inválido, ya usado, o inactivo / email duplicado |

---

### 1.3 Login
**Público — No requiere token**

```
POST /api/users/login
```

**Body:**
```json
{
  "email": "juan@example.com",
  "password": "Password123"
}
```

**Respuesta 200:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": 1,
  "role": "CLIENTE"
}
```

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 401 | Credenciales incorrectas |
| 403 | Usuario inactivo (bloqueado por admin) |

---

### 1.4 Listar Todos los Usuarios
**Requiere: `ADMINISTRADOR`**

```
GET /api/users/
```

**Respuesta 200:**
```json
[
  {
    "id": 1,
    "firstName": "Juan",
    "email": "juan@example.com",
    "role": "CLIENTE",
    "active": true
  }
]
```

---

### 1.5 Obtener Usuario por ID
**Requiere: `ADMINISTRADOR` o el propio usuario**

```
GET /api/users/{id}
```

**Respuesta 200:** igual a 1.1

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 403 | Intentar ver datos de otro usuario sin ser ADMIN |
| 404 | Usuario no existe |

---

### 1.6 Actualizar Usuario
**Requiere: el propio usuario (owner)**

```
PUT /api/users/{id}
```

**Body:** mismos campos que el registro (se actualiza solo lo que se envíe)

**Respuesta 200:** UserDTO actualizado

---

### 1.7 Activar / Desactivar Usuario
**Requiere: `ADMINISTRADOR`**

```
PATCH /api/users/{id}/status
```

Alterna el campo `active` (true ↔ false). Un usuario inactivo no puede hacer login.

**Respuesta 200:** UserDTO con el nuevo estado

---

### 1.8 Dashboard Cliente
**Requiere: `CLIENTE`**

```
GET /api/users/dashboard/cliente
```

**Respuesta 200:** UserDTO del usuario autenticado

---

### 1.9 Dashboard Proveedor
**Requiere: `PROVEEDOR`**

```
GET /api/users/dashboard/proveedor
```

**Respuesta 200:** UserDTO del usuario autenticado

---

### 1.10 Dashboard Admin
**Requiere: `ADMINISTRADOR`**

```
GET /api/users/dashboard/admin
```

**Respuesta 200:** UserDTO del usuario autenticado

---

## 2. Franjas Horarias (Schedules) — `/api/schedules`

### 2.1 Crear Franja Horaria
**Requiere: `PROVEEDOR`**

```
POST /api/schedules/
```

**Body:**
```json
{
  "startTime": "2026-05-10T09:00:00",
  "endTime": "2026-05-10T10:00:00",
  "availableSlots": 5
}
```

**Respuesta 200:**
```json
{
  "id": 1,
  "providerId": 2,
  "providerName": "María García",
  "serviceType": "Peluquería",
  "startTime": "2026-05-10T09:00:00",
  "endTime": "2026-05-10T10:00:00",
  "availableSlots": 5,
  "active": true,
  "createdAt": "2026-05-02T14:30:00"
}
```

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | `endTime` ≤ `startTime` |
| 409 | Franja solapada con otra existente del mismo proveedor |

---

### 2.2 Consultar Franjas Disponibles
**Público — No requiere token**

```
GET /api/schedules/available
```

**Query params (todos opcionales):**
| Param | Tipo | Ejemplo | Descripción |
|-------|------|---------|-------------|
| `providerId` | Long | `2` | Filtrar por proveedor |
| `serviceType` | String | `Peluquería` | Filtrar por tipo de servicio |
| `date` | LocalDate | `2026-05-10` | Filtrar por fecha (solo franjas de ese día) |

**Ejemplo:**
```
GET /api/schedules/available?serviceType=Peluquería&date=2026-05-10
```

**Respuesta 200:**
```json
[
  {
    "id": 1,
    "providerId": 2,
    "providerName": "María García",
    "serviceType": "Peluquería",
    "startTime": "2026-05-10T09:00:00",
    "endTime": "2026-05-10T10:00:00",
    "availableSlots": 5,
    "active": true
  }
]
```

> Solo retorna franjas con `active = true` y `availableSlots > 0`.

---

### 2.3 Mis Franjas (Proveedor)
**Requiere: `PROVEEDOR`**

```
GET /api/schedules/mine
```

**Respuesta 200:** Lista de `ScheduleResponseDTO` del proveedor autenticado

---

### 2.4 Activar / Desactivar Franja
**Requiere: `PROVEEDOR` (solo las propias)**

```
PATCH /api/schedules/{id}/status
```

Alterna el campo `active` de la franja. Una franja inactiva no aparece en `/available`.

**Respuesta 200:** `ScheduleResponseDTO` con el nuevo estado

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 403 | Intentar modificar una franja que no le pertenece |
| 404 | Franja no existe |

---

## 3. Reservas (Bookings) — `/api/bookings`

### 3.1 Crear Reserva
**Requiere: `CLIENTE`**

```
POST /api/bookings/
```

**Body:**
```json
{
  "scheduleId": 1
}
```

**Respuesta 200:**
```json
{
  "id": 1,
  "clientId": 1,
  "clientName": "Juan Pérez",
  "scheduleId": 1,
  "providerId": 2,
  "providerName": "María García",
  "serviceType": "Peluquería",
  "startTime": "2026-05-10T09:00:00",
  "endTime": "2026-05-10T10:00:00",
  "status": "CONFIRMED",
  "createdAt": "2026-05-02T14:35:00"
}
```

> Al crear la reserva, `availableSlots` de la franja se decrementa en 1.

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | Franja inactiva o sin cupos disponibles |
| 404 | Franja no existe |

---

### 3.2 Mis Reservas — Historial con filtros *(Sprint 3 — HU-11)*
**Requiere: `CLIENTE`**

```
GET /api/bookings/mine
```

**Query params (todos opcionales):**
| Param | Tipo | Ejemplo | Descripción |
|-------|------|---------|-------------|
| `status` | `CONFIRMED` \| `CANCELLED` \| `RESCHEDULED` | `CANCELLED` | Filtrar por estado |
| `from` | ISO DateTime | `2026-01-01T00:00:00` | Inicio del rango de fechas |
| `to` | ISO DateTime | `2026-12-31T23:59:59` | Fin del rango de fechas |

**Respuesta 200:** Lista de `BookingResponseDTO` del cliente autenticado (vacía si no hay reservas)

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | `from` posterior a `to` (rango inválido) |

---

### 3.3 Cancelar Reserva *(Sprint 3 — HU-10)*
**Requiere: `CLIENTE` (solo propietario de la reserva)**

```
PATCH /api/bookings/{id}/cancel
```

> Libera el cupo de la franja horaria. Registra `cancelledAt`.

**Respuesta 200:** `BookingResponseDTO` con `status: "CANCELLED"` y `cancelledAt` poblado

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | La reserva ya está cancelada |
| 403 | No es el propietario de la reserva |
| 404 | Reserva no existe |

---

### 3.4 Reagendar Reserva *(Sprint 3 — HU-10)*
**Requiere: `CLIENTE` (solo propietario de la reserva)**

```
PATCH /api/bookings/{id}/reschedule
```

**Body:**
```json
{
  "newScheduleId": 5
}
```

> Libera el cupo del horario anterior y descuenta uno del nuevo. Registra `updatedAt`.

**Respuesta 200:** `BookingResponseDTO` con `status: "RESCHEDULED"`, nuevo `scheduleId` y `updatedAt` poblado

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | Reserva cancelada / nuevo horario sin cupos o inactivo |
| 403 | No es el propietario de la reserva |
| 404 | Reserva o nuevo horario no existe |

---

### 3.5 Reservas del Proveedor *(Sprint 3 — HU-11)*
**Requiere: `PROVEEDOR`**

```
GET /api/bookings/provider/mine
```

**Query params (opcionales):**
| Param | Tipo | Descripción |
|-------|------|-------------|
| `from` | ISO DateTime | Inicio del rango |
| `to` | ISO DateTime | Fin del rango |

**Respuesta 200:** Lista de `BookingResponseDTO` del proveedor autenticado

---

### 3.6 Reporte Operativo — Admin *(Sprint 3 — HU-12)*
**Requiere: `ADMINISTRADOR`**

```
GET /api/bookings/report
```

**Query params (opcionales):** `from`, `to` (ISO DateTime)

**Respuesta 200:**
```json
{
  "total": 42,
  "confirmed": 30,
  "cancelled": 8,
  "rescheduled": 4,
  "dateFrom": "2026-01-01T00:00:00",
  "dateTo": "2026-12-31T23:59:59"
}
```

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | Rango de fechas inválido |
| 403 | Rol no autorizado |

---

### 3.7 Reporte Operativo — Proveedor *(Sprint 3 — HU-12)*
**Requiere: `PROVEEDOR`**

```
GET /api/bookings/report/mine
```

**Query params (opcionales):** `from`, `to` (ISO DateTime)

**Respuesta 200:**
```json
{
  "total": 10,
  "confirmed": 7,
  "cancelled": 2,
  "rescheduled": 1,
  "dateFrom": null,
  "dateTo": null,
  "occupancy": [
    {
      "scheduleId": 1,
      "startTime": "2026-06-10T09:00:00",
      "endTime": "2026-06-10T10:00:00",
      "totalSlots": 5,
      "usedSlots": 3,
      "availableSlots": 2,
      "occupancyRate": 60.0
    }
  ]
}
```

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | Rango de fechas inválido |

---

## 4. Códigos de Proveedor — `/api/provider-codes`

### 4.1 Generar Código
**Requiere: `ADMINISTRADOR`**

```
POST /api/provider-codes/
```

> No requiere body. El código se genera automáticamente con formato `PROV-XXXXXXXX-XXX`.

**Respuesta 200:**
```json
{
  "id": 1,
  "code": "PROV-A1B2C3-XYZ",
  "used": false,
  "active": true
}
```

---

### 4.2 Listar Códigos
**Requiere: `ADMINISTRADOR`**

```
GET /api/provider-codes/
```

**Respuesta 200:**
```json
[
  {
    "id": 1,
    "code": "PROV-A1B2C3-XYZ",
    "used": false,
    "active": true
  }
]
```

---

### 4.3 Desactivar Código
**Requiere: `ADMINISTRADOR`**

```
PATCH /api/provider-codes/{id}/deactivate
```

Desactiva un código que **no haya sido usado**.

**Respuesta 200:** `ProviderCodeResponseDTO` con `active: false`

**Errores posibles:**
| Código | Causa |
|--------|-------|
| 400 | El código ya fue usado (no se puede desactivar) |
| 404 | Código no existe |

---

## Límites de tasa (Rate Limiting)

| Rol / Estado | Límite |
|---|---|
| `ADMINISTRADOR` | Sin límite |
| `PROVEEDOR` | 300 peticiones / minuto |
| `CLIENTE` | 100 peticiones / minuto |
| Anónimo (por IP) | 100 peticiones / minuto |

Cuando se supera el límite: **HTTP 429 Too Many Requests**

---

## Códigos de Error Globales

| Código | Descripción |
|--------|-------------|
| 400 | Bad Request — validación fallida o lógica de negocio |
| 401 | Unauthorized — token ausente o inválido |
| 403 | Forbidden — sin permisos para el recurso |
| 404 | Not Found — recurso no existe |
| 409 | Conflict — colisión de datos (email duplicado, franja solapada) |
| 429 | Too Many Requests — límite de tasa excedido |
| 500 | Internal Server Error |
