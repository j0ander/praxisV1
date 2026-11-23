# Interfaces

## 1. Lineamientos generales

- La interfaz es una aplicación React + Vite (`src/main/webui`) que consume los endpoints JSON de `CONTRATOS_REST.md`. No hay renderizado en servidor.
- Están implementadas las rutas de acceso y estructura académica (`/login`, `/inicio`, `/admin/usuarios`, `/admin/usuarios/nuevo`, `/admin/usuarios/:id`, `/admin/facultades`, `/admin/carreras`, `/admin/periodos`, `/admin/tipos-practica`, `/estudiantes` y `/docentes`) y las de empresas (UI-010 y UI-011: `/empresas`, `/empresas/:id` para ADM/COO y `/mi-empresa` para REP). Las demás rutas de este documento siguen siendo propuestas sujetas a aprobación. Implementado no equivale a validado manualmente en navegador: esa validación sigue pendiente.
- Solo se documentan las pantallas del MVP (`PRODUCTO.md` §5). Actividades, horas, informes, evaluaciones, certificados y reportes avanzados no tienen pantalla. La auditoría básica y los historiales del MVP sí están incluidos (UI-022); la trazabilidad avanzada no.
- La interfaz no autoriza: oculta opciones según el rol, pero siempre decide la API (`SEGURIDAD.md`, RN-003). Los permisos se rigen por CR-5: el coordinador no administra usuarios ni estructura académica, el tutor empresarial no recomienda postulaciones y la autoridad solo consulta auditoría.
- No se muestran catálogos ni requisitos inventados. Si no hay registros, la pantalla presenta el estado vacío; la validación de la postulación se aplica solo a los requisitos existentes y la administración de requisitos está fuera del MVP (CR-6).
- Terminología en español, la del glosario de `PRODUCTO.md`. Fechas con hora se muestran en `America/Guayaquil`.
- Cada rol ve solo sus opciones; los campos obligatorios se marcan y los mensajes de validación indican la causa (RNF-006).
- Estilo visual: tomado del login de ejemplo (azul marino, dorado, fondo crema, títulos con tipografía serif y cuerpo sans). No existe un documento `PROMPT_PANTALLAS.md`; no se crea uno vacío ni se usa como referencia.
- **Referencias visuales: pendientes.** Ninguna pantalla tiene diseño aprobado; la imagen de login recibida solo define el estilo, no el contenido.

## 2. Estructura de navegación

**Entrada:** `/login`. Con sesión válida se redirige a `/inicio`. El diseño previsto redirigía a `/cambiar-password` cuando `debeCambiarPassword` era verdadero, pero esa redirección **no está implementada**: depende de API-004 y UI-002, pendientes. El indicador se registra, no restringe el acceso.

**Menú lateral por rol** (el usuario con varios roles ve la unión de sus menús):

| Rol | Opciones |
|---|---|
| Administrador | Inicio · Usuarios · Facultades · Carreras · Periodos · Tipos de práctica · Empresas · Ofertas (consulta) · Prácticas · Auditoría |
| Coordinador | Inicio · Empresas · Ofertas (revisión) · Postulaciones · Prácticas · Auditoría · Usuarios y estructura académica (solo consulta; la administración corresponde a `ADMINISTRADOR`) |
| Estudiante | Inicio · Oportunidades · Mis postulaciones · Mi práctica |
| Representante empresarial | Inicio · Mis ofertas · Postulaciones · Mi empresa (`/mi-empresa`; solo REP con membresía activa `ADMIN_EMPRESA`, verificada mediante API-086, sin IDs fijos) |
| Docente tutor / Tutor empresarial | Inicio · Mis prácticas |
| Autoridad | Inicio · Ofertas (consulta) · Prácticas · Auditoría (consulta) |

> Estas opciones reflejan CR-5, CR-6 y CR-8: la administración de usuarios y estructura es solo del administrador; el tutor empresarial no recomienda; la autoridad solo consulta; la definición de requisitos no tiene pantalla en el MVP; la asignación de responsables y fechas sí está disponible, pero «Iniciar» no.

**Redirecciones:**

