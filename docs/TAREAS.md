# Tareas de implementación

## 1. Objetivo

Organizar la implementación del MVP de PRAXIS en fases y tareas verificables, relacionadas con los documentos de `docs/`. Cada tarea indica sus documentos de referencia, dependencias y criterios de aceptación.

El MVP cubre: usuarios y acceso, estructura académica, perfiles, empresas y convenios, ofertas, postulaciones, asignación de docentes tutores y auditoría (`PRODUCTO.md` §5).

## 2. Fases

| Fase | Nombre | Tareas |
|---|---|---|
| 1 | Preparación y base técnica | TK-001 a TK-005 |
| 2 | Seguridad y acceso | TK-006 a TK-010 |
| 3 | Estructura académica | TK-011 a TK-014 |
| 4 | Usuarios y perfiles | TK-015 a TK-017 |
| 5 | Empresas y convenios | TK-018 a TK-020 |
| 6 | Ofertas | TK-021 a TK-025 |
| 7 | Postulaciones | TK-026 a TK-029 |
| 8 | Prácticas y asignación | TK-030 y TK-031 |
| 9 | Auditoría, cierre y validación | TK-032 a TK-035 |

## 3. Tareas

Todas las tareas tienen Responsable: **Por asignar** y Estado: **Pendiente**.

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

### TK-004 Contexto de sesión para los triggers
- **Descripción:** fijar `praxis.usuario_id`, `praxis.motivo` y `praxis.sistema` dentro de cada transacción de escritura.
- **Documentos:** `MODELO_DATOS.md` §7.3, `ARQUITECTURA.md` §7 y §12, `SEGURIDAD.md`.
- **Dependencias:** TK-007.
- **Resultado esperado:** mecanismo reutilizable por todos los Services.
- **Criterios de aceptación:**
  - Una operación auditable sin usuario autenticado es rechazada.
  - El valor existe solo dentro de la transacción (`set_config(..., true)`).
  - Rechazos y cancelaciones registran su motivo.
  - No se escriben directamente las columnas gestionadas por triggers.

### TK-005 Base del frontend
- **Descripción:** configurar React + Vite en `src/main/webui`: estructura de la aplicación, rutas, cliente HTTP, manejo de 401, guardia de rutas por rol y estados comunes.
- **Documentos:** `INTERFACES.md` §1, §2, §8, §9; `ARQUITECTURA.md`.
- **Dependencias:** TK-001.
- **Resultado esperado:** esqueleto navegable con menú por rol.
- **Criterios de aceptación:**
  - Una ruta sin sesión redirige a `/login`.
  - El menú muestra solo las opciones del rol, y la API sigue autorizando.
  - Los estados de la sección 8 de `INTERFACES.md` están disponibles como componentes.
  - El diseño respeta el estilo definido en `INTERFACES.md` §1 (tomado del login de ejemplo); no existe `PROMPT_PANTALLAS.md` y no se crea uno vacío.

### Fase 2. Seguridad y acceso

### TK-006 Entidades de seguridad
- **Descripción:** crear las entidades y repositorios de `rol`, `usuario`, `usuario_rol` y `usuario_carrera` (claves compuestas con `@EmbeddedId`).
- **Documentos:** `MODELO_DATOS.md` §4.1 a §4.6, `ARQUITECTURA.md` §7.
- **Dependencias:** TK-002.
- **Resultado esperado:** entidades que reflejan la tabla real.
- **Criterios de aceptación:**
  - Nombres y tipos coinciden con `MODELO_DATOS.md`.
  - `hashPassword`, `intentosFallidos` y `bloqueadoHasta` llevan `@JsonIgnore`.
  - No se asigna `id` desde el código.

