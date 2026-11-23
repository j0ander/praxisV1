# Seguridad

## 1. Introduccion

Este documento define cómo PRAXIS identifica a sus usuarios, qué puede hacer cada rol y cómo se protegen los datos. Es una guía práctica para implementar autenticación, autorización y control de acceso. No pretende ser un sistema invulnerable: busca cubrir lo esencial de forma clara y fácil de mantener.

Cinco principios guían todo el documento:

1. Todo requiere sesión, salvo el inicio de sesión.
2. La API siempre autoriza. Ocultar un botón en pantalla no protege nada (RN-003). La base de datos es una segunda barrera, no la primera.
3. Cada quien accede solo a lo suyo: a lo que su rol permite y a los registros que le pertenecen o que están en su ámbito.
4. Las contraseñas se almacenan mediante BCrypt. Nunca se guardan ni se muestran en texto plano.
5. La sesión es tradicional del servidor, con cookie HttpOnly. No se usan JWT ni tokens de acceso.

## 2. Autenticacion

### 2.1. Inicio de sesión

1. El usuario envía su correo y contraseña.
2. El sistema comprueba que la cuenta esté activa y no bloqueada.
3. El sistema compara la contraseña con el hash guardado mediante BCrypt.
4. Si es correcta: se reinicia el conteo de intentos fallidos, se registra `ultimo_login` y el servidor crea la sesión.
5. El servidor envía la cookie de sesión HttpOnly; el navegador la conserva y la envía en las siguientes peticiones.
6. Si `debe_cambiar_password` es verdadero, la condición queda registrada en el usuario. No equivale a una restricción de acceso efectiva: la aplicación obligatoria depende de API-004, que sigue pendiente y no está implementada. Mientras tanto no se bloquean rutas ni operaciones por ese indicador.

### 2.2. Identificación del usuario

- El usuario se identifica por su correo, único y en minúsculas.

### 2.3. Credenciales

| Aspecto | Regla |
|---|---|
| Almacenamiento | Solo el hash BCrypt en `usuario.hash_password`. Nunca la contraseña en texto plano. |
| Visibilidad | La contraseña y el hash nunca se devuelven en respuestas, ni se escriben en logs ni en auditoría. |
| Recuperación | La contraseña no se puede consultar; solo se cambia o se restablece. |
| Claves | Las credenciales se leen del entorno; no se guardan en el repositorio. |

### 2.4. Intentos fallidos y bloqueo

Cada fallo suma a `intentos_fallidos`. Al superar el límite, la cuenta se bloquea hasta `bloqueado_hasta`; pasado ese momento vuelve a estar activa. Este es el control básico del MVP. El límite de intentos y la duración del bloqueo se manejan como parámetros configurables, sin valores fijos en el código; no se incorporan bloqueos avanzados (por IP, progresivos ni con CAPTCHA), que quedan como mejoras futuras.

### 2.5. Cambio y recuperación de contraseña

- Cambio: el usuario autenticado cambia su propia contraseña. La nueva se guarda cifrada.
- Recuperación ("olvidé mi contraseña"): No forma parte del MVP. Mientras tanto, el administrador restablece la contraseña y activa `debe_cambiar_password`.

### 2.5.1. Inicialización del primer administrador

El arranque puede crear el primer administrador solo si se habilita explícitamente con `praxis.seguridad.admin-inicial.enabled=true`. El valor predeterminado es `false` y, mientras esté deshabilitado, el observador no ejecuta consultas ni operaciones de inicialización. No se activa automáticamente por perfiles `dev` o `test`.

Las credenciales se leen únicamente de `ADMIN_CORREO` y `ADMIN_PASSWORD`. Si falta alguna o está vacía, el arranque se detiene con un mensaje que nombra las variables, sin incluir valores, sin crear credenciales predeterminadas y sin escribir en la base.

Cuando está habilitado y las variables son válidas, el proceso:

