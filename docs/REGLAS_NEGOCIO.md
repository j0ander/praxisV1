# Reglas de negocio

## 1. Introducción

Define las condiciones que PRAXIS debe respetar al ejecutar sus operaciones, las restricciones de cada proceso y los resultados esperados.
Cubre: usuarios y accesos, gestión académica, empresas y convenios, ofertas, postulaciones, formalización de prácticas, ejecución y seguimiento, evaluación y certificación, y trazabilidad. No incluye bolsas de empleo externas ni procesos de titulación ajenos a las prácticas.

## 2. Reglas generales

### RN-001 - Unicidad de cuentas de usuario
Descripción: Cada persona tiene una sola cuenta.
Condición: El correo (en minúsculas y con formato válido) y el número de documento (cédula de 10 dígitos o pasaporte alfanumérico de 5 a 20) no deben estar registrados.
Resultado: La cuenta se registra.
Si no se cumple: Se rechaza el registro indicando el dato duplicado o inválido.

### RN-002 - Acceso según estado de la cuenta
Descripción: El acceso depende del estado de la cuenta y de las credenciales.
Condición: La cuenta debe estar activa, no bloqueada (`bloqueado_hasta` vencido o vacío) y las credenciales deben ser válidas. La contraseña se guarda cifrada, nunca se muestra ni se recupera (solo se actualiza o restablece) y `debe_cambiar_password` obliga a cambiarla al ingresar.
Resultado: El usuario accede a las funciones autorizadas.
Si no se cumple: Se deniega el acceso; los intentos fallidos se acumulan hasta el bloqueo temporal.

### RN-003 - Operaciones según rol
Descripción: Cada operación corresponde a un rol activo: `ESTUDIANTE`, `DOCENTE`, `TUTOR_EMPRESARIAL`, `COORDINADOR`, `ADMINISTRADOR`, `AUTORIDAD` (solo lectura) y `REPRESENTANTE_EMPRESA`.
Condición: El usuario debe estar activo y tener el rol requerido por la operación. Su ámbito institucional (carreras en `usuario_carrera`, o `alcance_global`) limita la información que gestiona; lo aplica la API.
Resultado: La operación se ejecuta dentro del ámbito autorizado.
Si no se cumple: Se rechaza la operación. La base de datos actúa como segunda barrera; la API debe autorizar siempre.

### RN-004 - Perfiles académicos vinculados a la cuenta
Descripción: Estudiantes y docentes tienen un único perfil asociado a su usuario.
Condición: El usuario debe existir. El perfil de estudiante exige carrera. El perfil de docente se crea automáticamente al asignar el rol `DOCENTE`.
Resultado: El perfil queda vinculado y disponible para los procesos académicos.
Si no se cumple: Se rechaza la creación o vinculación si el usuario no existe o el perfil ya existe.

### RN-005 - Coherencia de la estructura académica
Descripción: Cada carrera pertenece a una facultad; estudiantes, requisitos y perfiles de oferta se asocian a carreras existentes.
Condición: Las carreras referenciadas deben existir y, para asignarlas a perfiles de oferta o postular, estar activas.
Resultado: La estructura académica organiza perfiles y prácticas.
Si no se cumple: Se rechazan las relaciones inexistentes o con carreras inactivas.

### RN-006 - Un único periodo académico activo
Descripción: Solo un periodo puede estar activo a la vez.
Condición: No debe existir otro periodo activo, y la fecha de fin no puede ser anterior a la de inicio.
Resultado: El periodo queda activo.
Si no se cumple: Se impide la activación.

## 3. Reglas por proceso

