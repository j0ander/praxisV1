# Seguridad

## 1. Estado actual

PRAXIS tiene un **login básico de prototipo**: verifica correo y contraseña contra los usuarios existentes y devuelve datos mínimos y roles para que React muestre u oculte pantallas. No constituye autenticación de la API ni autorización real.

- No hay sesión del servidor, cookies, JWT, tokens Bearer ni renovación/caducidad. Tras recargar el navegador se solicita el login nuevamente.
- No hay identidad del usuario por petición: no se usan `SecurityIdentity`, `@Authenticated` ni `@RolesAllowed`.
- La API no autoriza por rol: los códigos de rol (`ADMINISTRADOR`, `COORDINADOR`, `ESTUDIANTE`, `DOCENTE`, `TUTOR_EMPRESARIAL`, `REPRESENTANTE_EMPRESA`, `AUTORIDAD`) se conservan como datos de dominio y solo alimentan la matriz visual del frontend.
- No se contabilizan intentos fallidos, no hay bloqueo de cuentas ni cambio obligatorio de contraseña.
- No hay administrador inicial: no existe proceso de arranque que cree cuentas ni credenciales.
- No se configura zona horaria de negocio en el código. Las fechas automáticas compatibles se calculan con `LocalDate.now()`. Las columnas `TIMESTAMPTZ` gestionadas por la base (`created_at`, `updated_at`) se conservan y se leen como `OffsetDateTime`, sin convertirlas a `LocalDate`.

## 2. Contraseñas

- BCrypt se usa para **generar** (`BcryptUtil.bcryptHash`) y **verificar** (`BcryptUtil.matches`) contraseñas. La verificación ocurre solo en `POST /usuarios/login`.
- La contraseña en texto plano nunca se almacena, se muestra, se registra en logs ni se devuelve en una respuesta. Tampoco se guarda en `localStorage`, `sessionStorage` ni otro mecanismo del navegador.
- `hashPassword`, `intentosFallidos` y `bloqueadoHasta` nunca se serializan. El login responde 401 genérico si la cuenta no existe, está inactiva o la contraseña no coincide.

## 3. Columnas retenidas solo en la base de datos

El esquema existente **no se modifica**. La tabla `usuario` conserva `hash_password`, `debe_cambiar_password`, `intentos_fallidos`, `bloqueado_hasta` y `ultimo_login`, y subsisten `activo`, `alcance_global`, `rol`, `usuario_rol` y `usuario_carrera`. El login solo lee `hash_password` y `activo`; las columnas de intentos, bloqueo y `ultimo_login` no se usan (`MODELO_DATOS.md` §4).

Los triggers de la base pueden leer parámetros de sesión como `praxis.usuario_id`. Como la aplicación ya no fija ese contexto, esas escrituras pueden ser rechazadas por la base; el detalle y el pendiente están en `CONTEXTO_ACTOR.md`.

## 4. Validaciones que siguen vigentes

- Formato inválido o campo obligatorio ausente: 400 `DATOS_INVALIDOS`.
- Credenciales incorrectas en el login: 401 `CREDENCIALES_INVALIDAS`, con mensaje genérico.
- Registro inexistente: 404 `NO_ENCONTRADO`.
- Incumplimiento de una regla de negocio, duplicado o conflicto de concurrencia: 409 (`REGLA_NEGOCIO`, `DUPLICADO`, `CONFLICTO_CONCURRENCIA`).
- Error interno: 500 `ERROR_INTERNO`, sin detalles.
- Consultas parametrizadas (Panache), sin concatenar valores del usuario en JPQL.
- Sin secretos en logs ni en respuestas; sin credenciales versionadas.
- La base actúa como segunda barrera: constraints y triggers rechazan operaciones inválidas. No se escriben columnas gestionadas por triggers.

No se devuelve 403 ni hay códigos `SIN_SESION`/`SIN_PERMISO` para el resto de la API. El formato de errores está en `CONTRATOS_REST.md` §6.

## 5. Pendiente a futuro

Quedan para una decisión aprobada y una tarea autorizada, sin cambios de esquema:

- Sesiones del servidor, cookies, caducidades, cierre de sesión en servidor y autenticación de la API.
- Autorización real por rol y permisos, con su matriz correspondiente.
- Control de intentos fallidos, bloqueo de cuentas y cambio obligatorio de contraseña.
- Recuperación de contraseña e inicialización del primer administrador.
- Restablecimiento del contexto de actor exigido por los triggers.

El ocultamiento de menús y rutas en React no sustituye ninguno de estos elementos.
