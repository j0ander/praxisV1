# Requisitos

## Introducción

Este documento especifica los requisitos funcionales y no funcionales de **PRAXIS**, plataforma que gestiona los procesos académicos y administrativos de las prácticas preprofesionales. Establece las capacidades del sistema, sus condiciones de calidad y las restricciones de desarrollo. Cada requisito tiene un identificador único para facilitar su seguimiento, implementación y verificación.

## Requisitos funcionales

### RF-001 - Administración de usuarios

Descripción:
El sistema debe permitir registrar, consultar, modificar, activar y desactivar usuarios con sus datos personales, institucionales y de contacto.
Actor:
Administrador, coordinador.
Resultado esperado:
Usuario registrado o actualizado, con el estado de cuenta reflejado.
Prioridad:
Alta.

### RF-002 - Autenticación y asignación de roles

Descripción:
El sistema debe autenticar usuarios por credenciales y asignarles roles, controlando el acceso según el rol y el ámbito institucional.
Actor:
Usuario del sistema, administrador.
Resultado esperado:
Acceso solo a las funcionalidades correspondientes y roles registrados.
Prioridad:
Alta.

### RF-003 - Seguridad de las cuentas

Descripción:
El sistema debe controlar intentos fallidos de ingreso, bloquear temporalmente la cuenta al superar el límite y permitir recuperar o actualizar contraseñas.
Actor:
Usuario del sistema, administrador.
Resultado esperado:
Intentos controlados, bloqueo al superar el límite y credenciales recuperables.
Prioridad:
Alta.

### RF-004 - Administración de la estructura académica

Descripción:
El sistema debe permitir administrar facultades, carreras y periodos académicos, sus relaciones y la identificación del periodo vigente.
Actor:
Administrador, coordinador.
Resultado esperado:
Facultades, carreras y periodos registrados y relacionados; periodo vigente identificado.
Prioridad:
Alta.

### RF-005 - Administración de perfiles académicos

Descripción:
El sistema debe permitir registrar y administrar perfiles académicos de estudiantes y docentes, vinculados a su cuenta y carrera.
Actor:
Administrador, coordinador.
Resultado esperado:
Perfil registrado y relacionado con su cuenta y carrera.
Prioridad:
Alta.

### RF-006 - Gestión de requisitos académicos

Descripción:
El sistema debe permitir definir requisitos académicos institucionales o por carrera y registrar su cumplimiento por estudiante y periodo.
Actor:
Coordinador, administrador.
Resultado esperado:
Requisitos definidos y cumplimiento consultable.
Prioridad:
Media.
Alcance MVP:
La definición y administración de requisitos queda fuera del MVP (CR-6). En el MVP solo se valida la postulación sobre los requisitos que ya existan; no se insertan requisitos ni datos institucionales ficticios.

### RF-007 - Administración de empresas y participantes empresariales

Descripción:
El sistema debe permitir registrar y administrar empresas, sus representantes y tutores empresariales.
Actor:
Administrador, coordinador, representante empresarial.
Resultado esperado:
Empresa registrada con representantes y tutores asociados.
Prioridad:
Alta.

### RF-008 - Gestión de convenios institucionales

Descripción:
El sistema debe permitir gestionar convenios con empresas, registrando documentación, vigencia y estado, y verificando su validez.
Actor:
Administrador, coordinador.
Resultado esperado:
Convenio registrado y asociado a la empresa, permitiendo determinar si respalda una práctica.
Prioridad:
Alta.

### RF-009 - Registro y administración de ofertas

Descripción:
El sistema debe permitir que las empresas registren y administren ofertas con sus condiciones, periodo, tipo de práctica y convenio.
Actor:
Representante empresarial.
Resultado esperado:
Oferta registrada y disponible para revisión institucional.
Prioridad:
Alta.

### RF-010 - Perfiles y plazas de las ofertas

Descripción:
El sistema debe permitir definir uno o varios perfiles por oferta con carreras habilitadas, plazas y requisitos de los postulantes.
Actor:
Representante empresarial.
Resultado esperado:
Perfiles registrados con carreras, plazas y requisitos.
Prioridad:
Alta.

### RF-011 - Revisión y aprobación de ofertas

Descripción:
El sistema debe permitir que la coordinación revise, apruebe o rechace ofertas antes de publicarlas, registrando observaciones.
Actor:
Coordinador.
Resultado esperado:
Oferta aprobada o rechazada, con decisión y observaciones registradas.
Prioridad:
Alta.

