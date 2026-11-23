# Producto

## 1. Descripción general

PRAXIS es un sistema que centraliza los procesos académicos y administrativos de las prácticas preprofesionales de una institución de educación superior. Conecta a estudiantes, empresas, tutores, docentes y coordinadores, y cubre el ciclo completo: registro de ofertas, postulación, selección, asignación de responsables, seguimiento y cierre.

## 2. Problema que resuelve

La información de las prácticas (estudiantes, empresas, convenios, ofertas, postulaciones, tutores, evaluaciones) suele estar dispersa. Esto dificulta consultar el estado de las postulaciones, controlar las ofertas, asignar responsables, supervisar el avance y mantener un historial de decisiones. PRAXIS unifica esta información y ordena el proceso en etapas relacionadas.

## 3. Objetivos

**Objetivo general:** Gestionar de forma centralizada las prácticas preprofesionales, desde la publicación de ofertas hasta la evaluación y finalización.

**Objetivos específicos:**
- Centralizar la información de usuarios, estudiantes, docentes, empresas y tutores.
- Administrar facultades, carreras y periodos académicos.
- Registrar empresas y convenios.
- Gestionar y revisar las ofertas de prácticas.
- Permitir a los estudiantes consultar ofertas y postularse.
- Facilitar la selección de postulantes y la asignación de tutores.
- Registrar actividades y horas, y evaluar el desempeño.
- Mantener un historial de operaciones y decisiones (trazabilidad).
- Ofrecer información organizada para la toma de decisiones.

## 4. Usuarios y roles

| Rol | Propósito y responsabilidad general |
| :--- | :--- |
| **Administrador** | Gestiona usuarios, roles, permisos y estructura académica. |
| **Estudiante** | Consulta ofertas, se postula y participa en el seguimiento de su práctica. |
| **Coordinador** | Gestiona empresas y ofertas, revisa postulaciones, selecciona estudiantes, asigna docentes tutores y controla el avance. |
| **Docente tutor** | Supervisa académicamente al estudiante: revisa actividades y horas, registra observaciones y evalúa. |
| **Tutor empresarial** | Supervisa al estudiante en la empresa, valida actividades y evalúa su desempeño. |
| **Representante empresarial** | Administra los datos de su empresa y registra ofertas para revisión. |
| **Autoridad** | Consulta reportes e información consolidada para apoyar la toma de decisiones. |

> *Nota: El acceso depende del rol y puede limitarse a una carrera o unidad académica, o ser institucional.*

> *Precisión del MVP (CR-5, CR-6 y CR-8): la tabla anterior describe la visión completa del producto. En el MVP la administración de usuarios y estructura académica corresponde solo al administrador; el coordinador no la administra. El tutor empresarial no recomienda postulaciones: lo hace el representante de su empresa. La autoridad solo consulta auditoría. La definición y administración de requisitos queda fuera del MVP, aunque la validación de postulación sí se aplica sobre los requisitos existentes. La asignación de tutores y fechas sí está incluida; la transición a `EN_CURSO` y las etapas posteriores no.*

## 5. Alcance del MVP

La primera versión cubre la organización institucional y el proceso previo al inicio de las prácticas:

- **Usuarios y acceso:** registro, autenticación, roles y control de acceso.
- **Estructura académica:** facultades, carreras y periodos.
- **Empresas y convenios:** empresas, representantes y convenios.
- **Perfiles de estudiantes:** datos personales y académicos.
- **Ofertas:** registro por representantes empresariales y aprobación o rechazo por coordinadores.
- **Postulaciones:** consulta de ofertas aprobadas, postulación de estudiantes y aceptación o rechazo por coordinadores.
- **Asignación:** designación de docentes tutores y tutores empresariales, y registro de fechas de la práctica aprobada (CR-8). No se incluye la transición a `EN_CURSO`.
- **Auditoría básica:** registro de acciones relevantes e historiales necesarios para la trazabilidad de las operaciones del MVP. Los reportes avanzados y la trazabilidad avanzada quedan fuera.
- **Concurrencia básica:** control optimista mediante `@Version`, verificación transaccional de la disponibilidad de plazas al aceptar postulaciones y respuesta `409 Conflict` cuando exista un conflicto. No se usan mecanismos avanzados de concurrencia.

La validación de requisitos en la postulación se realiza sobre los requisitos que ya existan; no se crean catálogos ni datos institucionales ficticios. La administración de requisitos permanece fuera del MVP (CR-6).

## 6. Funcionalidades principales

Capacidades del sistema en su visión completa, sin detallar requisitos:

1. **Administración institucional:** usuarios, roles, permisos, facultades, carreras y periodos.
2. **Requisitos académicos:** definición y verificación de las condiciones para participar.
3. **Gestión empresarial:** empresas, representantes, tutores y convenios.
4. **Ofertas:** registro, revisión, aprobación y publicación.
5. **Postulaciones:** consulta de ofertas, solicitudes y selección de estudiantes.
6. **Formalización de prácticas:** registro de las prácticas aprobadas y asignación de responsables.
7. **Actividades y horas:** control de lo realizado y las horas cumplidas.
8. **Seguimiento y evaluación:** supervisión, observaciones, informes, evaluaciones e incidencias.
9. **Finalización y certificación:** verificación de cumplimiento y emisión de certificados.
10. **Reportes y trazabilidad:** información consolidada e historial de decisiones.

## 7. Fuera del alcance del MVP

- Gestión automatizada de requisitos académicos.
- Etapas posteriores a la asignación de tutores: registro y validación de actividades y horas, seguimiento continuo, informes y visitas.
- Evaluaciones de desempeño e incidencias.
- Cierre formal y certificados de cumplimiento.
- Reportes avanzados, indicadores y trazabilidad avanzada. La auditoría básica y los historiales del MVP sí están incluidos.
- Mecanismos avanzados de concurrencia (bloqueos distribuidos, colas o servicios externos). La concurrencia básica con `@Version`, control transaccional de plazas y respuesta `409 Conflict` sí está incluida.

## 8. Glosario

| Término | Definición |
| :--- | :--- |
| **Práctica preprofesional** | Actividad en la que el estudiante aplica sus conocimientos en un entorno laboral o institucional. |
| **Empresa receptora** | Organización que ofrece y recibe estudiantes para prácticas. |
| **Convenio institucional** | Acuerdo de colaboración entre la institución y la empresa. |
| **Oferta de práctica** | Oportunidad publicada por una empresa, con sus condiciones y actividades. |
| **Postulación** | Solicitud de un estudiante para participar en una oferta. |
| **Representante empresarial** | Persona que representa a la empresa en el sistema. |
| **Tutor empresarial** | Persona de la empresa que acompaña al estudiante. |
| **Docente tutor** | Docente responsable del seguimiento académico. |
| **Coordinador** | Responsable institucional de las prácticas en su ámbito académico. |
| **Periodo académico** | Intervalo con el que la institución organiza sus actividades. |
| **Asignación de tutor** | Designación del docente a cargo del seguimiento de una práctica. |
| **Seguimiento** | Supervisión del avance de una práctica. |
| **Incidencia** | Situación relevante que afecta el desarrollo de una práctica. |
| **Auditoría** | Registro de quién hizo qué y cuándo. |
| **Trazabilidad** | Capacidad de reconstruir el historial de un proceso. |