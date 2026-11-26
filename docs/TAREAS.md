# Tareas de implementación

## 1. Objetivo

Organizar la implementación del MVP de PRAXIS en fases y tareas verificables, relacionadas con los documentos de `docs/`. Cada tarea indica sus documentos de referencia, dependencias y criterios de aceptación.

El MVP cubre: usuarios y acceso, estructura académica, perfiles, empresas y convenios, ofertas, postulaciones, asignación de docentes tutores y auditoría (`PRODUCTO.md` §5).

## 2. Fases

| Fase | Nombre | Tareas |
|---|---|---|
| 1 | Preparación y base técnica | TK-001 a TK-005 |
| 2 | Seguridad y acceso (sin vigencia; pendiente a futuro) | TK-006 a TK-010 |
| 3 | Estructura académica | TK-011 a TK-014 |
| 4 | Usuarios y perfiles | TK-015 a TK-017 |
| 5 | Empresas y convenios | TK-018 a TK-020 |
| 6 | Ofertas | TK-021 a TK-025 |
| 7 | Postulaciones | TK-026 a TK-029 |
| 8 | Prácticas y asignación | TK-030 y TK-031 |
| 9 | Auditoría, cierre y validación | TK-032 a TK-035 |

## 3. Tareas

Todas las tareas tienen Responsable: **Por asignar** y Estado: **Pendiente**.

**Nota de vigencia.** La aplicación no tiene autenticación, sesión, cookies ni autorización por rol o ámbito. Las referencias a roles, carreras o empresa en tareas académicas son **datos de dominio o reglas de negocio**, no permisos aplicados por la API ni por la interfaz. Las tareas y criterios de seguridad (Fase 2, TK-004 y la parte de seguridad de TK-034) están marcados como no vigentes y conservan sus identificadores solo por trazabilidad.

### Fase 1. Preparación y base técnica

### TK-001 Verificar el proyecto y la configuración local
- **Descripción:** comprobar que el proyecto compila y arranca contra la base existente, y confirmar versiones y comandos.
- **Documentos:** `ARQUITECTURA.md` §9, `AGENTS.md`.
- **Dependencias:** ninguna.
- **Resultado esperado:** proyecto ejecutable en modo desarrollo; `AGENTS.md` con comandos verificados.
- **Criterios de aceptación:**
  - `./mvnw quarkus:dev` arranca con JDK 25 y Quarkus 3.40.1 y conecta con la base mediante la configuración local externa de `config/local.properties`, cargada con `-Dquarkus.config.locations`.
  - El proyecto compila y ejecuta con JDK 25 (LTS 25.0.2); no se usa otra versión de Java.
  - `./mvnw test` y `./mvnw package` terminan sin errores.
  - Hibernate no genera ni modifica el esquema.
  - No hay credenciales en el repositorio.

### TK-002 Validar y corregir el módulo de referencia (Facultad)
- **Descripción:** implementar `Facultad` (Entity, Repository, Service, Resource) y corregir las inconsistencias de `ARQUITECTURA.md` §10 antes de replicar el patrón.
- **Documentos:** `ARQUITECTURA.md`, `MODELO_DATOS.md` §4.4, `CONTRATOS_REST.md` (API-005 a API-009).
- **Dependencias:** TK-001.
- **Resultado esperado:** módulo funcional y `ARQUITECTURA.md` actualizado.
- **Criterios de aceptación:**
  - `FacultadService.crear` y el Resource son coherentes: el Resource construye y devuelve una respuesta REST explícita (`FacultadResponse`) con los datos necesarios, sin devolver la entidad JPA directamente.
  - El tipo del `id` coincide con la base (`BIGINT` → `Long`).
  - Buscar un id inexistente responde 404; código o nombre duplicado responde 409.
  - Las columnas `created_at` y `updated_at` no se escriben desde el código.
  - Las cinco operaciones se verifican con pruebas o llamadas manuales documentadas.

### TK-003 Manejo uniforme de errores
- **Descripción:** convertir excepciones de negocio, de validación y de la base (restricciones y triggers) al formato de `CONTRATOS_REST.md` §6.
- **Documentos:** `CONTRATOS_REST.md` §6, `ARQUITECTURA.md` §7.
- **Dependencias:** TK-002.
- **Resultado esperado:** respuestas de error uniformes.
- **Criterios de aceptación:**
  - Cada código HTTP de la tabla devuelve `codigo` y `mensaje`.
  - Un error de trigger diferido se traduce a 409 al confirmar la transacción.
  - Un conflicto de `version` o de plaza ocupada se traduce a 409 `CONFLICTO_CONCURRENCIA`.
  - Ninguna respuesta incluye SQL, trazas ni rutas internas.

### TK-004 Contexto de sesión para los triggers (no vigente; eliminada)
- **Estado:** eliminada y pendiente a futuro. La aplicación ya no fija `praxis.usuario_id`, `praxis.motivo` ni `praxis.sistema`.
- **Consecuencia:** los triggers que leen `praxis.usuario_id` pueden rechazar escrituras; ver `CONTEXTO_ACTOR.md`.
- **Documentos:** `MODELO_DATOS.md` §7.3, `ARQUITECTURA.md` §7 y §12, `CONTEXTO_ACTOR.md`.
- **No se escriben** directamente las columnas gestionadas por triggers. El esquema no se modifica.

### TK-005 Base del frontend
- **Descripción:** configurar React + Vite en `src/main/webui`: estructura de la aplicación, rutas, cliente HTTP y estados comunes.
- **Documentos:** `INTERFACES.md` §1, §2, §8, §9; `ARQUITECTURA.md`.
- **Dependencias:** TK-001.
- **Resultado esperado:** esqueleto navegable con menú institucional único.
- **Criterios de aceptación:**
  - No hay manejo de 401, guardias de ruta ni redirección a `/login`: no existe sesión ni autorización por rol.
  - Los estados de la sección 8 de `INTERFACES.md` están disponibles como componentes.
  - El diseño respeta el estilo institucional descrito en `INTERFACES.md` §1; no existe `PROMPT_PANTALLAS.md` y no se crea uno vacío.