### RF-012 - Consulta y búsqueda de ofertas

Descripción:
El sistema debe permitir que los estudiantes consulten y busquen ofertas según su carrera y condiciones de participación.
Actor:
Estudiante.
Resultado esperado:
El estudiante visualiza las ofertas disponibles para su perfil y sus condiciones.
Prioridad:
Alta.

### RF-013 - Registro de postulaciones

Descripción:
El sistema debe permitir que los estudiantes se postulen a ofertas disponibles, verificando requisitos académicos y del perfil.
Actor:
Estudiante.
Resultado esperado:
Postulación registrada, vinculada al estudiante y la oferta, y disponible para revisión.
Prioridad:
Alta.

### RF-014 - Revisión empresarial de postulaciones

Descripción:
El sistema debe permitir que las empresas revisen las postulaciones recibidas y registren recomendaciones y observaciones.
Actor:
Representante empresarial, tutor empresarial.
Resultado esperado:
Postulaciones consultables, con recomendaciones y observaciones registradas.
Prioridad:
Media.

### RF-015 - Resolución de postulaciones

Descripción:
El sistema debe permitir que la coordinación apruebe o rechace las postulaciones.
Actor:
Coordinador.
Resultado esperado:
Decisión registrada; las postulaciones aprobadas quedan habilitadas para formalizar una práctica.
Prioridad:
Alta.

### RF-016 - Formalización de prácticas

Descripción:
El sistema debe permitir formalizar las prácticas aprobadas, registrando estudiante, empresa, convenio, tipo, periodo, responsables y condiciones académicas.
Actor:
Coordinador.
Resultado esperado:
Práctica registrada con sus responsables y condiciones, lista para iniciar.
Prioridad:
Alta.

### RF-017 - Gestión del ciclo de vida de las prácticas

Descripción:
El sistema debe gestionar los estados de las prácticas, controlar las condiciones de cada transición y conservar el historial.
Actor:
Coordinador, docente tutor.
Resultado esperado:
Estado actual consultable y cambios registrados.
Prioridad:
Media.

### RF-018 - Registro de actividades y horas

Descripción:
El sistema debe permitir que los estudiantes registren actividades y horas, controlando las condiciones establecidas.
Actor:
Estudiante.
Resultado esperado:
Actividades y horas registradas y progreso actualizado.
Prioridad:
Media.

### RF-019 - Revisión y validación de actividades y horas

Descripción:
El sistema debe permitir que docentes y tutores empresariales validen u observen los registros de horas y consulten el progreso.
Actor:
Docente tutor, tutor empresarial.
Resultado esperado:
Registros validados u observados y horas validadas actualizadas.
Prioridad:
Media.

### RF-020 - Gestión de informes de prácticas

Descripción:
El sistema debe permitir cargar, revisar, observar y aprobar informes parciales y finales en formato PDF.
Actor:
Estudiante, docente tutor.
Resultado esperado:
Informes almacenados con su estado (entregado, observado, aprobado) y observaciones consultables.
Prioridad:
Media.

### RF-021 - Registro de visitas de seguimiento

Descripción:
El sistema debe permitir registrar las visitas de seguimiento realizadas durante las prácticas.
Actor:
Docente tutor, tutor empresarial, coordinador.
Resultado esperado:
Visita registrada, asociada a la práctica y consultable por los responsables.
Prioridad:
Media.

### RF-022 - Gestión de incidencias

Descripción:
El sistema debe permitir registrar incidencias de las prácticas, gestionar su atención y resolución, y conservar el historial.
Actor:
Docente tutor, tutor empresarial, coordinador.
Resultado esperado:
Incidencia registrada, con estado y acciones consultables.
Prioridad:
Media.

### RF-023 - Evaluación del desempeño

Descripción:
El sistema debe permitir que docentes y tutores empresariales evalúen al estudiante con criterios y calificaciones establecidos.
Actor:
Docente tutor, tutor empresarial.
Resultado esperado:
Evaluaciones registradas con sus criterios y calificaciones.
Prioridad:
Media.

### RF-024 - Resultado final de la práctica

Descripción:
El sistema debe determinar la aprobación o reprobación de la práctica según las horas requeridas y las evaluaciones.
Actor:
Sistema, coordinador.
Resultado esperado:
Nota final calculada y práctica marcada como finalizada o reprobada.
Prioridad:
Media.

### RF-025 - Emisión y gestión de certificados

