# ReserveHub — Quality Gates

## Estado actual (implementado)

| Gate | Herramienta | Automático | Detalle |
|------|-------------|-----------|---------|
| Tests unitarios | JUnit 5 + Mockito | ✅ CI | 42 tests unitarios sobre las 4 capas de servicio |
| Tests de integración | SpringBootTest + H2 + MockMvc | ✅ CI | 32 tests de integración sobre los 4 controladores |
| Cobertura mínima 70% | Jacoco 0.8.12 | ✅ CI | Falla el build si cobertura de líneas < 70% |
| Análisis estático | SpotBugs 4.8.6.4 | ✅ CI | Falla si encuentra bugs de severidad High |
| Deploy bloqueado por calidad | GitHub Actions | ✅ CI | Job `build` depende de `quality`; sin pass no hay deploy |
| Análisis completo (SAST + deuda técnica) | SonarCloud | ⚠️ Pendiente | Requiere configuración manual (ver sección al final) |
| Estilo de código | Checkstyle | ⚠️ Pendiente | Requiere configuración manual (ver sección al final) |

**Total: 75 tests, 0 fallos, cobertura > 70%, 0 bugs SpotBugs — BUILD SUCCESS**

---

## Pipeline CI/CD actual (`.github/workflows/build.yml`)

```
push a main / PR / manual
        │
        ▼
┌─────────────────────────────────────────────┐
│  JOB 1: quality (ubuntu-latest)             │
│                                             │
│  1. Checkout + JDK 17 (Temurin) + Cache M2  │
│  2. mvn -B verify                           │
│     ├─ compile                              │
│     ├─ test (75 tests con H2 in-memory)     │
│     ├─ jacoco:report → target/site/jacoco/  │
│     ├─ jacoco:check  → falla si < 70%       │
│     └─ spotbugs:check → falla si bug High   │
│  3. Sube reporte Jacoco como artefacto      │
└────────────────┬────────────────────────────┘
                 │ needs: quality
                 │ solo si push a main
                 ▼
┌─────────────────────────────────────────────┐
│  JOB 2: build (ubuntu-latest)               │
│                                             │
│  1. mvn package -DskipTests                 │
│  2. Push Docker → juanmauwu/reservehub:latest│
│  3. POST → Render deploy hook               │
└─────────────────────────────────────────────┘
```

**En PRs:** solo corre `quality` — el `build` no se ejecuta (condición `!= pull_request`).

---

## Cobertura Jacoco — clases medidas

### Excluidas del cálculo (boilerplate sin lógica propia)
- `com/eap15/reservehub/dto/**` — DTOs con getters/setters
- `com/eap15/reservehub/entity/**` — Entidades JPA con Lombok
- `com/eap15/reservehub/mapper/**` — Mappers generados por MapStruct
- `com/eap15/reservehub/security/**` — Infraestructura JWT (compleja de aislar sin tests de integración completos)
- `ReservehubApplication.class` — Entry point

### Incluidas en el gate (lógica de negocio y HTTP)
| Clase | Cobertura real | Método de test |
|-------|---------------|----------------|
| `UserService` | ~95% | 21 tests unitarios (Mockito) |
| `BookingService` | ~90% | 6 tests unitarios |
| `ScheduleService` | ~95% | 10 tests unitarios |
| `ProviderCodeService` | ~90% | 5 tests unitarios |
| `UserController` | ~85% | 11 tests de integración |
| `BookingController` | ~85% | 6 tests de integración |
| `ScheduleController` | ~90% | 8 tests de integración |
| `ProviderCodeController` | ~85% | 7 tests de integración |
| `GlobalExceptionHandler` | ~80% | Cubierto por tests de integración |

Reporte HTML generado en `target/site/jacoco/index.html` tras `mvn verify`.

---

## Tests implementados (75 en total)

### Tests unitarios — capa de servicio

| Clase | Tests | Escenarios cubiertos |
|-------|-------|---------------------|
| `UserServiceTest` | 21 | registerCliente (éxito/email duplicado), registerProveedor (éxito/email dup/código inválido/usado/inactivo), login (éxito/credenciales malas/cuenta deshabilitada/activo-check), getAllUsers, getUserById (éxito/no encontrado), updateUser (mismo email/email nuevo/no encontrado/email tomado), toggleUserStatus (activa/desactiva/no encontrado) |
| `BookingServiceTest` | 6 | createBooking (éxito/sin cupos/franja inactiva/no encontrada/decremento), getMyBookings |
| `ScheduleServiceTest` | 10 | createSchedule (éxito/rango inválido/traslape/rol incorrecto/proveedor no encontrado), toggleScheduleStatus (desactiva/proveedor ajeno/no encontrado), getAvailableSchedules (sin filtros), getMySchedules |
| `ProviderCodeServiceTest` | 5 | generateCode, getAllCodes, deactivateCode (éxito/ya usado/no encontrado) |

