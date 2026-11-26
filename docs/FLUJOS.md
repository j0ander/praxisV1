# Flujos del sistema

## 1. Introducción

Describe cómo se desarrollan los procesos principales de PRAXIS desde la perspectiva de cada actor, para comprender el producto antes de implementar sus operaciones. Las condiciones detalladas están en `REGLAS_NEGOCIO.md` (RN-) y los comportamientos requeridos en `REQUISITOS.md` (RF-); aquí solo se referencian.

**Alcance del MVP:** se implementan ofertas, postulaciones, empresas y convenios, estructura académica, usuarios, formalización, asignación de responsables y fechas, y auditoría. Se incorpora un login básico de prototipo que verifica credenciales y filtra pantallas en React; no hay sesión ni autorización en la API (`SEGURIDAD.md`). Los flujos `FL-010`, `FL-011`, `FL-018`, `FL-019`, `FL-020`, `FL-021` y `FL-022` (actividades y horas, informes, visitas, evaluaciones y certificados) y los reportes consolidados de `FL-024` son mejoras futuras (`PRODUCTO.md` §5 y §7; CR-6 y CR-8). `FL-017` conserva en el MVP únicamente la asignación de responsables y fechas.

Actores:

| Actor | Rol en los flujos |
|---|---|
| Estudiante | Consulta ofertas, postula, registra horas y entrega informes. |
| Representante de empresa | Publica ofertas y recomienda postulantes (`ADMIN_EMPRESA` o `PUBLICADOR`). |
| Coordinador | Revisa ofertas, resuelve postulaciones, formaliza prácticas, gestiona convenios, requisitos y certificados. |
| Docente tutor | Supervisa, valida horas, revisa informes, visita y evalúa. |
| Tutor empresarial | Valida horas y evalúa el desempeño. |
| Administrador | Gestiona usuarios, roles y catálogos. |
| Autoridad | Consulta reportes (solo lectura). |
| Sistema | Ejecuta procesos automáticos. |

## 2. Flujo de acceso y contraseña

### FL-001 - Inicio de sesión (básico, prototipo)
**Estado:** implementado. El usuario envía correo y contraseña a `POST /usuarios/login`; el backend verifica la cuenta activa y el hash BCrypt y devuelve datos básicos y roles. React conserva esa respuesta en memoria, muestra el menú permitido y ofrece cerrar sesión. No hay sesión del servidor, cookie, JWT ni tokens; al recargar se pide el login de nuevo. No se contabilizan intentos fallidos ni bloqueos y los roles no autorizan llamadas REST.
**Referencias:** RF-002 y RF-003, RN-002 y RN-003. La matriz visual de módulos está en `INTERFACES.md` §2.

### FL-002 - Actualización de contraseña (no vigente en su parte de acceso)
**Estado:** no hay cambio obligatorio, restablecimiento ni recuperación de contraseña desde la interfaz. BCrypt se usa para generar hashes en la administración de usuarios y para verificarlos en el login básico. Las columnas de intentos, bloqueo y `ultimo_login` no se usan (`SEGURIDAD.md`, `MODELO_DATOS.md` §4.2).
**Referencias:** RF-003 y RN-002: marcados como no vigentes en su parte de ingreso, intentos y bloqueo.

## 3. Flujos de empresa

### FL-003 - Publicar una oferta de prácticas
Objetivo: 
Que la empresa proponga una convocatoria para revisión de la universidad.
Actores: 
Representante de empresa; sistema.
Precondiciones: 
Empresa activa con convenio vigente; usuario miembro activo de la empresa; periodo académico activo.
Flujo principal:
1. El representante crea la oferta: título, descripción, tipo de práctica, periodo, convenio y fechas de actividades y de cierre.
2. El sistema la registra como `BORRADOR`.
3. El representante agrega uno o más perfiles con plazas, carreras destinatarias y requisitos laborales.
4. El representante envía la oferta a revisión.
5. El sistema revalida las condiciones, congela las horas requeridas y la nota mínima del tipo de práctica y la pasa a `PENDIENTE_REVISION`.