1. Bloquea la fila del rol `ADMINISTRADOR` para serializar inicializaciones concurrentes.
2. Verifica que el rol exista.
3. Comprueba si existe cualquier usuario con ese rol, activo o inactivo.
4. Si existe, termina sin modificar cuentas, contraseñas ni relaciones.
5. Si no existe, valida correo y contraseña con las reglas ya usadas para usuarios, verifica que el correo no esté registrado y crea un usuario activo, con `debe_cambiar_password=true`, `alcance_global=true` y hash BCrypt.
6. Asigna exclusivamente el rol `ADMINISTRADOR` en la misma transacción.

No se crea ningún endpoint de inicialización ni mecanismo de registro público. La contraseña, el hash y el correo no se escriben en logs. Este mecanismo no activa ni implementa el cambio obligatorio de contraseña (API-004), que sigue fuera de este alcance.

### 2.6. Sesión y cookie (decisión oficial CR-2)

- La autenticación usa **sesiones tradicionales del servidor Quarkus** con una **cookie de sesión HttpOnly** (`ARQUITECTURA.md` §11).
- La contraseña se verifica con **BCrypt**. No se usan JWT, Bearer tokens, refresh tokens ni cookies autocontenidas.
- El identificador de la sesión se guarda en un almacén sencillo **en memoria del servidor**, gestionado por una sola clase; no se crea ninguna tabla nueva para almacenar sesiones.
- La cookie contiene solo un **identificador aleatorio** de sesión: ni credenciales ni datos del usuario. Se envía con `HttpOnly=true`, `Secure` cuando la conexión usa HTTPS, `Path=/` y el `SameSite` que corresponda al origen real del frontend React.
- La expiración de la sesión es **fija y configurable**; no hay renovación automática en el MVP.
- `POST /auth/logout` invalida la sesión en el servidor. Una sesión ausente, vencida o inválida responde 401.
- La asignación o modificación de roles, la desactivación de la cuenta y el restablecimiento de contraseña invalidan **todas** las sesiones activas del usuario afectado y no afectan las sesiones de otros usuarios. La invalidación se notifica con un evento simple de la capa de aplicación publicado dentro de la transacción del Service; un observador `AFTER_SUCCESS` la ejecuta solo después de que la transacción confirme. Si la operación falla o hay rollback, las sesiones se conservan.
- Para impedir que un login concurrente registre una sesión con información obsoleta, `SesionStore` mantiene una **generación por usuario** en memoria. El login obtiene la generación **antes** de consultar los roles, la asocia a la sesión creada y la creación se rechaza si la generación ya cambió. La comprobación y la inserción comparten un cerrojo con la invalidación, que incrementa la generación y elimina las sesiones del usuario. Al recuperar una sesión se vuelve a comprobar su generación. El mapa de generaciones no se depura: está acotado por el número de usuarios del sistema y una limpieza segura exigiría rastrear logins en vuelo, complejidad desproporcionada para el MVP.
- Los resultados de pruebas, las limitaciones de verificación y el dictamen de las incidencias H-01 y H-02 están consolidados en `TAREAS.md` §6.3.2. Dictamen: **H-01 CORREGIDO CON LIMITACIONES DOCUMENTADAS**; la corrección es adecuada para el MVP, pero no se presenta como verificada integralmente en un entorno real. H-02 quedó **CORREGIDO DOCUMENTALMENTE** y su alcance efectivo está en `CONTEXTO_ACTOR.md`.
- La protección CSRF no forma parte del MVP; se considera una mejora futura de seguridad. No se instalará `quarkus-rest-csrf` ni se implementará `CSRFHandler` u otro mecanismo propio durante el MVP. El origen del frontend se restringe con CORS.
- Medidas avanzadas (gestión de sesiones por dispositivo, 2FA, recuperación por correo, CAPTCHA y límites por IP) son mejoras futuras.

### 2.7. Bloqueo de cuenta y sesiones activas (limitación del MVP)

