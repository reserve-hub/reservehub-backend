# ReserveHub — Testing en Producción (Render + Swagger UI)

Esta guía cubre cómo probar todos los flujos de la API desplegada en **Render** usando **Swagger UI**, gestionando correctamente los tokens JWT de sesión.

---

## Requisitos previos

- Acceso a la URL de producción en Render: `https://<tu-servicio>.onrender.com`
- Al menos un usuario `ADMINISTRADOR` insertado en la base de datos de producción (Supabase)
- Navegador moderno (Chrome / Firefox recomendado)

> **Nota — Cold start en Render (plan gratuito):** Si el servicio lleva más de 15 minutos inactivo, la primera petición puede tardar 30–60 segundos mientras la instancia arranca. Espera y reintenta si ves un timeout.

---

## Acceder a Swagger UI

Abre en el navegador:

```
https://<tu-servicio>.onrender.com/swagger-ui.html
```

Verás todos los endpoints agrupados por controlador. Desde aquí puedes ejecutar peticiones directamente sin necesidad de curl ni Postman.

---

## Gestión del token JWT en Swagger UI

El token JWT dura **24 horas** (`app.jwt.expiration-ms=86400000`). Si recibes un `401 Unauthorized` inesperado, tu token expiró — vuelve a hacer login y reautoriza.

> **Requisito técnico:** El botón **Authorize** y los candados junto a cada endpoint aparecen únicamente porque la clase `OpenApiConfig` declara el esquema de seguridad `bearerAuth`. Sin esa configuración, Swagger UI no muestra ningún control de autenticación.

### Paso 1 — Obtener el token

1. Expande la sección **`users-controller`**
2. Haz clic en `POST /api/users/login`
3. Pulsa **Try it out**
4. En el cuerpo ingresa tus credenciales y pulsa **Execute**
5. En la respuesta copia el valor del campo `token` (la cadena `eyJhbGci...`)

### Paso 2 — Autorizar en Swagger UI

1. Haz clic en el botón **Authorize** (parte superior derecha de la página, junto al título de la API)
2. Se abre un modal con el campo **bearerAuth (http, Bearer)**
3. En el campo **Value** pega **solo el token**, sin la palabra `Bearer`:
   ```
   eyJhbGciOiJIUzUxMiJ9...
   ```
   > SpringDoc antepone `Bearer ` automáticamente porque el esquema está configurado como `scheme = "bearer"`.
4. Pulsa **Authorize** → **Close**

A partir de este momento, todos los endpoints que ejecutes incluirán el header `Authorization: Bearer <token>`. El candado cerrado junto a cada endpoint confirma que estás autenticado.

### Paso 3 — Cambiar de rol

1. Haz clic en **Authorize** → **Logout**
2. Haz login con las credenciales del otro rol y copia el nuevo token
3. Vuelve a **Authorize** y pega el nuevo token

---

## Flujo 1 — Registro y login de Cliente

### 1.1 Registrar cliente

1. `POST /api/users/register/cliente` → **Try it out**
2. Body:
```json
{
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "password": "Password123",
  "phone": "3001234567"
}
```
3. **Execute** → Respuesta esperada: `200` con el `UserDTO` del cliente creado.

### 1.2 Login y autorizar como cliente

1. `POST /api/users/login` → **Try it out**
2. Body:
```json
{
  "email": "juan@example.com",
  "password": "Password123"
}
```
3. Copia el `token` de la respuesta
4. **Authorize** → pega `Bearer <token>` → **Authorize** → **Close**

---

## Flujo 2 — Admin genera código y registra Proveedor

### 2.1 Login como admin

Primero, asegúrate de que existe un `ADMINISTRADOR` en la base de datos de producción. Si no, inserta uno directamente en Supabase:

```sql
INSERT INTO users (first_name, last_name, email, password, phone, role, active)
VALUES (
  'Admin', 'ReserveHub', 'admin@reservehub.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  '3000000000', 'ADMINISTRADOR', true
);
```
*(hash de bcrypt para la contraseña `password`)*

Luego en Swagger:

1. `POST /api/users/login` con `admin@reservehub.com` / `password`
2. Copia el token y **Authorize** con él

### 2.2 Generar código de proveedor

1. `POST /api/provider-codes/` → **Try it out** → **Execute** (no requiere body)
2. Respuesta esperada:
```json
{
  "id": 1,
  "code": "PROV-A1B2C3-XYZ",
  "used": false,
  "active": true
}
```
3. Copia el valor de `code`.

### 2.3 Registrar proveedor con el código

1. Haz **Logout** en Authorize (para no mezclar tokens en los logs)
2. `POST /api/users/register/proveedor` → **Try it out**
3. Body:
```json
{
  "firstName": "María",
  "lastName": "García",
  "email": "maria@example.com",
  "password": "Password123",
  "phone": "3109876543",
  "serviceType": "Peluquería",
  "serviceDescription": "Cortes y tintes profesionales",
  "providerCode": "PROV-A1B2C3-XYZ"
}
```
4. Respuesta esperada: `200` con el `UserDTO` del proveedor.

