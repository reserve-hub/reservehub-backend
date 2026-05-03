# ReserveHub — Guía de Testing Manual

Esta guía explica cómo probar cada endpoint manualmente usando **curl** y **Postman**.

---

## Requisitos previos

- Servidor corriendo: `http://localhost:8080`
- PostgreSQL configurado (variables de entorno o `.env`)
- Al menos un usuario `ADMINISTRADOR` en la base de datos

---

## Configuración inicial — crear ADMINISTRADOR

Si la base de datos está vacía, inserta el primer admin directamente en PostgreSQL:

```sql
INSERT INTO users (first_name, last_name, email, password, phone, role, active)
VALUES (
  'Admin',
  'ReserveHub',
  'admin@reservehub.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: "password"
  '3000000000',
  'ADMINISTRADOR',
  true
);
```

---

## Variables de entorno para curl

Guarda el token en una variable para reutilizarlo:

```bash
# Tras hacer login, guarda el token:
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# Úsalo en peticiones protegidas:
curl -H "Authorization: Bearer $TOKEN" ...
```

---

## Flujo 1 — Registro y Login de Cliente

### Paso 1: Registrar cliente

```bash
curl -X POST http://localhost:8080/api/users/register/cliente \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Juan",
    "lastName": "Pérez",
    "email": "juan@example.com",
    "password": "Password123",
    "phone": "3001234567"
  }'
```

**Respuesta esperada:** HTTP 200 con el UserDTO del cliente creado.

---

### Paso 2: Login como cliente

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "Password123"
  }'
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": 1,
  "role": "CLIENTE"
}
```

Copia el valor de `token` para usarlo en las siguientes peticiones.

---

## Flujo 2 — Admin genera código y registra Proveedor

### Paso 1: Login como admin

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@reservehub.com",
    "password": "password"
  }'
```

Guarda el token del admin:
```bash
ADMIN_TOKEN="<token del admin>"
```

---

### Paso 2: Generar código de proveedor

```bash
curl -X POST http://localhost:8080/api/provider-codes/ \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta esperada:**
```json
{
  "id": 1,
  "code": "PROV-A1B2C3-XYZ",
  "used": false,
  "active": true
}
```

Copia el valor de `code`.

---

### Paso 3: Registrar proveedor con el código

```bash
curl -X POST http://localhost:8080/api/users/register/proveedor \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "María",
    "lastName": "García",
    "email": "maria@example.com",
    "password": "Password123",
    "phone": "3109876543",
    "serviceType": "Peluquería",
    "serviceDescription": "Cortes y tintes profesionales",
    "providerCode": "PROV-A1B2C3-XYZ"
  }'
```

**Respuesta esperada:** HTTP 200 con el UserDTO del proveedor creado.

---

### Paso 4: Login como proveedor

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria@example.com",
    "password": "Password123"
  }'
```

Guarda el token del proveedor:
```bash
PROVEEDOR_TOKEN="<token del proveedor>"
```

---

## Flujo 3 — Proveedor crea franja horaria

### Paso 1: Crear franja

```bash
curl -X POST http://localhost:8080/api/schedules/ \
  -H "Authorization: Bearer $PROVEEDOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2026-05-10T09:00:00",
    "endTime": "2026-05-10T10:00:00",
    "availableSlots": 5
  }'
```

**Respuesta esperada:** HTTP 200 con el ScheduleResponseDTO.

---

### Paso 2: Ver mis franjas

```bash
curl http://localhost:8080/api/schedules/mine \
  -H "Authorization: Bearer $PROVEEDOR_TOKEN"
```

---

### Paso 3: Desactivar una franja (toggles active)

```bash
curl -X PATCH http://localhost:8080/api/schedules/1/status \
  -H "Authorization: Bearer $PROVEEDOR_TOKEN"
```

---

## Flujo 4 — Cliente consulta y reserva una franja

### Paso 1: Consultar franjas disponibles (sin token)

```bash
# Sin filtros
curl http://localhost:8080/api/schedules/available

# Con filtros
curl "http://localhost:8080/api/schedules/available?serviceType=Peluquería&date=2026-05-10"

# Por proveedor
curl "http://localhost:8080/api/schedules/available?providerId=2"
```

---

### Paso 2: Reservar una franja

Guarda el token del cliente:
```bash
CLIENTE_TOKEN="<token del cliente>"
```

```bash
curl -X POST http://localhost:8080/api/bookings/ \
  -H "Authorization: Bearer $CLIENTE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scheduleId": 1
  }'
```