### TK-007 Autenticación y sesión
- **Descripción:** implementar API-001, API-002 y API-003 con sesiones tradicionales del servidor Quarkus, sesiones sencillas en memoria, cookie HttpOnly, verificación BCrypt y control básico de intentos fallidos y bloqueo temporal. Antes de usar APIs internas de Quarkus, verificar su existencia y compatibilidad con la versión efectiva 3.40.1 sobre los JAR del proyecto.
- **Documentos:** `ARQUITECTURA.md` §11 y §12, `SEGURIDAD.md` §2 y §7, `FLUJOS.md` FL-001, `CONTRATOS_REST.md` §2 y §6.
- **Dependencias:** TK-006; CR-2 resuelto (sesiones tradicionales con cookie HttpOnly).
- **Resultado esperado:** inicio y cierre de sesión funcionales; identidad y roles disponibles en cada petición.
- **Criterios de aceptación:**
  - Contraseña verificada con BCrypt (`BcryptUtil`); nunca se devuelve ni se registra.
  - Cada fallo suma a `intentosFallidos`; superado el límite configurable, la cuenta se bloquea hasta `bloqueadoHasta`.
  - Un acceso correcto reinicia los intentos y registra `ultimoLogin`.
  - Cuenta inactiva, bloqueada o con credenciales erróneas devuelve el mismo 401 genérico.
  - La sesión se almacena en memoria en una clase única; sin tabla de sesiones, sin JWT, sin cookies autocontenidas, sin renovación automática y sin sesiones distribuidas.
  - La cookie contiene solo un identificador aleatorio, con `HttpOnly=true` y `Secure` en HTTPS; su duración, `Max-Age`, nombre y `SameSite` se leen de configuración, sin valores fijos en el código (`SEGURIDAD.md` §2.8).
  - Si la cuenta se bloquea después del ingreso, la sesión activa sigue vigente; el bloqueo solo impide iniciar nuevas sesiones. No se implementa cierre masivo ni consulta a la base en cada petición para detectar bloqueos (`SEGURIDAD.md` §2.7).
  - `logout` invalida la sesión; una petición sin sesión válida responde 401 con `SIN_SESION` y formato `{codigo, mensaje}`.
  - `@Authenticated` y `@RolesAllowed` producen 401/403 con `SIN_SESION`/`SIN_PERMISO` y formato `{codigo, mensaje}`; se verifica si los mappers internos de Quarkus interceptan esas excepciones y, de ser así, se documenta la resolución antes de dar la tarea por terminada.
  - Las APIs internas de Quarkus que se utilicen quedan confinadas en una sola clase adaptadora y se revalidan en cada actualización.
  - El adaptador devuelve la identidad desde `authenticate(...)` como `Uni<SecurityIdentity>`; la implementación de `SecurityIdentity` es una clase interna y se instancia una vez por petición autenticada, sin campo compartido ni llamada manual a `CurrentIdentityAssociation.setIdentity(...)`. No se usa `QuarkusSecurityIdentity`.
  - La identidad contiene solo `id`, correo y roles; en las pruebas se verifica que `CurrentIdentityAssociation.current()` devuelve la identidad de cada petición y que dos usuarios distintos no comparten instancia.
  - La protección CSRF queda fuera del MVP como mejora futura de seguridad; no se implementa ni se valida en esta tarea.

### TK-008 Cambio y restablecimiento de contraseña
- **Descripción:** implementar API-004 y API-030, y la obligación de cambio con `debeCambiarPassword`.
- **Documentos:** `SEGURIDAD.md` §2.5, `FLUJOS.md` FL-002.
- **Dependencias:** TK-007.
- **Resultado esperado:** el usuario cambia su contraseña; el administrador la restablece.
- **Criterios de aceptación:**
  - La nueva contraseña se guarda con BCrypt.
  - Tras el cambio obligatorio, `debeCambiarPassword` queda en falso.
  - Un usuario con cambio obligatorio no accede a otras operaciones hasta cambiarla.
  - No existe recuperación por correo (fuera del MVP).

  **Nota de Fase 5:** API-004 sigue pendiente y no está implementada. `debeCambiarPassword` se registra en API-026 y API-030, pero no bloquea rutas ni operaciones; no se modificaron login, sesión ni cookie para este fin. UI-002 y su redirección obligatoria tampoco están implementadas.

### TK-009 Autorización por rol y ámbito
- **Descripción:** habilitar `@RolesAllowed` con los códigos de rol y un componente reutilizable que verifique propiedad y ámbito (`alcance_global`, `usuario_carrera`, `empresa_usuario`).
- **Documentos:** `SEGURIDAD.md` §3, §4, §5, `REGLAS_NEGOCIO.md` RN-003.
- **Dependencias:** TK-007.
- **Resultado esperado:** infraestructura de autorización usada por todos los módulos.
- **Criterios de aceptación:**
  - Sin sesión → 401; sin rol → 403; registro ajeno o fuera de ámbito → 404.
  - Un usuario con varios roles recibe la unión de permisos.
  - Los listados se filtran en la consulta, no después de traer todos los registros.
  - La lista de verificación de `SEGURIDAD.md` §7.3 se cumple.

