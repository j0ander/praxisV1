# PRAXIS

Sistema web para la gestión de prácticas preprofesionales. La entrega actual cubre la base técnica y las fases 1 a 5 (`TK-001` a `TK-020`), incluidas las correcciones de Fase 5. TK-018, TK-019 y TK-020 están **aprobados formalmente**, con **limitaciones documentadas** de integración y validación manual; consulta `docs/TAREAS.md` §6.1 y §6.3.

**Supabase es la única base de datos.** No se necesita PostgreSQL local, Docker ni Testcontainers. No existe infraestructura de pruebas automatizadas en el proyecto.

## 1. Requisitos

| Requisito | Versión / detalle |
|---|---|
| Git | Para clonar el repositorio. |
| JDK | 25 (LTS 25.0.2), disponible en PATH. |
| Maven | No requiere instalación: se usa el Maven Wrapper incluido (Maven 3.9.11). |
| Quarkus | 3.40.1, fijado en `pom.xml`. |
| Node.js | 20.19+ para el frontend (`src/main/webui`). |
| npm | Incluido con Node.js; se usa `npm ci` con `package-lock.json`. |
| Supabase | Proyecto PostgreSQL 15+ existente, con el esquema ya creado y datos aprovisionados. |

PRAXIS **no crea** la base, **no ejecuta migraciones** y **no modifica** tablas, datos, triggers ni vistas. Los archivos `docs/db/V1` a `V4` son referencia de solo lectura; no vuelvas a ejecutarlos.

## 2. Clonar el repositorio

```bash
git clone https://github.com/j0ander/praxisV1.git
cd praxisV1
```

Verifica la versión de Java con el wrapper:

```powershell
.\mvnw.cmd --version
```

En Linux/macOS puede ser necesario dar permiso de ejecución una sola vez: `chmod +x ./mvnw`.

## 3. Configurar la conexión privada a Supabase

React no accede a PostgreSQL: solo consume REST de Quarkus. La conexión usa **Session Pooler**, puerto **5432** y **SSL** (`sslmode=require`).

1. Copia la plantilla sin credenciales:

   ```powershell
   Copy-Item config\local.properties.example config\local.properties
   ```

2. Edita `config/local.properties` con los datos del proyecto Supabase actual:

   | Propiedad | Contenido |
   |---|---|
   | `quarkus.datasource.jdbc.url` | URL JDBC del Session Pooler, puerto `5432`, con `sslmode=require`. |
   | `quarkus.datasource.username` | Usuario `postgres.<referencia-del-proyecto>`. |
   | `quarkus.datasource.password` | Contraseña del proyecto actual. |

`config/local.properties` está excluido por `.gitignore`: **no se versiona, no se comparte y no debe exponerse en consolas, informes ni logs**. No uses host directo, `.env` ni variables de entorno para esta conexión.

Quarkus carga el archivo automáticamente porque `application.properties` declara:

```properties
quarkus.config.locations=config/local.properties
quarkus.datasource.devservices.enabled=false
quarkus.hibernate-orm.schema-management.strategy=none
```

La ruta es relativa a la raíz del proyecto, por lo que no hace falta pasar ningún parámetro `-Dquarkus.config.locations`. Dev Services está desactivado: Quarkus nunca intenta iniciar PostgreSQL mediante Docker. Hibernate nunca genera ni valida el esquema.

## 4. Iniciar el backend

Desde la raíz del proyecto, en PowerShell:

```powershell
.\mvnw.cmd quarkus:dev
```

El backend queda disponible en `http://localhost:8080`. Las rutas REST están en la raíz, sin prefijo `/api`. Durante el arranque no se ejecutan pruebas ni se intenta conectar con PostgreSQL local o Docker.

Para compilar sin pruebas:

```powershell
.\mvnw.cmd -DskipTests compile
```

No hay comandos de prueba: la infraestructura de tests y sus dependencias fueron eliminadas.

### Login básico del prototipo

El backend expone `POST /usuarios/login`: recibe `{ "correo": "...", "password": "..." }`, verifica la cuenta activa y el hash BCrypt con `BcryptUtil.matches`, y responde 200 con `id`, `nombres`, `apellidos`, `correo` y `roles`. Las credenciales incorrectas responden 401 con un mensaje genérico; no se revela si el correo existe.

La interfaz conserva esos datos solo en `useState`: no hay sesión de servidor, cookies, JWT ni almacenamiento en el navegador. Al recargar la página vuelve a pedirse el login. El menú y las rutas se filtran por rol únicamente de forma visual; la API REST sigue sin autorización. No se incluyen credenciales de prueba en este archivo ni en el repositorio.

## 5. Iniciar el frontend

Desde la raíz del proyecto:

```powershell
cd src\main\webui
npm ci
npm run dev
```