### 2.4 Login y autorizar como proveedor

1. `POST /api/users/login` con `maria@example.com` / `Password123`
2. Copia el token → **Authorize** → `Bearer <token>`

---

## Flujo 3 — Proveedor crea franja horaria

> Asegúrate de que el token activo en Swagger pertenece al rol `PROVEEDOR`.

### 3.1 Crear franja

1. `POST /api/schedules/` → **Try it out**
2. Body (ajusta la fecha para que sea futura):
```json
{
  "startTime": "2026-06-01T09:00:00",
  "endTime": "2026-06-01T10:00:00",
  "availableSlots": 5
}
```
3. Respuesta esperada: `200` con el `ScheduleResponseDTO`.

### 3.2 Ver mis franjas

1. `GET /api/schedules/mine` → **Try it out** → **Execute**

### 3.3 Desactivar una franja

1. `PATCH /api/schedules/{id}/status` → **Try it out**
2. Ingresa el `id` de la franja → **Execute**
3. El estado `active` hace toggle.

---

## Flujo 4 — Cliente consulta y reserva una franja

### 4.1 Consultar franjas disponibles (sin token)

Este endpoint es público. Puedes ejecutarlo incluso sin estar autorizado en Swagger:

1. `GET /api/schedules/available` → **Try it out**
2. Parámetros opcionales:
   - `serviceType`: `Peluquería`
   - `date`: `2026-06-01`
   - `providerId`: `2`
3. **Execute**

### 4.2 Reservar una franja

> Cambia el token activo en Swagger al del rol `CLIENTE`.

1. Logout → login con `juan@example.com` → Authorize
2. `POST /api/bookings/` → **Try it out**
3. Body:
```json
{
  "scheduleId": 1
}
```
4. Respuesta esperada: `200` con `BookingResponseDTO` y `"status": "CONFIRMED"`.

### 4.3 Ver mis reservas

1. `GET /api/bookings/mine` → **Try it out** → **Execute**

---

## Flujo 5 — Admin gestiona usuarios y códigos

> Token activo debe ser de rol `ADMINISTRADOR`.

| Acción | Endpoint |
|--------|----------|
| Listar usuarios | `GET /api/users/` |
| Ver usuario por ID | `GET /api/users/{id}` |
| Bloquear/desbloquear usuario | `PATCH /api/users/{id}/status` |
| Listar códigos de proveedor | `GET /api/provider-codes/` |
| Desactivar código | `PATCH /api/provider-codes/{id}/deactivate` |

Todos usan **Try it out** → ingresa el `{id}` si aplica → **Execute**.

---

## Casos de error a verificar

### 400 — Email duplicado

Repite el registro de `juan@example.com` con otro body. Respuesta esperada: `400` con mensaje de error.

### 400 — Franja con rango inválido

```json
{
  "startTime": "2026-06-01T10:00:00",
  "endTime":   "2026-06-01T09:00:00",
  "availableSlots": 3
}
```
Respuesta esperada: `400` — `endTime` debe ser posterior a `startTime`.

### 400 — Reservar franja sin cupos

Crea una franja con `availableSlots: 1`, resérvala, y luego intenta reservarla de nuevo. Respuesta esperada: `400` sin cupos disponibles.

### 401 — Sin token / token expirado

1. Haz clic en **Authorize** → **Logout** para limpiar el token
2. Ejecuta `GET /api/bookings/mine`
3. Respuesta esperada: `401 Unauthorized`

Si tu token tiene más de 24 horas, también recibirás `401`. Solución: volver a hacer login.

### 403 — Rol incorrecto

Con token de `CLIENTE` activo, intenta:

`POST /api/schedules/` con cualquier body válido → Respuesta esperada: `403 Forbidden`

### 429 — Rate limit excedido

El límite es **100 peticiones por minuto** para usuarios anónimos. Para verificarlo sin curl, abre las **DevTools** del navegador (F12 → pestaña Network), ejecuta el endpoint `GET /api/schedules/available` más de 100 veces seguidas mediante el botón Execute o un script de consola:

```javascript
// Pega esto en la consola del navegador (DevTools)
for (let i = 1; i <= 110; i++) {
  fetch('https://<tu-servicio>.onrender.com/api/schedules/available')
    .then(r => console.log(`Request ${i}: ${r.status}`));
}
```

A partir de la petición 101 deberías ver `429 Too Many Requests`.

---

## Consejos para trabajar con tokens en Swagger UI

| Situación | Acción |
|-----------|--------|
| Cambias de rol durante la prueba | Logout → nuevo login → Authorize con el nuevo token |
| Recibes `401` inesperado | Verifica si el token expiró (TTL = 24 h); vuelve a hacer login |
| Quieres probar un endpoint público sin autenticación | Logout en Authorize para asegurarte de que no se envía token |
| Necesitas probar con varios usuarios del mismo rol | No hay multi-sesión en Swagger; abre una pestaña de incógnito o usa Postman en paralelo |
| El servidor tarda en responder (primera petición) | Cold start de Render — espera ~60 s y reintenta |