### Fase 2. Seguridad y acceso (login básico vigente; resto pendiente)

Los identificadores se conservan por trazabilidad. Está vigente solo el login básico de prototipo (`POST /usuarios/login`) y el filtro visual de menús y rutas. No hay sesión, cookies, autorización por rol ni control de acceso en la API (`SEGURIDAD.md`, `ARQUITECTURA.md` §11).

### TK-006 Entidades de seguridad (no vigente)
- **Estado:** tarea eliminada. Las entidades y repositorios de `rol`, `usuario`, `usuario_rol` y `usuario_carrera` se conservan como **datos de dominio** sobre el esquema existente, sin cambios de tablas ni columnas.
- **Documentos:** `MODELO_DATOS.md` §4.1 a §4.6, `ARQUITECTURA.md` §7.
- **Vigente:** nombres y tipos acordes con `MODELO_DATOS.md`; `hashPassword`, `intentosFallidos` y `bloqueadoHasta` nunca se serializan; el `id` no se asigna desde el código.

### TK-007 Autenticación y sesión (no vigente; eliminada)
- **Estado:** eliminada y pendiente a futuro. API-001 a API-004 y las rutas `/auth` no están vigentes.
- **No aplica:** sesiones, cookies, `SecurityIdentity`, `@Authenticated`, `@RolesAllowed`, intentos fallidos, bloqueos, caducidades ni generaciones de sesión.
- **Se conserva:** BCrypt solo para generar hashes al crear o actualizar contraseñas (`ARQUITECTURA.md` §7).

### TK-008 Cambio y restablecimiento de contraseña (no vigente; eliminada)
- **Estado:** eliminada y pendiente a futuro. API-004 y API-030 no están vigentes; no hay cambio obligatorio de contraseña ni recuperación.
- **Se conserva:** el hash BCrypt al crear o actualizar contraseñas y las columnas existentes en la BD, sin uso para control de acceso.

### TK-009 Autorización por rol y ámbito (no vigente; eliminada)
- **Estado:** eliminada y pendiente a futuro. No hay `@RolesAllowed`, lista de verificación de permisos ni verificación de propiedad o ámbito por actor.
- **Se conservan:** los roles y carreras como datos de dominio; las reglas académicas y de negocio de cada operación.

### TK-010 Pantallas de acceso (no vigente; eliminada)
- **Estado:** eliminada y pendiente a futuro. UI-001 y UI-002 no existen: no hay `/login`, `/cambiar-password` ni redirecciones a ellas. UI-003 se documenta sin acceso por rol (`INTERFACES.md` §3).

### Fase 3. Estructura académica

### TK-011 Pantalla de facultades
- **Descripción:** UI-006 sobre el módulo ya implementado.
- **Documentos:** `INTERFACES.md` UI-006, `CONTRATOS_REST.md` API-005 a API-009.
- **Dependencias:** TK-002, TK-005, TK-009.
- **Resultado esperado:** administración de facultades desde la interfaz.
- **Criterios de aceptación:**
  - Crear, editar y desactivar funcionan para ADM; el resto solo consulta.
  - Un duplicado muestra el mensaje de la API.

### TK-012 Carreras
- **Descripción:** backend (API-010 a API-013) y pantalla UI-007.
- **Documentos:** `MODELO_DATOS.md` §4.5, `CONTRATOS_REST.md`, `INTERFACES.md` UI-007.
- **Dependencias:** TK-011.
- **Resultado esperado:** carreras administrables por facultad.
- **Criterios de aceptación:**
  - Respeta la restricción única (facultad, nombre).
  - El filtro por facultad funciona.
  - Una carrera desactivada no se ofrece en nuevas asignaciones.

### TK-013 Periodos académicos
- **Descripción:** backend (API-014 a API-019) y pantalla UI-008.
- **Documentos:** `MODELO_DATOS.md` §4.7, `REGLAS_NEGOCIO.md` RN-006.
- **Dependencias:** TK-011.
- **Resultado esperado:** periodos con un único activo.
- **Criterios de aceptación:**
  - Activar un segundo periodo devuelve 409 con la causa.
  - Una fecha de fin anterior al inicio se rechaza.

### TK-014 Tipos de práctica
- **Descripción:** backend (API-020 a API-023) y pantalla UI-009.
- **Documentos:** `MODELO_DATOS.md` §4.15, `ARQUITECTURA.md` §12.
- **Dependencias:** TK-011.
- **Resultado esperado:** catálogo administrable.
- **Criterios de aceptación:**
  - Horas requeridas y nota mínima se guardan como datos, sin valores fijos en el código.
  - Valores fuera de rango (horas ≤ 0, nota fuera de 0–20) se rechazan.

### Fase 4. Usuarios y perfiles

### TK-015 Usuarios
- **Descripción:** backend API-024 a API-032.
- **Documentos:** `MODELO_DATOS.md` §4.2, `REGLAS_NEGOCIO.md` RN-001, RN-003, RN-004, `SEGURIDAD.md`.
- **Dependencias:** TK-006, TK-009.
- **Resultado esperado:** administración de cuentas, roles y carreras.
- **Criterios de aceptación:**
  - Correo en minúsculas; correo o documento duplicado devuelve 409.
  - La creación deja `debeCambiarPassword` en verdadero.
  - Las respuestas no exponen datos de credenciales.
  - Al asignar el rol `DOCENTE` existe el perfil de docente.