### RN-007 - Requisitos académicos del estudiante
Descripción: Un requisito puede ser global o de una carrera; su cumplimiento se registra por estudiante y periodo.
Condición: El requisito debe existir y estar activo, y aplicar a la carrera del estudiante. El cumplimiento es único por estudiante, requisito y periodo; `CUMPLIDO` y `RECHAZADO` exigen verificador y fecha.
Resultado: El cumplimiento queda registrado y define la elegibilidad del estudiante.
Si no se cumple: Se rechaza el registro. Un estudiante sin los requisitos aplicables no es elegible. La base de datos no bloquea la postulación por requisitos; esa validación recae en la API. En el MVP la API aplica esta validación sobre los requisitos aplicables existentes; la definición y el registro automatizados como módulo (RF-006) son mejora futura (CR-6). No se inventan requisitos ni se insertan datos institucionales o de prueba: si no hay requisitos registrados, no se genera una validación ficticia y la carga de requisitos queda sujeta a un mecanismo de administración aprobado.

### RN-008 - Identificación única de empresas
Descripción: Cada empresa se registra una sola vez.
Condición: El RUC debe tener 13 dígitos y no estar registrado.
Resultado: La empresa queda disponible para asociar usuarios, tutores y convenios.
Si no se cumple: Se rechaza el registro.

### RN-009 - Usuarios y tutores de la empresa
Descripción: Los tutores empresariales pertenecen a una sola empresa. Los usuarios que publican ofertas se vinculan a la empresa como `ADMIN_EMPRESA` o `PUBLICADOR`.
Condición: La empresa debe existir y estar activa. El miembro debe estar activo y tener el rol `REPRESENTANTE_EMPRESA`. El primer miembro activo debe ser `ADMIN_EMPRESA` y la empresa no puede quedar sin uno. Solo coordinador, administrador o `ADMIN_EMPRESA` gestionan miembros.
Resultado: El usuario queda vinculado y puede operar dentro de su empresa.
Si no se cumple: Se rechaza la vinculación. Los miembros no se eliminan; se desactivan.

### RN-010 - Vigencia de convenios
Descripción: Las ofertas y prácticas requieren un convenio vigente de la empresa receptora.
Condición: El convenio debe pertenecer a la empresa, tener estado efectivo `VIGENTE` (no suspendido, terminado ni vencido por fecha) y fecha de fin no anterior a la de inicio.
Resultado: El convenio puede usarse en ofertas y prácticas.
Si no se cumple: Se impide su uso o registro.

### RN-011 - Creación de ofertas
Descripción: Solo la empresa autorizada crea ofertas.
Condición: La empresa debe estar activa y el creador debe ser miembro activo de ella (usuario autenticado).
Resultado: La oferta se registra.
Si no se cumple: Se impide la creación.

### RN-012 - Validaciones para enviar y aprobar una oferta
Descripción: Al enviarse a revisión y al aprobarse, la oferta se revalida y se congelan las horas requeridas y la nota mínima del tipo de práctica.
Condición: Empresa activa; convenio vigente; tipo de práctica activo; periodo activo y no finalizado; actividades dentro de la vigencia del convenio y del periodo; fecha de cierre no vencida y no posterior al inicio de actividades; al menos un perfil con plazas.
Resultado: La oferta pasa a `PENDIENTE_REVISION` o `PUBLICADA` con sus condiciones académicas congeladas.
Si no se cumple: Se rechaza la transición indicando la condición incumplida.

### RN-013 - Perfiles y plazas
Descripción: Una oferta tiene uno o más perfiles con plazas, carreras destinatarias y requisitos laborales.
Condición: Plazas mayores a 0; nombre único dentro de la oferta; carreras activas (sin carreras asignadas, el perfil queda abierto a todas); las plazas no pueden reducirse por debajo de las ya aceptadas.
Resultado: El perfil define quién puede postular.
Si no se cumple: Se rechaza el cambio. Un perfil con postulaciones no se elimina.

### RN-014 - Aprobación previa por coordinación
Descripción: La empresa propone; la coordinación aprueba o rechaza antes de publicar.
Condición: La oferta debe estar en `PENDIENTE_REVISION`. Solo un `COORDINADOR` activo la aprueba o rechaza; el rechazo exige motivo.
Resultado: `PUBLICADA` (visible para estudiantes) o `RECHAZADA` con observaciones conservadas.
Si no se cumple: Una oferta no aprobada no se muestra ni recibe postulaciones.