| Operación | Destino |
|---|---|
| Inicio de sesión correcto | `/inicio` (o `/cambiar-password` si es obligatorio). |
| Cierre de sesión o sesión vencida (401) | `/login`. |
| Crear oferta | Detalle de la oferta (UI-014). |
| Postularse | Mis postulaciones (UI-017). |
| Aceptar postulación | Detalle de la práctica creada (UI-021). |
| Ruta sin permiso | Estado "Operación no permitida" (sección 8). |

**Restricciones:** toda ruta, salvo `/login`, exige sesión. Los identificadores de registros ajenos muestran "No encontrado" (la API responde 404).

## 3. Interfaces públicas y comunes

### UI-001 Inicio de sesión
**Rol:** público (sin sesión).
**Objetivo:** permitir el acceso con correo y contraseña.
**Ruta:** `/login`.
**Elementos:** panel institucional con nombre y descripción de PRAXIS (sin estadísticas inventadas); formulario con correo y contraseña; botón "Ingresar".
**Acciones:** ingresar (API-001).
**Información:** ninguna.
**Estados:** cargando; credenciales incorrectas; cuenta desactivada o bloqueada (el mensaje es genérico y no revela la causa); error de conexión.
**Restricciones:** única pantalla sin sesión. La autenticación crea una sesión del servidor con cookie HttpOnly y BCrypt; no se usan JWT ni tokens. No incluye "olvidé mi contraseña" (fuera del MVP) ni accesos de demostración.
**Referencias:** FL-001; RF-002, RF-003; RN-002.

### UI-002 Cambio de contraseña (pendiente, no implementada)
**Rol:** todos los autenticados (según el diseño).
**Objetivo:** que el usuario cambie su contraseña, de forma voluntaria u obligatoria.
**Ruta prevista:** `/cambiar-password`. No existe pantalla ni ruta implementada; su habilitación depende de API-004.
**Elementos:** contraseña actual, nueva y confirmación; indicación de la política mínima aprobada (campos obligatorios y BCrypt; sin complejidad ni expiración).
**Acciones previstas:** guardar (API-004, pendiente); cancelar (solo si no es obligatorio).
**Estados:** cargando; contraseña actual incorrecta; formulario inválido; éxito.
**Restricciones previstas:** si es obligatorio, no se muestra el menú ni se permite navegar. Actualmente `debeCambiarPassword` solo queda registrado: no bloquea rutas ni muestra esta pantalla.
**Referencias:** FL-002; RF-003; RN-002.

### UI-003 Panel de inicio
**Rol:** todos.
**Objetivo:** punto de entrada con accesos rápidos según el rol.
**Ruta:** `/inicio`.
**Elementos:** saludo con nombre y rol; accesos a las opciones del menú; tarjetas de pendientes: coordinador (ofertas por revisar, postulaciones pendientes), representante (ofertas por estado), estudiante (postulaciones activas), docente y tutor (prácticas asignadas).
**Acciones:** navegar a cada sección.
**Información:** conteos calculados con los listados existentes. No hay endpoint de indicadores (los reportes están fuera del MVP).
**Estados:** cargando; sin pendientes; error de carga.
**Referencias:** API-003.

## 4. Interfaces de administración y estructura académica

### UI-004 Listado de usuarios
**Rol:** administrador.
**Objetivo:** consultar y buscar cuentas.
**Ruta:** `/admin/usuarios`.
**Elementos:** buscador por nombre, correo o documento; filtros por rol y estado; tabla (nombre, correo, roles, estado); botón "Nuevo usuario".
**Acciones:** buscar; filtrar; abrir; activar o desactivar (API-028, API-029); restablecer contraseña (API-030).
**Información:** nombres, correo, roles, activo/desactivado. Nunca contraseña, intentos ni bloqueo.
**Estados:** cargando; sin resultados; error de carga; confirmación de desactivación.
**Referencias:** API-024; FL-013; RF-001.