Descripción:
El sistema debe permitir emitir, consultar, descargar, verificar y anular certificados de finalización.
Actor:
Coordinador, estudiante, autoridad, usuario externo autorizado para verificar.
Resultado esperado:
Certificado generado con código único, descargable y verificable; los anulados conservan su historial.
Prioridad:
Media.

### RF-026 - Reportes e indicadores institucionales

Descripción:
El sistema debe generar reportes e indicadores consolidados sobre la gestión de las prácticas.
Actor:
Coordinador, administrador, autoridad.
Resultado esperado:
Reportes consultables por los usuarios autorizados.
Prioridad:
Media.

### RF-027 - Registro de auditoría y trazabilidad

Descripción:
El sistema debe registrar las operaciones relevantes para identificar qué acción se hizo, quién la hizo y cuándo.
Actor:
Sistema, administrador, coordinador.
Resultado esperado:
Eventos registrados con operación, usuario y momento, consultables por usuarios autorizados.
Prioridad:
Alta.

## 3. Requisitos no funcionales

### RNF-001 - Protección de credenciales

Categoría:
Seguridad.
Descripción:
Las contraseñas no deben almacenarse ni mostrarse en texto plano en ningún componente del sistema.
Verificación:
No son recuperables en su forma original; no aparecen en respuestas ni en registros de auditoría.
Prioridad:
Alta.

### RNF-002 - Control de acceso

Categoría:
Seguridad.
Descripción:
El acceso a la información y a las operaciones debe depender estrictamente del rol y el ámbito institucional del usuario.
Verificación:
Solo se ejecutan operaciones autorizadas; un usuario limitado a una carrera no accede a otras; los accesos no autorizados se rechazan.
Prioridad:
Alta.

### RNF-003 - Protección de la información

Categoría:
Seguridad e integridad.
Descripción:
Los datos personales, académicos, empresariales y administrativos deben protegerse frente a accesos o modificaciones no autorizadas.
Verificación:
Cada consulta o modificación verifica permisos; los datos sensibles no se muestran sin autorización.
Prioridad:
Alta.

### RNF-004 - Integridad y consistencia

Categoría:
Integridad.
Descripción:
El sistema debe mantener la coherencia referencial entre usuarios, perfiles, empresas, convenios, ofertas, postulaciones y prácticas.
Verificación:
No se aceptan referencias inexistentes; las operaciones rechazadas no dejan datos parciales.
Prioridad:
Alta.

### RNF-005 - Trazabilidad de operaciones

Categoría:
Auditoría.
Descripción:
El sistema debe conservar registros inalterables que permitan reconstruir las acciones ejecutadas en los distintos procesos.
Verificación:
Los eventos auditables incluyen usuario, operación y momento; los usuarios ordinarios no pueden modificarlos.
Prioridad:
Alta.

### RNF-006 - Usabilidad

Categoría:
Usabilidad.
Descripción:
Las funcionalidades deben presentarse de forma comprensible, intuitiva y utilizando la terminología propia del dominio académico.
Verificación:
Opciones mostradas según el rol; campos requeridos identificados; mensajes de validación claros; estados comprensibles.
Prioridad:
Alta.

### RNF-007 - Compatibilidad de acceso

Categoría:
Compatibilidad.
Descripción:
El sistema debe permitir el acceso desde navegadores web modernos, sin requerir la instalación de aplicaciones de escritorio.
Verificación:
Las funciones principales operan correctamente en los navegadores web soportados por el proyecto.
Prioridad:
Alta.

### RNF-008 - Mantenibilidad

Categoría:
Mantenibilidad.
Descripción:
La arquitectura del sistema debe permitir añadir nuevas funciones y corregir errores sin afectar procesos no relacionados.
Verificación:
Los cambios no alteran módulos ajenos; las funciones son verificables mediante pruebas.
Prioridad:
Media.

### RNF-009 - Disponibilidad de la información

Categoría:
Disponibilidad.
Descripción:
Los usuarios autorizados deben poder consultar la información registrada mientras el servicio esté operativo, sin pérdidas por operaciones fallidas.
Verificación:
La información registrada permanece disponible; los errores no exponen datos internos.
Prioridad:
Alta.

### RNF-010 - Gestión de archivos

Categoría:
Compatibilidad e integridad.
Descripción:
El sistema debe validar estrictamente el formato de los archivos cargados, en especial los informes de prácticas.
Verificación:
Solo se aceptan archivos PDF válidos; los demás formatos se rechazan; los aceptados se asocian correctamente a la práctica.
Prioridad:
Media.