### TK-016 Perfiles de estudiante y docente
- **Descripción:** backend API-033 a API-038.
- **Documentos:** `MODELO_DATOS.md` §4.8, §4.9, `REGLAS_NEGOCIO.md` RN-004.
- **Dependencias:** TK-012, TK-015.
- **Resultado esperado:** perfiles vinculados a usuario y carrera.
- **Criterios de aceptación:**
  - Un perfil de estudiante exige carrera activa.
  - Un perfil duplicado por usuario devuelve 409.
  - La carrera y el ámbito institucional se conservan como datos de dominio; la restricción por rol o ámbito no está vigente como control de acceso.

### TK-017 Pantallas de usuarios
- **Descripción:** UI-004 y UI-005.
- **Documentos:** `INTERFACES.md` UI-004, UI-005.
- **Dependencias:** TK-015, TK-016, TK-005.
- **Resultado esperado:** gestión de usuarios desde la interfaz.
- **Criterios de aceptación:**
  - Filtros, estados y mensajes de validación implementados.
  - Nunca se muestra contraseña ni hash; los campos de intentos y bloqueo no se usan.

### Fase 5. Empresas y convenios

### TK-018 Empresas y convenios
- **Descripción:** backend API-039 a API-047.
- **Documentos:** `MODELO_DATOS.md` §4.10, §4.14, `REGLAS_NEGOCIO.md` RN-008, RN-010, §4.3.
- **Dependencias:** TK-002. Las referencias anteriores a TK-009 y TK-004 no están vigentes (`SEGURIDAD.md`, `CONTEXTO_ACTOR.md`).
- **Resultado esperado:** empresas y convenios administrables.
- **Criterios de aceptación:**
  - RUC de 13 dígitos y único.
  - Los convenios devuelven el estado efectivo (incluye `VENCIDO`).
  - Suspender y terminar funcionan; las fechas incoherentes se rechazan.

### TK-019 Miembros y tutores empresariales
- **Descripción:** backend API-048 a API-053.
- **Documentos:** `MODELO_DATOS.md` §4.11, §4.27, `REGLAS_NEGOCIO.md` RN-009, `SEGURIDAD.md` §3.
- **Dependencias:** TK-018, TK-015.
- **Resultado esperado:** vinculación de usuarios a empresas.
- **Criterios de aceptación:**
  - El primer miembro debe ser `ADMIN_EMPRESA`; la empresa no queda sin administrador activo.
  - Los miembros se desactivan, no se eliminan.
  - `REPRESENTANTE_EMPRESA` y `ADMIN_EMPRESA` son datos de dominio; la restricción de operación por empresa no está vigente como control de acceso.

### TK-020 Pantallas de empresas
- **Descripción:** UI-010 y UI-011.
- **Documentos:** `INTERFACES.md` UI-010, UI-011.
- **Dependencias:** TK-018, TK-019, TK-005.
- **Resultado esperado:** gestión de empresas desde la interfaz.
- **Criterios de aceptación:**
  - Las cuatro pestañas de UI-011 funcionan como detalle institucional en `/empresas/:id`.
  - No hay vista «Mi empresa» ni operaciones condicionadas a un representante autenticado; API-086 no está vigente.

### Fase 6. Ofertas

### TK-021 Ofertas: cabecera y ciclo de estados
- **Descripción:** backend API-054 a API-058 y API-062 a API-067.
- **Documentos:** `MODELO_DATOS.md` §4.28, §7.2, `REGLAS_NEGOCIO.md` RN-011 a RN-016, §4.5, `FLUJOS.md` FL-003, FL-004, FL-015.
- **Dependencias:** TK-018, TK-019, TK-013, TK-014, TK-004.
- **Resultado esperado:** ofertas con su ciclo completo hasta publicación y cierre.
- **Criterios de aceptación:**
  - Cada transición se verifica contra la tabla de §4.5 de `REGLAS_NEGOCIO.md`.
  - Rechazar y cancelar exigen motivo.
  - Editar una oferta `PUBLICADA` la devuelve a `PENDIENTE_REVISION`.
  - Los snapshots, fechas de publicación y resolución no se escriben desde el código; se relee el valor final tras `flush()`.
  - `PUBLICADOR` y `ADMIN_EMPRESA` se conservan como datos de dominio; la limitación por rol o empresa no está vigente como control de acceso.

### TK-022 Perfiles de oferta
- **Descripción:** backend API-059 a API-061, con carreras y requisitos laborales.
- **Documentos:** `MODELO_DATOS.md` §4.30 a §4.32, `REGLAS_NEGOCIO.md` RN-013.
- **Dependencias:** TK-021.
- **Resultado esperado:** perfiles con plazas, carreras y requisitos.
- **Criterios de aceptación:**
  - Plazas > 0 y nombre único en la oferta.
  - No se reducen plazas por debajo de las aceptadas.
  - Un perfil con postulaciones no se elimina.
  - Sin carreras asignadas, el perfil queda abierto a todas.

### TK-023 Pantallas de ofertas del representante
- **Descripción:** UI-012 (vista del representante), UI-013 y UI-014 (acciones del representante).
- **Documentos:** `INTERFACES.md` UI-012 a UI-014.
- **Dependencias:** TK-021, TK-022, TK-005.
- **Resultado esperado:** la empresa crea, edita y envía ofertas.
- **Criterios de aceptación:**
  - Se advierte que editar una oferta publicada la devuelve a revisión.
  - Se muestran las observaciones de una oferta rechazada.
  - Las acciones visibles dependen del estado de la oferta; el rol de empresa es dato de dominio, no permiso.

### TK-024 Pantallas de revisión de ofertas (coordinador)
- **Descripción:** UI-012 y UI-014 con las acciones del coordinador.
- **Documentos:** `INTERFACES.md` UI-012, UI-014, `FLUJOS.md` FL-015.
- **Dependencias:** TK-021, TK-005.
- **Resultado esperado:** bandeja de revisión con aprobar, rechazar, cerrar y cancelar.
- **Criterios de aceptación:**
  - Las ofertas en `PENDIENTE_REVISION` se destacan.
  - Rechazar y cancelar piden motivo antes de enviar.
  - Si la aprobación falla, se muestra la causa devuelta por la API.