### RN-015 - Modificación de ofertas publicadas
Descripción: Un cambio sustancial (título, descripción, tipo, periodo, convenio, fechas, perfiles, carreras o requisitos) exige nueva revisión.
Condición: Edita la empresa autorizada (`ADMIN_EMPRESA` o el `PUBLICADOR` creador). El tipo de práctica no cambia si ya hay postulaciones.
Resultado: La oferta publicada vuelve a `PENDIENTE_REVISION`, deja de recibir postulaciones y el cambio queda registrado.
Si no se cumple: Se rechaza la edición. Las ofertas `CERRADA` o `CANCELADA` no se editan.

### RN-016 - Disponibilidad, cierre y cancelación de ofertas
Descripción: Solo las ofertas vigentes se muestran como oportunidades.
Condición: La oferta debe estar `PUBLICADA` y dentro de su fecha de cierre. Se cierra (`CERRADA`) automáticamente al vencer el plazo o al cubrirse todas las plazas, o por un coordinador. Se cancela (`CANCELADA`, con motivo) por un coordinador o el `ADMIN_EMPRESA`.
Resultado: La oferta deja de aceptar postulaciones al cerrarse o cancelarse.
Si no se cumple: Se excluye de las ofertas disponibles. Las ofertas no se eliminan.

### RN-017 - Condiciones para postularse
Descripción: El estudiante se postula a un perfil de una oferta.
Condición: Postula el propio estudiante autenticado; oferta `PUBLICADA` y dentro del plazo; convenio vigente; empresa, tipo y periodo habilitados; carrera activa del estudiante incluida en el perfil; perfil con plazas disponibles.
Resultado: La postulación se registra como `PENDIENTE`.
Si no se cumple: Se rechaza indicando la causa.

### RN-018 - Decisión de coordinación y plazas
Descripción: Solo la coordinación acepta o rechaza postulaciones. La plaza se ocupa al aceptar.
Condición: Para aceptar: coordinador activo, oferta `PUBLICADA` o `CERRADA`, convenio vigente, empresa, tipo y periodo habilitados, plaza disponible y estudiante sin otra práctica vigente en el periodo. La disponibilidad de la plaza se comprueba y se reserva dentro de la misma transacción; `postulacion` usa control optimista (`@Version`). Rechazar exige motivo.
Resultado: `ACEPTADA` (ocupa plaza y genera la práctica en la misma transacción) o `RECHAZADA`. Si otra operación cambió el registro o agotó la plaza, la respuesta es 409 `CONFLICTO_CONCURRENCIA`.
Si no se cumple: Se rechaza la decisión. Una decisión ya tomada solo se corrige por operación administrativa de coordinador de ámbito o administrador, con motivo y auditoría, sin cambiar el estado de la postulación (CR-4; RN-039, `MODELO_DATOS.md` §7.3).

### RN-019 - Retiro y cancelación de postulaciones
Descripción: El estudiante retira su postulación pendiente; el coordinador cancela una aceptada.
Condición: Retira solo el estudiante propietario. Cancela un coordinador activo, con motivo, y solo si la práctica asociada está `CANCELADA` o `RECHAZADA`.
Resultado: `RETIRADA` permite repostular; `CANCELADA` libera la plaza.
Si no se cumple: Se rechaza la operación.

### RN-020 - Formalización de la práctica
Descripción: Aceptar una postulación genera la práctica en la misma operación.
Condición: La práctica se crea en estado `APROBADA` con fecha de aprobación, creada por el coordinador autenticado. Debe coincidir con la oferta (estudiante, empresa, convenio, tipo, periodo, horas y nota mínima) y con la carrera actual del estudiante. No exige docente ni tutor: se asignan antes de iniciar. La aceptación de la postulación y la creación de la práctica se ejecutan en una única transacción; `practica` también usa `@Version`. Los errores de los triggers diferidos se traducen a 409 al confirmar la transacción.
Resultado: Quedan vinculados estudiante, empresa, convenio, tipo de práctica y periodo; la plaza queda ocupada de forma definitiva solo si se confirma la transacción.
Si no se cumple: No se confirma la aceptación; una postulación `ACEPTADA` no puede quedar sin práctica.

