# Contratos REST

## 1. Convenciones generales

### 1.1. Formato

- JSON en UTF-8 (`Content-Type: application/json`).
- Nombres de campos en `camelCase`, iguales a los atributos de las entidades.
- Identificadores numéricos (`Long`). Fechas calendario `yyyy-MM-dd`; fechas con hora en ISO 8601 con zona (`OffsetDateTime`).
- Las fechas automáticas compatibles que se generan en código usan `LocalDate.now()`. No se configura zona horaria de negocio en la aplicación: las columnas `TIMESTAMPTZ` gestionadas por la base (`created_at`, `updated_at`) se conservan y se leen como `OffsetDateTime`, sin convertirlas a `LocalDate`.
- Los listados no se paginan en el MVP; la paginación es una mejora futura (CR-7).
- Los filtros se envían como parámetros de consulta y son opcionales.

### 1.2. Rutas

Se sigue el patrón del módulo de referencia (`ARQUITECTURA.md` §10), y las acciones de negocio se expresan como `POST` sobre el recurso:

| Operación | Patrón |
|---|---|
| Listar | `GET /recurso/listarTodos` |
| Buscar | `GET /recurso/buscar/{id}` |
| Crear | `POST /recurso` → 201 |
| Actualizar | `PUT /recurso` → 200 (el id va en el cuerpo) |
| Acción de negocio | `POST /recurso/{id}/accion` → 200 con el recurso actualizado |

Las rutas se escriben **relativas a la raíz, sin prefijo `/api`**, según `ARQUITECTURA.md` §10 (CR-1). El módulo de referencia usa `/facultades`. Las rutas del frontend React (por ejemplo `/empresas`) son rutas de la interfaz y no colisionan con los recursos JSON del backend.

### 1.3. Roles: referencia de dominio, no autorización

La columna "Roles" de las tablas de endpoints usa las abreviaturas ADM (administrador), COO (coordinador), EST (estudiante), DOC (docente tutor), TUT (tutor empresarial), REP (representante empresarial) y AUT (autoridad). **No está vigente como control de acceso**: no hay sesión, identidad de actor, `@RolesAllowed` ni verificación de permisos en la API. Los roles se conservan como datos de dominio (`rol`, `usuario_rol`) y el login básico los devuelve solo para que React oculte o muestre pantallas. Las reglas académicas y de negocio de cada operación sí siguen vigentes.

### 1.4. Reglas de entrada y salida

1. No hay usuario autenticado ni actor en la API: no se usa `SecurityIdentity` (`praxis.usuarioId`) y el cliente no puede enviar un identificador que determine quién es el actor. **Aclaración (TK-018):** no se envían estado, fechas de resolución, quién resolvió, la empresa derivada del ámbito de un actor ni `creadaPorUsuarioId`. En `Convenio`, `empresaId` es la empresa titular del convenio y, conforme a §4.1 (API-044 y API-045), se envía como dato obligatorio y se valida contra `empresa`. **Aclaración (TK-019):** en API-048 a API-053 la empresa se identifica exclusivamente por el parámetro de ruta `{empresaId}`; `empresaId` no se envía en el cuerpo. Identifica la empresa titular de la membresía o del tutor, no a un usuario actor; el Service valida su existencia y sus reglas de negocio.
2. **Entrada:** las entidades se reciben directamente cuando no contienen datos sensibles (`ARQUITECTURA.md` §8); las estructuras de entrada compuestas se declaran como clases internas del Resource. **Salida:** el Service gestiona la lógica de negocio y el Resource construye respuestas REST explícitas como clases internas; ninguna entidad JPA ni proyección de vista se devuelve ni se serializa directamente al cliente, ni en operaciones individuales ni en listados. No se introducen DTOs independientes ni mappers adicionales. **TK-019:** las entradas de API-049 y API-050 son la entidad `EmpresaUsuario` y las de API-052 y API-053 la entidad `TutorEmpresarial` (ninguna contiene datos sensibles); el Service valida los campos inmutables y el Resource construye las respuestas con las clases internas `MiembroResumen` y `TutorEmpresarialResponse`. No se declaran clases internas de entrada ni DTOs independientes para estas operaciones.
3. `Usuario` nunca se devuelve como entidad: `hashPassword`, `intentosFallidos` y `bloqueadoHasta` nunca se serializan; se usa una estructura propia.
4. El contexto de actor (`praxis.usuario_id`, `praxis.motivo`) ya no se fija desde la aplicación (`CONTEXTO_ACTOR.md`). Los triggers de la base que lo requieren pueden rechazar la escritura; no se altera el esquema ni se desactivan triggers. **TK-018:** las escrituras de `empresa` y `convenio` no tenían un trigger que exigiera este contexto. **TK-019:** API-049 y API-050 escriben `empresa_usuario`, donde los triggers `validar_empresa_usuario` y `auditar_empresa_usuario` leen `praxis.usuario_id`. No se introduce `ContextoTransaccional`, contexto global ni limpieza manual.
5. Algunos valores guardados pueden diferir de los enviados por acción de los triggers; los endpoints devuelven el valor final.
6. No hay `DELETE` salvo en facultad (módulo de referencia) y perfil de oferta. En lo demás se desactiva con `PUT`. El `DELETE` de facultad se conserva en el contrato (API-009), respeta la integridad referencial y devuelve 409 si existen carreras asociadas; la interfaz no lo expone y solo ofrece desactivar.
7. La concurrencia básica del MVP usa `@Version` en `postulacion` y `practica`, comprueba la disponibilidad de plazas dentro de la transacción y responde 409 `CONFLICTO_CONCURRENCIA` cuando otra operación ocupó la plaza o cambió el registro. Aceptar una postulación y crear su práctica se ejecutan en una misma transacción. No se usan mecanismos avanzados de concurrencia.
8. No se crean catálogos ni requisitos ficticios: las validaciones se aplican sobre los registros existentes y la ausencia de datos se gestiona con listados vacíos. La administración de requisitos sigue fuera del MVP (CR-6).

### 1.5. Errores comunes a todos los endpoints