- Si una cuenta se bloquea después de haber iniciado sesión, las sesiones que ya existen **no se invalidan automáticamente**: conservan su vigencia hasta expirar o hasta que el usuario cierre sesión.
- Un usuario bloqueado **no puede iniciar una nueva sesión**; el bloqueo se comprueba al iniciar sesión (§2.1, RN-002).
- No se implementa cierre masivo de sesiones ni se consulta PostgreSQL en cada petición únicamente para detectar bloqueos.
- Esta limitación se documenta expresamente como propia del MVP y comprende únicamente el bloqueo por intentos fallidos. No cubre la desactivación de la cuenta, el cambio de roles ni el restablecimiento de contraseña, que sí invalidan sesiones según §2.6.

### 2.8. Parámetros configurables y límites de la sesión en memoria

Los parámetros siguientes son configurables. **No hay evidencia documental de que sus valores estén formalmente aprobados**: los que figuran en `application.properties` son la configuración vigente del entorno actual, no una decisión institucional. Esa configuración **no se modificó** durante la Fase 5 y no se presenta como aprobada.

| Parámetro | Valor vigente en `application.properties` | Estado de aprobación |
|---|---|---|
| Duración de la sesión | `PT30M` (30 minutos) | Sin aprobación documentada |
| Duración de la cookie (`Max-Age`) | `PT30M` (30 minutos) | Sin aprobación documentada |
| Número máximo de intentos fallidos | `5` | Sin aprobación documentada; parámetro configurable según §2.4 |
| Duración del bloqueo de cuenta | `PT15M` (15 minutos) | Sin aprobación documentada; parámetro configurable según §2.4 |
| Nombre de la cookie | `PRAXIS_SESSION` | Sin aprobación documentada |
| Atributo `SameSite` | `Lax` | Sin aprobación documentada; depende del origen real del frontend |

Se conservan `HttpOnly=true` y `Secure` cuando la conexión utiliza HTTPS. La cookie no contiene credenciales ni datos personales. Los valores de esta tabla solo describen lo configurado; cualquier cambio requiere una decisión aprobada y una tarea autorizada.

Limitaciones del almacenamiento en memoria:

- Las sesiones se pierden al reiniciar el servidor.
- El diseño corresponde a una sola instancia; no hay sesiones distribuidas ni renovación automática.

### 2.9. Identidad por petición (decisión técnica aprobada)

- La identidad que consumen `@Authenticated` y `@RolesAllowed` se genera **por petición autenticada** y nunca se comparte entre usuarios.
- Un único adaptador implementa `HttpAuthenticationMechanism` y devuelve `Uni<SecurityIdentity>` desde `authenticate(...)`. La implementación de `SecurityIdentity` es una clase interna de ese adaptador.
- Cada identidad contiene solo `id`, correo y roles del usuario. No incluye contraseña, hash, intentos fallidos ni datos de la sesión.
- La instancia no se guarda en un campo del adaptador. Quarkus asocia la identidad devuelta al `RoutingContext`; el adaptador no llama `CurrentIdentityAssociation.setIdentity(...)`.
- No se usa `QuarkusSecurityIdentity`, no se agregan dependencias y las APIs internas quedan confinadas en el adaptador (`ARQUITECTURA.md` §11).
- En las pruebas de implementación se verifica que `CurrentIdentityAssociation.current()` devuelve la identidad correspondiente a cada petición y que dos usuarios distintos no comparten instancia.

## 3. Autorizacion

Autorizar es comprobar qué puede hacer un usuario ya identificado. Autenticación y autorización son pasos distintos: haber iniciado sesión no da permiso para todo.

Cada operación pasa por tres preguntas, en este orden:

| # | Pregunta | Dónde se verifica | Si falla |
|---|---|---|---|
| 1 | ¿Hay sesión válida? | Capa de autenticación (antes del Resource) | 401 |
| 2 | ¿Tiene el rol que exige la operación? | Resource | 403 |
| 3 | ¿El registro es suyo o está dentro de su ámbito? | Service | 404 |

La sesión se identifica por la cookie HttpOnly descrita en §2.6; no se aceptan credenciales en cabeceras Bearer ni JWT. El estado de la cuenta (activa o bloqueada) se comprueba al iniciar sesión. La desactivación, el cambio de roles y el restablecimiento de contraseña invalidan las sesiones activas, con control de generaciones para logins concurrentes (§2.6); el bloqueo por intentos fallidos mantiene la limitación descrita en §2.7.