### TK-025 Oportunidades del estudiante
- **Descripción:** UI-015 y la consulta de detalle de UI-016.
- **Documentos:** `INTERFACES.md` UI-015, UI-016, `CONTRATOS_REST.md` API-055, API-056.
- **Dependencias:** TK-021, TK-022, TK-005.
- **Resultado esperado:** el estudiante consulta ofertas de su carrera.
- **Criterios de aceptación:**
  - Solo aparecen ofertas `PUBLICADA` dentro de plazo con perfiles dirigidos a su carrera.
  - Un perfil sin plazas se muestra como cerrado.
  - El estudiante no accede a ofertas en otros estados.

### Fase 7. Postulaciones

### TK-026 Postulaciones del estudiante
- **Descripción:** backend API-068, API-069, API-072, API-073 y API-074.
- **Documentos:** `MODELO_DATOS.md` §4.34, `REGLAS_NEGOCIO.md` RN-017, RN-019, §4.6, `FLUJOS.md` FL-008, FL-009.
- **Dependencias:** TK-016, TK-022, TK-004.
- **Resultado esperado:** postular, editar mensaje y retirar.
- **Criterios de aceptación:**
  - La postulación se registra para un estudiante existente; no hay usuario autenticado ni actor de sesión. El criterio anterior de "estudiante autenticado" no está vigente.
  - Se rechazan oferta no publicada o vencida, convenio no vigente, carrera no incluida, perfil sin plazas y postulación duplicada.
  - Los requisitos académicos aplicables se validan en la API solo sobre los registros existentes (RD-021; CR-6 resuelto: validación en la API, módulo de definición futuro). No se insertan catálogos, requisitos ni datos institucionales ficticios; un listado vacío se gestiona sin generar validaciones inventadas.
  - La visualización restringida por propietario no está vigente como control de acceso; las reglas de elegibilidad y plazas sí se aplican.

### TK-027 Recomendación, resolución y formalización
- **Descripción:** backend API-070, API-071 y API-075 a API-079; aceptar crea la práctica `APROBADA` en la misma transacción.
- **Documentos:** `REGLAS_NEGOCIO.md` RN-018 a RN-021, §4.6, `FLUJOS.md` FL-005, FL-016, `MODELO_DATOS.md` §7.2.
- **Dependencias:** TK-026, TK-019.
- **Resultado esperado:** decisión de coordinación con práctica generada.
- **Criterios de aceptación:**
  - Aceptar genera la práctica con las condiciones congeladas y la carrera actual del estudiante en la misma transacción; una postulación `ACEPTADA` nunca queda sin práctica.
  - La disponibilidad de plazas se verifica y reserva dentro de la transacción que acepta la postulación; `postulacion` y `practica` usan `@Version`. No se introducen bloqueos distribuidos, colas ni mecanismos externos.
  - Dos aceptaciones simultáneas de la última plaza: solo una se confirma; la otra responde 409 `CONFLICTO_CONCURRENCIA`.
  - Se rechaza la aceptación sin plaza, sin convenio vigente o con otra práctica vigente en el periodo.
  - Rechazar y cancelar exigen motivo; cancelar exige práctica cancelada o rechazada.
  - La recomendación se rige por las reglas de negocio de la empresa como dato de dominio; la restricción por rol o ámbito no está vigente como control de acceso.

### TK-028 Pantallas de postulación del estudiante
- **Descripción:** acción de postular en UI-016 y pantalla UI-017.
- **Documentos:** `INTERFACES.md` UI-016, UI-017.
- **Dependencias:** TK-025, TK-026.
- **Resultado esperado:** el estudiante se postula y sigue sus solicitudes.
- **Criterios de aceptación:**
  - Se muestra la causa cuando la postulación es rechazada por reglas.
  - Retirar pide confirmación y solo está disponible en `PENDIENTE`.
  - No se muestra la recomendación de la empresa al estudiante.

### TK-029 Pantallas de postulaciones (coordinador y representante)
- **Descripción:** UI-018 y UI-019.
- **Documentos:** `INTERFACES.md` UI-018, UI-019.
- **Dependencias:** TK-027, TK-005.
- **Resultado esperado:** recomendar y resolver desde la interfaz.
- **Criterios de aceptación:**
  - La empresa se conserva como dato de dominio; la visibilidad restringida por representante no está vigente como control de acceso.
  - Las acciones dependen del estado de la postulación; el rol no concede permisos.
  - Tras aceptar se ofrece el enlace a la práctica creada.

### Fase 8. Prácticas y asignación

### TK-030 Prácticas y asignación de responsables
- **Descripción:** backend API-080 a API-082 y API-084; API-083 (`iniciar`) queda como mejora futura (CR-8).
- **Documentos:** `MODELO_DATOS.md` §4.16, §7.2, `REGLAS_NEGOCIO.md` RN-025, RN-026, §4.7, `FLUJOS.md` FL-017.
- **Dependencias:** TK-027, TK-016, TK-019.
- **Resultado esperado:** prácticas consultables y con responsables asignados.
- **Criterios de aceptación:**
  - El tutor empresarial debe ser de la empresa de la práctica.
  - La asignación exige tutor de la empresa y fechas; la transición a `EN_CURSO` (API-083) no se implementa en el MVP (CR-8).
  - Cancelar cancela la postulación y libera la plaza.
  - La propiedad, la empresa y el ámbito son datos de dominio; la visibilidad por rol no está vigente como control de acceso.
  - Se usa `@Version` en `practica` para la concurrencia; un conflicto al asignar responde 409 `CONFLICTO_CONCURRENCIA`. La asignación de tutores y fechas está en el MVP; `iniciar` (API-083) no.