### TK-010 Pantallas de acceso
- **Descripción:** construir UI-001, UI-002 y UI-003.
- **Documentos:** `INTERFACES.md` §3, `CONTRATOS_REST.md` API-001 a API-004.
- **Dependencias:** TK-005, TK-007, TK-008.
- **Resultado esperado:** flujo de login, cambio de contraseña e inicio.
- **Criterios de aceptación:**
  - Los estados de cada pantalla están implementados.
  - El login no muestra accesos de demostración ni enlace de recuperación.
  - Con cambio obligatorio, el menú no se muestra.

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
  - El coordinador solo opera en su ámbito.

### TK-017 Pantallas de usuarios
- **Descripción:** UI-004 y UI-005.
- **Documentos:** `INTERFACES.md` UI-004, UI-005.
- **Dependencias:** TK-015, TK-016, TK-005.
- **Resultado esperado:** gestión de usuarios desde la interfaz.
- **Criterios de aceptación:**
  - Filtros, estados y mensajes de validación implementados.
  - Nunca se muestran contraseña, intentos ni bloqueo.

### Fase 5. Empresas y convenios

### TK-018 Empresas y convenios
- **Descripción:** backend API-039 a API-047.
- **Documentos:** `MODELO_DATOS.md` §4.10, §4.14, `REGLAS_NEGOCIO.md` RN-008, RN-010, §4.3.
- **Dependencias:** TK-009, TK-004.
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
  - Un representante solo opera en su empresa.

### TK-020 Pantallas de empresas
- **Descripción:** UI-010 y UI-011.
- **Documentos:** `INTERFACES.md` UI-010, UI-011.
- **Dependencias:** TK-018, TK-019, TK-005.
- **Resultado esperado:** gestión de empresas desde la interfaz.
- **Criterios de aceptación:**
  - Las cuatro pestañas de UI-011 funcionan.
  - El representante ve solo su empresa y no edita convenios.

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
  - Un `PUBLICADOR` solo gestiona sus ofertas; el `ADMIN_EMPRESA`, todas las de su empresa.

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
  - Las acciones visibles dependen del estado y del rol de empresa.

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
  - Postula el estudiante autenticado; nunca se acepta un estudiante enviado por el cliente.
  - Se rechazan oferta no publicada o vencida, convenio no vigente, carrera no incluida, perfil sin plazas y postulación duplicada.
  - Los requisitos académicos aplicables se validan en la API solo sobre los registros existentes (RD-021; CR-6 resuelto: validación en la API, módulo de definición futuro). No se insertan catálogos, requisitos ni datos institucionales ficticios; un listado vacío se gestiona sin generar validaciones inventadas.
  - Un estudiante nunca ve postulaciones ajenas.

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
  - Un representante solo recomienda sobre postulaciones de su empresa.

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
  - El representante solo ve postulaciones de su empresa.
  - Las acciones dependen del rol y estado de la postulación.
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
  - Cada rol ve solo prácticas propias, de su empresa o de su ámbito.
  - Se usa `@Version` en `practica` para la concurrencia; un conflicto al asignar responde 409 `CONFLICTO_CONCURRENCIA`. La asignación de tutores y fechas está en el MVP; `iniciar` (API-083) no.

### TK-031 Pantallas de prácticas
- **Descripción:** UI-020 y UI-021.
- **Documentos:** `INTERFACES.md` UI-020, UI-021.
- **Dependencias:** TK-030, TK-005.
- **Resultado esperado:** consulta de prácticas y asignación desde la interfaz.
- **Criterios de aceptación:**
  - Solo el coordinador ve las acciones de asignar y cancelar; «iniciar» no aparece en el MVP (CR-8).
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
  - El coordinador ve solo su ámbito.

### TK-033 Cierre automático de ofertas vencidas
- **Descripción:** programar la ejecución periódica de `cerrar_ofertas_vencidas()`.
- **Documentos:** `FLUJOS.md` FL-023, `MODELO_DATOS.md` §7.2, `REGLAS_NEGOCIO.md` RN-016.
- **Dependencias:** TK-021, TK-004.
- **Resultado esperado:** ofertas vencidas pasan a `CERRADA` sin intervención.
- **Criterios de aceptación:**
  - Se ejecuta con `praxis.sistema = 'true'`.
  - El historial identifica la operación como del sistema.
  - Las postulaciones pendientes de ofertas cerradas siguen pudiendo resolverse.