| HTTP | Cuándo |
|---|---|
| 400 | Cuerpo o parámetros con formato inválido. |
| 404 | Registro inexistente. |
| 500 | Error interno, sin detalles. |

El único 401 del proyecto corresponde al login básico. No se devuelve 403: no hay autorización por rol en la API. En las tablas solo se listan los errores adicionales (409).

## 2. Login básico de prototipo (sin sesión)

`POST /usuarios/login` recibe `{ "correo": "...", "password": "..." }`, verifica la cuenta activa y la contraseña con `BcryptUtil.matches`, y responde 200 con `LoginResponse`: `id`, `nombres`, `apellidos`, `correo` y `roles`. No crea cookie, token, sesión de servidor ni identidad de petición: React conserva la respuesta solo en memoria mientras la aplicación está abierta.

Las credenciales inválidas responden 401 `CREDENCIALES_INVALIDAS` con el mensaje genérico «Correo o contraseña incorrectos.», sin distinguir correo inexistente, cuenta inactiva ni contraseña incorrecta. El hash nunca se devuelve ni se registra.

API-001 a API-004 (`/auth/login`, `/auth/logout`, `/auth/yo` y `/auth/cambiarPassword`) siguen **no vigentes** y quedan pendientes a futuro. No hay rutas `/auth`, ni cookies, ni `SesionResponse`, `CredencialesRequest` ni `CambioPasswordRequest`. Tampoco hay intentos fallidos, bloqueos ni caducidades de sesión.

La tabla `usuario` mantiene sus columnas existentes porque el esquema no se cambia; las columnas de intentos, bloqueo y `ultimo_login` no se usan (`SEGURIDAD.md`, `MODELO_DATOS.md` §4.2).

## 3. Endpoints por módulo

### 3.1. Estructura académica

| ID | Método y ruta | Descripción | Roles | Entrada | Respuesta | Errores | Refs |
|---|---|---|---|---|---|---|---|
| API-005 | `GET /facultades/listarTodos` | Lista facultades. | Todos | – | 200 `FacultadResponse[]` | – | RF-004 |
| API-006 | `GET /facultades/buscar/{id}` | Obtiene una facultad. | Todos | – | 200 `FacultadResponse` | – | RF-004 |
| API-007 | `POST /facultades` | Crea una facultad. | ADM | `Facultad` | 201 `FacultadResponse` | 409 código o nombre duplicado | RF-004 |
| API-008 | `PUT /facultades` | Actualiza o desactiva (`activa`). | ADM | `Facultad` | 200 `FacultadResponse` | 409 duplicado | RF-004 |
| API-009 | `DELETE /facultades/eliminar/{id}` | Se conserva como patrón de referencia; respeta la integridad referencial. La UI no lo expone: solo ofrece desactivar (API-008). | ADM | – | 204 | 409 `REGLA_NEGOCIO` tiene carreras asociadas | RF-004 |
| API-010 | `GET /carreras/listarTodos?facultadId=` | Lista carreras. | Todos | – | 200 `CarreraResponse[]` | – | RF-004 |
| API-011 | `GET /carreras/buscar/{id}` | Obtiene una carrera. | Todos | – | 200 `CarreraResponse` | – | RF-004 |
| API-012 | `POST /carreras` | Crea una carrera. | ADM | `Carrera` | 201 `CarreraResponse` | 409 código o nombre duplicado en la facultad | RF-004; RN-005 |
| API-013 | `PUT /carreras` | Actualiza o desactiva. | ADM | `Carrera` | 200 `CarreraResponse` | 409 duplicado | RF-004 |
| API-014 | `GET /periodos/listarTodos` | Lista periodos. | Todos | – | 200 `PeriodoAcademicoResponse[]` | – | RF-004 |
| API-015 | `GET /periodos/buscar/{id}` | Obtiene un periodo. | Todos | – | 200 `PeriodoAcademicoResponse` | – | RF-004 |
| API-016 | `POST /periodos` | Crea un periodo (inactivo). | ADM | `PeriodoAcademico` | 201 | 409 código duplicado; fin anterior al inicio | RF-004; RN-006 |
| API-017 | `PUT /periodos` | Actualiza datos del periodo. | ADM | `PeriodoAcademico` | 200 | 409 fechas incoherentes | RF-004 |
| API-018 | `POST /periodos/{id}/activar` | Activa el periodo. | ADM | – | 200 | 409 ya existe otro activo | RN-006 |
| API-019 | `POST /periodos/{id}/desactivar` | Desactiva el periodo. | ADM | – | 200 | – | RN-006 |
| API-020 | `GET /tiposPractica/listarTodos` | Lista tipos de práctica. | Todos | – | 200 `TipoPracticaResponse[]` | – | RF-009 |
| API-021 | `GET /tiposPractica/buscar/{id}` | Obtiene un tipo. | Todos | – | 200 `TipoPracticaResponse` | – | RF-009 |
| API-022 | `POST /tiposPractica` | Crea un tipo. | ADM | `TipoPractica` | 201 | 409 nombre duplicado; horas o nota fuera de rango | RN-022 |
| API-023 | `PUT /tiposPractica` | Actualiza o desactiva. | ADM | `TipoPractica` | 200 | 409 duplicado o fuera de rango | RN-022 |

Nota: `FacultadResponse`, `CarreraResponse`, `PeriodoAcademicoResponse` y `TipoPracticaResponse` son estructuras de salida construidas por el Resource correspondiente (§4.2); la entrada sigue siendo la entidad cuando no contiene datos sensibles (`ARQUITECTURA.md` §8).

### 3.2. Usuarios y perfiles