Las reglas de negocio (estado de la oferta, convenio vigente, plazas, etc.) se aplican después y están en `REGLAS_NEGOCIO.md`; no son parte de este documento.

Rol (pregunta 2). Se declara en el Resource con la anotación estándar, usando los códigos de rol:

```java
@RolesAllowed("COORDINADOR")
```

Un usuario puede tener varios roles; sus permisos son la suma de los de cada uno.

Propiedad y ámbito (pregunta 3). El identificador del usuario autenticado siempre procede de `SecurityIdentity` (`praxis.usuarioId`); el cliente no puede determinar la identidad del actor enviando un `usuarioId`. El `empresaId` puede recibirse en la ruta o en el cuerpo cuando el contrato aprobado lo establece, pero recibirlo no significa confiar en él: el Service valida existencia, pertenencia y ámbito de autorización. Se responde 404 (y no 403) para no revelar que el registro existe. API-086 deriva su ámbito exclusivamente del actor autenticado y no acepta `empresaId` ni `usuarioId` del cliente.

Los tres tipos de límite son:

| Tipo | Aplica a | Cómo se determina |
|---|---|---|
| Propio | Estudiante, docente tutor, tutor empresarial | El registro está vinculado a su usuario o perfil (por ejemplo, la postulación es de su estudiante). |
| Empresa | Representante empresarial | El registro pertenece a su empresa (`empresa_usuario`). Dentro de ella, `ADMIN_EMPRESA` gestiona datos y miembros; `PUBLICADOR` solo lo que creó. Para «Mi empresa», API-086 exige membresía activa `ADMIN_EMPRESA` y el `PUBLICADOR` no accede a esa pantalla. El `ADMIN_EMPRESA` consulta convenios y tutores sin modificarlos; editar convenios y crear o editar tutores son operaciones exclusivas de ADM y COO. |
| Ámbito institucional | Coordinador, autoridad y demás roles institucionales | `usuario.alcance_global` o las carreras de `usuario_carrera`. La base no impone cómo se usan; lo aplica la API. |

**Convención de permisos de interfaz y endpoints.** UI-011 (detalle de empresa) es exclusiva de REP con membresía activa `ADMIN_EMPRESA`; un REP `PUBLICADOR` no accede a esa pantalla. Los permisos de la interfaz no se infieren de los endpoints ni se trasladan a ellos: API-040 y API-043 conservan los permisos de sus contratos; API-051 permite a REP consultar tutores de su empresa sin exigir `ADMIN_EMPRESA`; API-052 y API-053 son exclusivas de ADM y COO; API-086 exige REP con membresía activa `ADMIN_EMPRESA`. Cualquier discrepancia entre contrato e implementación se reporta, no se resuelve ampliando o restringiendo permisos de forma unilateral.

**Autorización efectiva de API-086 (`GET /empresas/miEmpresa`).** La operación exige sesión válida y rol global `REPRESENTANTE_EMPRESA` (`@RolesAllowed`); el identificador del usuario se obtiene únicamente de `SecurityIdentity` mediante `praxis.usuarioId`, nunca de ruta, consulta o cuerpo. El Service comprueba además que el usuario exista y esté activo y que conserve el rol global asignado; después exige exactamente una membresía activa `ADMIN_EMPRESA` del propio usuario, con usuario activo y rol global REP. No se filtra por `empresa.activa`: una empresa inactiva sigue siendo visible para su `ADMIN_EMPRESA` y se reporta en `activa`. Cero membresías responden 404 `NO_ENCONTRADO` y dos o más responden 409 `REGLA_NEGOCIO`, sin elegir una ni devolver identificadores de otras empresas. Un `PUBLICADOR` no puede utilizar la operación ni acceder a «Mi empresa»; el frontend oculta la opción cuando API-086 no responde 200. No se creó una regla de pertenencia única: la ambigüedad se resuelve con 409.

## 4. Roles y permisos

### 4.1. Roles