### TK-031 Pantallas de prácticas
- **Descripción:** UI-020 y UI-021.
- **Documentos:** `INTERFACES.md` UI-020, UI-021.
- **Dependencias:** TK-030, TK-005.
- **Resultado esperado:** consulta de prácticas y asignación desde la interfaz.
- **Criterios de aceptación:**
  - Las acciones de asignar y cancelar se muestran sin restricción por rol; «iniciar» no aparece en el MVP (CR-8).
  - Los selectores de tutor muestran solo tutores de la empresa de la práctica.

### Fase 9. Auditoría, cierre y validación

### TK-032 Auditoría
- **Descripción:** backend API-085 y pantalla UI-022.
- **Documentos:** `MODELO_DATOS.md` §4.26, `REGLAS_NEGOCIO.md` RN-039, `INTERFACES.md` UI-022.
- **Dependencias:** TK-009, TK-005.
- **Resultado esperado:** consulta de auditoría de solo lectura.
- **Criterios de aceptación:**
  - Ningún endpoint modifica ni elimina registros de auditoría.
  - Los filtros por usuario, entidad y fechas funcionan.
  - El filtrado por rol o ámbito no está vigente como control de acceso; los roles y carreras son datos de dominio.

### TK-033 Cierre automático de ofertas vencidas
- **Descripción:** programar la ejecución periódica de `cerrar_ofertas_vencidas()`.
- **Documentos:** `FLUJOS.md` FL-023, `MODELO_DATOS.md` §7.2, `REGLAS_NEGOCIO.md` RN-016.
- **Dependencias:** TK-021. La referencia anterior a TK-004 no está vigente (`CONTEXTO_ACTOR.md`).
- **Resultado esperado:** ofertas vencidas pasan a `CERRADA` sin intervención.
- **Criterios de aceptación:**
  - El cierre automático se ejecuta sin fijar `praxis.sistema`: ese parámetro ya no se configura desde la aplicación; los triggers que lo requieran pueden rechazar la operación.
  - No se inventa un usuario \"sistema\" ni un identificador fijo para evitar el rechazo.
  - Las postulaciones pendientes de ofertas cerradas siguen pudiendo resolverse.

### TK-034 Verificación de reglas (parte de seguridad no vigente)
- **Descripción:** pruebas de los escenarios de `FLUJOS.md` §6 y de las reglas académicas y de negocio vigentes. La parte de seguridad de la tarea queda eliminada y pendiente a futuro.
- **Documentos:** `FLUJOS.md`, `REGLAS_NEGOCIO.md`, `SEGURIDAD.md`, `CONTEXTO_ACTOR.md`.
- **Dependencias:** TK-021 a TK-030.
- **Resultado esperado:** evidencia de cumplimiento de reglas de negocio; sin verificación de permisos porque no existen.
- **Criterios de aceptación:**
  - No se prueban login, sesión, cookies, roles ni permisos: no están vigentes.
  - Las ofertas no publicadas no se exponen como oportunidades por regla de negocio, no por autorización.
  - Las estructuras de entrada no aceptan estado, fechas de resolución, quién resolvió ni identificadores del actor.
  - Las estructuras de salida no exponen credenciales ni campos de seguridad.

### TK-035 Revisión final de la documentación
- **Descripción:** recorrer el checklist de la guía maestra y resolver contradicciones entre documentos.
- **Documentos:** todos.
- **Dependencias:** TK-034.
- **Resultado esperado:** documentación consistente con lo implementado.
- **Criterios de aceptación:**
  - Los pendientes de `CONTRATOS_REST.md`, `INTERFACES.md` y `SEGURIDAD.md` están resueltos o registrados.
  - Los comandos de `AGENTS.md` están verificados.
  - No hay contradicciones entre archivos.

## 4. Dependencias

| Fase | Depende de |
|---|---|
| 1 | Configuración del entorno y acceso a la base. |
| 2 | No vigente; pendiente a futuro. No es dependencia de las fases académicas. |
| 3 | Fase 1. |
| 4 | Fase 3. |
| 5 | Fase 4. |
| 6 | Fases 3 y 5. |
| 7 | Fases 4 y 6. |
| 8 | Fases 5 y 7. |
| 9 | Fases anteriores. |

Decisiones ya resueltas: CR-1 y CR-4 a CR-8 (prefijo de la API, correcciones administrativas sin control de acceso por rol, validaciones académicas, paginación y límite del MVP). CR-2 y CR-3 están vigentes solo como login básico y filtro visual de pantallas; sesión, cookies, permisos reales, intentos y bloqueos quedan pendientes a futuro.

Pendientes que no bloquean el MVP: diseño de fases posteriores a la asignación y decisiones de módulos futuros (reportes, módulo de requisitos, `EN_CURSO` y etapas siguientes). No hay parámetros de sesión, cookie, intentos ni bloqueo: esas funciones fueron eliminadas.

## 5. Criterios de aceptación generales

Se aplican a toda tarea además de los propios:

1. Respeta la arquitectura `Resource → Service → Repository → Entity` y el módulo de referencia.
2. No modifica el esquema de la base, no crea migraciones ni scripts de alteración.
3. Consulta `MODELO_DATOS.md` antes de crear o cambiar entidades.
4. No declara roles en el Resource ni verifica propiedad o ámbito por actor: no hay autorización por rol en la aplicación. Los roles y carreras se conservan como datos de dominio.
5. No expone credenciales ni secretos.
6. No implementa funcionalidades fuera del MVP.
7. No introduce sesiones, cookies, JWT ni tokens de acceso.
8. El comportamiento puede verificarse con pruebas automatizadas o con un procedimiento manual descrito.

