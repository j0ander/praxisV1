# Contexto de actor

## Estado actual

El contexto de actor y el disparador de los parámetros de sesión **fueron eliminados** de la aplicación.

- Ya no existe `ContextoActorService` ni `ContextoActorRepository`: la aplicación no ejecuta `set_config('praxis.usuario_id', ..., true)` antes de escribir.
- Ya no se obtiene un usuario de una sesión ni se pasa un `actorId` desde el Resource al Service. No se fijan `praxis.motivo` ni `praxis.sistema` desde la capa de aplicación.
- `hashPassword`, `intentosFallidos` y `bloqueadoHasta` u otras columnas de la BD no sustituyen al actor: no se inventa un identificador de usuario, un valor fijo ni un usuario "sistema" para evitar el error.

## Consecuencia en la base de datos

Los triggers `validar_empresa_usuario` y `auditar_empresa_usuario` (`docs/db/V3__ofertas_practicas.sql:364-445`) leen `current_setting('praxis.usuario_id', TRUE)` mediante `praxis_actor()`. Al no recibir el valor, **pueden rechazar o fallar** las escrituras de `empresa_usuario` (API-049 y API-050). La ausencia del contexto también afecta a cualquier otra operación auditada cuyos triggers dependan de esos parámetros.

Según el DDL vigente, las escrituras de `empresa`, `convenio` y `tutor_empresarial` no tenían un trigger que exigiera este contexto. El esquema, los triggers y los parámetros de la base **no se modifican** (`MODELO_DATOS.md` §7.3).

## Pendiente a futuro

Restaurar el contexto de actor exigido por los triggers requiere una decisión aprobada sobre el origen del identificador y una tarea autorizada, siempre sin cambios de esquema. Mientras tanto, la limitación se documenta y no se resuelve escribiendo columnas gestionadas por triggers ni desactivando validaciones de la base.