| Rol (`rol.codigo`) | Qué hace en el sistema | Límite principal |
|---|---|---|
| `ADMINISTRADOR` | Gestiona usuarios, roles y estructura académica. | Institucional (normalmente global). |
| `COORDINADOR` | Gestiona empresas y convenios, revisa ofertas, decide postulaciones y asigna responsables. | Ámbito institucional (su carrera). |
| `ESTUDIANTE` | Consulta ofertas y se postula. | Propio. |
| `DOCENTE` | Tutor académico de prácticas que se le asignan. | Propio. |
| `TUTOR_EMPRESARIAL` | Acompaña al estudiante en la empresa. | Propio. |
| `REPRESENTANTE_EMPRESA` | Administra su empresa y registra ofertas. | Empresa. |
| `AUTORIDAD` | Consulta información consolidada. Solo lectura. | Ámbito institucional. |

### 4.2. Matriz de permisos del MVP

Leyenda: Sí = sin límite adicional; Propio, Empresa, Ámbito = según la sección 3; No = no permitido; Futuro = no incluido en el MVP. Roles: ADM = Administrador, COO = Coordinador, EST = Estudiante, DOC = Docente tutor, TUT = Tutor empresarial, REP = Representante empresarial, AUT = Autoridad.

| Operación | ADM | COO | EST | DOC | TUT | REP | AUT |
|---|---|---|---|---|---|---|---|
| Iniciar sesión y cambiar su propia contraseña | Sí | Sí | Sí | Sí | Sí | Sí | Sí |
| Consultar facultades, carreras y periodos | Sí | Sí | Sí | Sí | Sí | Sí | Sí |
| Administrar facultades, carreras y periodos | Sí | No | No | No | No | No | No |
| Crear, modificar, activar y desactivar usuarios | Sí | No | No | No | No | No | No |
| Asignar roles y carreras a usuarios | Sí | No | No | No | No | No | No |
| Gestionar perfiles de estudiantes y docentes | Sí | Ámbito | No | No | No | No | No |
| Registrar empresas (API-041) | Sí | Sí | No | No | No | No | No |
| Editar datos de una empresa (API-042) | Sí | Sí | No | No | No | Empresa (solo `ADMIN_EMPRESA`, la propia) | No |
| Gestionar convenios: crear, editar, suspender y terminar (API-044 a API-047) | Sí | Sí | No | No | No | No | No |
| Consultar convenios (API-043) | Sí | Sí | No | No | No | Empresa (la propia) | No |
| Gestionar miembros de una empresa (API-048 a API-050) | Sí | Sí | No | No | No | Empresa (solo `ADMIN_EMPRESA`, la propia) | No |
| Consultar tutores empresariales (API-051) | Sí | Sí | No | No | No | Empresa (la propia) | No |
| Crear o editar tutores empresariales (API-052, API-053) | Sí | Sí | No | No | No | No | No |
| Definir requisitos académicos y registrar su cumplimiento (fuera del MVP) | Futuro | Futuro | No | No | No | No | No |
| Crear, editar y enviar a revisión sus ofertas | No | No | No | No | No | Empresa | No |
| Aprobar o rechazar ofertas | No | Ámbito | No | No | No | No | No |
| Cerrar o cancelar ofertas | No | Ámbito | No | No | No | Empresa (solo cancelar, `ADMIN_EMPRESA`) | No |
| Consultar ofertas publicadas y vigentes | Sí | Sí | Sí | No | No | Empresa | Sí |
| Consultar ofertas en cualquier estado | Sí | Ámbito | No | No | No | Empresa | Sí |
| Postular, consultar y retirar sus postulaciones | No | No | Propio | No | No | No | No |
| Recomendar a postulantes de su empresa | No | No | No | No | No | Empresa | No |
| Aceptar, rechazar o cancelar postulaciones | No | Ámbito | No | No | No | No | No |
| Corregir una decisión ya tomada (operación administrativa) | Sí | Ámbito | No | No | No | No | No |
| Asignar docente tutor y tutor empresarial a una práctica | No | Ámbito | No | No | No | No | No |
| Consultar prácticas | Sí | Ámbito | Propio | Propio | Propio | Empresa | Sí |
| Consultar auditoría | Sí | Ámbito | No | No | No | No | Sí |
| Consultar reportes consolidados (fuera del MVP) | Futuro | Futuro | No | No | No | No | Futuro |

