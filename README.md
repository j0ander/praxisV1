# PRAXIS

Sistema web para la gestión de prácticas preprofesionales. La entrega actual cubre la base técnica y las fases 1 a 5 (`TK-001` a `TK-020`), incluidas las correcciones de Fase 5. TK-018, TK-019 y TK-020 están **aprobados formalmente**, con **limitaciones documentadas** de integración y validación manual; consulta `docs/TAREAS.md` §6.1 y §6.3.

Esta guía permite clonar el proyecto, configurar Supabase y ejecutar backend y frontend desde cero. No se usan credenciales versionadas, PostgreSQL local ni variables de entorno para la conexión principal.

## 1. Requisitos

| Requisito | Versión / detalle |
|---|---|
| Git | Para clonar el repositorio. |
| JDK | 25 (LTS 25.0.2). El wrapper exige `JAVA_HOME` apuntando al JDK 25. |
| Maven | No requiere instalación: se usa el Maven Wrapper incluido (Maven 3.9.11). |
| Quarkus | 3.40.1, fijado en `pom.xml`. |
| Node.js | 20.19+ para el frontend (`src/main/webui`). |
| npm | Incluido con Node.js; se usa `npm ci` con `package-lock.json`. |
| Supabase | Proyecto PostgreSQL 15+ existente, con el esquema ya creado y datos aprovisionados. |

PRAXIS **no crea** la base, **no ejecuta migraciones** y **no modifica** tablas, datos, triggers ni vistas. Los archivos `docs/db/V1` a `V4` son referencia de solo lectura; no vuelvas a ejecutarlos.

## 2. Clonar el repositorio

El comando es el mismo en Windows PowerShell y en Linux/macOS:

```bash
git clone https://github.com/j0ander/praxisV1.git
cd praxisV1
```

Si `JAVA_HOME` no está definido, ajústalo para la sesión antes de usar el wrapper.

**Windows PowerShell**

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'
.\mvnw.cmd --version
```

**Linux / macOS**

```bash
export JAVA_HOME=/ruta/al/jdk-25
./mvnw --version
```

En Linux/macOS puede ser necesario dar permiso de ejecución al wrapper una sola vez:

```bash
chmod +x ./mvnw
```

## 3. Configurar la conexión a Supabase

React no accede a PostgreSQL: solo consume REST de Quarkus. La conexión se hace mediante **Session Pooler**, puerto **5432** y **SSL**.

### 3.1. Crear el archivo local

Copia la plantilla sin credenciales:

**Windows PowerShell**

```powershell
Copy-Item config\local.properties.example config\local.properties
```

**Linux / macOS**

```bash
cp config/local.properties.example config/local.properties
```

`config/local.properties` está excluido por `.gitignore`. No se versiona, no se comparte y nunca debe contener valores de un proyecto antiguo.

### 3.2. Completar las tres propiedades

Edita `config/local.properties` con los datos del **proyecto Supabase actual**:

| Propiedad | Contenido |
|---|---|
| `quarkus.datasource.jdbc.url` | URL JDBC del Session Pooler, puerto `5432`, con `sslmode=require`. |
| `quarkus.datasource.username` | Usuario `postgres.<referencia-del-proyecto>`. |
| `quarkus.datasource.password` | Contraseña del proyecto actual. |

La URL debe tener el formato general:

```text
jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
```

No uses el host directo `db.<referencia>.supabase.com`, PostgreSQL local, `.env` ni variables de entorno para esta conexión. Quarkus carga el archivo únicamente cuando se pasa `-Dquarkus.config.locations` con una URI absoluta, como se muestra en la sección siguiente.

Hibernate está configurado con `quarkus.hibernate-orm.schema-management.strategy=none`: nunca genera, actualiza ni valida el esquema.

## 4. Iniciar el backend

### 4.1. Modo desarrollo

**Windows PowerShell**

```powershell
.\mvnw.cmd "-Dquarkus.config.locations=$(([Uri](Resolve-Path 'config\local.properties').Path).AbsoluteUri)" quarkus:dev
```

**Linux / macOS**

```bash
CONFIG_URI="file://$(pwd)/config/local.properties"
./mvnw "-Dquarkus.config.locations=$CONFIG_URI" quarkus:dev
```

El backend queda disponible en `http://localhost:8080`. Las rutas REST están en la raíz, sin prefijo `/api`.