### RN-021 - Una práctica vigente por estudiante y periodo
Descripción: Un estudiante no puede tener dos prácticas vigentes en el mismo periodo.
Condición: Se consideran vigentes las prácticas que no estén `RECHAZADA`, `CANCELADA` ni `REPROBADA`.
Resultado: La práctica puede formalizarse.
Si no se cumple: Se impide la formalización.

### RN-022 - Conservación de condiciones académicas
Descripción: Las horas requeridas y la nota mínima se fijan desde el tipo de práctica y no cambian durante la práctica.
Condición: Ambos valores se congelan en la oferta y en la práctica; no se modifican una vez la práctica sale de `POSTULADA`.
Resultado: Progreso y resultado se calculan con las condiciones originales.
Si no se cumple: Se rechaza cualquier modificación.

> **Numeración RN-023 / RN-024:** no hay reglas con esos identificadores en este documento; la numeración pasa de RN-022 a RN-025. RN-023 se resolvió como RN-021 en el escenario de práctica vigente por estudiante y periodo (`FLUJOS.md` §6). RN-024 permanece como referencia heredada sin equivalente verificable: no se inventó una regla para completar la secuencia.

### RN-025 - Responsables asignados
Descripción: Para iniciar, la práctica requiere docente tutor, tutor empresarial y fechas de ejecución.
Condición: El tutor empresarial debe pertenecer a la empresa de la práctica. Para pasar a `EN_CURSO` deben existir docente, tutor, fecha de inicio y de fin.
Resultado: Los responsables supervisan la práctica.
Si no se cumple: Se impide asignar un tutor de otra empresa e iniciar la práctica incompleta. En el MVP solo se asignan responsables y fechas; la transición a `EN_CURSO` (API-083) es mejora futura (CR-8).

### RN-026 - Cancelación de prácticas
Descripción: Una práctica aprobada no se elimina; se cancela.
Condición: La práctica no puede borrarse si alguna vez fue aprobada. Cancelar es posible desde cualquier estado no terminal.
Resultado: La práctica queda `CANCELADA` y su postulación aceptada pasa a `CANCELADA`, liberando la plaza.
Si no se cumple: Se rechaza el borrado.

### RN-027 - Registro diario de horas
Descripción: El estudiante registra horas solo durante la ejecución.
Condición: Práctica `EN_CURSO`; fecha no futura (zona Ecuador) y dentro del periodo de ejecución; horas mayores a 0 y hasta 8 por día; un solo registro por práctica y fecha. El registro de actividades (`actividad_practica`) no tiene restricciones en la base de datos; exigir `EN_CURSO` corresponde a la API.
Resultado: El registro se almacena.
Si no se cumple: Se rechaza el registro.

### RN-028 - Validación y protección de horas
Descripción: Docente tutor o tutor empresarial validan los registros; los validados son evidencia.
Condición: El registro validado exige validador y fecha de validación. Una vez validado no se modifica ni elimina.
Resultado: Las horas validadas suman al total acumulado.
Si no se cumple: Se rechaza la modificación o el borrado.

### RN-029 - Cumplimiento del total de horas
Descripción: La práctica avanza a evaluación solo con las horas requeridas.
Condición: La suma de horas validadas debe ser igual o mayor a las horas requeridas congeladas. La base de datos no calcula esta suma; lo verifica la API.
Resultado: La práctica pasa a `HORAS_COMPLETAS`.
Si no se cumple: Permanece `EN_CURSO`.

### RN-030 - Informes de práctica
Descripción: El estudiante entrega informes `PARCIAL` o `FINAL` que el docente tutor revisa.
Condición: El archivo debe ser PDF de hasta 2 MB, con huella SHA-256. El docente registra la decisión.
Resultado: El informe queda `ENTREGADO`, `OBSERVADO` o `APROBADO`.
Si no se cumple: Se rechaza el archivo no válido; un informe con observaciones pendientes no se considera aprobado.