## 6. Estado de avance

| Tarea | Responsable | Estado |
|---|---|---|
| TK-001 a TK-035 | Por asignar | Pendiente |

Estados permitidos: Pendiente, En elaboración, En revisión, Aprobado. Al asignar responsables, desglosar esta tabla tarea por tarea. Las tareas no vigentes (Fase 2, TK-004 y la parte de seguridad de TK-034) se conservan solo por trazabilidad.

### 6.1. Seguimiento individual de la Fase 5

Desglose autorizado de la fila global, que **se conserva sin cambios**. El usuario aprobó formalmente TK-018, TK-019 y TK-020 el 22 de noviembre de 2026; el valor `Aprobado` es el estado permitido por esta sección. La Fase 5 quedó cerrada formalmente con limitaciones documentadas. La aprobación **no implica certificación de integración**: se mantienen visibles la ausencia de pruebas contra PostgreSQL, la no verificación de triggers ni de `v_convenio_estado`, la falta de certificación de concurrencia real de API-050 y la ausencia de pruebas manuales de navegador. Las evidencias, el inventario de cambios y las limitaciones están consolidados en §6.3.

| Tarea | Responsable | Estado | Evidencia |
|---|---|---|---|
| TK-018 Empresas y convenios | Por asignar | Aprobado | Pruebas unitarias de `EmpresaRepositoryTest`, `EmpresaServiceTest`, `ConvenioServiceTest` y `ConvenioResourceTest`; build frontend; detalle en §6.3 |
| TK-019 Miembros y tutores empresariales | Por asignar | Aprobado | Pruebas unitarias de `EmpresaUsuarioServiceTest`, `EmpresaUsuarioRepositoryTest`, `EmpresaUsuarioResourceTest` y `TutorEmpresarialResourceTest`; build frontend; detalle en §6.3 |
| TK-020 Pantallas de empresas | Por asignar | Aprobado | UI-010 y UI-011 implementadas como detalle institucional; build frontend; sin prueba manual de navegador; detalle en §6.3. API-086 no está vigente |

El consolidado, el inventario de cambios, las evidencias y las limitaciones están en §6.3. Esa sección distingue implementación, auditoría documental, pruebas ejecutadas y validaciones no ejecutadas.

### 6.2. Seguimiento individual de la estructura académica

La fila global de la sección 6 se conserva sin cambios. El usuario aprobó formalmente TK-012 y TK-013 el 22 de noviembre de 2026; el valor `Aprobado` es el estado permitido por esta sección. El usuario aprobó formalmente TK-014 el 22 de noviembre de 2026 como **Aprobada con observaciones**: el estado `Aprobado` se conserva por ser el único valor permitido en la sección 6, y las observaciones (B-2, prioridad baja) quedan registradas en la evidencia, en `docs/INTERFACES.md` y en §6.3. La aprobación **no certifica pruebas manuales de navegador, integración con PostgreSQL ni verificación de concurrencia real**. Las evidencias y limitaciones están consolidadas en §6.3.

| Tarea | Responsable | Estado | Evidencia |
|---|---|---|---|
| TK-012 Carreras | Por asignar | Aprobado | UI-007 implementada sobre API-010 a API-013; build frontend correcto; auditoría estática; M-1 corregido con contador de solicitudes mediante `useRef`; sin prueba manual de navegador; detalle en §6.3 |
| TK-013 Periodos académicos | Por asignar | Aprobado | UI-008 implementada sobre API-014 a API-019 en cuatro archivos autorizados; build frontend correcto; `PeriodoAcademicoServiceTest` 7/7; M-1, M-2 y B-1 corregidos; auditoría final sin hallazgos; sin navegador, PostgreSQL real ni concurrencia real; detalle en §6.3 |
| TK-014 Tipos de práctica | Por asignar | Aprobado | Aprobada con observaciones; UI-009 implementada sobre API-020, API-022 y API-023 en cuatro archivos autorizados; build frontend correcto; auditoría con dictamen APROBADA CON OBSERVACIONES; B-1 corregido; B-2 pendiente (prioridad baja); sin navegador, PostgreSQL real ni concurrencia real; detalle en §6.3 |

TK-011 / UI-006 (Facultades) también está implementada en `/admin/facultades` sobre API-005 a API-009. No hay registro de aprobación formal en esta documentación; su estado no se modifica. Las observaciones de accesibilidad y manejo de errores están en `docs/INTERFACES.md`.

### 6.3. Evidencias, decisiones y limitaciones consolidadas

Esta sección conserva la información vigente de las entregas aprobadas para que los documentos de cierre no sean necesarios como fuente independiente. No registra aprobaciones nuevas ni modifica los estados de §6.1 y §6.2. Las decisiones permanentes siguen en su documento: arquitectura en `ARQUITECTURA.md`, estado de seguridad en `SEGURIDAD.md`, contratos en `CONTRATOS_REST.md`, reglas en `REGLAS_NEGOCIO.md`, modelo en `MODELO_DATOS.md` e interfaces en `INTERFACES.md`. El material relativo a autenticación, sesión, permisos, actor o «Mi empresa» se conserva solo como evidencia histórica y no está vigente.

#### 6.3.1. Fase 5 (TK-018, TK-019 y TK-020)

**Decisiones y casos vigentes.** API-039 a API-047 (empresas y convenios) y API-048 a API-053 (miembros y tutores) permanecen según sus contratos académicos y de dominio. Las correcciones aceptadas se reflejan en los documentos correspondientes: RUC de 13 dígitos con 400 para formato inválido y 409 para duplicado; `estadoEfectivo` con `VENCIDO` derivado de `v_convenio_estado` sin escribir columnas gestionadas por la base; `ConvenioResumen` expuesto por el Service sin DTO independiente; bloqueo pesimista de la fila de empresa; miembros con bajas lógicas sin dejar la empresa sin `ADMIN_EMPRESA`; y API-053 con `id` del tutor en el cuerpo y `empresaId` en la ruta. No están vigentes API-086 (`GET /empresas/miEmpresa`), el acceso REP a «Mi empresa», el contexto de actor en API-049 y API-050 ni la matriz de permisos; esos elementos quedan pendientes a futuro (`CONTRATOS_REST.md`, `CONTEXTO_ACTOR.md`, `SEGURIDAD.md`).