### UI-005 Formulario de usuario
**Rol:** administrador.
**Objetivo:** crear o editar una cuenta con sus roles, carreras y perfil académico.
**Ruta:** `/admin/usuarios/nuevo`, `/admin/usuarios/:id`.
**Elementos:** datos personales (correo, nombres, apellidos, tipo y número de documento, teléfono); alcance global; selector de roles; carreras del ámbito; sección de perfil (estudiante: carrera, código, fecha de nacimiento; docente: código y dedicación); contraseña temporal al crear.
**Acciones:** guardar (API-026, API-027); asignar roles (API-031); asignar carreras (API-032); guardar perfil (API-035, API-036, API-038).
**Estados:** cargando; formulario inválido; correo o documento duplicado; éxito.
**Restricciones:** el perfil de docente se crea al asignar el rol `DOCENTE`; el de estudiante requiere carrera. La creación deja la cuenta con cambio de contraseña obligatorio.
**Referencias:** FL-013; RF-001, RF-002, RF-005; RN-001, RN-004.

### UI-006 Facultades
**Rol:** consulta para cualquier usuario autenticado; crear, editar y desactivar solo administrador.
**Objetivo:** administrar facultades.
**Ruta:** `/admin/facultades`.
**Elementos:** tabla (código, nombre, estado); diálogo de creación y edición.
**Acciones:** crear (API-007); editar o desactivar (API-008). No se muestra la acción de eliminar.
**Estados:** cargando; sin resultados; código o nombre duplicado; error.
**Restricciones:** desactivar deja de ofrecerla para nuevas operaciones. El contrato REST conserva `DELETE /facultades/eliminar/{id}` (API-009) como patrón de referencia, pero la interfaz no lo expone: la eliminación debe respetar la integridad referencial y responde 409 si existen carreras asociadas. No se asume que una facultad con registros relacionados pueda eliminarse físicamente.
**Referencias:** API-005, API-008, API-009; FL-012; RF-004; RN-005.

### UI-007 Carreras
**Rol:** consulta para cualquier usuario autenticado; crear, editar y desactivar solo administrador.
**Objetivo:** administrar carreras por facultad.
**Ruta:** `/admin/carreras`.
**Elementos:** filtro por facultad; tabla (código, nombre, facultad, estado); diálogo de creación y edición.
**Acciones:** crear (API-012); editar o desactivar (API-013).
**Estados:** como UI-006.
**Restricciones:** API-011 queda sin consumo directo; el selector de facultades incluye también las inactivas con identificación visual y no bloquea su selección, porque el backend valida existencia, no estado. `listarCarreras(parametros = {})` conserva el comportamiento sin argumentos de los consumidores existentes.
**Referencias:** API-010; RF-004; RN-005.

### UI-008 Periodos académicos
**Rol:** consulta para cualquier usuario autenticado; crear, editar, activar y desactivar solo administrador.
**Objetivo:** administrar periodos y fijar el activo.
**Ruta:** `/admin/periodos`.
**Elementos:** tabla (código, nombre, fechas, activo); diálogo de creación y edición; acción "Activar".
**Acciones:** crear (API-016); editar (API-017); activar (API-018); desactivar (API-019).
**Estados:** como UI-006; "ya existe otro periodo activo"; "fecha de fin anterior al inicio".
**Restricciones:** solo un periodo activo. La creación deja el periodo inactivo; la edición no envía `activo` y no modifica el estado; toda mutación recarga el listado porque API-016 a API-019 devuelven 200/201 sin cuerpo. API-015 no se consume directamente.
**Referencias:** FL-012; RN-006.

### UI-009 Tipos de práctica
**Rol:** consulta para cualquier usuario autenticado; crear, editar y desactivar solo administrador. La ruta no tiene guarda exclusiva de rol, de acuerdo con API-020 y API-021; las escrituras están restringidas por `@RolesAllowed("ADMINISTRADOR")`.
**Objetivo:** mantener el catálogo de tipos con sus horas requeridas y nota mínima.
**Ruta:** `/admin/tipos-practica`.
**Elementos:** tabla (nombre, horas requeridas, nota mínima, estado); diálogo de creación y edición.
**Acciones:** crear (API-022); editar o desactivar (API-023).
**Estados:** como UI-006; sin tipos registrados (el catálogo no trae datos iniciales).
**Restricciones:** horas y nota mínima son datos, no valores fijos en el código (pendientes de aprobación institucional). La interfaz no inserta datos de prueba ni valores institucionales ficticios: muestra el estado vacío y espera a que exista un mecanismo de administración aprobado. La validación local acepta enteros y hasta dos decimales y rechaza valores como `1.234` o `0.001` sin redondearlos. API-021 no se consume directamente.
**Referencias:** API-020; FL-012; RN-012, RN-022.