### RN-031 - Protección de informes aprobados
Descripción: Un informe aprobado se conserva como evidencia.
Condición: El informe está `APROBADO`. Solo puede existir un informe `FINAL` aprobado por práctica.
Resultado: El documento y su archivo permanecen intactos.
Si no se cumple: Se rechaza su modificación o eliminación.

### RN-032 - Visitas e incidencias
Descripción: Las visitas del docente y las incidencias se asocian a una práctica existente.
Condición: Las visitas las registra el docente. La incidencia nace `ABIERTA` y se resuelve indicando responsable y fecha.
Resultado: Quedan disponibles para consulta y gestión.
Si no se cumple: No se registran sobre prácticas inexistentes ni se resuelven incidencias no registradas.

### RN-033 - Habilitación de la evaluación
Descripción: La evaluación final requiere ejecución completa.
Condición: Práctica `HORAS_COMPLETAS` con horas validadas y docente y tutor empresarial asignados. La base de datos solo admite evaluaciones en `EN_CURSO` o `HORAS_COMPLETAS`.
Resultado: Se habilita el registro de evaluaciones.
Si no se cumple: Se impide evaluar.

### RN-034 - Evaluación por ambos responsables
Descripción: El resultado considera la evaluación del docente tutor y del tutor empresarial.
Condición: Una evaluación por práctica y rol evaluador (`DOCENTE`, `TUTOR_EMPRESARIAL`), con nota entre 0 y 20.
Resultado: Las evaluaciones quedan asociadas a la práctica.
Si no se cumple: No se determina el resultado si falta una evaluación o la nota está fuera de rango.

### RN-035 - Resultado final
Descripción: El resultado compara la nota final con la nota mínima congelada.
Condición: Existen horas validadas y ambas evaluaciones; la práctica en `EVALUADA` tiene `nota_final`.
Resultado: `FINALIZADA` si la nota final es mayor o igual a la mínima; `REPROBADA` si es menor.
Si no se cumple: No se determina el resultado.

### RN-036 - Emisión de certificados
Descripción: Solo las prácticas finalizadas satisfactoriamente generan certificado.
Condición: La práctica debe estar `FINALIZADA` y no tener otro certificado `VIGENTE`.
Resultado: Se emite el certificado con datos congelados y un código UUID único.
Si no se cumple: Se impide la emisión.

### RN-037 - Verificación de certificados
Descripción: Cada certificado se verifica por su código.
Condición: El código debe existir en el sistema.
Resultado: Se informa la autenticidad y el estado (`VIGENTE` o `ANULADO`).
Si no se cumple: Se informa que no puede verificarse.

### RN-038 - Anulación de certificados
Descripción: Un coordinador autorizado puede anular un certificado.
Condición: Se registran motivo, fecha y responsable. El certificado no se elimina ni se modifican sus datos congelados.
Resultado: El certificado queda `ANULADO`, de forma definitiva.
Si no se cumple: Se rechaza la anulación o cualquier cambio posterior.

### RN-039 - Registro de operaciones relevantes
Descripción: Las operaciones relevantes dejan evidencia inmutable e identifican al responsable.
Condición: Se registran en auditoría o historial las transiciones de postulaciones, cambios de estado de ofertas y prácticas, cambios de cabecera de ofertas, cambios de membresía empresarial y correcciones administrativas. Toda operación auditable exige usuario autenticado, salvo procesos del sistema (cierre automático de ofertas).
Resultado: El evento queda disponible para actores autorizados.
Si no se cumple: La operación no se completa. Los historiales y la auditoría no se modifican.

### RN-040 - Consistencia de reportes
Descripción: Los reportes se generan solo a partir de registros existentes; `AUTORIDAD` tiene acceso de solo lectura.
Condición: Los datos deben conservar sus relaciones con estudiantes, empresas, prácticas y periodos.
Resultado: Los indicadores reflejan la información institucional.
Si no se cumple: No se presenta información inexistente o inconsistente.