| ID | Método y ruta | Descripción | Roles | Entrada | Respuesta | Errores | Refs |
|---|---|---|---|---|---|---|---|
| API-024 | `GET /usuarios/listarTodos?rol=&activo=&texto=` | Lista usuarios sin datos de credenciales. | ADM (ámbito) | – | 200 `UsuarioResumen[]` | – | RF-001 |
| API-025 | `GET /usuarios/buscar/{id}` | Detalle con roles y carreras. | ADM (ámbito) | – | 200 `UsuarioResumen` | – | RF-001 |
| API-026 | `POST /usuarios` | Crea el usuario con contraseña temporal; `debeCambiarPassword` = true. | ADM | `UsuarioRequest` | 201 `UsuarioResumen` | 409 correo o documento duplicado o inválido | RF-001; RN-001 |
| API-027 | `PUT /usuarios` | Actualiza datos personales. | ADM | `UsuarioRequest` (con `id`) | 200 `UsuarioResumen` | 409 duplicado | RF-001 |
| API-028 | `POST /usuarios/{id}/activar` | Reactiva la cuenta. | ADM | – | 200 | – | RF-001 |
| API-029 | `POST /usuarios/{id}/desactivar` | Desactiva la cuenta. | ADM | – | 200 | – | RF-001 |
| API-030 | `POST /usuarios/{id}/restablecerPassword` | **No vigente; pendiente a futuro.** Fijaba contraseña temporal y `debeCambiarPassword`. | ADM | `RestablecerPasswordRequest` | 204 | – | RF-003 |
| API-031 | `PUT /usuarios/{id}/roles` | Reemplaza los roles. Al asignar `DOCENTE` el perfil se crea automáticamente. | ADM | `AsignarRolesRequest` | 200 | 409 rol inexistente | RF-002; RN-003, RN-004 |
| API-032 | `PUT /usuarios/{id}/carreras` | Reemplaza las carreras del ámbito. | ADM | `AsignarCarrerasRequest` | 200 | 409 carrera inexistente | RF-002; RN-003 |
| API-033 | `GET /estudiantes/listarTodos?carreraId=` | Lista perfiles de estudiante. | ADM, COO (ámbito) | – | 200 `EstudianteResponse[]` | – | RF-005 |
| API-034 | `GET /estudiantes/buscar/{id}` | Obtiene un perfil. | ADM, COO (ámbito) | – | 200 `EstudianteResponse` | – | RF-005 |
| API-035 | `POST /estudiantes` | Crea el perfil de un usuario. | ADM, COO (ámbito) | `Estudiante` | 201 | 409 perfil ya existente; carrera inactiva | RF-005; RN-004 |
| API-036 | `PUT /estudiantes` | Actualiza el perfil. | ADM, COO (ámbito) | `Estudiante` | 200 | 409 código duplicado | RF-005 |
| API-037 | `GET /docentes/listarTodos` | Lista docentes (para asignar tutores). | ADM, COO | – | 200 `DocenteResponse[]` | – | RF-005 |
| API-038 | `PUT /docentes` | Actualiza código y dedicación. | ADM, COO (ámbito) | `Docente` | 200 | 409 código duplicado | RF-005 |

### 3.3. Empresas

| ID | Método y ruta | Descripción | Roles | Entrada | Respuesta | Errores | Refs |
|---|---|---|---|---|---|---|---|
| API-039 | `GET /empresas/listarTodos?texto=` | Lista empresas. | ADM, COO | – | 200 `EmpresaResponse[]` | – | RF-007 |
| API-040 | `GET /empresas/buscar/{id}` | Obtiene una empresa. | ADM, COO, REP (la propia) | – | 200 `EmpresaResponse` | – | RF-007 |
| API-041 | `POST /empresas` | Registra una empresa. | ADM, COO | `Empresa` | 201 | 400 `DATOS_INVALIDOS` si el RUC falta o tiene formato inválido; 409 `DUPLICADO` si ya está registrado | RF-007; RN-008 |
| API-042 | `PUT /empresas` | Actualiza o desactiva. | ADM, COO, REP (`ADMIN_EMPRESA`, la propia) | `Empresa` | 200 | 400 `DATOS_INVALIDOS` si el RUC falta o tiene formato inválido; 409 `DUPLICADO` si ya está registrado | RF-007 |
| API-043 | `GET /convenios/listarPorEmpresa/{empresaId}` | Convenios con estado efectivo (incluye `VENCIDO`). | ADM, COO, REP (la propia) | – | 200 `ConvenioResumen[]` | – | RF-008 |
| API-044 | `POST /convenios` | Registra un convenio `VIGENTE`. | ADM, COO | `Convenio` | 201 | 409 fechas incoherentes; número duplicado | RF-008; RN-010 |
| API-045 | `PUT /convenios` | Actualiza datos del convenio. | ADM, COO | `Convenio` | 200 | 409 fechas incoherentes | RF-008 |
| API-046 | `POST /convenios/{id}/suspender` | Pasa a `SUSPENDIDO`. | ADM, COO | – | 200 | – | RN-010 |
| API-047 | `POST /convenios/{id}/terminar` | Pasa a `TERMINADO`. | ADM, COO | – | 200 | – | RN-010 |
| API-048 | `GET /empresas/{empresaId}/miembros` | Lista miembros de la empresa. | ADM, COO, REP (`ADMIN_EMPRESA`, la propia) | – | 200 `MiembroResumen[]` | 404 si la empresa o el recurso no existe | RF-007 |
| API-049 | `POST /empresas/{empresaId}/miembros` | Vincula un usuario activo. | ADM, COO, REP (`ADMIN_EMPRESA`, la propia) | `EmpresaUsuario` | 201 `MiembroResumen` | 400 `activo=false`, campo inmutable u otro dato inválido; 404 empresa o usuario inexistente; 409 `REGLA_NEGOCIO` (empresa inactiva, usuario inactivo o sin rol REP, primer miembro no ADMIN); 409 `DUPLICADO` | RF-007; RN-009 |
| API-050 | `PUT /empresas/{empresaId}/miembros/{id}` | Cambia rol de empresa o activa/desactiva. | ADM, COO, REP (`ADMIN_EMPRESA`, la propia) | `EmpresaUsuario` | 200 `MiembroResumen` | 400 campo inmutable u otro dato inválido; 404 membresía ajena a la empresa; 409 `REGLA_NEGOCIO` si la empresa quedaría sin administrador activo | RN-009 |
| API-051 | `GET /empresas/{empresaId}/tutores` | Lista tutores empresariales de la empresa. | ADM, COO, REP (la propia) | – | 200 `TutorEmpresarialResponse[]` | 404 si la empresa o el recurso no existe | RF-007 |
| API-052 | `POST /empresas/{empresaId}/tutores` | Registra un tutor de la empresa. | ADM, COO | `TutorEmpresarial` | 201 `TutorEmpresarialResponse` | 400 campo inmutable u otro dato inválido; 404 empresa o usuario inexistente; 409 `REGLA_NEGOCIO` (usuario inactivo o sin rol `TUTOR_EMPRESARIAL` activo); 409 `DUPLICADO` si ya es tutor de otra empresa | RF-007; RN-009 |
| API-053 | `PUT /empresas/{empresaId}/tutores` | Actualiza el cargo del tutor. | ADM, COO | `TutorEmpresarial` | 200 `TutorEmpresarialResponse` | 400 campo inmutable u otro dato inválido; 404 si el tutor no pertenece a la empresa de la ruta | RF-007 |