Alternativos:
- A1. El representante retira la oferta a `BORRADOR` mientras está en revisión.
- A2. El representante cancela la oferta (`CANCELADA`) indicando el motivo; solo el `ADMIN_EMPRESA` o un coordinador pueden hacerlo.
- A3. Un `PUBLICADOR` solo gestiona las ofertas que creó; el `ADMIN_EMPRESA`, todas las de su empresa.

Excepciones:
- E1. Convenio no vigente, actividades fuera de su vigencia o del periodo, cierre vencido o sin perfiles con plazas: se rechaza el envío indicando la causa.

Resultado: 
La oferta queda `PENDIENTE_REVISION`.
Referencias: 
RF-009, RF-010; RN-011, RN-012, RN-013, RN-016.

### FL-004 - Corregir o modificar una oferta
Objetivo: 
Ajustar una oferta rechazada o ya publicada.
Actores: 
Representante de empresa.
Precondiciones: 
La oferta está `RECHAZADA` o `PUBLICADA`.
Flujo principal:
1. El representante consulta las observaciones de la coordinación.
2. Edita la oferta, sus perfiles, carreras o requisitos.
3. Reenvía la oferta a revisión.

Alternativos:
- A1. Edición de una oferta `PUBLICADA`: el sistema la devuelve a `PENDIENTE_REVISION` y deja de recibir postulaciones hasta nueva aprobación.
- A2. Si ya hay postulaciones, el tipo de práctica no se puede cambiar; debe crearse una nueva oferta.

Excepciones: 
E1. Ofertas `CERRADA` o `CANCELADA`: no se editan.
Resultado: 
La oferta vuelve a revisión con sus cambios registrados.
Referencias: 
RF-009, RF-010, RF-011; RN-015.

### FL-005 - Recomendar postulantes
Objetivo: 
Que la empresa aporte su valoración antes de la decisión de la coordinación.
Actores: 
Representante de empresa.
Precondiciones: 
Existen postulaciones `PENDIENTE` a perfiles de una oferta de su empresa.
Flujo principal:
1. El representante consulta los postulantes de cada perfil.
2. Registra para cada uno `RECOMENDADO` o `NO_RECOMENDADO` y una observación.
3. El sistema guarda la recomendación y su historial.

Alternativos: 
A1. El representante cambia una recomendación previa; no puede volver a `SIN_EVALUAR`.
Excepciones: 
E1. Postulación ya resuelta u oferta en revisión: no se admiten recomendaciones.
Resultado: 
La coordinación dispone de la recomendación de la empresa; esta no obliga su decisión.
Referencias: 
RF-014; RN-019.

### FL-006 - Gestionar usuarios de la empresa
Objetivo: 
Controlar quién publica ofertas por la empresa.
Actores (datos de dominio, sin control de acceso): 
`ADMIN_EMPRESA`, coordinador o administrador.
Precondiciones: 
La empresa está registrada y activa; el usuario tiene el rol `REPRESENTANTE_EMPRESA` como dato de dominio.
Flujo principal:
1. El actor vincula al usuario a la empresa como `ADMIN_EMPRESA` o `PUBLICADOR`.
2. El sistema valida y registra la membresía.

Alternativos: 
A1. Se desactiva una membresía; el registro se conserva, no se elimina. No hay sesión ni permisos que se desactiven.
Excepciones: 
E1. El primer miembro no es `ADMIN_EMPRESA`, o se intenta dejar la empresa sin administrador activo: se rechaza.
Resultado: 
El usuario queda vinculado a la empresa como dato de dominio; la operación no depende de un usuario autenticado ni de un ámbito de acceso.
Referencias: 
RF-007; RN-009.

## 4. Flujos de alumno