**Correcciones aceptadas por bloque (evidencia histórica).** Bloque 1: AL-09 (membresía efectiva), AL-01 (sin contexto de actor en empresas/convenios), AL-02 y MED-10 (estado efectivo), MED-08 (bloqueo pesimista) y MED-01 (RUC inválido 400 / duplicado 409). Bloque 2: AL-04 (SQL nativo de solo lectura justificado), AL-07 (actor desde `SecurityIdentity` y `empresaId` validado), AL-10 (alcance de `ContextoActorService`), MED-02 (Resource sin dependencia del Repository), MED-03 (API-053 documentada) y MED-04 (permisos solo documentados, sin cambios). Bloque 3: MED-09 (contexto de navegación frente a roles múltiples), LOW-03 (visibilidad REP condicionada a API-086), LOW-04 (reinicio de pestaña al cambiar de empresa), LOW-05 (una sola llamada a API-086) y LOW-06 (sin mensaje prematuro de permiso). Bloque 4: CR-01 (seguimiento individual), CR-02 (cierres individuales), CR-03 (distinción entre registrar, editar, gestionar y consultar), AL-03 (rutas implementadas), AL-05 (README actualizados), AL-06 (API-004 pendiente), AL-08 (valores vigentes frente a decisiones aprobadas), MED-05 (referencia obsoleta), MED-06 y MED-07 (procedencia de la evidencia) y MED-08 (limitaciones de integración). Los hallazgos referidos a actor, `SecurityIdentity`, `ContextoActorService`, roles, permisos, API-086 y visibilidad REP corresponden a decisiones ya removidas y no están vigentes. Las validaciones de integración y navegador siguen pendientes; API-004 y UI-002 quedan pendientes a futuro.

**Pruebas ejecutadas.** Validación final del 22 de noviembre de 2026, JDK 25.0.2, Maven offline, sin `clean`, sin integración y sin base de datos:

| Clase | Pruebas | Fallos | Errores |
|---|---:|---:|---:|
| `ConvenioServiceTest` | 21 | 0 | 0 |
| `ConvenioResourceTest` | 2 | 0 | 0 |
| `EmpresaUsuarioRepositoryTest` | 8 | 0 | 0 |
| `EmpresaUsuarioServiceTest` | 12 | 0 | 0 |
| `TutorEmpresarialResourceTest` | 5 | 0 | 0 |
| `EmpresaUsuarioResourceTest` | 5 | 0 | 0 |
| `EmpresaResourceMiEmpresaTest` (histórico, no vigente) | 3 | 0 | 0 |
| `EmpresaServiceTest` | 27 | 0 | 0 |
| `EmpresaRepositoryTest` | 2 | 0 | 0 |
| **Total** | **85** | **0** | **0** |

Build frontend de esa validación: `npm run build`, 56 módulos transformados, bundle `index-CSa_d07j.js` de 273,88 kB, sin errores.

**Evidencia histórica.** La ejecución anterior de TK-020 registró `EmpresaServiceTest` 24 pruebas, `EmpresaUsuarioRepositoryTest` 7 y `EmpresaResourceMiEmpresaTest` 3, con bundle `index-BLbwiBZb.js` de 273,05 kB; se conserva como reporte histórico, no como ejecución de las correcciones de Fase 5. `target/surefire-reports` refleja solo la ejecución más reciente. En Fase 1.1 se estableció una conexión JDBC de solo lectura; no se repitió en Fase 5.

**Limitaciones.** No se ejecutaron `@QuarkusTest`, la suite Maven completa, integración con PostgreSQL o Supabase, triggers de `empresa_usuario`, consulta real de `v_convenio_estado`, concurrencia real de API-050 ni pruebas manuales de navegador. API-004 y UI-002 están pendientes a futuro. La revisión frontend fue build y análisis estático. En esa fase no había matriz de permisos vigente: `SEGURIDAD.md` documentaba la eliminación de autenticación y autorización, y `INTERFACES.md` UI-011 describía solo operaciones institucionales.

**Nota posterior al login básico.** La limitación anterior corresponde a TK-020. El prototipo actual sí incorpora `POST /usuarios/login` y una matriz visual de módulos por rol en `src/main/webui/src/permisos.js`, verificada en navegador; no reintroduce sesión, cookies ni autorización en la API. La matriz está documentada en `INTERFACES.md` §2 y la limitación de seguridad en `SEGURIDAD.md`.

**Inventario de cambios (histórico).** Backend: `EmpresaRepository.java` (bloqueo pesimista), `EmpresaService.java` (RUC, convenios y reglas de empresa), `EmpresaUsuarioService.java` (membresía efectiva y reglas), `EmpresaUsuarioRepository.java` (consultas de membresía y justificación de SQL nativo), `EmpresaResource.java`, `EmpresaUsuarioResource.java` (API-048 a API-050), `TutorEmpresarialResource.java` (API-051 a API-053), `ConvenioService.java` (`ConvenioResumen`) y `ConvenioResource.java` (publicación de esa estructura). Frontend: `AppRoutes.jsx`, `Navegacion.jsx`, `LayoutPrincipal.jsx`, `DetalleEmpresa.jsx` y `empresaService.js`; `MiEmpresa.jsx` y las referencias a API-086 son evidencia histórica no vigente. Pruebas: `EmpresaRepositoryTest`, `EmpresaServiceTest`, `EmpresaUsuarioServiceTest`, `EmpresaResourceMiEmpresaTest` (no vigente), `ConvenioServiceTest`, `EmpresaUsuarioRepositoryTest`, `EmpresaUsuarioResourceTest`, `TutorEmpresarialResourceTest` y `ConvenioResourceTest`. No se modificaron entidades, esquema, migraciones, triggers ni vistas. La documentación técnica vigente está en los documentos indicados al inicio de esta sección.