**API-086 (`GET /empresas/miEmpresa`) no está vigente** y queda pendiente a futuro: dependía de un usuario autenticado y de una membresía activa `ADMIN_EMPRESA`. La estructura de salida `MiEmpresaResponse` tampoco está vigente. La interfaz de empresa es únicamente el detalle institucional de `UI-011` sobre `/empresas/:id` (`INTERFACES.md`).

**Notas de API-048 a API-053 (TK-019):**

- Códigos comunes: 404 empresa o recurso inexistente; 400 `DATOS_INVALIDOS` por campo obligatorio ausente, formato inválido o envío de campos inmutables; 409 `REGLA_NEGOCIO` por incumplimiento de una regla de negocio; 409 `DUPLICADO` por restricción única. Formato de error según §6.
- La empresa se identifica solo con `{empresaId}` en la ruta de API-048 a API-053. El Service verifica el ámbito: ADM y COO sin límite de empresa; REP únicamente sobre la suya, con `ADMIN_EMPRESA` activo en API-048 a API-050. API-051 conserva el acceso de REP a su empresa para consultar tutores; API-052 y API-053 son exclusivas de ADM y COO.
- Las bajas de miembros son lógicas (`activo=false` en API-050): `empresa_usuario` no se elimina físicamente.
- Decisión aprobada para TK-020: en UI-011 el representante `ADMIN_EMPRESA` consulta tutores (API-051) en solo lectura; no crea ni edita tutores (API-052 y API-053 continúan restringidas a ADM y COO) y no edita convenios (API-044 a API-047 permanecen restringidas a ADM y COO).
- Cuerpo de respuesta: API-049 y API-050 devuelven `MiembroResumen`; API-052 y API-053 devuelven `TutorEmpresarialResponse`, conforme a §4.2 y §5. API-048 y API-051 devuelven los arreglos `MiembroResumen[]` y `TutorEmpresarialResponse[]`, respectivamente.

### 3.4. Ofertas

| ID | Método y ruta | Descripción | Roles | Entrada | Respuesta | Errores | Refs |
|---|---|---|---|---|---|---|---|
| API-054 | `GET /ofertas/listarGestion?estado=&empresaId=&periodoId=` | Ofertas en cualquier estado dentro del alcance del rol. | REP (empresa; `PUBLICADOR` solo las suyas), COO (ámbito), ADM, AUT | – | 200 `OfertaResumen[]` | – | RF-009, RF-011 |
| API-055 | `GET /ofertas/listarDisponibles?texto=` | Ofertas `PUBLICADA` dentro de plazo; para EST, solo con perfiles de su carrera. | EST, COO, ADM, AUT, REP (su empresa) | – | 200 `OfertaResumen[]` | – | RF-012; RN-016 |
| API-056 | `GET /ofertas/buscar/{id}` | Detalle con perfiles, carreras, requisitos y cupos. | Según alcance | – | 200 `OfertaDetalle` | – | RF-009, RF-012 |
| API-057 | `POST /ofertas` | Crea la oferta en `BORRADOR`. | REP | `OfertaRequest` | 201 `OfertaDetalle` | 409 convenio no vigente; fechas incoherentes; sin periodo activo | RF-009; RN-011, RN-012 |
| API-058 | `PUT /ofertas` | Actualiza la oferta. Si estaba `PUBLICADA` vuelve a `PENDIENTE_REVISION`. | REP (`ADMIN_EMPRESA` o creador) | `OfertaRequest` (con `id`) | 200 `OfertaDetalle` | 409 estado no editable; tipo de práctica con postulaciones | RF-009; RN-015 |
| API-059 | `POST /ofertas/{id}/perfiles` | Agrega un perfil. | REP (`ADMIN_EMPRESA` o creador) | `PerfilOfertaRequest` | 201 `PerfilOferta` | 409 plazas ≤ 0; nombre duplicado; carrera inactiva | RF-010; RN-013 |
| API-060 | `PUT /ofertas/{id}/perfiles/{perfilId}` | Actualiza un perfil. | REP (`ADMIN_EMPRESA` o creador) | `PerfilOfertaRequest` | 200 `PerfilOferta` | 409 plazas por debajo de las aceptadas | RF-010; RN-013 |
| API-061 | `DELETE /ofertas/{id}/perfiles/{perfilId}` | Elimina un perfil sin postulaciones. | REP (`ADMIN_EMPRESA` o creador) | – | 204 | 409 `REGLA_NEGOCIO` el perfil tiene postulaciones | RN-013 |
| API-062 | `POST /ofertas/{id}/enviarRevision` | `BORRADOR` o `RECHAZADA` → `PENDIENTE_REVISION`. Congela horas y nota. | REP (`ADMIN_EMPRESA` o creador) | – | 200 `OfertaDetalle` | 409 incumple RN-012 (indica la causa) | RF-010; RN-012 |
| API-063 | `POST /ofertas/{id}/retirar` | `PENDIENTE_REVISION` → `BORRADOR`. | REP (`ADMIN_EMPRESA` o creador) | – | 200 | 409 estado no permite | FL-003 |
| API-064 | `POST /ofertas/{id}/aprobar` | `PENDIENTE_REVISION` → `PUBLICADA`. | COO (ámbito) | – | 200 `OfertaDetalle` | 409 convenio, periodo o plazos no válidos | RF-011; RN-012, RN-014 |
| API-065 | `POST /ofertas/{id}/rechazar` | `PENDIENTE_REVISION` → `RECHAZADA`. | COO (ámbito) | `MotivoRequest` | 200 | 409 sin motivo | RF-011; RN-014 |
| API-066 | `POST /ofertas/{id}/cerrar` | `PUBLICADA` → `CERRADA`. | COO (ámbito) | – | 200 | 409 estado no permite | RN-016 |
| API-067 | `POST /ofertas/{id}/cancelar` | Pasa a `CANCELADA`. | COO (ámbito), REP (`ADMIN_EMPRESA`) | `MotivoRequest` | 200 | 409 sin motivo o estado final | RN-016 |