**Alcance de la trazabilidad en el MVP:** la auditoría básica y los historiales necesarios para las operaciones del MVP (cambios de oferta, postulaciones, prácticas, membresías y correcciones) sí forman parte del sistema, conforme a RN-039. Los reportes consolidados, los indicadores y la trazabilidad avanzada quedan fuera del MVP.

**Catálogos sin datos:** `tipo_practica`, `requisito` y el resto de catálogos no traen registros iniciales. La API no inserta datos institucionales ni de prueba; gestiona listados vacíos y aplica las validaciones solo sobre los registros que existan. La administración de requisitos permanece fuera del MVP (CR-6).

**Concurrencia básica del MVP:** se implementa control optimista con `@Version` en `postulacion` y `practica`, verificación transaccional de plazas al aceptar postulaciones y respuesta 409 `CONFLICTO_CONCURRENCIA` cuando la operación no pueda completarse por un conflicto. No se incorporan bloqueos distribuidos, colas ni mecanismos externos.


## 4. Estados y transiciones

### 4.1. Cuenta de usuario

Representación: `usuario.activo`, `usuario.bloqueado_hasta`, `usuario.intentos_fallidos` (no hay columna de estado).
Estado inicial: Activa.
Estados posibles: Activa, Bloqueada, Desactivada.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| Activa | Intentos fallidos superan el límite | Bloqueada | Límite configurable, sin valor fijo en el código (CR-3). |
| Bloqueada | Vence `bloqueado_hasta` | Activa | Se cumple el tiempo de bloqueo. |
| Activa o Bloqueada | Desactivar cuenta | Desactivada | Operación del administrador. |
| Desactivada | Reactivar cuenta | Activa | Operación del administrador. |

### 4.2. Periodo académico

Representación: `periodo_academico.activo` (índice único parcial).
Estado inicial: No activo.
Estados posibles: No activo, Activo.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| No activo | Activar | Activo | No existe otro periodo activo. |
| Activo | Desactivar | No activo | Operación autorizada. |

### 4.3. Convenio

Representación: `convenio.estado` (`VIGENTE`, `SUSPENDIDO`, `TERMINADO`). `VENCIDO` no se almacena: lo deriva la vista `v_convenio_estado`.
Estado inicial: `VIGENTE`.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| `VIGENTE` | Pasa `fecha_fin` | `VENCIDO` (derivado) | Fecha de fin anterior a hoy (zona Ecuador). |
| `VIGENTE` | Suspender | `SUSPENDIDO` | Operación autorizada. |
| `VIGENTE` o `SUSPENDIDO` | Terminar | `TERMINADO` | Operación autorizada. |

La base de datos no restringe estas transiciones manuales. Solo el estado efectivo `VIGENTE` habilita el uso del convenio.

### 4.4. Cumplimiento de requisito del estudiante

Representación: `requisito_estudiante.estado`.
Estado inicial: `PENDIENTE`.
Estados posibles: `PENDIENTE`, `CUMPLIDO`, `RECHAZADO`.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| `PENDIENTE` | Verificar como cumplido | `CUMPLIDO` | Se registran verificador y fecha. |
| `PENDIENTE` | Verificar como incumplido | `RECHAZADO` | Se registran verificador y fecha. |

### 4.5. Oferta de prácticas