### FL-007 - Consultar ofertas disponibles
Objetivo: 
Que el estudiante encuentre oportunidades acordes a su carrera.
Actores: 
Estudiante.
Precondiciones: 
Perfil de estudiante con carrera activa.
Flujo principal:
1. El estudiante accede al listado de ofertas.
2. El sistema muestra las ofertas `PUBLICADAS`, dentro del plazo, con perfiles dirigidos a su carrera o abiertos a todas.
3. El estudiante busca y abre el detalle de una oferta.

Alternativos: 
A1. Un perfil sin plazas disponibles se muestra como cerrado.
Excepciones: 
E1. No hay ofertas disponibles: el sistema lo informa.
Resultado: 
El estudiante identifica el perfil al que desea postular.
Referencias: 
RF-012; RN-014, RN-016.

### FL-008 - Postularse a un perfil
Objetivo: 
Registrar el interés del estudiante en una plaza.
Actores: 
Estudiante.
Precondiciones: 
El estudiante consultó una oferta disponible (FL-007).
Flujo principal:
1. El estudiante selecciona un perfil y, opcionalmente, escribe un mensaje.
2. El sistema verifica las condiciones de participación y la disponibilidad de plazas.
3. El sistema registra la postulación como `PENDIENTE`.
4. El sistema confirma el registro.

Alternativos:
- A1. El estudiante edita su mensaje mientras la empresa no haya recomendado.
- A2. El estudiante repostula a un perfil del que se había retirado.

Excepciones:
- E1. Perfil no dirigido a su carrera, oferta cerrada, convenio no vigente o perfil sin plazas: se rechaza indicando la causa.
- E2. Ya existe una postulación vigente, rechazada, aceptada o cancelada a ese perfil: se rechaza.
- E3. Requisitos académicos incumplidos: se rechaza (validación a cargo de la API; CR-6).

Resultado: 
La postulación queda `PENDIENTE` y visible para la empresa y la coordinación.
Referencias: 
RF-013; RN-007, RN-017, RN-018.

### FL-009 - Retirar una postulación
Objetivo: 
Que el estudiante desista de una postulación.
Actores: 
Estudiante.
Precondiciones: 
La postulación está `PENDIENTE`.
Flujo principal:
1. El estudiante selecciona su postulación y solicita retirarla.
2. El sistema la marca como `RETIRADA` y lo registra en auditoría.

Excepciones: 
E1. La postulación ya fue aceptada o rechazada: no se puede retirar.
Resultado: 
La postulación se retira y el estudiante puede repostular al perfil.
Referencias: 
RF-013; RN-018, RN-021.

### FL-010 - Registrar actividades y horas
Objetivo: 
Documentar el avance de la práctica.
Actores: 
Estudiante.
Precondiciones: 
Su práctica está `EN_CURSO`.
Flujo principal:
1. El estudiante registra la fecha, las horas y la descripción de la jornada.
2. El sistema valida las condiciones del registro.
3. El sistema lo guarda como no validado y actualiza el progreso.

Alternativos: 
A1. El estudiante modifica o elimina un registro mientras no esté validado.
Excepciones: 
E1. Fecha futura o fuera del periodo, más de 8 horas, registro ya existente para el día, o práctica que no está en curso: se rechaza.
Resultado: 
El registro queda pendiente de validación.
Referencias: 
RF-018; RN-027.

### FL-011 - Entregar un informe
Objetivo: 
Presentar informes parciales o finales.
Actores: 
Estudiante.
Precondiciones: 
Práctica formalizada.
Flujo principal:
1. El estudiante selecciona el tipo de informe (`PARCIAL` o `FINAL`) y carga el PDF.
2. El sistema valida el archivo y lo guarda como `ENTREGADO`.

Alternativos: 
A1. Informe `OBSERVADO`: el estudiante entrega una versión corregida.
Excepciones: 
E1. Archivo que no es PDF o excede 2 MB: se rechaza.
Resultado: 
El informe queda disponible para revisión.
Referencias: 
RF-020; RN-030.