Vite inicia en `http://127.0.0.1:5173`. Sin usuario en memoria se muestra `/login`; después del login se habilitan `/inicio` y las rutas permitidas por el rol: `/admin/usuarios`, `/admin/usuarios/nuevo`, `/admin/usuarios/:id`, `/admin/facultades`, `/admin/carreras`, `/admin/periodos`, `/admin/tipos-practica`, `/estudiantes`, `/docentes`, `/empresas` y `/empresas/:id`. Las peticiones REST van a `http://127.0.0.1:8080`; si el backend está en otra dirección, `PRAXIS_API` permite sobrescribir el destino del proxy.

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
| `Datasource '<default>' was deactivated ... URL is not set` | `config/local.properties` no existe o está incompleto. | Copia la plantilla y completa las tres propiedades; reinicia con `.\mvnw.cmd quarkus:dev`. |
| `Connection refused` | Host o puerto incorrectos. | Usa el host **pooler** de Supabase y puerto `5432`; no uses el host directo. |
| `password authentication failed` | Usuario o contraseña equivocados, o credenciales de otro proyecto. | Copia los valores del proyecto Supabase actual; usuario `postgres.<referencia>`. |
| `SSL` / `server does not support SSL` | Falta `sslmode=require` en la URL. | Agrégalo a la URL JDBC en `config/local.properties`. |
| Versión de Java incorrecta | `java -version` no muestra 25. | Instala JDK 25 y asegúrate de que `java` esté en PATH antes de ejecutar el wrapper. |
| `mvnw: Permission denied` en Linux/macOS | Falta permiso de ejecución. | Ejecuta `chmod +x ./mvnw`. |
| El puerto `8080` está ocupado | Otro proceso está escuchando. | Detén ese proceso antes de iniciar Quarkus. |
| La página carga pero las peticiones fallan | Backend detenido o proxy incorrecto. | Inicia el backend y verifica que el frontend use `http://127.0.0.1:8080` o `PRAXIS_API`. |
| Errores de tabla o columna inexistente | La base Supabase no tiene el esquema previo. | No ejecutes migraciones desde la aplicación; aprovisiona Supabase con la estructura esperada antes de iniciar. |
| `config/local.properties` aparece como archivo a subir | La regla de `.gitignore` no se aplicó. | Verifica con `git check-ignore -v config/local.properties`. Nunca lo incluyas en un commit. |

## 8. Seguridad y publicación

- `.gitignore` excluye `.env`, `*.env`, `config/local.properties`, `target/`, `node_modules/`, `src/main/webui/dist/`, logs, temporales y configuraciones personales de IDE.
- Solo se versiona la plantilla `config/local.properties.example`, sin credenciales reales.
- No hay contraseñas, tokens, claves privadas ni cadenas de conexión reales en los archivos preparados para publicación.
- El wrapper `.mvn/wrapper/maven-wrapper.jar` debe permanecer versionado; no es un artefacto compilado.
- No hay administrador inicial ni credenciales versionadas: la aplicación no crea cuentas en el arranque. El login básico verifica credenciales existentes en Supabase y no reintroduce sesiones ni autorización (`SEGURIDAD.md`).

## 9. Módulos implementados

- Backend: login básico (`POST /usuarios/login`), estructura académica (facultades, carreras, periodos y tipos de práctica), usuarios y perfiles, empresas y convenios, y miembros y tutores empresariales. No hay rutas `/auth` ni `GET /empresas/miEmpresa` (API-086).
- Frontend: pantalla de login, panel de inicio con accesos por rol, administración de usuarios, listados de estudiantes y docentes, y listado y detalle institucional de empresas. No hay pantalla «Mi empresa».
- Contratos y estado de seguridad: `docs/CONTRATOS_REST.md` y `docs/SEGURIDAD.md`; matriz visual de roles y módulos en `docs/INTERFACES.md` §2.

**No vigente / pendiente a futuro:** API-001 a API-004, API-030, API-086, UI-002, sesiones, cookies, autorización real por rol, intentos fallidos, bloqueos, cambio obligatorio de contraseña y recuperación. UI-001 está vigente solo como login básico de prototipo. El indicador `debeCambiarPassword` se conserva en la base pero no restringe el acceso. BCrypt se usa en el alta, el restablecimiento y la verificación de contraseñas. Tampoco están implementados los módulos de ofertas, postulaciones, prácticas ni auditoría (API-054 en adelante) ni sus pantallas. No se inicien esas tareas sin autorización.

## 10. Estado y seguimiento

- Seguimiento de estados: `docs/TAREAS.md` §6.1 y §6.2; la fila global `TK-001 a TK-035 | Por asignar | Pendiente` se conserva sin cambios.
- Evidencias, inventario de cambios y limitaciones consolidadas: `docs/TAREAS.md` §6.3. La verificación de conectividad posterior a Fase 5 está en §6.3.4.
- Decisiones de arquitectura y estado de seguridad: `docs/ARQUITECTURA.md` y `docs/SEGURIDAD.md`.
- Sin commits, push ni merge en esta entrega.