### Inventario de interfaces implementadas

| Interfaz | Ruta | roles | Operaciones implementadas |
|---|---|---|---|
| UI-001 | `/login` | público | Ingresar (API-001). |
| UI-003 | `/inicio` | autenticado | Navegación y accesos según rol. |
| UI-004 y UI-005 | `/admin/usuarios`, `/admin/usuarios/nuevo`, `/admin/usuarios/:id` | `ADMINISTRADOR` | Consulta, alta, edición, roles, carreras, activación y restablecimiento (API-024 a API-032). |
| UI-006 | `/admin/facultades` | consulta: autenticados; escrituras: `ADMINISTRADOR` | Crear (API-007), editar y desactivar (API-008). API-009 no se expone. |
| UI-007 | `/admin/carreras` | consulta: autenticados; escrituras: `ADMINISTRADOR` | Crear (API-012), editar y desactivar (API-013); filtro por facultad. |
| UI-008 | `/admin/periodos` | consulta: autenticados; escrituras: `ADMINISTRADOR` | Crear (API-016), editar (API-017), activar (API-018) y desactivar (API-019). API-015 sin consumo directo. |
| UI-009 | `/admin/tipos-practica` | consulta: autenticados; escrituras: `ADMINISTRADOR` | Crear (API-022), editar y desactivar (API-023). API-021 sin consumo directo. |
| UI-010 | `/empresas` | `ADMINISTRADOR`, `COORDINADOR` | Buscar, abrir detalle y crear (API-039, API-041). |
| UI-011 | `/empresas/:id`, `/mi-empresa` | `ADMINISTRADOR`, `COORDINADOR`; REP con `ADMIN_EMPRESA` activo validado por API-086 | Cuatro pestañas con operaciones por rol (API-040, API-042 a API-053, API-086). |

Además están implementados los listados `/estudiantes` y `/docentes` para `ADMINISTRADOR` y `COORDINADOR`. La autorización efectiva siempre corresponde a la API; la interfaz solo controla visibilidad. Las evidencias de builds, pruebas y auditorías están en `TAREAS.md` §6.3.

### Observaciones pendientes de las interfaces implementadas

Estas observaciones **no** son funcionalidades implementadas:

1. **TK-011 / UI-006:** los diálogos declaran `aria-modal="true"` pero no gestionan el foco inicial, la trampa de foco, la restauración del foco ni el cierre con Escape; el fondo no se marca como inerte. El error de desactivación se concatena en el texto del mensaje sin `role="alert"`. La navegación con teclado y la experiencia con lectores de pantalla pueden verse afectadas.
2. **TK-012 / UI-007:** la instantánea de la carrera capturada al abrir el diálogo puede quedar desactualizada si otro actor modifica el registro; no hay `@Version` en la entidad y no se implementó control optimista. La interpretación de «todos los datos disponibles» quedó cerrada en cinco columnas (Código, Nombre, Facultad, Estado y Acciones), sin `id`, `createdAt` ni `updatedAt`.
3. **TK-013 / UI-008:** sin pruebas manuales de navegador, integración contra PostgreSQL real ni verificación de concurrencia entre activaciones simultáneas; los mensajes y permisos se comprobaron por inspección del código, el contrato y las pruebas unitarias existentes.
4. **TK-014 / UI-009 — B-2 (prioridad baja):** los diálogos todavía no gestionan completamente el foco inicial, la trampa de foco, la restauración del foco ni el cierre con Escape. El fondo tampoco se marca como inerte y el error de desactivación no utiliza `role="alert"`. Consecuencia: la navegación mediante teclado y la experiencia con lectores de pantalla pueden verse afectadas. La corrección queda pendiente y fuera del alcance aprobado de TK-014.
5. **General:** las pantallas implementadas se verificaron con build y revisión estática; no se realizaron pruebas manuales de navegador ni pruebas contra PostgreSQL real.