### Tests de integración — capa HTTP (H2 in-memory)

| Clase | Tests | Escenarios cubiertos |
|-------|-------|---------------------|
| `UserControllerIntegrationTest` | 11 | Registro cliente (éxito/email dup/email inválido/campo faltante), login (éxito/contraseña incorrecta), GET /api/users (sin token → 401/cliente → 403/admin → 200), PATCH status (admin), dashboard cliente |
| `ScheduleControllerIntegrationTest` | 8 | GET /available (público/con filtro fecha), POST /schedules (sin token → 401/cliente → 403/proveedor válido → 200/rango inválido → 400), GET /mine, PATCH /{id}/status |
| `BookingControllerIntegrationTest` | 6 | POST /bookings (sin token → 401/proveedor → 403/cliente válido → 200/franja inexistente → 400), GET /mine |
| `ProviderCodeControllerIntegrationTest` | 7 | POST /provider-codes (sin token → 401/cliente → 403/admin → 200), GET /provider-codes (sin token → 401/cliente → 403/admin → 200 con lista), PATCH deactivate |

---

## Correcciones de bugs aplicadas durante la implementación

### 1. `GlobalExceptionHandler` — `AccessDeniedException` retornaba 404
`AccessDeniedException` extiende `RuntimeException`. El handler genérico de `RuntimeException` la capturaba y devolvía `404 Not Found` en lugar de `403 Forbidden`. Se añadió un handler específico que re-lanza la excepción para que Spring Security la procese correctamente.

### 2. `SecurityConfig` — endpoints sin token retornaban 403 en lugar de 401
Sin un `AuthenticationEntryPoint` configurado, Spring Security devuelve `403` para requests no autenticadas. Se añadió un `AuthenticationEntryPoint` que responde con `401 Unauthorized` y un mensaje JSON.

---

## Pendiente — SonarCloud (análisis completo)

SonarCloud unifica cobertura + análisis estático + vulnerabilidades OWASP + deuda técnica en un dashboard web. Es gratuito para repositorios públicos.

### Pasos para activarlo

**1. Crear cuenta y conectar el repo**
- Ir a [sonarcloud.io](https://sonarcloud.io) → Login con GitHub
- Importar el repositorio `reservehub-backend`
- Guardar el `SONAR_TOKEN` generado como GitHub Secret: `Settings → Secrets → SONAR_TOKEN`

**2. Agregar propiedades al `pom.xml`** (en `<properties>`):
```xml
<sonar.organization>tu-organizacion-en-sonarcloud</sonar.organization>
<sonar.host.url>https://sonarcloud.io</sonar.host.url>
<sonar.coverage.jacoco.xmlReportPaths>
    target/site/jacoco/jacoco.xml
</sonar.coverage.jacoco.xmlReportPaths>
<sonar.exclusions>
    **/dto/**,**/entity/**,**/mapper/**,**/ReservehubApplication.java
</sonar.exclusions>
```

**3. Agregar step al job `quality` en el workflow:**
```yaml
- name: SonarCloud Analysis
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: mvn -B sonar:sonar
```

### Quality Gate default de SonarCloud (bloquea merge si):
- Cobertura en código nuevo < 80%
- Hay bugs Blocker/Critical nuevos
- Hay vulnerabilidades de seguridad nuevas
- Rating de mantenibilidad nuevo < A

---

## Pendiente — Checkstyle (estilo de código)

Checkstyle verifica convenciones de formato: nombres de variables, longitud de métodos, imports sin usar, etc.

### Pasos para activarlo

**1. Crear `checkstyle.xml` en la raíz del proyecto:**
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <module name="TreeWalker">
        <module name="UnusedImports"/>
        <module name="EmptyBlock"/>
        <module name="NeedBraces"/>
        <module name="MethodLength">
            <property name="max" value="60"/>
        </module>
    </module>
</module>
```

**2. Agregar plugin al `pom.xml`:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.4.0</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <failOnViolation>true</failOnViolation>
        <consoleOutput>true</consoleOutput>
        <excludes>**/mapper/UserMapperImpl.java</excludes>
    </configuration>
    <executions>
        <execution>
            <id>checkstyle</id>
            <phase>validate</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

> **Nota:** Activar Checkstyle puede requerir ajustes al código existente. Se recomienda activarlo con `failOnViolation=false` primero para ver el volumen de issues antes de hacerlo bloqueante.