## Restricciones

A continuación se documentan las limitaciones técnicas, funcionales y de negocio que condicionan el diseño, desarrollo y despliegue de PRAXIS:

### Limitaciones técnicas y de arquitectura

Entorno de ejecución: El sistema debe operar íntegramente como una aplicación web accesible desde navegadores modernos, descartando clientes de escritorio o aplicaciones móviles nativas en esta fase.
Gestión documental: El intercambio y almacenamiento de informes académicos está restringido exclusivamente al formato estándar PDF para garantizar la integridad y visualización uniforme de los documentos.
Independencia de datos previos: El sistema se diseña como una solución centralizada desde cero (*greenfield*) para unificar información dispersa; no depende de la migración obligatoria de una base de datos heredada previa, aunque su modelo relacional debe asegurar la integridad referencial estricta entre entidades.

### Restricciones funcionales y de dominio

Ámbito del sistema: PRAXIS se circunscribe estrictamente a la gestión administrativa y académica de prácticas preprofesionales, sin abarcar bolsas de empleo externas o procesos de titulación ajenos a la práctica.
Control de acceso estricto: Ningún usuario puede operar fuera de los límites de su rol y unidad académica (carrera o facultad) asignada.
Flujo secuencial obligatorio: No se permite omitir etapas del ciclo de vida; toda oferta requiere aprobación institucional antes de ser pública, toda postulación debe cumplir requisitos previos y toda formalización exige una postulación aprobada, un convenio vigente y responsables asignados.

### Restricciones institucionales y parámetros pendientes

Sujeción normativa: Las reglas de negocio (vigencia de convenios, elegibilidad académica y estructura organizacional) están subordinadas a los reglamentos vigentes de la institución de educación superior.
Parámetros pendientes de aprobación: El desarrollo no debe fijar valores rígidos para reglas que aún requieren validación oficial por parte de las autoridades institucionales. Estos deben gestionarse como parámetros configurables:
  * Horas requeridas por tipo de práctica y límites de registro diario.
  * Nota mínima de aprobación, criterios y ponderaciones de evaluación.
  * Requisitos académicos específicos por carrera y periodo.
  * Documentación obligatoria para la formalización de prácticas.
  * Plazos de vigencia de convenios y tiempos límite para revisión de ofertas/postulaciones.
  * Políticas y flujos específicos para la recuperación de contraseñas.

### Limitaciones del alcance (MVP)

Alcance incluido en la primera versión: El desarrollo inicial (MVP) se limita al flujo previo al inicio efectivo de la práctica: gestión de usuarios y accesos, estructura académica, perfiles de estudiantes, empresas y convenios, ciclo de ofertas (registro, revisión y consulta), ciclo de postulaciones (registro y resolución), formalización inicial con asignación de docente tutor y registro básico de auditoría.
Funcionalidades excluidas del MVP: Quedan fuera de esta primera entrega todos los procesos de ejecución, seguimiento y cierre: registro y validación de actividades/horas, entrega de informes, visitas de seguimiento, gestión de incidencias, evaluaciones de desempeño, emisión de certificados y reportes analíticos avanzados.

## Trazabilidad

Cada requisito se vincula con una necesidad u objetivo del producto, el módulo o proceso donde se aplica y el criterio que permite verificarlo.