Representación: `oferta_practica.estado`.
Estado inicial: `BORRADOR`.
Estados posibles: `BORRADOR`, `PENDIENTE_REVISION`, `PUBLICADA`, `RECHAZADA`, `CERRADA`, `CANCELADA`.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| `BORRADOR` | Enviar a revisión | `PENDIENTE_REVISION` | Empresa autorizada; cumple RN-012. |
| `PENDIENTE_REVISION` | Aprobar | `PUBLICADA` | Coordinador activo; cumple RN-012. |
| `PENDIENTE_REVISION` | Rechazar | `RECHAZADA` | Coordinador activo; con motivo. |
| `PENDIENTE_REVISION` | Retirar | `BORRADOR` | Empresa autorizada. |
| `RECHAZADA` | Corregir | `BORRADOR` | Empresa autorizada. |
| `RECHAZADA` | Reenviar | `PENDIENTE_REVISION` | Empresa autorizada; cumple RN-012. |
| `PUBLICADA` | Modificación sustancial | `PENDIENTE_REVISION` | Empresa autorizada. |
| `PUBLICADA` | Plazo vencido o plazas cubiertas | `CERRADA` | Proceso del sistema o coordinador. |
| `BORRADOR`, `PENDIENTE_REVISION`, `RECHAZADA` o `PUBLICADA` | Cancelar | `CANCELADA` | Coordinador o `ADMIN_EMPRESA`; con motivo. |

`CERRADA` y `CANCELADA` son estados finales.

### 4.6. Postulación