Notas:

- Las filas de empresas y convenios sustituyen a la antigua fila única «Registrar y editar empresas y convenios» para eliminar la ambigüedad: registrar empresas no es lo mismo que editar los datos de la propia empresa, y gestionar convenios no es lo mismo que consultarlos. Los roles y ámbitos aprobados no se amplían ni se restringen; solo se desglosan por operación y se enlazan a sus API.
- El REP `ADMIN_EMPRESA` edita los datos y gestiona los miembros de **su** empresa, consulta convenios y tutores de esa empresa, y no gestiona convenios ni crea o edita tutores. El REP `PUBLICADOR` no entra en UI-011; las lecturas permitidas a REP sobre su empresa no exigen `ADMIN_EMPRESA` salvo donde la matriz lo indica.
- `AUTORIDAD` nunca modifica datos: en toda operación de escritura es No. Consulta auditoría en el MVP.
- "Consultar prácticas" para DOC y TUT significa solo las que tienen asignadas.
- `TUTOR_EMPRESARIAL` no recomienda en el MVP; recomienda solo el representante de la empresa (`ADMIN_EMPRESA` o `PUBLICADOR`).
- La administración de estructura académica y de usuarios corresponde solo a `ADMINISTRADOR` en el MVP.
- Lo que `PRODUCTO.md` deja fuera del MVP (actividades, horas, informes, evaluaciones, certificados, reportes y módulo de requisitos) se incorporará a esta matriz cuando se implemente.
- La transición de la práctica a `EN_CURSO` (API-083) no es parte del MVP; en él solo se asignan responsables y fechas.

## 5. Proteccion de recursos

### 5.1. Quién puede acceder

| Recurso | ¿Sesión? | Roles | Restricción adicional |
|---|---|---|---|
| Inicio de sesión | No | Cualquiera | Es el único recurso público. Aplica el control de intentos. |
| Datos propios y cambio de contraseña | Sí | Todos | Solo los del propio usuario. |
| Estructura académica (consulta) | Sí | Todos | Solo lectura. |
| Estructura académica (administración) | Sí | `ADMINISTRADOR` | Ver matriz. |
| Usuarios y roles | Sí | `ADMINISTRADOR` | Administración de cuentas y roles. |
| Perfiles de estudiantes y docentes | Sí | `ADMINISTRADOR`, `COORDINADOR` | Coordinador limitado a su ámbito. |
| Empresas, miembros y convenios | Sí | `ADMINISTRADOR`, `COORDINADOR`, `REPRESENTANTE_EMPRESA` | Representante solo en su empresa. «Mi empresa» exige membresía activa `ADMIN_EMPRESA` (API-086); un `PUBLICADOR` no accede a esa pantalla. |
| Ofertas | Sí | `REPRESENTANTE_EMPRESA`, `COORDINADOR`, `ESTUDIANTE`, `ADMINISTRADOR`, `AUTORIDAD` | Estudiante: solo ofertas `PUBLICADA` dentro de plazo. Representante: solo las de su empresa. |
| Postulaciones | Sí | `ESTUDIANTE`, `REPRESENTANTE_EMPRESA`, `COORDINADOR` | Estudiante: solo las suyas. Representante: solo las de perfiles de su empresa. |
| Prácticas | Sí | Todos los roles con acceso | Cada rol, solo las suyas, las de su empresa o las de su ámbito. |
| Auditoría | Sí | `ADMINISTRADOR`, `COORDINADOR`, `AUTORIDAD` | Solo lectura; coordinador limitado a su ámbito. Nadie modifica la auditoría. Los reportes consolidados son mejora futura. |

### 5.2. Propiedad de los registros

Conocer un identificador no da derecho a usar el registro. Reglas sencillas:

1. Se verifica la pertenencia antes de actuar. Al recibir un id (de postulación, oferta, práctica…), el Service carga el registro y comprueba que el usuario sea su dueño o esté en el ámbito correspondiente. Si no, responde 404.
2. Los listados se filtran en la consulta, no después de traer todo. Un estudiante nunca recibe postulaciones de otros.
3. El cliente no controla los campos sensibles. Los DTO de entrada no incluyen estado, fechas de resolución, quién resolvió ni campos gestionados por triggers. Así no se pueden modificar enviando campos extra.
4. Las respuestas muestran lo necesario. Nunca incluyen `hash_password`, `intentos_fallidos` ni `bloqueado_hasta`; el documento de identidad solo se muestra a quien corresponde.


## 6. Validaciones de seguridad

| Riesgo | Medida |
|---|---|
| Datos de entrada inválidos | Validación de formato en los DTO de entrada y rechazo con 400. La base también valida con sus restricciones. |
| Inyección SQL | Solo consultas parametrizadas (Panache). Nunca se arma una consulta concatenando texto del usuario. |
| Sesión | Cookie HttpOnly, `Secure` en HTTPS y `SameSite` apropiado; CORS restringido al origen del frontend. Sin JWT ni tokens Bearer. La protección CSRF queda fuera del MVP como mejora futura: no se instalará `quarkus-rest-csrf` ni se implementará `CSRFHandler` u otro mecanismo propio. |
| Errores que revelan información | Los mensajes no incluyen detalles internos, SQL ni rutas. Los mensajes de autenticación son genéricos. |
| Secretos en logs | Contraseñas y hashes nunca se registran. |
| Acceso indebido a un registro ajeno | Verificación de propiedad y ámbito. |
| Fallos en la capa de aplicación | La base actúa como segunda barrera: constraints y triggers rechazan operaciones inválidas. |
| Control de orígenes | Si la interfaz React corre en otro origen, la API permite solo ese origen (CORS restringido); nunca "cualquier origen". |
| Trazabilidad | Se registran en auditoría de las operaciones. |
| Dependencias | Mantener actualizadas las dependencias del `pom.xml` y no agregar nuevas sin aprobación. |

## 7. Consideraciones adicionales

### 7.1. Lo que este plan no cubre (por diseño)

Para mantener el esquema sencillo, el MVP no incluye: recuperación de contraseña por correo, autenticación en dos pasos, JWT ni tokens, gestión de sesiones por dispositivo, datos en reposo, CAPTCHA ni límites avanzados de solicitudes por IP. Son ampliaciones futuras.

### 7.2. Decisiones aprobadas y pendientes

| # | Decisión | Estado | Alcance |
|---|---|---|---|
| 1 | Límite de intentos y duración del bloqueo como parámetros configurables, sin valores fijos. | Aprobada | MVP |
| 2 | Política mínima de contraseña: campos obligatorios y BCrypt; sin complejidad ni expiración. | Aprobada | MVP |
| 3 | Recuperación de contraseña fuera del MVP; restablecimiento por administrador. | Aprobada | MVP |
| 4 | Códigos de respuesta: 401 sin sesión, 403 sin rol, 404 para registros ajenos o fuera de ámbito. | Aprobada | MVP |
| 5 | Reglas de ámbito (`alcance_global` y `usuario_carrera`) por rol. | Aprobada en su forma básica (§3 y §4.2) | MVP |
| 6 | Eventos de seguridad adicionales en la auditoría. | Pendiente | Mejora futura |

### 7.3. Lista de verificación mínima

Antes de dar por implementado un módulo, comprobar que:

1. Ningún recurso, salvo el inicio de sesión, responde sin sesión.
2. Cada operación de escritura tiene su rol declarado en el Resource.
3. Los DTO de entrada no aceptan estado, fechas de resolución ni identificadores del actor.
4. Los DTO de salida no exponen datos de credenciales.
5. La sesión se mantiene con cookie HttpOnly; no aparece JWT ni tokens en el código ni en las respuestas.
6. `logout` invalida la sesión. La protección CSRF no se implementa en el MVP (mejora futura): no se instala `quarkus-rest-csrf` ni se implementa `CSRFHandler` o un mecanismo propio.