## 5. Interfaces de empresas y ofertas

### UI-010 Listado de empresas
**Rol:** administrador, coordinador.
**Objetivo:** consultar y registrar empresas receptoras.
**Ruta:** `/empresas`.
**Elementos:** buscador por RUC o razón social; tabla (RUC, razón social, estado); botón "Nueva empresa". El indicador de convenio vigente no se muestra en el listado; el estado efectivo se consulta en el detalle mediante API-043.
**Acciones:** buscar; abrir detalle; crear (API-041).
**Estados:** cargando; sin resultados; RUC inválido o duplicado; error.
**Referencias:** API-039; FL-014; RF-007; RN-008.

### UI-011 Detalle de empresa
**Rol:** administrador, coordinador; representante con rol global `REPRESENTANTE_EMPRESA` y membresía activa `ADMIN_EMPRESA` en su empresa ("Mi empresa").
**Objetivo:** administrar datos, convenios, miembros y tutores de una empresa.
**Ruta y acceso:** ADM y COO acceden desde el listado con `/empresas/:id`. El acceso REP usa la ruta `/mi-empresa`, sin parámetro de empresa: la pantalla contenedora llama a API-086, toma `empresaId` de `MiEmpresaResponse` y abre UI-011 con ese valor. La opción «Mi empresa» aparece en el menú solo si API-086 responde 200; un `PUBLICADOR` (sin membresía `ADMIN_EMPRESA`) no ve la opción y, si navega directamente a `/mi-empresa`, recibe 404 y no se monta el detalle. No se usa `/empresas` ni `/empresas/:id` para REP: manipular la URL no permite abrir otra empresa. El `empresaId` se mantiene solo en memoria del componente; nunca se escribe en `localStorage` ni `sessionStorage`.
**Elementos:** pestañas: Datos (formulario de empresa), Convenios (tabla con número, fechas, estado efectivo, documento), Miembros (usuario, rol de empresa, activo), Tutores empresariales (usuario, cargo).
**Acciones por rol:**
- ADM/COO: editar empresa (API-042); crear, editar, suspender o terminar convenio (API-044 a API-047); vincular o actualizar miembros (API-049, API-050); crear o editar tutores (API-052, API-053).
- REP `ADMIN_EMPRESA`: editar los datos de su empresa (API-042) y gestionar sus miembros (API-049, API-050). Consulta convenios (API-043) y tutores (API-051) sin modificarlos. No crea ni edita tutores (API-052, API-053) y no edita convenios (API-044 a API-047).
**Estados:** cargando; sin convenios; sin miembros; sin tutores; error de validación; operación no permitida; 401 (sesión vencida, se aplica el mecanismo existente); 403 (sin permiso); 404 (sin empresa asociada con `ADMIN_EMPRESA`); 409 (múltiples membresías, sin seleccionar ninguna); error de red con opción de reintento.
**Restricciones:** el representante solo ve la empresa devuelta por API-086; los datos completos del detalle se cargan con API-040 usando ese `empresaId`, y el Service vuelve a verificar el ámbito. La empresa inactiva sigue siendo visible para su `ADMIN_EMPRESA` y su estado se muestra desde `MiEmpresaResponse`. La empresa no puede quedar sin `ADMIN_EMPRESA`. El enlace «Volver al listado» de ADM/COO se sustituye por «Volver a Mi empresa» en REP.
**Estado de implementación:** UI-010 y UI-011 están implementadas y autorizadas; se verificaron con build frontend y revisión estática de rutas y condiciones. La validación manual de navegador sigue pendiente y no se afirma que se haya realizado.
**Referencias:** API-086, API-040, API-042, API-043, API-048 a API-053; FL-006, FL-014; RN-009, RN-010.