El cierre automático de ofertas vencidas lo ejecuta un proceso del sistema (`cerrar_ofertas_vencidas()`); no es un endpoint.

### 3.5. Postulaciones

| ID | Método y ruta | Descripción | Roles | Entrada | Respuesta | Errores | Refs |
|---|---|---|---|---|---|---|---|
| API-068 | `POST /postulaciones` | Registra la postulación de un estudiante a un perfil; queda `PENDIENTE`. | EST | `PostulacionRequest` | 201 `PostulacionDetalle` | 409 `REGLA_NEGOCIO` oferta no publicada o vencida; convenio no vigente; carrera no incluida; sin plazas; requisitos incumplidos; 409 `DUPLICADO` postulación existente | RF-013; RN-017 |
| API-069 | `GET /postulaciones/listarMias` | Postulaciones del estudiante. | EST | – | 200 `PostulacionResumen[]` | – | RF-013 |
| API-070 | `GET /postulaciones/listarGestion?estado=&ofertaId=&recomendacion=` | Postulaciones del ámbito. | COO | – | 200 `PostulacionResumen[]` | – | RF-015 |
| API-071 | `GET /postulaciones/listarPorOferta/{ofertaId}` | Postulantes de una oferta de la empresa. | REP, COO | – | 200 `PostulacionResumen[]` | – | RF-014 |
| API-072 | `GET /postulaciones/buscar/{id}` | Detalle. | EST (propia), REP (su empresa), COO (ámbito) | – | 200 `PostulacionDetalle` | – | RF-014, RF-015 |
| API-073 | `PUT /postulaciones/{id}/mensaje` | Edita el mensaje mientras no haya recomendación. | EST | `MensajeRequest` | 200 | 409 ya hay recomendación o decisión | FL-008 |
| API-074 | `POST /postulaciones/{id}/retirar` | `PENDIENTE` → `RETIRADA`. | EST | – | 200 | 409 ya resuelta | RF-013; RN-019 |
| API-075 | `POST /postulaciones/{id}/recomendar` | Registra `RECOMENDADO` o `NO_RECOMENDADO` con observación. | REP (su empresa) | `RecomendacionRequest` | 200 | 409 ya resuelta u oferta en revisión | RF-014; RN-019 |
| API-076 | `POST /postulaciones/{id}/aceptar` | `PENDIENTE` → `ACEPTADA` y crea la práctica `APROBADA` en la misma transacción; la plaza se verifica y reserva transaccionalmente. | COO (ámbito) | – | 200 `PostulacionDetalle` (con `practicaId`) | 409 `REGLA_NEGOCIO` sin plazas; convenio no vigente; práctica vigente en el periodo; sin periodo activo; 409 `CONFLICTO_CONCURRENCIA` plaza ocupada por otra aceptación o conflicto de `version` | RF-015, RF-016; RN-018, RN-020, RN-021 |
| API-077 | `POST /postulaciones/{id}/rechazar` | `PENDIENTE` → `RECHAZADA`. | COO (ámbito) | `MotivoRequest` | 200 | 409 sin motivo | RF-015; RN-018 |
| API-078 | `POST /postulaciones/{id}/cancelar` | `ACEPTADA` → `CANCELADA`. | COO (ámbito) | `MotivoRequest` | 200 | 409 la práctica aún no está cancelada o rechazada | RN-019 |
| API-079 | `POST /postulaciones/{id}/corregirDecision` | Corrige una decisión ya tomada, con motivo y auditoría. | ADM, COO (ámbito) | `CorreccionRequest` | 200 | 409 sin motivo | RN-018 |

### 3.6. Prácticas y auditoría

| ID | Método y ruta | Descripción | Roles | Entrada | Respuesta | Errores | Refs |
|---|---|---|---|---|---|---|---|
| API-080 | `GET /practicas/listarTodos?estado=&periodoId=&carreraId=` | Prácticas dentro del alcance del rol. | ADM, COO (ámbito), AUT, EST/DOC/TUT (propias), REP (su empresa) | – | 200 `PracticaResumen[]` | – | RF-016 |
| API-081 | `GET /practicas/buscar/{id}` | Detalle de la práctica. | Según alcance | – | 200 `PracticaDetalle` | – | RF-016, RF-017 |
| API-082 | `PUT /practicas/{id}/asignaciones` | Asigna docente tutor, tutor empresarial y fechas. | COO (ámbito) | `AsignacionRequest` | 200 `PracticaDetalle` | 409 `REGLA_NEGOCIO` tutor de otra empresa; fechas incoherentes; 409 `CONFLICTO_CONCURRENCIA` conflicto de `version` en la práctica | RF-016; RN-025 |
| API-083 | `POST /practicas/{id}/iniciar` | `APROBADA` → `EN_CURSO` (fuera del MVP; mejora futura, CR-8). | COO (ámbito) | – | 200 | 409 falta docente, tutor o fechas | RF-017; RN-025 |
| API-084 | `POST /practicas/{id}/cancelar` | Pasa a `CANCELADA`; cancela su postulación y libera la plaza. | COO (ámbito) | `MotivoRequest` | 200 | 409 estado terminal | RN-026 |
| API-085 | `GET /auditoria/listarTodos?usuarioId=&entidad=&desde=&hasta=` | Consulta de auditoría. Solo lectura. | ADM, COO (ámbito), AUT | – | 200 `AuditoriaResponse[]` | – | RF-027; RN-039 |