### TK-034 Verificación de reglas y seguridad
- **Descripción:** pruebas de los escenarios de `FLUJOS.md` §6 y de la lista de `SEGURIDAD.md` §7.3.
- **Documentos:** `FLUJOS.md`, `REGLAS_NEGOCIO.md`, `SEGURIDAD.md`.
- **Dependencias:** TK-021 a TK-030.
- **Resultado esperado:** evidencia de cumplimiento de reglas y permisos.
- **Criterios de aceptación:**
  - Ninguna operación, salvo el login, responde sin sesión.
  - Un estudiante no accede a postulaciones, ofertas no publicadas ni prácticas ajenas.
  - Los DTO de entrada no aceptan estado ni identificadores del actor.
  - Los DTO de salida no exponen credenciales.

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
| 2 | Fase 1; CR-2 resuelto (sesiones tradicionales con cookie HttpOnly). |
| 3 | Fases 1 y 2. |
| 4 | Fases 2 y 3. |
| 5 | Fases 2 y 4. |
| 6 | Fases 3 y 5. |
| 7 | Fases 4 y 6. |
| 8 | Fases 5 y 7. |
| 9 | Fases anteriores. |

Decisiones ya resueltas: CR-1 a CR-8 (prefijo de la API, sesiones tradicionales con cookie HttpOnly y BCrypt, seguridad básica, correcciones administrativas, permisos, validaciones académicas, paginación y límite del MVP). Ya no bloquean el inicio de las fases.

Pendientes que no bloquean el MVP: valores concretos de los parámetros de sesión (duración, `Max-Age`, nombre de cookie y `SameSite`) y de intentos y bloqueo (configurables, sin fijar en código), eventos de seguridad adicionales en la auditoría, diseño de fases posteriores a la asignación y decisiones de módulos futuros (reportes, módulo de requisitos, `EN_CURSO` y etapas siguientes).

## 5. Criterios de aceptación generales

Se aplican a toda tarea además de los propios:

1. Respeta la arquitectura `Resource → Service → Repository → Entity` y el módulo de referencia.
2. No modifica el esquema de la base, no crea migraciones ni scripts de alteración.
3. Consulta `MODELO_DATOS.md` antes de crear o cambiar entidades.
4. Declara el rol en el Resource y verifica propiedad y ámbito en el Service.
5. No expone credenciales ni secretos.
6. No implementa funcionalidades fuera del MVP.
7. Usa sesiones tradicionales con cookie HttpOnly; no introduce JWT ni tokens de acceso.
7. El comportamiento puede verificarse con pruebas automatizadas o con un procedimiento manual descrito.

## 6. Estado de avance

| Tarea | Responsable | Estado |
|---|---|---|
| TK-001 a TK-035 | Por asignar | Pendiente |

Estados permitidos: Pendiente, En elaboración, En revisión, Aprobado. Al asignar responsables, desglosar esta tabla tarea por tarea.

### 6.1. Seguimiento individual de la Fase 5

Desglose autorizado de la fila global, que **se conserva sin cambios**. El usuario aprobó formalmente TK-018, TK-019 y TK-020 el 22 de noviembre de 2026; el valor `Aprobado` es el estado permitido por esta sección. La Fase 5 quedó cerrada formalmente con limitaciones documentadas. La aprobación **no implica certificación de integración**: se mantienen visibles la ausencia de pruebas contra PostgreSQL, la no verificación de triggers ni de `v_convenio_estado`, la falta de certificación de concurrencia real de API-050 y la ausencia de pruebas manuales de navegador. Las evidencias, el inventario de cambios y las limitaciones están consolidados en §6.3.

| Tarea | Responsable | Estado | Evidencia |
|---|---|---|---|
| TK-018 Empresas y convenios | Por asignar | Aprobado | Pruebas unitarias de `EmpresaRepositoryTest`, `EmpresaServiceTest`, `ConvenioServiceTest` y `ConvenioResourceTest`; build frontend; detalle en §6.3 |
| TK-019 Miembros y tutores empresariales | Por asignar | Aprobado | Pruebas unitarias de `EmpresaUsuarioServiceTest`, `EmpresaUsuarioRepositoryTest`, `EmpresaUsuarioResourceTest` y `TutorEmpresarialResourceTest`; build frontend; detalle en §6.3 |
| TK-020 Pantallas de empresas | Por asignar | Aprobado | API-086, UI-010 y UI-011 implementadas; build frontend; sin prueba manual de navegador; detalle en §6.3 |

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

