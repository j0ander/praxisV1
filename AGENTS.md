# AGENTS.md

## 1. Contexto del proyecto

PRAXIS es un sistema web que gestiona las prácticas preprofesionales de una institución de educación superior: ofertas, postulaciones, formalización, asignación de tutores y auditoría. Roles: administrador, coordinador, estudiante, docente tutor, tutor empresarial, representante empresarial y autoridad.

**Alcance del MVP:** usuarios y acceso, estructura académica, perfiles, empresas y convenios, ofertas, postulaciones, asignación de docentes tutores y auditoría. Fuera del MVP: actividades y horas, informes, visitas, incidencias, evaluaciones, certificados y reportes avanzados.

## 2. Stack tecnológico

Java 25 · Quarkus 3.40.1 · Quarkus REST (JSON) · CDI (ArC) · Hibernate ORM Panache (patrón Repository) · PostgreSQL 15+ · BCrypt · Maven · React + Vite (`src/main/webui`).

## 3. Arquitectura obligatoria

```
Resource -> Service -> Repository -> Entity -> PostgreSQL
```

- Paquete base `ec.edu.praxis`: `web/resource`, `application/service`, `infrastructure/repository`, `domain/model`. Paquetes planos, una clase por archivo.
- El módulo `Facultad` es el patrón de referencia (`docs/ARQUITECTURA.md` §10). Copiar su estilo.
- Inyección con `@Inject` sobre el campo; `@ApplicationScoped` en Service y Repository; sin interfaces.
- `@Transactional` solo en los métodos de escritura del Service.
- Sin DTO independientes: se usan entidades; las estructuras de entrada o salida compuestas son clases internas del Resource.
- Entidades: clases JPA simples, `@Column` explícito, `@Id @GeneratedValue(IDENTITY)`, estados como `enum` con `@Enumerated(STRING)`, claves foráneas como `Long`.

## 4. Restricciones

- No modificar el esquema de la base de datos, no inventar tablas ni columnas, no crear migraciones, no ejecutar scripts que alteren la base.
- Consultar `docs/MODELO_DATOS.md` antes de crear o modificar entidades. Hibernate nunca genera ni cambia el esquema.
- No agregar capas, interfaces ni abstracciones innecesarias. No agregar dependencias al `pom.xml` sin aprobación.
- No implementar funcionalidades fuera del MVP ni asumir aprobado lo que solo aparece como propuesta o "A confirmar".
- No exponer credenciales ni secretos. `hashPassword`, `intentosFallidos` y `bloqueadoHasta` nunca se serializan.
- No fijar valores de parámetros pendientes (horas requeridas, nota mínima, ponderaciones, límite de intentos, plazos): son datos o configuración.
- Los triggers de la base son una segunda barrera, pero la API siempre autoriza y valida.
- El cliente nunca envía estado, fechas de resolución, quién resolvió, `empresaId` ni el usuario actor.
- No escribir columnas gestionadas por triggers (snapshots, fechas de publicación y resolución de ofertas, `created_at`, `updated_at`). Si se necesita el valor final, relear tras `flush()`.
- Toda escritura auditable fija el contexto de sesión en la transacción (`praxis.usuario_id`, `praxis.motivo`, `praxis.sistema`); ver `docs/MODELO_DATOS.md` §7.3.

## 5. Base de datos

PostgreSQL ya existe y es la fuente de verdad. DDL de solo lectura en `docs/db/` (`V1` a `V4`); su presencia no autoriza a modificarlo. Claves `GENERATED ALWAYS`: el código nunca asigna `id`. Zona de negocio `America/Guayaquil`; las columnas `TIMESTAMPTZ` se leen como `OffsetDateTime`.

## 6. Convenciones generales

- Nombres del dominio en español; sufijos técnicos en inglés (`Repository`, `Service`, `Resource`).
- Métodos: `listar`, `buscar`, `crear`, `actualizar` y nombres de intención en el Repository.
- Errores de negocio como `RuntimeException`; provocan rollback. Formato de errores en `docs/CONTRATOS_REST.md` §6.
- Códigos de rol: `ADMINISTRADOR`, `COORDINADOR`, `ESTUDIANTE`, `DOCENTE`, `TUTOR_EMPRESARIAL`, `REPRESENTANTE_EMPRESA`, `AUTORIDAD`.
- Respuestas: 401 sin sesión, 403 sin rol, 404 registro ajeno o fuera de ámbito, 409 regla de negocio o duplicado.

## 7. Comandos del proyecto

Pendientes de verificación contra el proyecto real (`docs/TAREAS.md` TK-001).

```bash
./mvnw quarkus:dev      # desarrollo (Windows: mvnw.cmd quarkus:dev)
./mvnw test             # pruebas
./mvnw package          # construir
```

Comandos del frontend (`src/main/webui`): por verificar.

## 8. Configuración local

La conexión se lee desde `config/local.properties`, un archivo local no versionado (excluido en `.gitignore`) que se carga explícitamente con `-Dquarkus.config.locations` apuntando a la URI absoluta calculada en el arranque. No se usan variables de entorno ni `.env` para la conexión.

| Propiedad local | Propósito |
|---|---|
| `quarkus.datasource.jdbc.url` | URL JDBC del Session Pooler de Supabase, con SSL. |
| `quarkus.datasource.username` | Usuario del proyecto Supabase actual. |
| `quarkus.datasource.password` | Contraseña del proyecto Supabase actual. |

`application.properties` solo declara `quarkus.datasource.db-kind=postgresql`; la URL, el usuario y la contraseña se toman directamente del archivo local. La plantilla sin valores es `config/local.properties.example`. En Windows, el comando de arranque calcula la URI absoluta: `.\mvnw.cmd "-Dquarkus.config.locations=$(([Uri](Resolve-Path 'config\local.properties').Path).AbsoluteUri)" quarkus:dev`.

## 9. Documentos de referencia (consultar por tarea)

| Tipo de tarea | Documentos |
|---|---|
| Comprender el producto | `PRODUCTO.md` |
| Crear o modificar requisitos | `PRODUCTO.md`, `REQUISITOS.md` |
| Implementar una regla de negocio | `REGLAS_NEGOCIO.md`, `REQUISITOS.md` |
| Implementar un flujo completo | `FLUJOS.md`, `REGLAS_NEGOCIO.md` |
| Crear o modificar una entidad | `MODELO_DATOS.md`, `ARQUITECTURA.md` |
| Implementar persistencia | `MODELO_DATOS.md`, `ARQUITECTURA.md` |
| Crear un endpoint | `SEGURIDAD.md`, `CONTRATOS_REST.md`, `ARQUITECTURA.md` |
| Implementar autenticación | `SEGURIDAD.md`, `ARQUITECTURA.md` |
| Crear una pantalla | `INTERFACES.md`, `FLUJOS.md`, `CONTRATOS_REST.md` |
| Modificar permisos | `SEGURIDAD.md`, `REGLAS_NEGOCIO.md` |
| Planificar una funcionalidad | `TAREAS.md` y los documentos relacionados |

Todos los documentos están en `docs/`.

## 10. Instrucciones de trabajo

1. Leer este archivo, identificar el tipo de tarea y consultar solo los documentos necesarios.
2. Revisar el código existente antes de modificarlo y seguir el módulo de referencia.
3. Respetar los identificadores (`RF-`, `RN-`, `FL-`, `UI-`, `API-`, `TK-`) y los criterios de aceptación de la tarea.
4. Si la información es insuficiente, contradictoria o está marcada "A confirmar", detener la decisión afectada y pedir aclaración; no inventar una solución.
5. No resolver contradicciones entre documentos en silencio: señalarlas.