Sin endpoint por ahora: reportes consolidados (RF-026) y definición/registro de requisitos académicos (RF-006), ambos fuera del MVP (CR-6). Las validaciones académicas aplicables a la postulación sí se ejecutan en la API (RN-007, RN-017, RD-021). La transición a `EN_CURSO` (API-083) es mejora futura (CR-8).

## 4. Estructuras de entrada y salida

Las entradas son entidades del dominio cuando no contienen datos sensibles; las respuestas se declaran como clases internas del Resource correspondiente (`ARQUITECTURA.md` §8, §1.4). Los campos son la base de implementación y se confirman al construir cada módulo.

### 4.1. Entrada

**RestablecerPasswordRequest:** `passwordTemporal` (sí). No vigente; pendiente a futuro (API-030).
**UsuarioRequest:** `id` (solo en PUT), `correo`, `nombres`, `apellidos`, `tipoDocumento` (`CEDULA` o `PASAPORTE`), `numeroDocumento`, `telefono`, `alcanceGlobal`, `passwordTemporal` (solo al crear). El correo se guarda en minúsculas.
**AsignarRolesRequest:** `roles` (String[] con códigos de rol).
**AsignarCarrerasRequest:** `carreraIds` (Long[]).
**EmpresaUsuario:** entidad del dominio usada como cuerpo de entrada en API-049 y API-050 (`MODELO_DATOS.md` §4.27); no es una clase interna del Resource ni un DTO.

- Creación (API-049): se consumen `usuarioId` (Long, obligatorio), `rolEmpresa` (`ADMIN_EMPRESA` o `PUBLICADOR`, obligatorio) y `activo` (Boolean, obligatorio, solo se admite `true`). El Service fuerza `activo=true`: no se crea una membresía inactiva; `activo=false` se rechaza con 400 `DATOS_INVALIDOS`. `empresaId` se toma exclusivamente de la ruta; `id`, `createdAt` y `updatedAt` los genera la base y no admiten valor en el cuerpo (400 `DATOS_INVALIDOS`). El primer miembro activo de la empresa debe ser `ADMIN_EMPRESA`. Se validan empresa activa, usuario activo con rol global `REPRESENTANTE_EMPRESA` activo y unicidad de `(empresa_id, usuario_id)`.
- Actualización (API-050): la membresía se identifica por `{id}` en la ruta. Solo `rolEmpresa` y `activo` son modificables. `empresaId` y `usuarioId` son inmutables; `id` se toma de la ruta y `createdAt`/`updatedAt` los gestiona la base. Cualquier valor enviado en esos campos se rechaza con 400 `DATOS_INVALIDOS` y no puede alterar el registro. No se permite eliminar físicamente la membresía: la baja es `activo=false`. Si se desactiva o se degrada a `PUBLICADOR` al último `ADMIN_EMPRESA` activo de la empresa, se rechaza con 409 `REGLA_NEGOCIO`.

**TutorEmpresarial:** entidad del dominio usada como cuerpo de entrada en API-052 y API-053 (`MODELO_DATOS.md` §4.11); no es una clase interna del Resource ni un DTO.

- Creación (API-052): se consumen `usuarioId` (Long, obligatorio) y `cargo` (String, opcional; la columna admite nulo). `empresaId` se toma exclusivamente de la ruta; `id`, `createdAt` y `updatedAt` los genera la base y no admiten valor en el cuerpo (400 `DATOS_INVALIDOS`). El usuario debe existir, estar activo y tener el rol global `TUTOR_EMPRESARIAL` activo; `usuarioId` es único a nivel global, por lo que un usuario ya tutor de otra empresa produce 409 `DUPLICADO`.
- Actualización (API-053): `id` (Long, obligatorio en el cuerpo) identifica al tutor; `cargo` (String, opcional) es el único campo modificable. `empresaId` se toma de la ruta y `usuarioId` es inmutable; `createdAt` y `updatedAt` los gestiona la base. Cualquier valor enviado en `empresaId`, `usuarioId`, `createdAt` o `updatedAt` se rechaza con 400 `DATOS_INVALIDOS`. El Service verifica que el tutor pertenezca a la empresa de la ruta; si no, responde 404. **Decisión explícita del contrato:** a diferencia de API-050 (donde el `id` de la membresía se toma de la ruta), API-053 mantiene el `id` del tutor en el cuerpo de `TutorEmpresarial`; la ruta `PUT /empresas/{empresaId}/tutores` no incluye `{id}`. No es una inconsistencia accidental ni se trasladará el identificador a la ruta: el `empresaId` sigue siendo exclusivamente de la ruta y el Service comprueba la pertenencia del tutor antes de actualizar.

**Empresa:** entrada de API-041 y API-042; campos tomados de `MODELO_DATOS.md` §4.10.

- Creación (API-041): `ruc` (String, obligatorio, 13 dígitos), `razonSocial` (String, obligatorio), `nombreComercial` (String, opcional), `direccion` (String, opcional), `telefono` (String, opcional), `correo` (String, opcional). `activa` no se admite: la empresa se registra activa por defecto. `id`, `createdAt` y `updatedAt` no se envían.
- Actualización (API-042): `id` (Long, obligatorio, en el cuerpo), `ruc` (obligatorio), `razonSocial` (obligatorio), `nombreComercial`, `direccion`, `telefono`, `correo` (opcionales) y `activa` (Boolean, obligatorio). La empresa se identifica por `id` en el cuerpo; los campos editables se reemplazan con los valores enviados y los opcionales omitidos quedan `null`. `createdAt` y `updatedAt` no se envían.

**Convenio:** entrada de API-044 y API-045; campos tomados de `MODELO_DATOS.md` §4.14.