Esta sección conserva la información vigente de las entregas aprobadas para que los documentos de cierre no sean necesarios como fuente independiente. No registra aprobaciones nuevas ni modifica los estados de §6.1 y §6.2. Las decisiones permanentes siguen en su documento: arquitectura en `ARQUITECTURA.md`, permisos en `SEGURIDAD.md`, contratos en `CONTRATOS_REST.md`, reglas en `REGLAS_NEGOCIO.md`, modelo en `MODELO_DATOS.md` e interfaces en `INTERFACES.md`.

#### 6.3.1. Fase 5 (TK-018, TK-019 y TK-020)

**Decisiones y casos vigentes.** API-039 a API-047 (empresas y convenios), API-048 a API-053 (miembros y tutores) y API-086 (`GET /empresas/miEmpresa`) permanecen según sus contratos. Las correcciones aceptadas se reflejan en los documentos correspondientes: RUC de 13 dígitos con 400 para formato inválido y 409 para duplicado; `estadoEfectivo` con `VENCIDO` derivado de `v_convenio_estado` sin escribir columnas gestionadas por la base; `ConvenioResumen` expuesto por el Service sin DTO independiente; bloqueo pesimista de la fila de empresa; miembros con bajas lógicas sin dejar la empresa sin `ADMIN_EMPRESA`; contexto de actor solo en API-049 y API-050; API-053 con `id` del tutor en el cuerpo y `empresaId` en la ruta; API-086 con 0 membresías → 404 y más de una → 409, sin revelar identificadores; acceso REP a «Mi empresa» condicionado a API-086, con `PUBLICADOR` sin acceso. La matriz de permisos no se amplió ni se restringió.

**Correcciones aceptadas por bloque.** Bloque 1: AL-09 (membresía efectiva), AL-01 (sin contexto de actor en empresas/convenios), AL-02 y MED-10 (estado efectivo), MED-08 (bloqueo pesimista) y MED-01 (RUC inválido 400 / duplicado 409). Bloque 2: AL-04 (SQL nativo de solo lectura justificado), AL-07 (actor desde `SecurityIdentity` y `empresaId` validado), AL-10 (alcance de `ContextoActorService`), MED-02 (Resource sin dependencia del Repository), MED-03 (API-053 documentada) y MED-04 (permisos solo documentados, sin cambios). Bloque 3: MED-09 (contexto de navegación frente a roles múltiples), LOW-03 (visibilidad REP condicionada a API-086), LOW-04 (reinicio de pestaña al cambiar de empresa), LOW-05 (una sola llamada a API-086) y LOW-06 (sin mensaje prematuro de permiso). Bloque 4: CR-01 (seguimiento individual), CR-02 (cierres individuales), CR-03 (distinción entre registrar, editar, gestionar y consultar), AL-03 (rutas implementadas), AL-05 (README actualizados), AL-06 (API-004 pendiente), AL-08 (valores vigentes frente a decisiones aprobadas), MED-05 (referencia obsoleta), MED-06 y MED-07 (procedencia de la evidencia) y MED-08 (limitaciones de integración). Todos los hallazgos autorizados fueron tratados; no se rehicieron decisiones aceptadas ni se revirtieron bloques anteriores. No quedan hallazgos funcionales pendientes de esos bloques: solo las validaciones de integración y navegador, más API-004 y UI-002, que están fuera de la fase.

**Pruebas ejecutadas.** Validación final del 22 de noviembre de 2026, JDK 25.0.2, Maven offline, sin `clean`, sin integración y sin base de datos:

| Clase | Pruebas | Fallos | Errores |
|---|---:|---:|---:|
| `ConvenioServiceTest` | 21 | 0 | 0 |
| `ConvenioResourceTest` | 2 | 0 | 0 |
| `EmpresaUsuarioRepositoryTest` | 8 | 0 | 0 |
| `EmpresaUsuarioServiceTest` | 12 | 0 | 0 |
| `TutorEmpresarialResourceTest` | 5 | 0 | 0 |
| `EmpresaUsuarioResourceTest` | 5 | 0 | 0 |
| `EmpresaResourceMiEmpresaTest` | 3 | 0 | 0 |
| `EmpresaServiceTest` | 27 | 0 | 0 |
| `EmpresaRepositoryTest` | 2 | 0 | 0 |
| **Total** | **85** | **0** | **0** |