## 5. Flujos de universidad

### FL-012 - Administrar estructura académica y catálogos
Objetivo: 
Mantener facultades, carreras, periodos, tipos de práctica y requisitos.
Actores: 
Administrador; coordinador (requisitos).
Precondiciones: 
Sin sesión ni verificación de permisos por rol; los roles son datos de dominio.
Flujo principal:
1. El actor registra o modifica facultades y carreras.
2. Registra periodos académicos con sus fechas.
3. Activa el periodo vigente.
4. Registra los tipos de práctica con sus horas requeridas y nota mínima.
5. El coordinador define requisitos globales o por carrera (fuera del MVP; mejora futura, CR-6).

Alternativos: 
A1. Desactivar facultades, carreras o tipos deja de ofrecerlos para nuevas operaciones.
Excepciones: 
E1. Ya existe otro periodo activo: se rechaza la activación.
 E2. Fecha de fin anterior a la de inicio: se rechaza.
Resultado: 
La estructura académica queda lista para los procesos de prácticas.
Referencias: RF-004, RF-006; RN-005, RN-006, RN-007.

### FL-013 - Gestionar usuarios y perfiles
Objetivo: 
Registrar cuentas y habilitar a cada persona según su función.
Actores: 
Administrador.
Precondiciones: 
Usuario con rol `ADMINISTRADOR`.
Flujo principal:
1. El administrador registra al usuario con sus datos personales e institucionales.
2. Le asigna uno o más roles.
3. Si es estudiante, registra su perfil con carrera; si es docente, el sistema crea su perfil al asignar el rol.
4. Si es tutor empresarial, lo vincula a su empresa.

Alternativos: 
A1. El administrador desactiva o reactiva la cuenta.
Excepciones: 
E1. Correo o documento duplicado, o perfil ya existente: se rechaza.
Resultado: 
El usuario puede ingresar con las funciones de sus roles.
Referencias: 
RF-001, RF-002, RF-005; RN-001, RN-003, RN-004.

### FL-014 - Registrar empresa y convenio
Objetivo: 
Dar de alta a las empresas receptoras y su convenio institucional.
Actores: 
Coordinador.
Precondiciones: 
Usuario con rol `COORDINADOR`.
Flujo principal:
1. El coordinador registra la empresa con su RUC.
2. Registra el convenio: número, fechas de vigencia y documento.
3. El sistema deja el convenio `VIGENTE`.
4. El coordinador habilita a los representantes (FL-006) y registra los tutores empresariales.

Alternativos: 
A1. El coordinador suspende (`SUSPENDIDO`) o termina (`TERMINADO`) un convenio.
Excepciones: 
E1. RUC inválido o duplicado, o fechas incoherentes: se rechaza.
Resultado: 
La empresa puede publicar ofertas mientras su convenio esté vigente.
Referencias: 
RF-007, RF-008; RN-008, RN-009, RN-010.

### FL-015 - Revisar una oferta
Objetivo: 
Decidir si una oferta se publica.
Actores: 
Coordinador.
Precondiciones: 
Oferta en `PENDIENTE_REVISION`.
Flujo principal:
1. El coordinador revisa la oferta, sus perfiles y condiciones.
2. Aprueba la oferta.
3. El sistema revalida las condiciones y la pasa a `PUBLICADA`, visible para los estudiantes.

Alternativos: 
A1. El coordinador rechaza la oferta con un motivo; pasa a `RECHAZADA` y la empresa puede corregirla (FL-004).
Excepciones: 
E1. Convenio, periodo o plazos dejaron de ser válidos: se rechaza la aprobación indicando la causa.
Resultado: 
Oferta `PUBLICADA` o `RECHAZADA`, con la decisión registrada.
Referencias: 
RF-011; RN-012, RN-014.