**Incidencias de ejecución de TK-020 (histórico).** El mensaje inicial llegó truncado y se reanudó con las instrucciones completas; PowerShell requirió comillas en `-Dtest=...`; `mvnw.cmd` necesitó `JAVA_HOME` fijado solo en el ámbito del comando; el constructor de `Administracion` se hizo público para las pruebas del Service. La simplificación de `MiEmpresa` y `MiEmpresaResponse` corresponde a API-086, hoy no vigente. Ninguna incidencia cambió decisiones académicas aprobadas. Nota: la referencia a `JAVA_HOME` quedó obsoleta porque el wrapper ahora usa `java.exe` desde PATH y no requiere esa variable.

#### 6.3.2. Auditoría H-01 / H-02 (histórico; no vigente)

**H-01 (no vigente).** La invalidación de sesiones, el `SesionStore`, `AutenticacionService`, `SesionInvalidacionObserver` y `IdentidadSesionTest` pertenecen al diseño de autenticación eliminado. Las pruebas citadas en su momento (`SesionStoreTest` 21, `AutenticacionServiceTest` 12, `SesionInvalidacionObserverTest` 3, `UsuarioServiceTest` 24 e `IdentidadSesionTest` 5) se conservan solo como registro histórico. No hay sesiones que invalidar ni decisiones de duración, `Max-Age`, nombre de cookie o `SameSite` vigentes.

**H-02 (no vigente).** `ContextoActorService` y el fijado de `praxis.usuario_id` en API-049 y API-050 fueron eliminados. No se fijan `praxis.motivo`, `praxis.sistema` ni `praxis.correccion_decision` desde la aplicación. Los triggers que dependen de esos parámetros pueden rechazar escrituras; el riesgo y el pendiente están en `CONTEXTO_ACTOR.md`.

**Estado actual.** Los dictámenes históricos (H-01 corregido con limitaciones documentadas; H-02 corregido documentalmente) ya no describen funcionalidad vigente. No hay sesiones, cookies, bloqueos, despliegue multiinstancia con sesiones ni validación de identidad. Queda pendiente, sin cambios de esquema, resolver el contexto que exigen los triggers si se decide restablecerlo.

#### 6.3.3. Estructura académica (TK-011 a TK-014)

- TK-011 / UI-006: pantalla de facultades sobre API-005 a API-009. Sin aprobación formal registrada; estado sin cambios. Observaciones de accesibilidad y errores en `docs/INTERFACES.md`.
- TK-012 / UI-007: build frontend correcto (bundle `index-gufZihvK.js`, 285,14 kB) y auditoría estática; M-1 corregido con contador de solicitudes mediante `useRef`. La instantánea de la carrera puede quedar desactualizada por concurrencia; no hay `@Version`. La tabla conserva cinco columnas: Código, Nombre, Facultad, Estado y Acciones. API-011 queda sin consumo directo; el selector de facultades incluye inactivas con identificación visual; `listarCarreras(parametros = {})` conserva las llamadas sin argumentos de otros módulos.
- TK-013 / UI-008: builds correctos tras la implementación (`index-D2xe6L0l.js`, 291,10 kB) y tras las correcciones (`index-Fd_zyRjh.js`, 291,17 kB); `PeriodoAcademicoServiceTest` 7/7; M-1, M-2 y B-1 corregidos; auditoría final sin hallazgos. Sin verificación real de concurrencia entre activaciones. API-015 sin consumo directo; la creación deja el periodo inactivo, la edición no modifica el estado y toda mutación recarga el listado porque API-016 a API-019 devuelven 200/201 sin cuerpo.
- TK-014 / UI-009: builds correctos tras la implementación (`index-BginAHsz.js`, 296,78 kB) y tras B-1 (`index-B8sSOJWa.js`, 296,98 kB); auditoría con dictamen **APROBADA CON OBSERVACIONES**, sin hallazgos críticos, altos ni medios; B-1 corregido con la expresión `/^[+-]?(?:\d+(?:\.\d{0,2})?|\.\d{1,2})$/` aplicada a la cadena original antes de cualquier conversión. Comprobación ejecutada con Node.js: `1`, `1.2`, `1.23`, `+1.23` y `.5` aceptados; `1.234` y `0.001` rechazados por decimales; `0` y `-1` rechazados por rango. API-021 sin consumo directo. B-2 pendiente (prioridad baja). `TipoPractica` no tiene `@Version`: las actualizaciones concurrentes pueden resolverse por último-write-wins.

**Verificación.** Los builds y las pruebas unitarias citados se ejecutaron realmente. Las comprobaciones de contratos, reglas de negocio, rutas, errores y estados se verificaron por inspección y auditoría estática, no contra una instancia real. No se verificaron permisos porque no existen. No se realizaron pruebas manuales de navegador ni pruebas contra PostgreSQL en estas entregas.

#### 6.3.4. Verificación posterior de conexión con Supabase

Esta verificación es posterior a Fase 5 y no forma parte de sus evidencias ni modifica los estados de §6.1 y §6.2. Se comprobaron únicamente dos hechos: Quarkus inició correctamente con la configuración externa, y se ejecutó `SELECT 1`, que devolvió `1`. Esta comprobación de conectividad no demuestra el funcionamiento integral de todas las operaciones de PRAXIS ni sustituye pruebas de integración, verificación de triggers o pruebas manuales de navegador.