Representación: `postulacion.estado` y `postulacion.recomendacion_empresa`.
Estado inicial: `PENDIENTE` y `SIN_EVALUAR`.
Estados posibles: `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `RETIRADA`, `CANCELADA`.
Recomendación empresarial: `SIN_EVALUAR`, `RECOMENDADO`, `NO_RECOMENDADO`.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| `PENDIENTE` | Aceptar | `ACEPTADA` | Coordinador activo; cumple RN-020; se genera la práctica. |
| `PENDIENTE` | Rechazar | `RECHAZADA` | Coordinador activo; con motivo. |
| `PENDIENTE` | Retirar | `RETIRADA` | Estudiante propietario. |
| `ACEPTADA` | Cancelar | `CANCELADA` | Coordinador, con motivo; práctica `CANCELADA` o `RECHAZADA`. |
| `SIN_EVALUAR` | Recomendar o no recomendar | `RECOMENDADO` o `NO_RECOMENDADO` | Miembro activo de la empresa; postulación `PENDIENTE`. |
| `RECOMENDADO` o `NO_RECOMENDADO` | Cambiar recomendación | `NO_RECOMENDADO` o `RECOMENDADO` | Igual que la anterior; no vuelve a `SIN_EVALUAR`. |

`RECHAZADA`, `RETIRADA` y `CANCELADA` son estados finales.

### 4.7. Práctica

Representación: `practica.estado` (historial en `practica_estado_historial`).
Estado inicial: `APROBADA` si deriva de una postulación; `POSTULADA` si se crea directamente, sin convocatoria.
Estados posibles: `POSTULADA`, `APROBADA`, `EN_CURSO`, `HORAS_COMPLETAS`, `EVALUADA`, `FINALIZADA`, `REPROBADA`, `RECHAZADA`, `CANCELADA`.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| `POSTULADA` | Aprobar | `APROBADA` | Se registra fecha de aprobación. |
| `POSTULADA` | Rechazar | `RECHAZADA` | Operación autorizada. |
| `APROBADA` | Iniciar ejecución | `EN_CURSO` | Docente, tutor empresarial y fechas asignados. |
| `EN_CURSO` | Completar horas | `HORAS_COMPLETAS` | Horas validadas alcanzan las requeridas (RN-029). |
| `HORAS_COMPLETAS` | Registrar evaluaciones | `EVALUADA` | Existe `nota_final`. |
| `EVALUADA` | Resultado satisfactorio | `FINALIZADA` | `nota_final` ≥ `nota_minima_snapshot`. |
| `EVALUADA` | Resultado no satisfactorio | `REPROBADA` | `nota_final` < `nota_minima_snapshot`. |
| Cualquier estado no terminal | Cancelar | `CANCELADA` | Operación autorizada. |

`FINALIZADA`, `REPROBADA`, `RECHAZADA` y `CANCELADA` son estados finales. En el MVP la práctica queda en `APROBADA` con responsables y fechas asignados; el paso a `EN_CURSO` y los estados posteriores son mejoras futuras (CR-8).

### 4.8. Registro de horas

Representación: `registro_horas.validado` (booleano).
Estado inicial: No validado.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| No validado | Validar | Validado | Práctica `EN_CURSO`; validador y fecha registrados. |

El estado Validado es final: no se modifica ni elimina.

### 4.9. Informe

Representación: `informe.estado`.
Estado inicial: `ENTREGADO`.
Estados posibles: `ENTREGADO`, `OBSERVADO`, `APROBADO`.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| `ENTREGADO` | Revisar con observaciones | `OBSERVADO` | Docente tutor. |
| `ENTREGADO` | Revisar satisfactoriamente | `APROBADO` | Docente tutor. |
| `OBSERVADO` | Nueva entrega | `ENTREGADO` | Estudiante presenta versión corregida. |

`APROBADO` es final y está protegido por la base de datos. Las demás transiciones las controla la API.

### 4.10. Incidencia

Representación: `incidencia.estado`.
Estado inicial: `ABIERTA`.
Estados posibles: `ABIERTA`, `RESUELTA`.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| `ABIERTA` | Resolver | `RESUELTA` | Se registran responsable y fecha de resolución. |

### 4.11. Certificado

Representación: `certificado.estado`.
Estado inicial: `VIGENTE`.
Estados posibles: `VIGENTE`, `ANULADO`.

| Estado inicial | Evento | Estado final | Condición |
|---|---|---|---|
| (sin certificado) | Emitir | `VIGENTE` | Práctica `FINALIZADA`; sin otro certificado vigente. |
| `VIGENTE` | Anular | `ANULADO` | Coordinador autorizado; motivo, fecha y responsable. |

`ANULADO` es final.

## 5. Restricciones del dominio

| ID | Restricción | Alcance | Aplicación |
|---|---|---|---|
| RD-001 | No hay cuentas con correo o documento duplicado. | MVP | BD |
| RD-002 | No se activa un segundo periodo mientras exista otro activo. | MVP | BD |
| RD-003 | No hay perfiles de estudiante o docente duplicados por usuario. | MVP | BD |
| RD-004 | No se usan convenios vencidos, suspendidos, terminados o de otra empresa. | MVP | BD |
| RD-005 | Una oferta no se publica sin aprobación de coordinación. | MVP | BD |
| RD-006 | No hay postulaciones duplicadas del estudiante al mismo perfil (salvo `RETIRADA`). | MVP | BD |
| RD-007 | No se postula a ofertas no publicadas, vencidas o con el perfil sin plazas. | MVP | BD |
| RD-008 | No se acepta una postulación sin plaza disponible. | MVP | BD |
| RD-009 | Un estudiante no tiene dos prácticas vigentes en el mismo periodo. | MVP | BD |
| RD-010 | El tutor empresarial pertenece a la empresa de la práctica. | MVP | BD |
| RD-011 | Una práctica aprobada no se elimina; se cancela. | MVP | BD |
| RD-012 | Las horas, la nota mínima y los datos clave de la práctica se congelan al salir de `POSTULADA`. | MVP | BD |
| RD-013 | No se registran más de 8 horas diarias ni registros duplicados por fecha. | Posterior | BD |
| RD-014 | No se modifican ni eliminan horas validadas. | Posterior | BD |
| RD-015 | No se modifican ni eliminan informes aprobados; solo hay un informe final aprobado por práctica. | Posterior | BD |
| RD-016 | Hay una sola evaluación por práctica y rol evaluador, con nota entre 0 y 20. | Posterior | BD |
| RD-017 | Solo las prácticas `FINALIZADA` generan certificado, y solo uno `VIGENTE` por práctica. | Posterior | BD |
| RD-018 | Los certificados no se eliminan; `ANULADO` es definitivo. | Posterior | BD |
| RD-019 | Historiales y auditoría son inmutables; las ofertas no se eliminan. | MVP | BD |
| RD-020 | El paso a `HORAS_COMPLETAS` exige las horas validadas requeridas. | Posterior | API |
| RD-021 | Una postulación requiere los requisitos académicos aplicables. | MVP (validación en API; módulo de definición futuro) | API |