RF-001: Necesidad: gestionar usuarios y accesos. Módulo: administración de usuarios. Verificación: usuario registrado o actualizado con estado reflejado y validaciones aplicadas.
RF-002: Necesidad: controlar el acceso por rol y ámbito. Módulo: autenticación y seguridad. Verificación: acceso solo a funcionalidades permitidas por rol y ámbito.
RF-003: Necesidad: proteger las cuentas ante intentos fallidos. Módulo: seguridad de cuentas. Verificación: bloqueo tras el límite configurable y recuperación de contraseña funcional.
RF-004: Necesidad: organizar la estructura académica. Módulo: estructura académica. Verificación: facultades, carreras y periodos relacionados, con un único periodo vigente identificado.
RF-005: Necesidad: vincular usuarios con perfiles académicos. Módulo: perfiles académicos. Verificación: perfil asociado a una cuenta y carrera existentes.
RF-006: Necesidad: definir y verificar requisitos académicos. Módulo: requisitos académicos. Verificación: requisitos definidos y cumplimiento consultable.
RF-007: Necesidad: gestionar empresas y participantes. Módulo: empresas. Verificación: empresa registrada con representantes y tutores asociados.
RF-008: Necesidad: respaldar prácticas con convenios válidos. Módulo: convenios. Verificación: convenio vigente asociado a empresa y validez determinable.
RF-009: Necesidad: publicar ofertas de práctica. Módulo: ofertas. Verificación: oferta registrada y pendiente de revisión institucional.
RF-010: Necesidad: definir perfiles y plazas de las ofertas. Módulo: perfiles de oferta. Verificación: perfiles con carreras, plazas y requisitos válidos.
RF-011: Necesidad: aprobar ofertas antes de publicarlas. Módulo: revisión de ofertas. Verificación: oferta aprobada o rechazada con observaciones registradas.
RF-012: Necesidad: consultar ofertas compatibles. Módulo: búsqueda de ofertas. Verificación: solo se muestran ofertas aprobadas compatibles con el perfil del estudiante.
RF-013: Necesidad: postular a ofertas cumpliendo requisitos. Módulo: postulaciones. Verificación: postulación registrada solo si se cumplen requisitos y hay plazas disponibles.
RF-014: Necesidad: revisar postulaciones desde la empresa. Módulo: revisión empresarial. Verificación: recomendaciones y observaciones registradas.
RF-015: Necesidad: resolver postulaciones. Módulo: resolución de postulaciones. Verificación: decisión registrada y postulaciones aprobadas habilitadas para formalizar.
RF-016: Necesidad: formalizar la práctica. Módulo: formalización. Verificación: práctica registrada con responsables y condiciones académicas.
RF-017: Necesidad: controlar el ciclo de vida de la práctica. Módulo: ciclo de vida. Verificación: transiciones validadas e historial conservado.
RF-018: Necesidad: registrar actividades y horas. Módulo: ejecución de práctica. Verificación: registros dentro de los límites configurados y progreso actualizado.
RF-019: Necesidad: validar horas registradas. Módulo: validación de horas. Verificación: registros validados u observados por los responsables asignados.
RF-020: Necesidad: gestionar informes en PDF. Módulo: informes. Verificación: solo se aceptan PDF válidos; estados y observaciones registrados.
RF-021: Necesidad: registrar visitas de seguimiento. Módulo: seguimiento. Verificación: visita asociada a la práctica y consultable por los responsables.
RF-022: Necesidad: gestionar incidencias. Módulo: incidencias. Verificación: incidencia con estado e historial consultable.
RF-023: Necesidad: evaluar el desempeño. Módulo: evaluación. Verificación: evaluaciones con criterios y calificaciones registradas.
RF-024: Necesidad: determinar el resultado final. Módulo: cierre de práctica. Verificación: nota calculada y práctica finalizada o reprobada.
RF-025: Necesidad: emitir certificados verificables. Módulo: certificados. Verificación: certificado con código único, descargable y verificable.
RF-026: Necesidad: generar reportes institucionales. Módulo: reportes. Verificación: reportes consultables solo por usuarios autorizados.
RF-027: Necesidad: auditar operaciones. Módulo: auditoría. Verificación: eventos con usuario, operación y momento, inalterables y consultables por autorizados.
RNF-001: Necesidad: proteger credenciales. Categoría: seguridad. Verificación: contraseñas no recuperables ni expuestas.
RNF-002: Necesidad: controlar acceso por rol y ámbito. Categoría: seguridad. Verificación: toda operación verifica rol y ámbito.
RNF-003: Necesidad: proteger información sensible. Categoría: seguridad e integridad. Verificación: permisos verificados en cada consulta o modificación.
RNF-004: Necesidad: mantener integridad referencial. Categoría: integridad. Verificación: sin referencias inexistentes ni datos parciales.
RNF-005: Necesidad: conservar trazabilidad. Categoría: auditoría. Verificación: registros inalterables con usuario, operación y momento.
RNF-006: Necesidad: garantizar usabilidad. Categoría: usabilidad. Verificación: opciones por rol, campos identificados y mensajes claros.
RNF-007: Necesidad: permitir acceso web. Categoría: compatibilidad. Verificación: funciones principales operan en navegadores soportados.
RNF-008: Necesidad: facilitar mantenimiento. Categoría: mantenibilidad. Verificación: cambios no afectan módulos no relacionados.
RNF-009: Necesidad: mantener información disponible. Categoría: disponibilidad. Verificación: información disponible y errores sin exposición de datos.
RNF-010: Necesidad: validar archivos cargados. Categoría: compatibilidad e integridad. Verificación: solo PDF válidos asociados correctamente a la práctica.