### FL-016 - Resolver una postulación y formalizar la práctica
Objetivo: 
Decidir la postulación y, si se acepta, crear la práctica.
Actores: 
Coordinador; sistema.
Precondiciones: 
Postulación `PENDIENTE`; idealmente con la recomendación de la empresa (FL-005).
Flujo principal:
1. El coordinador revisa la postulación, el mensaje del estudiante y la recomendación de la empresa.
2. Acepta la postulación.
3. El sistema verifica plaza disponible, convenio vigente, habilitación de empresa, tipo y periodo, y que el estudiante no tenga otra práctica vigente en el periodo.
4. En la misma operación, el sistema marca la postulación `ACEPTADA` y crea la práctica `APROBADA` con las condiciones congeladas de la oferta.
5. Si se cubren todas las plazas de la oferta, el sistema la cierra.

Alternativos:
- A1. El coordinador rechaza la postulación con un motivo (`RECHAZADA`).
- A2. El coordinador cancela una postulación aceptada, con motivo; antes debe cancelar o rechazar la práctica.

Excepciones: 
E1. Sin plazas, convenio no vigente o práctica vigente del estudiante en el periodo: se rechaza la aceptación.
Resultado: 
Postulación resuelta y, si se acepta, práctica formalizada.
Referencias: 
RF-015, RF-016, RF-017; RN-018, RN-019, RN-020, RN-021, RN-022.

Nota: las referencias RN-023 y RN-024 de la versión anterior no tienen definición en `REGLAS_NEGOCIO.md`. RN-023 se correspondía con el escenario de práctica vigente por estudiante y periodo, cubierto por RN-021; RN-024 no pudo vincularse con certeza a ninguna regla documentada y no se sustituyó por una regla inventada. Queda registrado como pendiente de aclaración al final de esta sección.

### FL-017 - Asignar responsables e iniciar la práctica
Objetivo: 
Dejar la práctica lista para su ejecución.
Actores: 
Coordinador.
Precondiciones: 
Práctica `APROBADA`.
Flujo principal:
1. El coordinador asigna el docente tutor.
2. Asigna el tutor empresarial de la empresa receptora.
3. Registra las fechas de inicio y fin de la práctica.
4. (Mejora futura, CR-8) Inicia la práctica; el sistema la pasa a `EN_CURSO`. No se implementa en el MVP.

Alternativos: 
A1. El coordinador cancela la práctica (`CANCELADA`); su postulación aceptada también se cancela y se libera la plaza.
Excepciones: 
E1. Tutor de otra empresa, o falta docente, tutor o fechas: no se puede iniciar.
Resultado: 
La práctica `APROBADA` con responsables y fechas asignados, lista para iniciar en una versión futura.
Referencias: 
RF-016, RF-017; RN-025, RN-026.

### FL-018 - Validar horas
Objetivo: 
Confirmar las horas registradas por el estudiante.
Actores: 
Docente tutor o tutor empresarial.
Precondiciones: 
Práctica `EN_CURSO` con registros no validados.
Flujo principal:
1. El responsable consulta los registros y el progreso respecto al total requerido.
2. Valida cada registro.
3. El sistema guarda quién validó y cuándo, y suma las horas validadas.
4. Al alcanzar el total requerido, el sistema pasa la práctica a `HORAS_COMPLETAS`.

Alternativos: 
A1. El responsable observa un registro en lugar de validarlo (tratamiento pendiente de definir).
Excepciones: 
E1. Registro ya validado: no se modifica ni elimina.
Resultado: 
Horas validadas y práctica lista para evaluar cuando se completan.
Referencias: 
RF-019; RN-028, RN-029.

### FL-019 - Revisar un informe
Objetivo: 
Aprobar u observar el informe del estudiante.
Actores: 
Docente tutor.
Precondiciones: 
Informe `ENTREGADO`.
Flujo principal:
1. El docente abre el informe.
2. Lo aprueba y el sistema lo marca `APROBADO`.