Build frontend de esa validación: `npm run build`, 56 módulos transformados, bundle `index-CSa_d07j.js` de 273,88 kB, sin errores.

**Evidencia histórica.** La ejecución anterior de TK-020 registró `EmpresaServiceTest` 24 pruebas, `EmpresaUsuarioRepositoryTest` 7 y `EmpresaResourceMiEmpresaTest` 3, con bundle `index-BLbwiBZb.js` de 273,05 kB; se conserva como reporte histórico, no como ejecución de las correcciones de Fase 5. `target/surefire-reports` refleja solo la ejecución más reciente. En Fase 1.1 se estableció una conexión JDBC de solo lectura; no se repitió en Fase 5.

**Limitaciones.** No se ejecutaron `@QuarkusTest`, la suite Maven completa, integración con PostgreSQL o Supabase, triggers de `empresa_usuario`, consulta real de `v_convenio_estado`, concurrencia real de API-050 ni pruebas manuales de navegador. API-004 y UI-002 siguen pendientes. La revisión frontend fue build y análisis estático. La matriz completa de permisos sigue en `SEGURIDAD.md` §4.2 y la de operaciones por rol y pestaña en `INTERFACES.md` UI-011.

**Inventario de cambios.** Backend: `EmpresaRepository.java` (bloqueo pesimista y ámbito), `EmpresaService.java` (RUC, ámbito y API-086, sin contexto de actor), `EmpresaUsuarioService.java` (membresía efectiva y reglas), `EmpresaUsuarioRepository.java` (consultas de membresía y justificación de SQL nativo), `EmpresaResource.java` (API-086 y actor), `EmpresaUsuarioResource.java` (API-048 a API-050), `TutorEmpresarialResource.java` (API-051 a API-053), `ConvenioService.java` (`ConvenioResumen`) y `ConvenioResource.java` (publicación de esa estructura). Frontend: `AppRoutes.jsx`, `Navegacion.jsx`, `LayoutPrincipal.jsx`, `MiEmpresa.jsx`, `DetalleEmpresa.jsx` y `empresaService.js`. Pruebas: `EmpresaRepositoryTest`, `EmpresaServiceTest`, `EmpresaUsuarioServiceTest`, `EmpresaResourceMiEmpresaTest`, `ConvenioServiceTest`, `EmpresaUsuarioRepositoryTest`, `EmpresaUsuarioResourceTest`, `TutorEmpresarialResourceTest` y `ConvenioResourceTest` (nuevo). No se modificaron entidades, esquema, migraciones, triggers ni vistas. La documentación técnica vigente está en los documentos indicados al inicio de esta sección.

**Incidencias de ejecución de TK-020.** El mensaje inicial llegó truncado y se reanudó con las instrucciones completas; PowerShell requirió comillas en `-Dtest=...`; `mvnw.cmd` necesitó `JAVA_HOME` fijado solo en el ámbito del comando; el constructor de `Administracion` se hizo público para las pruebas del Service; y la revisión final simplificó `MiEmpresa` en el Service para dejar `MiEmpresaResponse` como única estructura de salida, reejecutando pruebas y build. Ninguna incidencia cambió decisiones aprobadas.

#### 6.3.2. Auditoría H-01 / H-02

**H-01.** La invalidación de sesiones usa una generación por usuario: el login obtiene la generación antes de consultar roles; la creación y la invalidación comparten cerrojo; la búsqueda descarta sesiones obsoletas; el evento se publica con `@Observes(during = TransactionPhase.AFTER_SUCCESS)`. La decisión de no depurar el mapa de generaciones está en `SEGURIDAD.md` §2.6 y `ARQUITECTURA.md` §11. Pruebas ejecutadas: `SesionStoreTest` 21, `AutenticacionServiceTest` 12, `SesionInvalidacionObserverTest` 3, `UsuarioServiceTest` 24 e `IdentidadSesionTest` 5 (65 enfocadas, 0 fallos); suite unitaria amplia de 216 pruebas, 0 fallos, sin las tres clases `@QuarkusTest`; `test-compile` offline finalizó con `BUILD SUCCESS`. Las pruebas concurrentes usaron `CountDownLatch`, no esperas basadas solo en `sleep`.

