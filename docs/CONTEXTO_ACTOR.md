# Contexto de actor para TK-019

## Alcance

Soporte mínimo para que las escrituras de `empresa_usuario` entreguen a los
triggers de PostgreSQL el identificador del usuario autenticado. No implementa
auditoría genérica, endpoints ni reglas de negocio de empresas.

Los triggers que lo exigen son `validar_empresa_usuario` y
`auditar_empresa_usuario` (`docs/db/V3__ofertas_practicas.sql:364-445`). Ambos
leen `current_setting('praxis.usuario_id', TRUE)` mediante `praxis_actor()`.

Esta exigencia alcanza únicamente a **API-049** (crear miembro) y **API-050**
(actualizar miembro), que son las dos operaciones REST que escriben
`empresa_usuario`. `ContextoActorService` se invoca dentro de sus transacciones,
antes del `flush()`. Las lecturas **API-048** y **API-051** no requieren fijar
contexto de actor por esta razón. Las operaciones de tutores **API-052** y
**API-053** escriben `tutor_empresarial`, sin trigger que exija este contexto, y
no deben incluirlo sin evidencia de una exigencia real. No se extiende el
mecanismo a otras operaciones por uniformidad artificial.

## Mecanismo

- `ContextoActorRepository.fijarActor(Long)` ejecuta:

  ```sql
  SELECT set_config('praxis.usuario_id', :actorId, true)
  ```

  El valor se pasa como texto y el tercer argumento es `true`.

- `ContextoActorService.fijarActor(Long)` valida el actor (rechaza `null`, `0` y
  negativos con `401 SIN_SESION`) y delega en el repositorio. Está anotado con
  `@Transactional(TxType.MANDATORY)`: exige una transacción ya activa, se une a
  ella y comparte su conexión JDBC. Si se invoca fuera de una transacción,
  Quarkus rechaza la llamada.

- El actor se obtiene de la identidad de sesión (`praxis.usuarioId`), nunca del
  cuerpo de la petición. El Resource lo entrega al Service, siguiendo el patrón
  ya usado por `EstudianteResource` y `DocenteResource`.

## Por qué no persiste entre transacciones

`set_config(..., true)` es de ámbito transaccional: PostgreSQL restaura el valor
anterior al terminar `COMMIT` o `ROLLBACK`. No se usa `SET` de sesión ni se
ejecuta una sentencia de limpieza manual, por lo que una conexión devuelta al
pool no conserva el actor de otra operación. Tampoco se guarda el actor en un
campo del Service o del Repository.

## Uso implementado en TK-019

Los Services que escriben `empresa_usuario` (API-049 y API-050):

1. Recibir `actorId` y `esAdmin` desde el Resource (patrón vigente).
2. Mantener su propio `@Transactional` en la escritura.
3. Llamar a `contextoActorService.fijarActor(actorId)` dentro de esa transacción,
   antes de persistir o modificar `empresa_usuario`.
4. Mantener en ese mismo Service la autorización por empresa y las reglas de
   `RN-009` (la base es una segunda barrera, no la única).

API-048 y API-051 son consultas; API-052 y API-053 escriben
`tutor_empresarial`, que no tiene trigger que exija este contexto.

## Operaciones que no requieren este contexto (TK-018)

Las escrituras de `empresa` y `convenio` (API-041, API-042 y API-044 a API-047)
no exigen `praxis.usuario_id`. En el DDL vigente esas tablas solo tienen el
trigger genérico `trg_<tabla>_updated`, que actualiza `updated_at`
(`docs/db/V1__esquema.sql:478-496`); no existe ningún trigger de validación ni
de auditoría sobre `empresa` o `convenio` que lea `praxis_actor()`. Por ello sus
Services no llaman a `ContextoActorService`: hacerlo sería anticiparse a una
exigencia que hoy no existe. La tabla `auditoria` está definida, pero estos
triggers no escriben en ella.

## Verificación y limitaciones

Las pruebas unitarias comprueban la sentencia y su parámetro, el rechazo sin
actor, la ausencia de estado compartido y la exigencia de transacción previa.
La comprobación real del trigger (actor recibido, rechazo sin actor y
restauración del valor tras `COMMIT`) requiere un entorno de integración con
escritura autorizada; no se ejecutó porque esta tarea prohíbe escribir en
`praxis_local` y en `praxis_test`.