Alternativos: 
A1. El docente registra observaciones: el informe pasa a `OBSERVADO` hasta una nueva entrega.
Excepciones: 
E1. Informe ya aprobado: no se modifica. 
E2. Ya existe un informe final aprobado: no se aprueba otro.
Resultado: 
Informe `APROBADO` u `OBSERVADO`.
Referencias: 
RF-020; RN-030, RN-031.

### FL-020 - Registrar visitas e incidencias
Objetivo: 
Dar seguimiento a la práctica y atender problemas.
Actores: 
Docente tutor; estudiante, tutor empresarial o coordinador (incidencias).
Precondiciones: 
Práctica formalizada.
Flujo principal:
1. El docente registra la fecha y observaciones de la visita.
2. Un participante reporta una incidencia; el sistema la registra `ABIERTA`.
3. El responsable de atención la resuelve y el sistema la marca `RESUELTA` con responsable y fecha.

Excepciones: 
E1. Práctica inexistente o incidencia no registrada: se rechaza.
Resultado: 
Seguimiento consultable por los responsables.
Referencias: 
RF-021, RF-022; RN-032.

### FL-021 - Evaluar y determinar el resultado
Objetivo: 
Calificar la práctica y fijar su resultado.
Actores: 
Docente tutor; tutor empresarial; sistema.
Precondiciones: 
Práctica `HORAS_COMPLETAS` con horas validadas y ambos responsables asignados.
Flujo principal:
1. El docente registra su evaluación (nota de 0 a 20 y criterios).
2. El tutor empresarial registra la suya.
3. El sistema calcula la nota final y pasa la práctica a `EVALUADA`.
4. El sistema compara la nota con la mínima congelada y define `FINALIZADA` o `REPROBADA`.

Excepciones: 
E1. Falta una evaluación o hay horas pendientes de validar: no se determina el resultado.
Resultado: 
Práctica `FINALIZADA` o `REPROBADA`. La fórmula de la nota final está pendiente de definir.
Referencias: 
RF-023, RF-024; RN-033, RN-034, RN-035.

### FL-022 - Emitir, verificar y anular certificados
Objetivo: 
Certificar las prácticas finalizadas y permitir su verificación.
Actores: 
Coordinador; estudiante; cualquier persona con el código.
Precondiciones: 
Práctica `FINALIZADA`.
Flujo principal:
1. El coordinador emite el certificado.
2. El sistema lo genera con un código único y datos congelados, en estado `VIGENTE`.
3. El estudiante lo consulta y descarga.
4. Un tercero ingresa el código y el sistema informa su autenticidad y estado.

Alternativos: 
A1. El coordinador anula el certificado con motivo; queda `ANULADO` de forma definitiva y sigue siendo verificable.
Excepciones: 
E1. Práctica reprobada o no finalizada, o ya existe un certificado vigente: se rechaza la emisión. 
E2. Código inexistente: no se puede verificar.
Resultado: 
Certificado `VIGENTE` verificable, o `ANULADO` con historial.
Referencias: 
RF-025; RN-036, RN-037, RN-038.

### FL-023 - Cierre automático de ofertas
Objetivo: 
Cerrar las ofertas cuyo plazo de postulación venció.
Actores: 
Sistema.
Precondiciones: 
Existen ofertas `PUBLICADA` con fecha de cierre pasada.
Flujo principal:
1. El sistema ejecuta periódicamente el proceso de cierre.
2. Pasa esas ofertas a `CERRADA` e identifica la operación como del sistema en el historial.

Resultado: 
Las ofertas dejan de recibir postulaciones; las pendientes pueden resolverse.
Referencias: 
RN-016, RN-039.

### FL-024 - Consultar reportes y auditoría
Objetivo: 
Apoyar la gestión y el control institucional.
Actores: 
Autoridad; coordinador; administrador.
Precondiciones: 
Sin sesión ni verificación de permisos por rol; la consulta no depende del rol `AUTORIDAD`.
Flujo principal:
1. El actor selecciona un reporte o consulta de auditoría y sus filtros.
2. El sistema presenta la información consolidada.