**H-02.** `ContextoActorService` fija únicamente `praxis.usuario_id` en las escrituras de `empresa_usuario` (API-049 y API-050) mediante `set_config(..., true)`. No fija `praxis.motivo`, `praxis.sistema` ni `praxis.correccion_decision`; no implementa auditoría genérica. Las ampliaciones de API-079 y API-085 quedan sujetas a sus tareas. El alcance está en `CONTEXTO_ACTOR.md`.

**Dictámenes y limitaciones.** H-01: **CORREGIDO CON LIMITACIONES DOCUMENTADAS**; H-02: **CORREGIDO DOCUMENTALMENTE**. Queda sin verificar la entrega transaccional real de `AFTER_SUCCESS` con PostgreSQL, el comportamiento ante rollback en un entorno real, las pruebas `@QuarkusTest`, el despliegue multiinstancia y la omisión de sesiones obsoletas tras la autenticación de una petición en vuelo. Una carrera concurrente puede rechazar el inicio de sesión de forma genérica sin crear una sesión obsoleta.

#### 6.3.3. Estructura académica (TK-011 a TK-014)

- TK-011 / UI-006: pantalla de facultades sobre API-005 a API-009. Sin aprobación formal registrada; estado sin cambios. Observaciones de accesibilidad y errores en `docs/INTERFACES.md`.
- TK-012 / UI-007: build frontend correcto (bundle `index-gufZihvK.js`, 285,14 kB) y auditoría estática; M-1 corregido con contador de solicitudes mediante `useRef`. La instantánea de la carrera puede quedar desactualizada por concurrencia; no hay `@Version`. La tabla conserva cinco columnas: Código, Nombre, Facultad, Estado y Acciones. API-011 queda sin consumo directo; el selector de facultades incluye inactivas con identificación visual; `listarCarreras(parametros = {})` conserva las llamadas sin argumentos de otros módulos.
- TK-013 / UI-008: builds correctos tras la implementación (`index-D2xe6L0l.js`, 291,10 kB) y tras las correcciones (`index-Fd_zyRjh.js`, 291,17 kB); `PeriodoAcademicoServiceTest` 7/7; M-1, M-2 y B-1 corregidos; auditoría final sin hallazgos. Sin verificación real de concurrencia entre activaciones. API-015 sin consumo directo; la creación deja el periodo inactivo, la edición no modifica el estado y toda mutación recarga el listado porque API-016 a API-019 devuelven 200/201 sin cuerpo.
- TK-014 / UI-009: builds correctos tras la implementación (`index-BginAHsz.js`, 296,78 kB) y tras B-1 (`index-B8sSOJWa.js`, 296,98 kB); auditoría con dictamen **APROBADA CON OBSERVACIONES**, sin hallazgos críticos, altos ni medios; B-1 corregido con la expresión `/^[+-]?(?:\d+(?:\.\d{0,2})?|\.\d{1,2})$/` aplicada a la cadena original antes de cualquier conversión. Comprobación ejecutada con Node.js: `1`, `1.2`, `1.23`, `+1.23` y `.5` aceptados; `1.234` y `0.001` rechazados por decimales; `0` y `-1` rechazados por rango. API-021 sin consumo directo. B-2 pendiente (prioridad baja). `TipoPractica` no tiene `@Version`: las actualizaciones concurrentes pueden resolverse por último-write-wins.

**Verificación.** Los builds y las pruebas unitarias citados se ejecutaron realmente. Las comprobaciones de contratos, permisos, rutas, errores y estados se verificaron por inspección y auditoría estática, no contra una instancia real. No se realizaron pruebas manuales de navegador ni pruebas contra PostgreSQL en estas entregas.

#### 6.3.4. Verificación posterior de conexión con Supabase

Esta verificación es posterior a Fase 5 y no forma parte de sus evidencias ni modifica los estados de §6.1 y §6.2. Se comprobaron únicamente dos hechos: Quarkus inició correctamente con la configuración externa, y se ejecutó `SELECT 1`, que devolvió `1`. Esta comprobación de conectividad no demuestra el funcionamiento integral de todas las operaciones de PRAXIS ni sustituye pruebas de integración, verificación de triggers o pruebas manuales de navegador.