### UI-012 Listado de ofertas
**Rol:** representante (las de su empresa), coordinador, administrador y autoridad.
**Objetivo:** gestionar o consultar ofertas en cualquier estado. Para el coordinador es la bandeja de revisión.
**Ruta:** `/ofertas`.
**Elementos:** filtros por estado, empresa y periodo; tabla (título, empresa, periodo, tipo, estado, fecha de cierre, plazas); botón "Nueva oferta" (solo representante).
**Acciones:** filtrar; abrir detalle; crear (navega a UI-013).
**Estados:** cargando; sin resultados; error. Las ofertas `PENDIENTE_REVISION` se destacan para el coordinador.
**Restricciones:** el `PUBLICADOR` ve solo las que creó; el `ADMIN_EMPRESA`, todas las de su empresa; coordinador, solo su ámbito.
**Referencias:** API-054; FL-003, FL-015; RF-009, RF-011.

### UI-013 Formulario de oferta
**Rol:** representante.
**Objetivo:** crear o editar una oferta con sus perfiles.
**Ruta:** `/ofertas/nueva`, `/ofertas/:id/editar`.
**Elementos:** datos de la oferta (título, descripción, convenio, tipo de práctica, periodo, fechas de actividades, fecha de cierre); lista de perfiles, cada uno con nombre, descripción, plazas, carreras destinatarias y requisitos laborales.
**Acciones:** guardar oferta (API-057, API-058); agregar, editar o eliminar perfiles (API-059 a API-061).
**Estados:** cargando; formulario inválido; convenio no vigente; fechas incoherentes; éxito.
**Restricciones:** edición solo en `BORRADOR` o `RECHAZADA`; si se edita una oferta `PUBLICADA` la oferta vuelve a revisión y se avisa antes de guardar. El tipo de práctica no cambia si ya hay postulaciones.
**Referencias:** FL-003, FL-004; RF-009, RF-010; RN-011, RN-013, RN-015.

### UI-014 Detalle de oferta
**Rol:** representante, coordinador, administrador, autoridad.
**Objetivo:** ver la oferta completa y ejecutar las acciones que correspondan a su estado y rol.
**Ruta:** `/ofertas/:id`.
**Elementos:** cabecera con estado y motivo de la última decisión; datos y condiciones congeladas (horas y nota mínima, cuando existan); perfiles con plazas disponibles, carreras y requisitos; enlace a las postulaciones de la oferta.
**Acciones:**
- Representante: editar, enviar a revisión (API-062), retirar (API-063), cancelar (API-067, solo `ADMIN_EMPRESA`).
- Coordinador: aprobar (API-064), rechazar con motivo (API-065), cerrar (API-066), cancelar con motivo (API-067).
**Estados:** cargando; error; acción no permitida; diálogo de motivo obligatorio (rechazar y cancelar).
**Referencias:** API-056; FL-003, FL-004, FL-015; RN-012, RN-014, RN-016.

## 6. Interfaces del estudiante

### UI-015 Oportunidades
**Rol:** estudiante.
**Objetivo:** encontrar ofertas publicadas dirigidas a su carrera.
**Ruta:** `/oportunidades`.
**Elementos:** buscador; tarjetas de oferta (título, empresa, periodo, fecha de cierre, perfiles con plazas disponibles).
**Acciones:** buscar; abrir detalle.
**Estados:** cargando; sin ofertas disponibles; error. Un perfil sin plazas se muestra como cerrado.
**Restricciones:** solo ofertas `PUBLICADA` dentro de plazo.
**Referencias:** API-055; FL-007; RF-012; RN-014, RN-016.

### UI-016 Detalle de oportunidad y postulación
**Rol:** estudiante.
**Objetivo:** revisar una oferta y postularse a un perfil.
**Ruta:** `/oportunidades/:id`.
**Elementos:** descripción, empresa, fechas, condiciones; perfiles con carreras, requisitos laborales y plazas; campo de mensaje opcional; botón "Postularme" por perfil.
**Acciones:** postularse (API-068).
**Estados:** cargando; ya postulado; perfil sin plazas; oferta cerrada; requisitos incumplidos o convenio no vigente (mensaje con la causa); éxito.
**Restricciones:** los requisitos académicos que se validan son solo los existentes; si no hay requisitos registrados no se muestran requisitos inventados ni se bloquea la postulación por su ausencia (CR-6).
**Referencias:** API-068; FL-008; RF-013; RN-007, RN-017.