### 4.2. Empaquetar sin ejecutar pruebas

**Windows PowerShell**

```powershell
.\mvnw.cmd "-Dquarkus.config.locations=$(([Uri](Resolve-Path 'config\local.properties').Path).AbsoluteUri)" package -DskipTests
```

**Linux / macOS**

```bash
CONFIG_URI="file://$(pwd)/config/local.properties"
./mvnw "-Dquarkus.config.locations=$CONFIG_URI" package -DskipTests
```

### 4.3. Pruebas unitarias (opcional)

Las pruebas usan una base aislada local, no Supabase. Su contraseña tampoco se versiona:

**Windows PowerShell**

```powershell
Copy-Item config\test-local.properties.example config\test-local.properties
# Edita config\test-local.properties con la contrasena de la base aislada local.
.\mvnw.cmd "-Dquarkus.config.locations=$(([Uri](Resolve-Path 'config\test-local.properties').Path).AbsoluteUri)" test
```

**Linux / macOS**

```bash
cp config/test-local.properties.example config/test-local.properties
# Edita config/test-local.properties con la contrasena de la base aislada local.
./mvnw "-Dquarkus.config.locations=file://$(pwd)/config/test-local.properties" test
```

El parámetro `-Dquarkus.config.locations` es obligatorio en ese comando: Maven lo propaga al proceso de pruebas y permite resolver `${praxis.test.datasource.password}` desde `config/test-local.properties`. Si se omite, las pruebas no encontrarán la contraseña.

La base aislada usada por las pruebas escucha en `127.0.0.1:55432` y se llama `praxis_test`; debe existir previamente con su esquema correspondiente. Este README no la aprovisiona. Las pruebas no son necesarias para iniciar la aplicación.

## 5. Iniciar el frontend

Desde la raíz del proyecto:

**Windows PowerShell**

```powershell
cd src\main\webui
npm ci
npm run dev
```

**Linux / macOS**

```bash
cd src/main/webui
npm ci
npm run dev
```

`npm ci` instala exactamente las versiones de `package-lock.json`. No reemplaces ese archivo por `npm install` salvo que actualices las dependencias de forma intencional.

Vite inicia en `http://127.0.0.1:5173` y redirige las rutas `/auth`, `/usuarios`, `/estudiantes`, `/docentes`, `/facultades`, `/carreras`, `/periodos`, `/tiposPractica`, `/empresas` y `/convenios` al backend en `http://127.0.0.1:8080`.

Si el backend está en otra dirección, `PRAXIS_API` es una opción **no requerida** para sobrescribir el destino del proxy:

**Windows PowerShell**

```powershell
$env:PRAXIS_API = 'http://127.0.0.1:8080'
npm run dev
```

**Linux / macOS**

```bash
PRAXIS_API=http://127.0.0.1:8080 npm run dev
```

## 6. Direcciones locales

| Servicio | Dirección |
|---|---|
| Backend Quarkus / REST | `http://localhost:8080` |
| Frontend React / Vite | `http://127.0.0.1:5173` |
| Vista previa del build frontend | `http://127.0.0.1:4173` (`npm run preview`) |

El proyecto no incluye la extensión de health, así que la comprobación de arranque es el mensaje `started in ...` y `Listening on: http://localhost:8080` en la consola de Quarkus.