Excepciones: 
E1. No hay datos suficientes: el sistema no presenta resultados inventados.
Resultado: 
Trazabilidad de las operaciones en el MVP; los reportes consolidados son mejora futura (CR-6).
Referencias: 
RF-026, RF-027; RN-039, RN-040.

## 6. Excepciones y escenarios alternativos

Situaciones que afectan a varios flujos.

| Escenario | Comportamiento esperado | Flujos | Reglas |
|---|---|---|---|
| Dos coordinadores aceptan la última plaza a la vez | Solo se confirma una aceptación; la otra se rechaza por falta de plazas con 409 `CONFLICTO_CONCURRENCIA`. | FL-016 | RN-018, RN-020 |
| El convenio vence o se suspende | No se admiten nuevas postulaciones ni aceptaciones. Las prácticas ya formalizadas no cambian automáticamente. | FL-008, FL-016 | RN-010 |
| Oferta publicada modificada con postulaciones | La oferta vuelve a revisión; no admite postulaciones, recomendaciones ni decisiones hasta su aprobación. Las postulaciones y las condiciones académicas originales se conservan. | FL-004, FL-005, FL-016 | RN-015 |
| Oferta cerrada con postulaciones pendientes | Siguen pudiendo recomendarse y resolverse. | FL-005, FL-016, FL-023 | RN-016 |
| Práctica cancelada o rechazada | Su postulación aceptada pasa a `CANCELADA` y se libera la plaza. | FL-017 | RN-026 |
| Estudiante con práctica vigente en el periodo | No puede ser aceptado en otra oferta del mismo periodo. | FL-016 | RN-021 |
| Estudiante retira y vuelve a postular | Permitido solo tras `RETIRADA`; no tras rechazo, aceptación o cancelación. | FL-008, FL-009 | RN-019 |
| Corrección de una decisión ya tomada | Se realiza con motivo y auditoría; no por el flujo ordinario. No hay restricción por rol o ámbito. | FL-016 | RN-018 (CR-4) |
| Sin periodo activo | No se pueden enviar ni aprobar ofertas, ni aceptar postulaciones. | FL-003, FL-015, FL-016 | RN-006, RN-012 |
| Usuario de empresa desactivado o sin rol | `REPRESENTANTE_EMPRESA`, `ADMIN_EMPRESA` y `PUBLICADOR` son datos de dominio; no hay pérdida de permisos por sesión ni por rol. Las ofertas no se eliminan por este dato. | FL-003, FL-006 | RN-009 |
| Cuenta bloqueada | No vigente; pendiente a futuro. No hay bloqueo de cuentas ni denegación de acceso. | FL-001 | RN-002 |
| Operación sin usuario identificado | La aplicación ya no fija `praxis.usuario_id`; los triggers que lo requieran pueden rechazar la escritura (`CONTEXTO_ACTOR.md`). No se inventa un actor. | Todos | RN-039 |

**Concurrencia básica del MVP:** la aceptación de postulaciones usa control optimista (`@Version` en `postulacion`) y comprueba y reserva la plaza dentro de la misma transacción que crea la práctica `APROBADA`; si otra operación ocupó la plaza o modificó la postulación, responde 409 `CONFLICTO_CONCURRENCIA`. La asignación de responsables usa control optimista (`@Version` en `practica`) y responde 409 `CONFLICTO_CONCURRENCIA` si otra operación modificó la práctica; no reserva plazas. No se usan bloqueos distribuidos, colas ni mecanismos externos.

**Referencias RN-023 y RN-024:** no existen reglas con esos identificadores en `REGLAS_NEGOCIO.md`. RN-023 se resolvió como RN-021 por coincidencia exacta con el escenario de práctica vigente por periodo. RN-024 se conserva aquí como referencia heredada sin equivalente verificable; no se creó una regla nueva ni se cambió el comportamiento funcional para completar la numeración.