### UI-017 Mis postulaciones
**Rol:** estudiante.
**Objetivo:** seguir el estado de sus postulaciones.
**Ruta:** `/mis-postulaciones`.
**Elementos:** tabla (oferta, perfil, empresa, estado, fecha); motivo de la decisión cuando exista; edición del mensaje; botón "Retirar".
**Acciones:** editar mensaje mientras no haya recomendación (API-073); retirar (API-074).
**Estados:** cargando; sin postulaciones; error; confirmación de retiro.
**Restricciones:** solo las propias; retirar solo si está `PENDIENTE`. No se muestra la recomendación de la empresa.
**Referencias:** API-069; FL-008, FL-009; RN-019.

## 7. Interfaces de postulaciones, prácticas y auditoría

### UI-018 Listado de postulaciones
**Rol:** coordinador, representante.
**Objetivo:** revisar postulaciones recibidas.
**Ruta:** `/postulaciones`.
**Elementos:** filtros por oferta, estado y recomendación; tabla (estudiante, carrera, oferta, perfil, estado, recomendación de la empresa).
**Acciones:** filtrar; abrir detalle.
**Estados:** cargando; sin resultados; error.
**Restricciones:** el representante ve solo postulaciones a ofertas de su empresa; el coordinador, las de su ámbito.
**Referencias:** API-070, API-071; FL-005, FL-016; RF-014, RF-015.

### UI-019 Detalle de postulación
**Rol:** coordinador, representante.
**Objetivo:** recomendar o resolver una postulación.
**Ruta:** `/postulaciones/:id`.
**Elementos:** datos del estudiante y de la oferta; mensaje del estudiante; recomendación y observación de la empresa; historial de la decisión; condiciones de plaza disponible.
**Acciones:**
- Representante: recomendar o no recomendar con observación (API-075).
- Coordinador: aceptar (API-076), rechazar con motivo (API-077), cancelar con motivo (API-078), corregir decisión (API-079, también administrador).
**Estados:** cargando; error; sin plazas; convenio o periodo no válidos (mensaje con la causa); conflicto de concurrencia (409: otra operación ocupó la plaza o cambió la postulación; se sugiere recargar); diálogo de motivo obligatorio; éxito con enlace a la práctica creada.
**Restricciones:** la oferta en revisión no admite recomendaciones ni decisiones. La recomendación es exclusiva del representante de la empresa; el tutor empresarial no la realiza (CR-5). Aceptar y crear la práctica ocurren en una sola operación del servidor.
**Referencias:** API-072; FL-005, FL-016; RN-018, RN-019, RN-020, RN-021.

### UI-020 Listado de prácticas
**Rol:** coordinador, administrador, autoridad, estudiante, docente tutor, tutor empresarial, representante.
**Objetivo:** consultar prácticas dentro del alcance de cada rol.
**Ruta:** `/practicas`.
**Elementos:** filtros por estado, periodo y carrera; tabla (estudiante, empresa, tipo, periodo, estado, docente, tutor).
**Acciones:** filtrar; abrir detalle.
**Estados:** cargando; sin prácticas; error.
**Restricciones:** estudiante, docente y tutor ven solo las propias; representante, las de su empresa; coordinador, su ámbito. En el MVP una práctica llega como máximo a `APROBADA` con responsables y fechas asignados.
**Referencias:** API-080; RF-016, RF-017.

### UI-021 Detalle de práctica y asignación
**Rol:** coordinador (acciones); los demás, solo lectura.
**Objetivo:** ver la práctica y asignar sus responsables.
**Ruta:** `/practicas/:id`.
**Elementos:** datos de estudiante, empresa, convenio, tipo, periodo; condiciones congeladas; estado; selectores de docente tutor y tutor empresarial (solo de la empresa); fechas de inicio y fin.
**Acciones:** asignar responsables y fechas (API-082); cancelar con motivo (API-084). «Iniciar» (API-083) no aparece en el MVP; es mejora futura.
**Estados:** cargando; error; tutor de otra empresa; fechas incoherentes; conflicto de concurrencia (409: otra operación modificó la práctica; se sugiere recargar); operación no permitida.
**Referencias:** API-082, API-084; FL-017; RN-020, RN-025, RN-026.