- Creación (API-044): `empresaId` (Long, obligatorio; empresa titular del convenio, no ámbito del actor), `numero` (String, obligatorio), `fechaInicio` (LocalDate, obligatoria), `fechaFin` (LocalDate, obligatoria) y `documentoUrl` (String, opcional). El convenio se crea `VIGENTE`. No se envían `id`, `estado`, `estadoEfectivo`, `createdAt` ni `updatedAt`.
- Actualización (API-045): `id` (Long, obligatorio, en el cuerpo), `empresaId`, `numero`, `fechaInicio` y `fechaFin` (obligatorios) y `documentoUrl` (opcional). El convenio se identifica por `id` en el cuerpo; los campos editables se reemplazan con los valores enviados. Solo se edita un convenio `VIGENTE`. `estado` no se admite: la creación siempre fija `VIGENTE` y la edición no lo modifica; las transiciones a `SUSPENDIDO` o `TERMINADO` se realizan solo mediante API-046 y API-047. `VENCIDO` es un estado efectivo derivado de `v_convenio_estado`: no se envía, no se persiste y `estadoEfectivo` aparece solo en la salida.

**OfertaRequest:** `id` (solo en PUT), `convenioId`, `tipoPracticaId`, `periodoId`, `titulo`, `descripcion`, `fechaInicioActividades`, `fechaFinActividades`, `fechaCierre`.
**PerfilOfertaRequest:** `nombre`, `descripcion`, `plazas` (> 0), `carreraIds` (Long[]; vacío = abierto a todas), `requisitos` (lista de `descripcion` y `obligatorio`).
**MotivoRequest:** `motivo` (String, obligatorio).
**PostulacionRequest:** `perfilId`, `mensaje` (opcional).
**MensajeRequest:** `mensaje`.
**RecomendacionRequest:** `recomendacion` (`RECOMENDADO` o `NO_RECOMENDADO`), `observacion`.
**CorreccionRequest:** `motivo` (obligatorio) y los datos de la decisión a corregir. Alcance resuelto (CR-4): solo ADM o COO de ámbito, sobre una decisión ya tomada, sin cambiar el estado de la postulación, con auditoría (RN-018, RN-039, `MODELO_DATOS.md` §7.3).
**AsignacionRequest:** `docenteId`, `tutorEmpresarialId`, `fechaInicio`, `fechaFin`.

### 4.2. Salida compuesta

Las estructuras `FacultadResponse`, `CarreraResponse`, `PeriodoAcademicoResponse`, `TipoPracticaResponse`, `EstudianteResponse`, `DocenteResponse`, `EmpresaResponse`, `MiembroResumen`, `TutorEmpresarialResponse` y `AuditoriaResponse` son clases internas del Resource correspondiente. Sus campos se toman de `MODELO_DATOS.md` §4; no se crean DTOs independientes ni se serializan entidades JPA.

**FacultadResponse:** `id`, `codigo`, `nombre`, `activa`, `createdAt`, `updatedAt`.
**CarreraResponse:** `id`, `facultadId`, `codigo`, `nombre`, `activa`, `createdAt`, `updatedAt`.
**PeriodoAcademicoResponse:** `id`, `codigo`, `nombre`, `fechaInicio`, `fechaFin`, `activo`, `createdAt`, `updatedAt`.
**TipoPracticaResponse:** `id`, `nombre`, `horasRequeridas`, `notaMinima`, `activo`, `createdAt`, `updatedAt`.
**EstudianteResponse:** `id`, `usuarioId`, `carreraId`, `codigoEstudiante`, `fechaNacimiento`, `createdAt`, `updatedAt`.
**DocenteResponse:** `id`, `usuarioId`, `codigoDocente`, `dedicacion`, `createdAt`, `updatedAt`.
**EmpresaResponse:** `id`, `ruc`, `razonSocial`, `nombreComercial`, `direccion`, `telefono`, `correo`, `activa`, `createdAt`, `updatedAt`.
**TutorEmpresarialResponse:** `id`, `usuarioId`, `empresaId`, `cargo`, `createdAt`, `updatedAt`.
**AuditoriaResponse:** `id`, `usuarioId`, `accion`, `entidad`, `entidadId`, `detalle`, `fecha`.

**UsuarioResumen:** `id`, `correo`, `nombres`, `apellidos`, `tipoDocumento`, `numeroDocumento`, `telefono`, `activo`, `alcanceGlobal`, `roles`, `carreraIds`.
**ConvenioResumen:** campos de `Convenio` más `estadoEfectivo` (vista `v_convenio_estado`).
**MiembroResumen:** `id`, `usuarioId`, `nombres`, `apellidos`, `correo`, `rolEmpresa`, `activo`. Los datos de usuario (`nombres`, `apellidos`, `correo`) se obtienen con una proyección de solo lectura; no se serializa la entidad `Usuario` ni se exponen sus campos internos.
**OfertaResumen:** `id`, `titulo`, `empresaId`, `empresaNombre`, `periodoId`, `tipoPracticaNombre`, `estado`, `fechaCierre`, `plazasTotales`, `plazasDisponibles`.
**OfertaDetalle:** todo `OfertaResumen` más `descripcion`, `convenioId`, `fechaInicioActividades`, `fechaFinActividades`, `horasRequeridasSnapshot`, `notaMinimaSnapshot`, `motivoDecision`, `perfiles` (lista de `PerfilOferta`).
**PerfilOferta:** `id`, `nombre`, `descripcion`, `plazas`, `aceptadas`, `disponibles`, `perfilCerrado` (vista `v_oferta_perfil_cupo`), `carreraIds`, `requisitos`.
**PostulacionResumen:** `id`, `estado`, `recomendacionEmpresa`, `fechaPostulacion`, `estudianteId`, `estudianteNombre`, `carreraNombre`, `ofertaId`, `ofertaTitulo`, `perfilNombre`, `empresaNombre`. Para el estudiante no incluye la recomendación de la empresa.
**PostulacionDetalle:** `PostulacionResumen` más `mensaje`, `observacionEmpresa`, `fechaResolucion`, `motivoDecision`, `practicaId`.
**PracticaResumen:** `id`, `estado`, `estudianteNombre`, `empresaNombre`, `tipoPracticaNombre`, `periodoId`, `docenteNombre`, `tutorEmpresarialNombre`.
**PracticaDetalle:** `PracticaResumen` más `convenioId`, `horasRequeridasSnapshot`, `notaMinimaSnapshot`, `fechaAprobacion`, `fechaInicio`, `fechaFin`, `postulacionId`, `observaciones`.