**Respuesta esperada:** HTTP 200 con BookingResponseDTO y `status: "CONFIRMED"`.

---

### Paso 3: Ver mis reservas

```bash
curl http://localhost:8080/api/bookings/mine \
  -H "Authorization: Bearer $CLIENTE_TOKEN"
```

---

## Flujo 5 — Admin gestiona usuarios y códigos

### Listar todos los usuarios

```bash
curl http://localhost:8080/api/users/ \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Ver usuario específico

```bash
curl http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Bloquear / desbloquear usuario

```bash
curl -X PATCH http://localhost:8080/api/users/1/status \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Listar todos los códigos de proveedor

```bash
curl http://localhost:8080/api/provider-codes/ \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Desactivar un código de proveedor

```bash
curl -X PATCH http://localhost:8080/api/provider-codes/1/deactivate \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## Casos de error a verificar

### Error 400 — Email duplicado en registro

```bash
# Registrar el mismo email dos veces
curl -X POST http://localhost:8080/api/users/register/cliente \
  -H "Content-Type: application/json" \
  -d '{"firstName":"X","lastName":"Y","email":"juan@example.com","password":"Pass123","phone":"300"}'
```

**Esperado:** HTTP 400 con mensaje de error.

---

### Error 400 — Franja con rango inválido

```bash
curl -X POST http://localhost:8080/api/schedules/ \
  -H "Authorization: Bearer $PROVEEDOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2026-05-10T10:00:00",
    "endTime": "2026-05-10T09:00:00",
    "availableSlots": 3
  }'
```

**Esperado:** HTTP 400 — endTime debe ser posterior a startTime.

---

### Error 400 — Reservar franja sin cupos

```bash
# Primero crea una franja con 1 cupo y resérvala
# Luego intenta reservarla de nuevo con el mismo o diferente cliente
curl -X POST http://localhost:8080/api/bookings/ \
  -H "Authorization: Bearer $CLIENTE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"scheduleId": 1}'
```

**Esperado:** HTTP 400 — sin cupos disponibles.

---

### Error 401 — Sin token en endpoint protegido

```bash
curl http://localhost:8080/api/bookings/mine
```

**Esperado:** HTTP 401 Unauthorized.

---

### Error 403 — Rol incorrecto

```bash
# Un CLIENTE intentando crear una franja (solo PROVEEDOR puede)
curl -X POST http://localhost:8080/api/schedules/ \
  -H "Authorization: Bearer $CLIENTE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"startTime":"2026-05-10T09:00:00","endTime":"2026-05-10T10:00:00","availableSlots":3}'
```

**Esperado:** HTTP 403 Forbidden.

---

### Error 429 — Rate limit excedido

```bash
# Enviar más de 100 peticiones en 1 minuto como usuario anónimo
for i in {1..110}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/schedules/available
done
```

**Esperado:** A partir de la petición 101, HTTP 429.

---

## Testing con Postman

### Importar colección

1. Abre Postman
2. **Import** → pega la URL del Swagger: `http://localhost:8080/v3/api-docs`
3. Postman genera automáticamente una colección con todos los endpoints

### Variables de entorno en Postman

Crea un **Environment** con estas variables:

| Variable | Valor inicial |
|----------|--------------|
| `base_url` | `http://localhost:8080` |
| `admin_token` | *(se llena tras login)* |
| `cliente_token` | *(se llena tras login)* |
| `proveedor_token` | *(se llena tras login)* |

### Script de login automático en Postman

En el endpoint `POST /api/users/login`, agrega este script en **Tests**:

```javascript
const response = pm.response.json();
if (response.role === "ADMINISTRADOR") {
    pm.environment.set("admin_token", response.token);
} else if (response.role === "CLIENTE") {
    pm.environment.set("cliente_token", response.token);
} else if (response.role === "PROVEEDOR") {
    pm.environment.set("proveedor_token", response.token);
}
```

En cada endpoint protegido, configura el header:
- **Key:** `Authorization`
- **Value:** `Bearer {{admin_token}}` (o el token correspondiente al rol)

---

## Swagger UI — Testing interactivo

Accede a `http://localhost:8080/swagger-ui.html` para:

1. Ver todos los endpoints documentados automáticamente
2. Usar el botón **Authorize** para ingresar el JWT token
3. Ejecutar peticiones directamente desde el navegador

**Formato del token en Swagger:** `Bearer eyJhbGciOiJIUzUxMiJ9...`