## 7. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Datasource '<default>' was deactivated ... URL is not set` | `config/local.properties` no existe, está vacío o no se pasó a Quarkus. | Copia la plantilla, completa las tres propiedades y usa el comando con `-Dquarkus.config.locations`. |
| `URI is not hierarchical` | Se usó `-Dquarkus.config.locations=file:config/local.properties`. | Usa la URI absoluta calculada con `Resolve-Path` en PowerShell o `file://$(pwd)/...` en Linux/macOS. |
| `Connection refused` | Host o puerto incorrectos. | Usa el host **pooler** de Supabase y puerto `5432`. No uses el host directo. |
| `password authentication failed` | Usuario o contraseña equivocados, o credenciales de otro proyecto. | Copia los valores del proyecto Supabase actual; usuario `postgres.<referencia>`. |
| `SSL` / `server does not support SSL` | Falta `sslmode=require`. | Agrégalo a la URL JDBC. |
| `JAVA_HOME` no válido o error de versión de clase | No se está usando JDK 25. | Instala JDK 25 y apunta `JAVA_HOME` a él. |
| `mvnw: Permission denied` en Linux/macOS | Falta permiso de ejecución. | Ejecuta `chmod +x ./mvnw`. |
| El puerto `8080` está ocupado | Otro proceso está escuchando. | Detén ese proceso antes de iniciar Quarkus. |
| La página carga pero las peticiones fallan | Backend detenido o proxy incorrecto. | Inicia el backend y verifica que el frontend use `http://127.0.0.1:8080` o `PRAXIS_API`. |
| `npm ci` falla | Node.js antiguo o `package-lock.json` ausente. | Usa Node.js 20.19+ y no elimines `package-lock.json`. |
| Errores de tabla o columna inexistente | La base Supabase no tiene el esquema previo. | No ejecutes migraciones desde la aplicación; aprovisiona Supabase con la estructura esperada antes de iniciar. |
| Las pruebas fallan por `praxis.test.datasource.password` | Falta `config/test-local.properties` o no se pasó en el comando. | Copia `config/test-local.properties.example`, completa el valor y ejecuta `test` con `-Dquarkus.config.locations` como en §4.3. |
| `config/local.properties` aparece como archivo a subir | La regla de `.gitignore` no se aplicó. | Después de inicializar Git, verifica con `git check-ignore -v config/local.properties`. Nunca lo incluyas en un commit. |

## 8. Seguridad y publicación

- `.gitignore` excluye `.env`, `*.env`, `config/local.properties`, `config/test-local.properties`, `target/`, `node_modules/`, `src/main/webui/dist/`, logs, temporales y configuraciones personales de IDE.
- Solo se versionan las plantillas `config/local.properties.example` y `config/test-local.properties.example`, sin credenciales reales.
- No hay contraseñas, tokens, claves privadas ni cadenas de conexión reales en los archivos preparados para publicación.
- El wrapper `.mvn/wrapper/maven-wrapper.jar` debe permanecer versionado; no es un artefacto compilado.
- La inicialización del primer administrador sigue deshabilitada por defecto (`praxis.seguridad.admin-inicial.enabled=false`). No es necesaria para arrancar y no debe habilitarse contra Supabase salvo una decisión explícita; cuando se habilita, sus valores se pasan solo al entorno de esa ejecución y nunca se versionan.

## 9. Módulos implementados

- Backend: autenticación y sesión (`/auth`), estructura académica (facultades, carreras, periodos y tipos de práctica), usuarios y perfiles, empresas y convenios, miembros y tutores empresariales, y consulta de la empresa del representante (`GET /empresas/miEmpresa`, API-086).
- Frontend: inicio de sesión, panel de inicio, administración de usuarios, listados de estudiantes y docentes, listado y detalle de empresas y la pantalla «Mi empresa» con sus pestañas de datos, convenios, miembros y tutores.
- Contratos y permisos vigentes: `docs/CONTRATOS_REST.md` y `docs/SEGURIDAD.md`.

**No implementado:** API-004 y la pantalla UI-002 de cambio de contraseña; el indicador `debeCambiarPassword` se registra pero no restringe el acceso. Tampoco están implementados los módulos de ofertas, postulaciones, prácticas ni auditoría (API-054 en adelante) ni sus pantallas. No se inicien esas tareas sin autorización.

## 10. Estado y seguimiento

- Seguimiento de estados: `docs/TAREAS.md` §6.1 y §6.2; la fila global `TK-001 a TK-035 | Por asignar | Pendiente` se conserva sin cambios.
- Evidencias, inventario de cambios y limitaciones consolidadas: `docs/TAREAS.md` §6.3. La verificación de conectividad posterior a Fase 5 está en §6.3.4.
- Decisiones de arquitectura y seguridad vigentes: `docs/ARQUITECTURA.md` y `docs/SEGURIDAD.md`.
- Sin commits, push ni merge en esta entrega.