Las respuestas `ConvenioResumen`, `OfertaResumen`, `OfertaDetalle` y `PerfilOferta` que incorporan datos de `v_convenio_estado` o `v_oferta_perfil_cupo` se arman con estas estructuras explícitas. El Repository puede devolver proyecciones sencillas de solo lectura; no se crean entidades JPA para las vistas ni una capa adicional.

Las operaciones cuya respuesta se indica solo como 200 o 201 sin tipo (por ejemplo API-016 a API-019, API-022, API-023, API-028 a API-032, API-035, API-036, API-038, API-041, API-042 y API-044 a API-047) quedan **pendientes de verificación al construir cada módulo**: no se fija aquí un contrato de salida nuevo sin evidencia de los campos que realmente necesita el cliente. Cuando se confirmen, se declararán como estructuras internas del Resource respectivo. API-048 a API-053 tienen salida declarada en esta sección y en §3.3: `MiembroResumen[]` (API-048), `MiembroResumen` (API-049 y API-050), `TutorEmpresarialResponse[]` (API-051) y `TutorEmpresarialResponse` (API-052 y API-053).

## 5. Formato de respuestas

- Creación: 201 con el recurso creado.
- Consulta, actualización y acciones de negocio: 200 con el recurso.
- Sin contenido: 204.
- Los listados devuelven un arreglo JSON; vacío si no hay resultados (nunca 404).
- En TK-019, los recursos creados o actualizados son `MiembroResumen` (API-049 y API-050) y `TutorEmpresarialResponse` (API-052 y API-053); las consultas devuelven `MiembroResumen[]` (API-048) y `TutorEmpresarialResponse[]` (API-051).

## 6. Manejo de errores

Formato uniforme:

```json
{ "codigo": "REGLA_NEGOCIO", "mensaje": "La oferta no tiene plazas disponibles." }
```

| HTTP | `codigo` | Causa |
|---|---|---|
| 400 | `DATOS_INVALIDOS` | Formato o campo obligatorio faltante. |
| 401 | `CREDENCIALES_INVALIDAS` | Solo `POST /usuarios/login`: correo o contraseña incorrectos, o cuenta inactiva. |
| 404 | `NO_ENCONTRADO` | Registro inexistente. |
| 409 | `REGLA_NEGOCIO` | Incumple una regla (RN) o un trigger rechaza la operación; el mensaje indica la causa. |
| 409 | `DUPLICADO` | Restricción única de la base. |
| 409 | `CONFLICTO_CONCURRENCIA` | Conflicto de `version` en `postulacion` o `practica`, o plaza ocupada por otra operación al aceptar una postulación. |
| 500 | `ERROR_INTERNO` | Mensaje genérico. |

Los códigos 409 se aplican así: `REGLA_NEGOCIO` a incumplimientos de condiciones del proceso, incluida la integridad referencial (hay registros dependientes; API-009 y API-061); `DUPLICADO` a registros existentes o restricciones únicas; `CONFLICTO_CONCURRENCIA` a conflictos de `version` o a la plaza ocupada por otra aceptación concurrente. El código 401 se reserva al login básico descrito en §2; no se añaden otros códigos.

Los errores nunca incluyen SQL, rutas, trazas ni datos de credenciales. Los errores de triggers diferidos aparecen al confirmar la transacción y se traducen con el mismo formato.

## 7. Relación con Qute

No aplica. `ARQUITECTURA.md` define React como interfaz; el backend solo expone JSON y no renderiza vistas del servidor. Las pantallas están en `INTERFACES.md`.

## 8. Decisiones resueltas (CR-1 a CR-8)

| # | Decisión resuelta | Alcance |
|---|---|---|
| CR-1 | Rutas sin prefijo `/api`, relativas a la raíz (`ARQUITECTURA.md` §10). | MVP |
| CR-2 | Vigente solo el login básico de prototipo (`POST /usuarios/login`): valida credenciales con BCrypt y entrega datos al cliente, sin sesión ni cookies. Sesiones, cookies y autenticación de la API siguen pendientes. |
| CR-3 | **No vigente; pendiente a futuro.** Seguridad de cuentas, intentos y bloqueos. |
| CR-4 | Corrección de decisiones con motivo y auditoría, sin cambio de estado. La restricción por rol o ámbito no está vigente. | MVP |
| CR-5 | **No vigente como autorización.** Existe una matriz visual de módulos por rol en React; no restringe llamadas REST. |
| CR-6 | Validaciones académicas en postulación, en el MVP; módulo de requisitos y reportes, futuros. | MVP / futura |
| CR-7 | Listados sin paginación en el MVP; paginación futura. | MVP / futura |
| CR-8 | Asignación de responsables y fechas en el MVP; `iniciar` (API-083) y etapas posteriores, futuras. | MVP / futura |

## 9. Precisiones de la Fase 2.1

- **Concurrencia básica (incluida en el MVP):** `@Version` en `postulacion` y `practica`, comprobación transaccional de plazas al aceptar postulaciones y 409 `CONFLICTO_CONCURRENCIA`. Aceptar y crear la práctica ocurren en una sola transacción. No se añaden bloqueos distribuidos, colas ni servicios externos.
- **Facultades:** API-009 se conserva, pero la interfaz solo desactiva (API-008); la eliminación respeta la integridad referencial y no se asume que sea posible con registros relacionados.
- **Vistas:** `v_convenio_estado` y `v_oferta_perfil_cupo` se consumen mediante proyecciones de solo lectura; no se mapean como entidades JPA y sus respuestas usan las estructuras explícitas de la sección 4.
- **Catálogos y requisitos:** no se inventan ni insertan datos institucionales o de prueba; la validación se aplica sobre los requisitos existentes y la administración de requisitos permanece fuera del MVP (CR-6).
- **Auditoría:** la auditoría básica y los historiales del MVP están incluidos; los reportes avanzados y la trazabilidad avanzada quedan fuera.