### UI-022 Auditoría
**Rol:** administrador, coordinador (su ámbito), autoridad (consulta).
**Objetivo:** consultar quién hizo qué y cuándo.
**Ruta:** `/auditoria`.
**Elementos:** filtros por usuario, entidad y rango de fechas; tabla (fecha, usuario, acción, entidad, detalle).
**Acciones:** filtrar; ver detalle del evento.
**Estados:** cargando; sin resultados; error.
**Restricciones:** solo lectura; nadie modifica la auditoría. Se muestra la auditoría básica y los historiales del MVP; los reportes consolidados y la trazabilidad avanzada no tienen pantalla. La autoridad accede solo en modo de consulta (CR-5).
**Referencias:** API-085; FL-024; RF-027; RN-039.

## 8. Estados de interfaz

| Estado | Comportamiento esperado |
|---|---|
| Cargando | Indicador de progreso; los botones de acción se deshabilitan. |
| Sin resultados | Mensaje claro y, si corresponde, la acción para crear el primer registro. |
| Error de carga | Mensaje genérico con opción de reintentar; sin detalles internos. |
| Éxito | Aviso breve y regreso al listado o detalle. |
| Formulario inválido | Mensaje junto a cada campo; se conserva lo escrito. |
| Operación no permitida | Mensaje con la causa de negocio devuelta por la API (409) o "No tiene permiso" (403). |
| Conflicto de concurrencia | La API responde 409 `CONFLICTO_CONCURRENCIA`; se informa que otra operación cambió el registro (o la plaza, cuando corresponda) y se ofrece recargar. |
| No encontrado | Registro inexistente o fuera del ámbito (404); no revela cuál de los dos. |
| Sesión vencida | Redirige a `/login` (401). |

## 9. Componentes reutilizables

| Componente | Uso |
|---|---|
| Estructura de la aplicación | Barra lateral por rol, encabezado con usuario y cierre de sesión, área de contenido. |
| Tabla con filtros | UI-004, 006 a 010, 012, 017, 018, 020, 022. |
| Diálogo de formulario | Catálogos simples (UI-006 a UI-009). |
| Diálogo de motivo obligatorio | Rechazar, cancelar y corregir decisiones. |
| Insignia de estado | Oferta, postulación, práctica, convenio y recomendación, con colores consistentes. |
| Tarjeta de oferta | UI-015. |
| Mensajes de estado | Sección 8. |
| Selector de periodo, carrera y empresa | Filtros y formularios. |

## 10. Adaptabilidad

Prioridad en pantallas de escritorio. Las tablas deben permitir desplazamiento horizontal en pantallas pequeñas. Compatibilidad con navegadores web modernos (RNF-007). El soporte móvil completo está por confirmar.

## 11. Pendientes y precisiones

**Pendientes**

1. Diseños y referencias visuales de las 22 pantallas: pendientes. No existe `PROMPT_PANTALLAS.md`; la imagen de login recibida solo define el estilo general.
2. Aprobación de las rutas del frontend: solo están implementadas y autorizadas las de UI-010 y UI-011 (`/empresas`, `/empresas/:id`, `/mi-empresa`); las restantes continúan pendientes.
3. Reportes consolidados: sin pantalla hasta definir su alcance.
4. Aclaración documental pendiente: la referencia RN-024 apareció en una versión anterior de `FLUJOS.md` (FL-016) y no tiene regla correspondiente en `REGLAS_NEGOCIO.md`; se conserva como referencia heredada sin inventar una regla.

**Decisiones resueltas (no reabrir)**

- CR-5: el coordinador no administra usuarios ni estructura académica; el tutor empresarial no recomienda postulaciones; la autoridad consulta auditoría en solo lectura.
- CR-6: la definición y administración de requisitos queda fuera del MVP; la validación de postulación se aplica sobre los requisitos existentes, sin datos precargados ni ficticios.
- CR-8: la asignación de tutores y fechas sí está en el MVP; «Iniciar» (API-083) y las etapas posteriores quedan fuera.
