# Modelo de datos

## 1. Información general

Documenta la estructura existente de la base de datos de PRAXIS. Es la referencia física para generar las entidades y sus relaciones.

| Elemento | Detalle |
|---|---|
| Motor | PostgreSQL 15+ (diseñado para PG 17). |
| Contenido | 35 tablas, 2 vistas, funciones y triggers. |
| Zona horaria | La base y sus vistas/triggers conservan su configuración; la aplicación no configura `America/Guayaquil` ni convierte fechas a esa zona. Las fechas automáticas en código usan `LocalDate.now()`; las columnas `TIMESTAMPTZ` gestionadas por la base se leen como `OffsetDateTime`. |

| Módulo | Tablas |
|---|---|
| Seguridad y estructura académica | `rol`, `usuario`, `usuario_rol`, `facultad`, `carrera`, `usuario_carrera`, `periodo_academico`, `estudiante`, `docente` |
| Empresas | `empresa`, `tutor_empresarial`, `empresa_usuario`, `convenio` |
| Requisitos | `requisito`, `requisito_estudiante` |
| Ofertas y postulaciones | `oferta_practica`, `oferta_estado_historial`, `oferta_perfil`, `oferta_perfil_carrera`, `oferta_perfil_requisito`, `oferta_perfil_cambio_historial`, `postulacion`, `postulacion_recomendacion_historial` |
| Prácticas | `tipo_practica`, `practica`, `practica_estado_historial`, `actividad_practica`, `registro_horas`, `informe`, `informe_archivo` |
| Seguimiento y auditoría | `evaluacion`, `visita_seguimiento`, `incidencia`, `certificado`, `auditoria` |

## 2. Fuente del esquema

DDL real, de solo lectura:

| Archivo | Contenido |
|---|---|
| `V1__esquema.sql` | 26 tablas, vista `v_convenio_estado`, triggers de prácticas, horas, informes, evaluaciones y certificados. |
| `V2__catalogos.sql` | Carga de los 6 roles iniciales. |
| `V3__ofertas_practicas.sql` | 9 tablas de ofertas y postulaciones, vista `v_oferta_perfil_cupo`, columna `practica.postulacion_id`, rol `REPRESENTANTE_EMPRESA`, funciones, triggers y proceso de cierre de ofertas. |
| `V4__perfiles_docentes_automaticos.sql` | Carga de datos: crea el perfil `docente` de los usuarios con rol `DOCENTE` que no lo tenían. |

## 3. Convenciones y tipos

- Nombres en `snake_case` y singular.
- Clave primaria: `BIGINT GENERATED ALWAYS AS IDENTITY` (el valor no se asigna manualmente), salvo las tablas con clave compuesta indicadas.
- Fechas de eventos: `TIMESTAMPTZ`; fechas calendario: `DATE`.
- Estados: `VARCHAR` con restricción `CHECK`; no hay tipos `ENUM` nativos.
- Otros tipos: `JSONB`, `UUID`, `BYTEA`, `NUMERIC(p,s)`, `BOOLEAN`.
- Columna `version BIGINT` en `practica`, `registro_horas`, `informe` y `postulacion`.
- Notación de columnas: Nulo (Sí/No); Clave (PK, FK, UK); Def = valor por defecto.
- Las claves foráneas, restricciones e índices están consolidados en las secciones 5, 7 y 8.

## 4. Tablas y columnas

### 4.1. rol
Roles del sistema.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| codigo | VARCHAR(30) | No | UK | Código del rol (ver sección 9). |
| nombre | VARCHAR(60) | No | – | Nombre del rol. |
| descripcion | TEXT | Sí | – | Descripción. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.2. usuario
Cuentas de usuario.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| correo | VARCHAR(120) | No | UK | Correo en minúsculas. |
| hash_password | VARCHAR(72) | No | – | Contraseña cifrada. |
| nombres | VARCHAR(100) | No | – | Nombres. |
| apellidos | VARCHAR(100) | No | – | Apellidos. |
| numero_documento | VARCHAR(20) | Sí | UK | Cédula o pasaporte. |
| tipo_documento | VARCHAR(20) | No | – | Def: 'CEDULA'. |
| telefono | VARCHAR(20) | Sí | – | Teléfono. |
| activo | BOOLEAN | No | – | Def: TRUE. |
| alcance_global | BOOLEAN | No | – | Def: FALSE. |
| debe_cambiar_password | BOOLEAN | No | – | Def: FALSE. |
| intentos_fallidos | INT | No | – | Def: 0. |
| bloqueado_hasta | TIMESTAMPTZ | Sí | – | Fin del bloqueo temporal. |
| ultimo_login | TIMESTAMPTZ | Sí | – | Último ingreso. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

> Las columnas `hash_password`, `debe_cambiar_password`, `intentos_fallidos`, `bloqueado_hasta` y `ultimo_login` permanecen en el esquema porque no se modifica la base. El login básico lee `hash_password` y `activo`; BCrypt se usa para generar y verificar hashes. `debe_cambiar_password`, `intentos_fallidos`, `bloqueado_hasta` y `ultimo_login` no se usan. `alcance_global` se conserva como dato de dominio, sin control de acceso por ámbito (`SEGURIDAD.md`).

### 4.3. usuario_rol
Relación usuario–rol. Clave primaria compuesta.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| usuario_id | BIGINT | No | PK, FK | Usuario. |
| rol_id | BIGINT | No | PK, FK | Rol. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.4. facultad
Facultades.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| codigo | VARCHAR(20) | No | UK | Código. |
| nombre | VARCHAR(150) | No | UK | Nombre. |
| activa | BOOLEAN | No | – | Def: TRUE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.5. carrera
Carreras de una facultad.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| facultad_id | BIGINT | No | FK | Facultad. |
| codigo | VARCHAR(20) | No | UK | Código. |
| nombre | VARCHAR(150) | No | – | Nombre. |
| activa | BOOLEAN | No | – | Def: TRUE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.6. usuario_carrera
Relación usuario–carrera. Clave primaria compuesta.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| usuario_id | BIGINT | No | PK, FK | Usuario. |
| carrera_id | BIGINT | No | PK, FK | Carrera. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.7. periodo_academico
Periodos académicos; solo uno puede estar activo.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| codigo | VARCHAR(20) | No | UK | Código. |
| nombre | VARCHAR(100) | No | – | Nombre. |
| fecha_inicio | DATE | No | – | Inicio. |
| fecha_fin | DATE | No | – | Fin. |
| activo | BOOLEAN | No | – | Def: FALSE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.8. estudiante
Perfil académico del estudiante.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| usuario_id | BIGINT | No | FK, UK | Usuario. |
| carrera_id | BIGINT | No | FK | Carrera. |
| codigo_estudiante | VARCHAR(30) | Sí | UK | Código institucional. |
| fecha_nacimiento | DATE | Sí | – | Fecha de nacimiento. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.9. docente
Perfil del docente.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| usuario_id | BIGINT | No | FK, UK | Usuario. |
| codigo_docente | VARCHAR(20) | Sí | UK | Código institucional. |
| dedicacion | VARCHAR(50) | Sí | – | Dedicación. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.10. empresa
Empresas receptoras.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| ruc | VARCHAR(13) | No | UK | RUC de 13 dígitos. |
| razon_social | VARCHAR(200) | No | – | Razón social. |
| nombre_comercial | VARCHAR(200) | Sí | – | Nombre comercial. |
| direccion | VARCHAR(250) | Sí | – | Dirección. |
| telefono | VARCHAR(20) | Sí | – | Teléfono. |
| correo | VARCHAR(120) | Sí | – | Correo. |
| activa | BOOLEAN | No | – | Def: TRUE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.11. tutor_empresarial
Tutor de una empresa (un usuario pertenece a una sola empresa).

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| usuario_id | BIGINT | No | FK, UK | Usuario. |
| empresa_id | BIGINT | No | FK | Empresa. |
| cargo | VARCHAR(100) | Sí | – | Cargo. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.12. requisito
Requisitos académicos; `carrera_id` nulo significa requisito global.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| nombre | VARCHAR(150) | No | – | Nombre. |
| descripcion | TEXT | Sí | – | Descripción. |
| obligatorio | BOOLEAN | No | – | Def: TRUE. |
| carrera_id | BIGINT | Sí | FK | Carrera; nulo = global. |
| activo | BOOLEAN | No | – | Def: TRUE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.13. requisito_estudiante
Cumplimiento de un requisito por estudiante y periodo.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| estudiante_id | BIGINT | No | FK | Estudiante. |
| requisito_id | BIGINT | No | FK | Requisito. |
| periodo_id | BIGINT | No | FK | Periodo. |
| estado | VARCHAR(20) | No | – | Def: 'PENDIENTE'. |
| fecha_verificacion | TIMESTAMPTZ | Sí | – | Fecha de verificación. |
| verificado_por | BIGINT | Sí | FK | Usuario verificador. |
| observacion | TEXT | Sí | – | Observación. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.14. convenio
Convenios con empresas.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| empresa_id | BIGINT | No | FK | Empresa. |
| numero | VARCHAR(50) | Sí | UK | Número de convenio. |
| fecha_inicio | DATE | No | – | Inicio de vigencia. |
| fecha_fin | DATE | No | – | Fin de vigencia. |
| estado | VARCHAR(20) | No | – | Def: 'VIGENTE'. |
| documento_url | VARCHAR(500) | Sí | – | Ubicación del documento. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.15. tipo_practica
Catálogo de tipos de práctica con sus condiciones académicas.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| nombre | VARCHAR(80) | No | UK | Nombre. |
| horas_requeridas | NUMERIC(6,2) | No | – | Horas requeridas. |
| nota_minima | NUMERIC(4,2) | No | – | Nota mínima. |
| activo | BOOLEAN | No | – | Def: TRUE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.16. practica
Práctica preprofesional. `postulacion_id` fue añadida en V3.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| estudiante_id | BIGINT | No | FK | Estudiante. |
| carrera_id | BIGINT | No | FK | Carrera del estudiante. |
| empresa_id | BIGINT | No | FK | Empresa. |
| convenio_id | BIGINT | No | FK (compuesta) | Convenio, con `empresa_id`. |
| tipo_practica_id | BIGINT | No | FK | Tipo de práctica. |
| periodo_id | BIGINT | No | FK | Periodo. |
| docente_id | BIGINT | Sí | FK | Docente tutor. |
| tutor_empresarial_id | BIGINT | Sí | FK (compuesta) | Tutor empresarial, con `empresa_id`. |
| estado | VARCHAR(30) | No | – | Def: 'POSTULADA'. |
| horas_requeridas_snapshot | NUMERIC(6,2) | No | – | Horas congeladas. |
| nota_minima_snapshot | NUMERIC(4,2) | No | – | Nota mínima congelada. |
| fecha_postulacion | TIMESTAMPTZ | No | – | Def: NOW(). |
| fecha_aprobacion | TIMESTAMPTZ | Sí | – | Fecha de aprobación. |
| fecha_inicio | DATE | Sí | – | Inicio de ejecución. |
| fecha_fin | DATE | Sí | – | Fin de ejecución. |
| nota_final | NUMERIC(4,2) | Sí | – | Nota final. |
| observaciones | TEXT | Sí | – | Observaciones. |
| creado_por | BIGINT | No | FK | Usuario creador. |
| version | BIGINT | No | – | Def: 0. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| postulacion_id | BIGINT | Sí | FK, UK | Postulación de origen; nulo = práctica directa. |

### 4.17. practica_estado_historial
Historial de estados de la práctica (inmutable).

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| practica_id | BIGINT | No | FK | Práctica. |
| estado_anterior | VARCHAR(30) | Sí | – | Estado previo. |
| estado_nuevo | VARCHAR(30) | No | – | Estado nuevo. |
| usuario_id | BIGINT | Sí | FK | Usuario responsable. |
| motivo | TEXT | Sí | – | Motivo. |
| fecha | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.18. actividad_practica
Actividades realizadas durante la práctica.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| practica_id | BIGINT | No | FK | Práctica. |
| fecha | DATE | No | – | Fecha de la actividad. |
| descripcion | TEXT | No | – | Descripción. |
| registrada_por | BIGINT | No | FK | Usuario que registra. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.19. registro_horas
Registro diario de horas.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| practica_id | BIGINT | No | FK | Práctica. |
| fecha | DATE | No | – | Fecha del registro. |
| horas | NUMERIC(4,2) | No | – | Horas del día. |
| descripcion | TEXT | Sí | – | Descripción. |
| validado | BOOLEAN | No | – | Def: FALSE. |
| validado_por | BIGINT | Sí | FK | Usuario validador. |
| fecha_validacion | TIMESTAMPTZ | Sí | – | Fecha de validación. |
| version | BIGINT | No | – | Def: 0. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.20. informe
Informes parciales y finales (metadatos).

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| practica_id | BIGINT | No | FK | Práctica. |
| tipo | VARCHAR(20) | No | – | Tipo de informe. |
| estado | VARCHAR(20) | No | – | Def: 'ENTREGADO'. |
| nombre_archivo | VARCHAR(255) | No | – | Nombre original. |
| nombre_interno | VARCHAR(255) | No | UK | Nombre interno único. |
| content_type | VARCHAR(60) | No | – | Def: 'application/pdf'. |
| subido_por | BIGINT | No | FK | Usuario que sube. |
| fecha_subida | TIMESTAMPTZ | No | – | Def: NOW(). |
| observaciones | TEXT | Sí | – | Observaciones de la revisión. |
| version | BIGINT | No | – | Def: 0. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.21. informe_archivo
Contenido binario del informe. La clave primaria es también clave foránea.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| informe_id | BIGINT | No | PK, FK | Informe. |
| contenido | BYTEA | No | – | Archivo PDF. |
| tamano_bytes | INT | No | – | Tamaño en bytes. |
| sha256 | VARCHAR(64) | No | – | Huella SHA-256 en hexadecimal. |

### 4.22. evaluacion
Evaluación de la práctica por docente o tutor empresarial.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| practica_id | BIGINT | No | FK | Práctica. |
| evaluador_usuario_id | BIGINT | No | FK | Usuario evaluador. |
| rol_evaluador | VARCHAR(30) | No | – | Rol con el que evalúa. |
| criterios | JSONB | Sí | – | Criterios. |
| nota | NUMERIC(4,2) | No | – | Nota. |
| observacion | TEXT | Sí | – | Observación. |
| fecha_evaluacion | TIMESTAMPTZ | No | – | Def: NOW(). |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.23. visita_seguimiento
Visitas de seguimiento del docente.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| practica_id | BIGINT | No | FK | Práctica. |
| docente_id | BIGINT | No | FK | Docente. |
| fecha | DATE | No | – | Fecha de la visita. |
| observaciones | TEXT | Sí | – | Observaciones. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.24. incidencia
Incidencias de una práctica.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| practica_id | BIGINT | No | FK | Práctica. |
| reportada_por | BIGINT | No | FK | Usuario que reporta. |
| descripcion | TEXT | No | – | Descripción. |
| estado | VARCHAR(20) | No | – | Def: 'ABIERTA'. |
| resuelta_por | BIGINT | Sí | FK | Usuario que resuelve. |
| fecha_resolucion | TIMESTAMPTZ | Sí | – | Fecha de resolución. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.25. certificado
Certificado de finalización. No tiene `updated_at`.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| practica_id | BIGINT | No | FK | Práctica. |
| codigo_verificacion | UUID | No | UK | Def: gen_random_uuid(). |
| datos_snapshot | JSONB | No | – | Datos congelados (debe ser objeto JSON). |
| estado | VARCHAR(20) | No | – | Def: 'VIGENTE'. |
| fecha_emision | TIMESTAMPTZ | No | – | Def: NOW(). |
| emitido_por | BIGINT | No | FK | Usuario emisor. |
| motivo_anulacion | TEXT | Sí | – | Motivo de anulación. |
| fecha_anulacion | TIMESTAMPTZ | Sí | – | Fecha de anulación. |
| anulado_por | BIGINT | Sí | FK | Usuario que anula. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.26. auditoria
Registro de auditoría (inmutable).

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| usuario_id | BIGINT | Sí | FK | Usuario responsable. |
| accion | VARCHAR(50) | No | – | Acción. |
| entidad | VARCHAR(50) | No | – | Entidad afectada. |
| entidad_id | BIGINT | Sí | – | Identificador de la entidad. |
| detalle | JSONB | Sí | – | Detalle. |
| fecha | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.27. empresa_usuario
Usuarios de una empresa que publican ofertas.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| empresa_id | BIGINT | No | FK | Empresa. |
| usuario_id | BIGINT | No | FK | Usuario. |
| rol_empresa | VARCHAR(20) | No | – | Def: 'PUBLICADOR'. |
| activo | BOOLEAN | No | – | Def: TRUE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.28. oferta_practica
Convocatoria de prácticas.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| empresa_id | BIGINT | No | FK | Empresa. |
| convenio_id | BIGINT | No | FK (compuesta) | Convenio, con `empresa_id`. |
| tipo_practica_id | BIGINT | No | FK | Tipo de práctica. |
| periodo_id | BIGINT | No | FK | Periodo. |
| titulo | VARCHAR(200) | No | – | Título. |
| descripcion | TEXT | Sí | – | Descripción. |
| fecha_inicio_actividades | DATE | No | – | Inicio de actividades. |
| fecha_fin_actividades | DATE | No | – | Fin de actividades. |
| fecha_cierre | DATE | No | – | Último día para postular. |
| horas_requeridas_snapshot | NUMERIC(6,2) | Sí | – | Horas congeladas. |
| nota_minima_snapshot | NUMERIC(4,2) | Sí | – | Nota mínima congelada. |
| fecha_primera_publicacion | TIMESTAMPTZ | Sí | – | Primera aprobación. |
| fecha_publicacion | TIMESTAMPTZ | Sí | – | Última publicación. |
| estado | VARCHAR(20) | No | – | Def: 'BORRADOR'. |
| creada_por_usuario_id | BIGINT | No | FK (compuesta) | Creador, con `empresa_id`. |
| resuelta_por_usuario_id | BIGINT | Sí | FK | Usuario que resolvió. |
| fecha_resolucion | TIMESTAMPTZ | Sí | – | Fecha de resolución. |
| motivo_decision | TEXT | Sí | – | Motivo de la última transición. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.29. oferta_estado_historial
Historial de estados de la oferta (inmutable).

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| oferta_id | BIGINT | No | FK | Oferta. |
| estado_anterior | VARCHAR(20) | Sí | – | Estado previo. |
| estado_nuevo | VARCHAR(20) | No | – | Estado nuevo. |
| usuario_id | BIGINT | Sí | FK | Usuario responsable. |
| es_sistema | BOOLEAN | No | – | Def: FALSE. |
| motivo | TEXT | Sí | – | Motivo. |
| fecha | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.30. oferta_perfil
Perfiles y plazas de una oferta.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| oferta_id | BIGINT | No | FK | Oferta. |
| nombre | VARCHAR(150) | No | – | Nombre del perfil. |
| descripcion | TEXT | Sí | – | Descripción. |
| plazas | INT | No | – | Plazas. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.31. oferta_perfil_carrera
Carreras a las que se dirige un perfil (sin filas = abierto a todas). Clave primaria compuesta.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| perfil_id | BIGINT | No | PK, FK | Perfil. |
| carrera_id | BIGINT | No | PK, FK | Carrera. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.32. oferta_perfil_requisito
Requisitos laborales del perfil.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| perfil_id | BIGINT | No | FK | Perfil. |
| descripcion | TEXT | No | – | Descripción. |
| obligatorio | BOOLEAN | No | – | Def: TRUE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.33. oferta_perfil_cambio_historial
Historial inmutable de cambios en perfiles, carreras y requisitos de ofertas.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| oferta_id | BIGINT | Sí | FK | Oferta. Nulabilidad física: la columna admite `NULL` por su definición (el script lo contempla como caso límite de eliminación en cascada del hijo). Regla de funcionamiento esperada: siempre está informado, porque `oferta_practica` no se elimina (lo impide `trg_proteger_borrado_oferta`) y la FK es `ON DELETE RESTRICT`. |
| perfil_id | BIGINT | Sí | – | Perfil afectado (sin FK). |
| tabla | VARCHAR(40) | No | – | Tabla modificada. |
| operacion | VARCHAR(10) | No | – | Tipo de operación. |
| registro_id | BIGINT | Sí | – | Registro afectado (sin FK). |
| antes | JSONB | Sí | – | Valores previos. |
| despues | JSONB | Sí | – | Valores nuevos. |
| usuario_id | BIGINT | No | FK | Usuario responsable. |
| fecha | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.34. postulacion
Postulación de un estudiante a un perfil.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| perfil_id | BIGINT | No | FK | Perfil. |
| estudiante_id | BIGINT | No | FK | Estudiante. |
| estado | VARCHAR(20) | No | – | Def: 'PENDIENTE'. |
| mensaje | TEXT | Sí | – | Mensaje del estudiante. |
| fecha_postulacion | TIMESTAMPTZ | No | – | Def: NOW(). |
| recomendacion_empresa | VARCHAR(20) | No | – | Def: 'SIN_EVALUAR'. |
| observacion_empresa | TEXT | Sí | – | Observación de la empresa. |
| evaluada_por_empresa_usuario_id | BIGINT | Sí | FK | Miembro de la empresa que evaluó. |
| fecha_evaluacion_empresa | TIMESTAMPTZ | Sí | – | Fecha de la recomendación. |
| resuelta_por | BIGINT | Sí | FK | Usuario que resolvió. |
| fecha_resolucion | TIMESTAMPTZ | Sí | – | Fecha de resolución. |
| motivo_decision | TEXT | Sí | – | Motivo de la decisión. |
| cancelada_por | BIGINT | Sí | FK | Usuario que canceló. |
| fecha_cancelacion | TIMESTAMPTZ | Sí | – | Fecha de cancelación. |
| version | BIGINT | No | – | Def: 0. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.35. postulacion_recomendacion_historial
Historial inmutable de recomendaciones empresariales.

| Columna | Tipo real | Nulo | Clave | Descripción |
|---|---|---|---|---|
| id | BIGINT | No | PK | Identificador. |
| postulacion_id | BIGINT | No | FK | Postulación. |
| recomendacion | VARCHAR(20) | No | – | Recomendación emitida. |
| observacion | TEXT | Sí | – | Observación. |
| empresa_usuario_id | BIGINT | No | FK | Miembro de la empresa. |
| usuario_id | BIGINT | No | FK | Usuario. |
| fecha | TIMESTAMPTZ | No | – | Def: NOW(). |

### 4.36. Vista v_convenio_estado
Deriva `VENCIDO` cuando un convenio `VIGENTE` tiene `fecha_fin` anterior a hoy (zona Ecuador).

Uso previsto: consulta de solo lectura. No se mapea como entidad JPA; el Repository puede devolver una proyección sencilla con `estadoEfectivo` y la respuesta REST usa una estructura explícita (`ConvenioResumen`), sin serializar la vista directamente.

| Columna | Origen |
|---|---|
| id, empresa_id, numero, fecha_inicio, fecha_fin, estado | `convenio` |
| estado_efectivo | Calculada: `estado`, o `VENCIDO`. |

### 4.37. Vista v_oferta_perfil_cupo
Cupos por perfil, calculados desde `postulacion`.

Uso previsto: consulta de solo lectura. No se mapea como entidad JPA; el Repository puede devolver una proyección sencilla con `aceptadas`, `disponibles` y `perfilCerrado`, y la respuesta REST usa `PerfilOferta`, sin serializar la vista directamente. El control transaccional de la plaza al aceptar postulaciones se resuelve en el Service, no escribiendo esta vista.

| Columna | Origen |
|---|---|
| perfil_id, oferta_id, nombre, plazas | `oferta_perfil` |
| aceptadas, pendientes, rechazadas, retiradas | Conteo de postulaciones por estado. |
| disponibles | `plazas` menos aceptadas. |
| perfil_cerrado | Verdadero si las aceptadas alcanzan las plazas. |

## 5. Claves primarias y foráneas

Claves primarias: `id` en todas las tablas, excepto: `usuario_rol` (`usuario_id`, `rol_id`), `usuario_carrera` (`usuario_id`, `carrera_id`), `oferta_perfil_carrera` (`perfil_id`, `carrera_id`) e `informe_archivo` (`informe_id`).

Claves foráneas. Sin `ON UPDATE` definido en ninguna. Las restricciones sin nombre explícito se marcan con "–".

| Origen | Destino | ON DELETE | Restricción |
|---|---|---|---|
| usuario_rol.usuario_id | usuario.id | CASCADE | – |
| usuario_rol.rol_id | rol.id | RESTRICT | – |
| carrera.facultad_id | facultad.id | RESTRICT | – |
| usuario_carrera.usuario_id | usuario.id | CASCADE | – |
| usuario_carrera.carrera_id | carrera.id | RESTRICT | – |
| estudiante.usuario_id | usuario.id | RESTRICT | – |
| estudiante.carrera_id | carrera.id | RESTRICT | – |
| docente.usuario_id | usuario.id | RESTRICT | – |
| tutor_empresarial.usuario_id | usuario.id | RESTRICT | – |
| tutor_empresarial.empresa_id | empresa.id | RESTRICT | – |
| requisito.carrera_id | carrera.id | RESTRICT | – |
| requisito_estudiante.estudiante_id | estudiante.id | RESTRICT | – |
| requisito_estudiante.requisito_id | requisito.id | RESTRICT | – |
| requisito_estudiante.periodo_id | periodo_academico.id | RESTRICT | – |
| requisito_estudiante.verificado_por | usuario.id | RESTRICT | – |
| convenio.empresa_id | empresa.id | RESTRICT | – |
| practica.estudiante_id | estudiante.id | RESTRICT | – |
| practica.carrera_id | carrera.id | RESTRICT | – |
| practica.empresa_id | empresa.id | RESTRICT | – |
| practica.tipo_practica_id | tipo_practica.id | RESTRICT | – |
| practica.periodo_id | periodo_academico.id | RESTRICT | – |
| practica.docente_id | docente.id | RESTRICT | – |
| practica.creado_por | usuario.id | RESTRICT | – |
| practica (convenio_id, empresa_id) | convenio (id, empresa_id) | RESTRICT | fk_practica_convenio_empresa |
| practica (tutor_empresarial_id, empresa_id) | tutor_empresarial (id, empresa_id) | RESTRICT | fk_practica_tutor_empresa |
| practica.postulacion_id | postulacion.id | RESTRICT | – |
| practica_estado_historial.practica_id | practica.id | CASCADE | – |
| practica_estado_historial.usuario_id | usuario.id | RESTRICT | – |
| actividad_practica.practica_id | practica.id | CASCADE | – |
| actividad_practica.registrada_por | usuario.id | RESTRICT | – |
| registro_horas.practica_id | practica.id | CASCADE | – |
| registro_horas.validado_por | usuario.id | RESTRICT | – |
| informe.practica_id | practica.id | CASCADE | – |
| informe.subido_por | usuario.id | RESTRICT | – |
| informe_archivo.informe_id | informe.id | CASCADE | – |
| evaluacion.practica_id | practica.id | CASCADE | – |
| evaluacion.evaluador_usuario_id | usuario.id | RESTRICT | – |
| visita_seguimiento.practica_id | practica.id | CASCADE | – |
| visita_seguimiento.docente_id | docente.id | RESTRICT | – |
| incidencia.practica_id | practica.id | CASCADE | – |
| incidencia.reportada_por | usuario.id | RESTRICT | – |
| incidencia.resuelta_por | usuario.id | RESTRICT | – |
| certificado.practica_id | practica.id | RESTRICT | – |
| certificado.emitido_por | usuario.id | RESTRICT | – |
| certificado.anulado_por | usuario.id | RESTRICT | – |
| auditoria.usuario_id | usuario.id | RESTRICT | – |
| empresa_usuario.empresa_id | empresa.id | RESTRICT | – |
| empresa_usuario.usuario_id | usuario.id | RESTRICT | – |
| oferta_practica.empresa_id | empresa.id | RESTRICT | – |
| oferta_practica.tipo_practica_id | tipo_practica.id | RESTRICT | – |
| oferta_practica.periodo_id | periodo_academico.id | RESTRICT | – |
| oferta_practica.resuelta_por_usuario_id | usuario.id | RESTRICT | – |
| oferta_practica (convenio_id, empresa_id) | convenio (id, empresa_id) | RESTRICT | fk_oferta_convenio_empresa |
| oferta_practica (empresa_id, creada_por_usuario_id) | empresa_usuario (empresa_id, usuario_id) | RESTRICT | fk_oferta_creador_empresa |
| oferta_estado_historial.oferta_id | oferta_practica.id | RESTRICT | – |
| oferta_estado_historial.usuario_id | usuario.id | RESTRICT | – |
| oferta_perfil.oferta_id | oferta_practica.id | RESTRICT | – |
| oferta_perfil_carrera.perfil_id | oferta_perfil.id | CASCADE | – |
| oferta_perfil_carrera.carrera_id | carrera.id | RESTRICT | – |
| oferta_perfil_requisito.perfil_id | oferta_perfil.id | CASCADE | – |
| oferta_perfil_cambio_historial.oferta_id | oferta_practica.id | RESTRICT | – |
| oferta_perfil_cambio_historial.usuario_id | usuario.id | RESTRICT | – |
| postulacion.perfil_id | oferta_perfil.id | RESTRICT | – |
| postulacion.estudiante_id | estudiante.id | RESTRICT | – |
| postulacion.evaluada_por_empresa_usuario_id | empresa_usuario.id | RESTRICT | – |
| postulacion.resuelta_por | usuario.id | RESTRICT | – |
| postulacion.cancelada_por | usuario.id | RESTRICT | – |
| postulacion_recomendacion_historial.postulacion_id | postulacion.id | RESTRICT | – |
| postulacion_recomendacion_historial.empresa_usuario_id | empresa_usuario.id | RESTRICT | – |
| postulacion_recomendacion_historial.usuario_id | usuario.id | RESTRICT | – |

## 6. Relaciones

Cardinalidades verificables por claves foráneas y restricciones únicas.

| Relación | Cardinalidad |
|---|---|
| usuario – estudiante / docente / tutor_empresarial | 1 a 0..1 (por `usuario_id` único). |
| usuario – rol (`usuario_rol`) | N a M. |
| usuario – carrera (`usuario_carrera`) | N a M. |
| facultad – carrera | 1 a N. |
| carrera – estudiante | 1 a N. |
| empresa – tutor_empresarial | 1 a N. |
| empresa – usuario (`empresa_usuario`) | N a M; un usuario por empresa una sola vez. |
| empresa – convenio | 1 a N. |
| convenio – oferta_practica / practica | 1 a N, siempre de la misma empresa. |
| estudiante – requisito (`requisito_estudiante`) | N a M, por periodo. |
| tipo_practica / periodo_academico – oferta_practica / practica | 1 a N. |
| oferta_practica – oferta_perfil | 1 a N. |
| oferta_perfil – carrera (`oferta_perfil_carrera`) | N a M; sin filas el perfil está abierto a todas. |
| oferta_perfil – oferta_perfil_requisito | 1 a N. |
| oferta_perfil – postulacion | 1 a N. |
| estudiante – postulacion | 1 a N; una vigente por perfil. |
| postulacion – practica | 1 a 0..1 (`practica.postulacion_id` único). |
| estudiante – practica | 1 a N; una vigente por periodo. |
| docente / tutor_empresarial – practica | 1 a N. |
| practica – historial, actividad, registro_horas, informe, visita, incidencia | 1 a N. |
| practica – evaluacion | 1 a N; una por rol evaluador. |
| practica – certificado | 1 a N; un solo `VIGENTE`. |
| informe – informe_archivo | 1 a 0..1. |
| oferta_practica – historiales (`oferta_estado_historial`, `oferta_perfil_cambio_historial`) | 1 a N. |
| postulacion – postulacion_recomendacion_historial | 1 a N. |

## 7. Restricciones

### 7.1. Únicas y CHECK

Las restricciones UNIQUE declaradas a nivel de columna se indican en la sección 4 (marca «UK»); esta tabla detalla los CHECK y las restricciones con nombre.

| Tabla | Restricción | Regla |
|---|---|---|
| rol | chk_rol_codigo | `codigo` en los 7 valores de la sección 9. |
| usuario | chk_usuario_correo_lower | `correo` en minúsculas. |
| usuario | chk_usuario_correo | Formato de correo válido. |
| usuario | chk_usuario_tipo_doc | `tipo_documento` IN ('CEDULA','PASAPORTE'). |
| usuario | chk_usuario_documento | Nulo; cédula de 10 dígitos; o pasaporte alfanumérico de 5 a 20. |
| usuario | chk_usuario_intentos | `intentos_fallidos` ≥ 0. |
| carrera | uq_carrera_facultad_nombre | Único (`facultad_id`, `nombre`). |
| periodo_academico | chk_periodo_fechas | `fecha_fin` ≥ `fecha_inicio`. |
| empresa | chk_empresa_ruc | RUC de 13 dígitos. |
| tutor_empresarial | uq_tutor_empresa | Único (`id`, `empresa_id`), destino de la FK compuesta. |
| requisito | uq_requisito_nombre_carrera | Único (`nombre`, `carrera_id`), tratando nulos como iguales. |
| requisito_estudiante | uq_req_estudiante_periodo | Único (`estudiante_id`, `requisito_id`, `periodo_id`). |
| requisito_estudiante | chk_req_estado | Valores de la sección 9. |
| requisito_estudiante | chk_req_verificacion | `PENDIENTE` sin verificador ni fecha; `CUMPLIDO`/`RECHAZADO` con ambos. |
| convenio | chk_convenio_fechas | `fecha_fin` ≥ `fecha_inicio`. |
| convenio | chk_convenio_estado | Valores de la sección 9. |
| convenio | uq_convenio_empresa | Único (`id`, `empresa_id`), destino de FK compuestas. |
| tipo_practica | chk_tipo_practica_horas | `horas_requeridas` > 0. |
| tipo_practica | chk_tipo_practica_nota | `nota_minima` entre 0 y 20. |
| practica | chk_practica_estado | Valores de la sección 9. |
| practica | chk_practica_horas_snap | `horas_requeridas_snapshot` > 0. |
| practica | chk_practica_nota_snap | `nota_minima_snapshot` entre 0 y 20. |
| practica | chk_practica_nota | `nota_final` nula o entre 0 y 20. |
| practica | chk_practica_fechas | Ambas fechas nulas, o ambas con `fecha_fin` ≥ `fecha_inicio`. |
| practica | chk_practica_aprob | `fecha_aprobacion` nula o ≥ `fecha_postulacion`. |
| practica | chk_practica_tiene_aprob | Salvo `POSTULADA`, `RECHAZADA` y `CANCELADA`, exige `fecha_aprobacion`. |
| practica | chk_practica_tiene_asignaciones | `EN_CURSO` y posteriores (hasta `REPROBADA`) exigen docente, tutor empresarial y ambas fechas. |
| practica | chk_practica_tiene_nota | `EVALUADA`, `FINALIZADA` y `REPROBADA` exigen `nota_final`. |
| practica | chk_practica_finalizada_aprob | `FINALIZADA` exige `nota_final` ≥ `nota_minima_snapshot`. |
| practica | chk_practica_reprobada | `REPROBADA` exige `nota_final` < `nota_minima_snapshot`. |
| practica | (UK) postulacion_id | Una práctica por postulación. |
| practica_estado_historial | chk_hist_estado_anterior / chk_hist_estado_nuevo | Valores de estado de práctica. |
| registro_horas | uq_registro_horas_dia | Único (`practica_id`, `fecha`). |
| registro_horas | chk_registro_horas_rango | `horas` > 0 y ≤ 8. |
| registro_horas | chk_registro_validacion | No validado sin validador ni fecha; validado con ambos. |
| informe | chk_informe_tipo / chk_informe_estado | Valores de la sección 9. |
| informe | chk_informe_pdf | `content_type` = 'application/pdf'. |
| informe_archivo | chk_archivo_tamano | `tamano_bytes` > 0 y ≤ 2 097 152. |
| informe_archivo | chk_archivo_tamano_real | `tamano_bytes` = longitud de `contenido`. |
| informe_archivo | chk_archivo_sha256 | 64 caracteres hexadecimales en minúscula. |
| evaluacion | uq_eval_practica_rol | Único (`practica_id`, `rol_evaluador`). |
| evaluacion | chk_eval_rol / chk_eval_nota | Rol de la sección 9; `nota` entre 0 y 20. |
| incidencia | chk_incidencia_estado | Valores de la sección 9. |
| incidencia | chk_incidencia_resolucion | `ABIERTA` sin resolutor ni fecha; `RESUELTA` con ambos. |
| certificado | chk_certificado_estado | Valores de la sección 9. |
| certificado | chk_certificado_snapshot | `datos_snapshot` es un objeto JSON. |
| certificado | chk_certificado_anulacion | `VIGENTE` sin datos de anulación; `ANULADO` con fecha, responsable y motivo. |
| empresa_usuario | uq_empresa_usuario | Único (`empresa_id`, `usuario_id`), destino de `fk_oferta_creador_empresa`. |
| empresa_usuario | chk_empresa_usuario_rol | Valores de la sección 9. |
| oferta_practica | chk_oferta_estado | Valores de la sección 9. |
| oferta_practica | chk_oferta_titulo | Título no vacío. |
| oferta_practica | chk_oferta_fechas_actividad | `fecha_fin_actividades` ≥ `fecha_inicio_actividades`. |
| oferta_practica | chk_oferta_cierre_antes_inicio | `fecha_cierre` ≤ `fecha_inicio_actividades`. |
| oferta_practica | chk_oferta_snapshot | En `PUBLICADA`/`CERRADA` ambos snapshots son obligatorios, con horas > 0 y nota 0–20. El CHECK admite snapshots con valores válidos en otros estados; el trigger `trg_validar_oferta` impide asignarlos directamente y los genera al publicar. |
| oferta_practica | chk_oferta_resolucion | `PUBLICADA`/`RECHAZADA` exigen resolutor y fecha; `CERRADA` exige fecha. |
| oferta_practica | chk_oferta_publicacion | `PUBLICADA`/`CERRADA` exigen ambas fechas de publicación. |
| oferta_practica | chk_oferta_motivo | `RECHAZADA`/`CANCELADA` exigen `motivo_decision` no vacío. |
| oferta_estado_historial | chk_ofhist_anterior / chk_ofhist_nuevo | Valores de estado de oferta. |
| oferta_estado_historial | chk_ofhist_responsable | `usuario_id` informado o `es_sistema` verdadero. |
| oferta_estado_historial | chk_ofhist_motivo | `RECHAZADA`/`CANCELADA` exigen motivo. |
| oferta_perfil | uq_perfil_oferta_nombre | Único (`oferta_id`, `nombre`). |
| oferta_perfil | chk_perfil_plazas | `plazas` > 0. |
| oferta_perfil_requisito | chk_perfil_req_desc | Descripción no vacía. |
| postulacion | chk_postulacion_estado / chk_postulacion_recomendacion | Valores de la sección 9. |
| postulacion | chk_postulacion_eval_empresa | `SIN_EVALUAR` sin datos de evaluación; otro valor exige evaluador y fecha. |
| postulacion | chk_postulacion_resolucion | `PENDIENTE`/`RETIRADA` sin resolutor ni fecha; `ACEPTADA`/`RECHAZADA` con ambos; `CANCELADA` queda exenta de esa regla (usa `cancelada_por`/`fecha_cancelacion`). |
| postulacion | chk_postulacion_cancelacion | `CANCELADA` con responsable y fecha; otros estados sin ellos. |
| postulacion | chk_postulacion_motivo | `RECHAZADA`/`CANCELADA` exigen `motivo_decision` no vacío. |
| postulacion_recomendacion_historial | chk_posthist_recomendacion | `RECOMENDADO` o `NO_RECOMENDADO`. |
| oferta_perfil_cambio_historial | chk_oferta_perfil_hist_operacion | `INSERT`, `UPDATE` o `DELETE`. |

### 7.2. Triggers

Los triggers de `updated_at` (`set_updated_at`, `BEFORE UPDATE`) existen en todas las tablas que tienen esa columna (25 en total: 20 generados dinámicamente en `V1` y 5 declarados en `V3`). En la tabla siguiente, «Función invocada» da el identificador exacto y «Función» conserva la descripción del comportamiento. Los demás:

| Tabla | Trigger | Evento | Función invocada | Función |
|---|---|---|---|---|
| registro_horas | trg_validar_registro_horas | BEFORE INSERT/UPDATE | `validar_registro_horas()` | Fecha no futura, dentro de las fechas de la práctica y práctica `EN_CURSO` (también al validar). |
| registro_horas | trg_bloquear_horas_validadas | BEFORE UPDATE/DELETE | `bloquear_horas_validadas()` | Bloquea modificar o borrar horas validadas. |
| practica | trg_validar_practica_insert | BEFORE INSERT | `validar_practica_insert()` | `carrera_id` debe coincidir con la del estudiante. |
| practica | trg_proteger_borrado_practica | BEFORE DELETE | `proteger_borrado_practica()` | No borra prácticas alguna vez aprobadas. |
| practica | trg_proteger_snapshots | BEFORE UPDATE | `proteger_snapshots_practica()` | Congela snapshots y llaves al salir de `POSTULADA`. |
| practica | trg_validar_transicion_practica | BEFORE UPDATE OF estado | `validar_transicion_practica()` | Aplica la máquina de estados de la práctica. |
| practica | trg_registrar_historial_practica | AFTER INSERT / UPDATE OF estado | `registrar_historial_practica()` | Escribe `practica_estado_historial`. |
| practica | trg_validar_practica_postulacion | BEFORE INSERT/UPDATE (columnas clave) | `validar_practica_postulacion()` | Coherencia con la postulación y la oferta de origen; protege el vínculo. |
| practica | trg_sincronizar_postulacion_practica | AFTER UPDATE OF estado | `sincronizar_postulacion_con_practica()` | Práctica `CANCELADA`/`RECHAZADA` cancela su postulación aceptada. |
| practica | trg_practica_mantiene_postulacion | AFTER DELETE/UPDATE OF postulacion_id (diferido) | `verificar_postulacion_con_practica()` | Verifica que una postulación aceptada conserve su práctica. |
| practica_estado_historial | trg_proteger_historial_update | BEFORE UPDATE | `proteger_historial_update()` | Impide modificar el historial. |
| auditoria | trg_auditoria_inmutable | BEFORE UPDATE/DELETE | `impedir_modificacion_auditoria()` | Impide modificar o borrar. |
| certificado | trg_proteger_certificado | BEFORE INSERT/UPDATE/DELETE | `proteger_certificado()` | Solo para prácticas `FINALIZADA`; inmutable; `ANULADO` definitivo; no se borra. |
| informe | trg_proteger_informe | BEFORE UPDATE/DELETE | `proteger_informe()` | Informe `APROBADO` inmutable e imborrable. |
| informe_archivo | trg_proteger_informe_archivo | BEFORE UPDATE/DELETE | `proteger_informe_archivo()` | Archivo inmutable si el informe está `APROBADO`. |
| evaluacion | trg_proteger_evaluacion | BEFORE INSERT/UPDATE/DELETE | `proteger_evaluacion()` | Solo con práctica `EN_CURSO` o `HORAS_COMPLETAS`. |
| empresa_usuario | trg_validar_empresa_usuario | BEFORE INSERT/UPDATE/DELETE | `validar_empresa_usuario()` | Reglas de membresía; no se borra. |
| empresa_usuario | trg_auditar_empresa_usuario | AFTER INSERT/UPDATE | `auditar_empresa_usuario()` | Registra en `auditoria`. |
| oferta_practica | trg_validar_oferta | BEFORE INSERT/UPDATE | `validar_oferta()` | Estados, validación de transiciones por acción, revalidaciones y snapshots. |
| oferta_practica | trg_auditar_cambios_cabecera_oferta | AFTER UPDATE | `auditar_cambios_cabecera_oferta()` | Registra cambios de cabecera en `auditoria`. |
| oferta_practica | trg_proteger_borrado_oferta | BEFORE DELETE | `proteger_borrado_oferta()` | Impide borrar. |
| oferta_practica | trg_registrar_historial_oferta | AFTER INSERT / UPDATE OF estado | `registrar_historial_oferta()` | Escribe `oferta_estado_historial`. |
| oferta_estado_historial, postulacion_recomendacion_historial, oferta_perfil_cambio_historial | trg_proteger_historial_oferta, trg_proteger_historial_recomendacion, trg_inmutable_hist_oferta_perfil | BEFORE UPDATE/DELETE | `proteger_historial_inmutable()` (los tres) | Impiden modificar o borrar. |
| oferta_perfil, oferta_perfil_carrera, oferta_perfil_requisito | trg_oferta_perfil_modificado, trg_oferta_perfil_carrera_modificado, trg_oferta_perfil_requisito_modificado | BEFORE INSERT/UPDATE/DELETE | `oferta_hijo_modificado()` (los tres) | Solo editables según el estado de la oferta; devuelven la oferta publicada a revisión. |
| oferta_perfil, oferta_perfil_carrera, oferta_perfil_requisito | trg_hist_oferta_perfil, trg_hist_oferta_perfil_carrera, trg_hist_oferta_perfil_requisito | AFTER INSERT/UPDATE/DELETE | `registrar_cambio_oferta_hijo()` (los tres) | Escriben `oferta_perfil_cambio_historial`. |
| oferta_perfil | trg_validar_perfil_plazas | BEFORE UPDATE | `validar_perfil_plazas()` | No reduce plazas por debajo de las aceptadas. |
| postulacion | trg_validar_postulacion | BEFORE INSERT/UPDATE | `validar_postulacion()` | Actor, recomendación, decisión, cupos y transiciones. |
| postulacion | trg_auditar_postulacion | AFTER UPDATE OF estado | `auditar_postulacion()` | Registra cada transición en `auditoria`. |
| postulacion | trg_auditar_creacion_postulacion | AFTER INSERT | `auditar_creacion_postulacion()` | Registra la creación en `auditoria`. |
| postulacion | trg_registrar_recomendacion_historial | AFTER UPDATE (recomendación u observación) | `registrar_recomendacion_historial()` | Escribe `postulacion_recomendacion_historial`. |
| postulacion | trg_cerrar_oferta_si_completa | AFTER UPDATE OF estado (a `ACEPTADA`) | `cerrar_oferta_si_completa()` | Cierra la oferta si todas las plazas están cubiertas. |
| postulacion | trg_postulacion_requiere_practica | AFTER INSERT/UPDATE (diferido) | `verificar_postulacion_con_practica()` | Una postulación `ACEPTADA` requiere su práctica en la misma transacción. |

Funciones invocables: `cerrar_ofertas_vencidas()` cierra las ofertas `PUBLICADA` con fecha de cierre pasada y devuelve la cantidad de ofertas cerradas. Las demás son auxiliares de los triggers (`praxis_actor`, `praxis_sistema`, `tiene_rol_activo`, `es_coordinador_activo`, `empresa_membresia_id`, `empresa_rol_activo`, `puede_gestionar_oferta`).

#### 7.2.1. Inventario de funciones

Los scripts definen **40 funciones** (14 en `V1` y 26 en `V3`) y ningún procedimiento almacenado. Las funciones de trigger son `plpgsql` con volatilidad por defecto (`VOLATILE`); las auxiliares son `sql STABLE`.

| Función | Script | Lenguaje / volatilidad | Relación con los triggers |
|---|---|---|---|
| `set_updated_at()` | `V1` | plpgsql / VOLATILE | 25 triggers `BEFORE UPDATE`: los 20 `trg_<tabla>_updated` generados dinámicamente en `V1` y los 5 explícitos de `V3` (`trg_empresa_usuario_updated`, `trg_oferta_practica_updated`, `trg_oferta_perfil_updated`, `trg_oferta_perfil_requisito_updated`, `trg_postulacion_updated`). |
| `validar_registro_horas()` | `V1` | plpgsql / VOLATILE | `trg_validar_registro_horas`. |
| `bloquear_horas_validadas()` | `V1` | plpgsql / VOLATILE | `trg_bloquear_horas_validadas`. |
| `validar_practica_insert()` | `V1` | plpgsql / VOLATILE | `trg_validar_practica_insert`. |
| `proteger_borrado_practica()` | `V1` | plpgsql / VOLATILE | `trg_proteger_borrado_practica`. |
| `proteger_snapshots_practica()` | `V1` | plpgsql / VOLATILE | `trg_proteger_snapshots`. |
| `validar_transicion_practica()` | `V1` | plpgsql / VOLATILE | `trg_validar_transicion_practica`. |
| `registrar_historial_practica()` | `V1` | plpgsql / VOLATILE | `trg_registrar_historial_practica`. |
| `proteger_historial_update()` | `V1` | plpgsql / VOLATILE | `trg_proteger_historial_update`. |
| `impedir_modificacion_auditoria()` | `V1` | plpgsql / VOLATILE | `trg_auditoria_inmutable`. |
| `proteger_certificado()` | `V1` | plpgsql / VOLATILE | `trg_proteger_certificado`. |
| `proteger_informe()` | `V1` | plpgsql / VOLATILE | `trg_proteger_informe`. |
| `proteger_informe_archivo()` | `V1` | plpgsql / VOLATILE | `trg_proteger_informe_archivo`. |
| `proteger_evaluacion()` | `V1` | plpgsql / VOLATILE | `trg_proteger_evaluacion`. |
| `praxis_actor()` | `V3` | sql / STABLE | Auxiliar. La invocan funciones de auditoría y validación de `V3`: `validar_empresa_usuario()`, `auditar_empresa_usuario()`, `validar_oferta()`, `auditar_cambios_cabecera_oferta()`, `registrar_historial_oferta()`, `oferta_hijo_modificado()`, `registrar_cambio_oferta_hijo()`, `validar_postulacion()`, `auditar_postulacion()`, `auditar_creacion_postulacion()`, `registrar_recomendacion_historial()` y `validar_practica_postulacion()`. |
| `praxis_sistema()` | `V3` | sql / STABLE | Auxiliar. La invocan `validar_oferta()` (validación de la transición a `CERRADA`) y `registrar_historial_oferta()`. |
| `tiene_rol_activo(p_usuario BIGINT, p_codigo TEXT)` | `V3` | sql / STABLE | Auxiliar. La invocan `es_coordinador_activo()`, `validar_empresa_usuario()`, `validar_oferta()`, `validar_postulacion()` y `validar_practica_postulacion()`. |
| `es_coordinador_activo(p_usuario BIGINT)` | `V3` | sql / STABLE | Auxiliar; definida en `V3` pero no invocada por ningún trigger del script (los controles usan `tiene_rol_activo(..., 'COORDINADOR')`). |
| `empresa_membresia_id(p_usuario BIGINT, p_empresa BIGINT)` | `V3` | sql / STABLE | Auxiliar. La invocan `empresa_rol_activo()` y `validar_postulacion()`. |
| `empresa_rol_activo(p_usuario BIGINT, p_empresa BIGINT)` | `V3` | sql / STABLE | Auxiliar. La invocan `puede_gestionar_oferta()`, `validar_empresa_usuario()` y `validar_oferta()`. |
| `puede_gestionar_oferta(p_actor BIGINT, p_empresa BIGINT, p_creador BIGINT)` | `V3` | sql / STABLE | Auxiliar. La invocan `validar_oferta()` y `oferta_hijo_modificado()`. |
| `validar_empresa_usuario()` | `V3` | plpgsql / VOLATILE | `trg_validar_empresa_usuario`. |
| `auditar_empresa_usuario()` | `V3` | plpgsql / VOLATILE | `trg_auditar_empresa_usuario`. |
| `validar_oferta()` | `V3` | plpgsql / VOLATILE | `trg_validar_oferta`. |
| `auditar_cambios_cabecera_oferta()` | `V3` | plpgsql / VOLATILE | `trg_auditar_cambios_cabecera_oferta`. |
| `proteger_borrado_oferta()` | `V3` | plpgsql / VOLATILE | `trg_proteger_borrado_oferta`. |
| `registrar_historial_oferta()` | `V3` | plpgsql / VOLATILE | `trg_registrar_historial_oferta`. |
| `proteger_historial_inmutable()` | `V3` | plpgsql / VOLATILE | `trg_proteger_historial_oferta`, `trg_proteger_historial_recomendacion` y `trg_inmutable_hist_oferta_perfil`. |
| `oferta_hijo_modificado()` | `V3` | plpgsql / VOLATILE | `trg_oferta_perfil_modificado`, `trg_oferta_perfil_carrera_modificado` y `trg_oferta_perfil_requisito_modificado`. |
| `registrar_cambio_oferta_hijo()` | `V3` | plpgsql / VOLATILE | `trg_hist_oferta_perfil`, `trg_hist_oferta_perfil_carrera` y `trg_hist_oferta_perfil_requisito`. |
| `validar_perfil_plazas()` | `V3` | plpgsql / VOLATILE | `trg_validar_perfil_plazas`. |
| `validar_postulacion()` | `V3` | plpgsql / VOLATILE | `trg_validar_postulacion`. |
| `auditar_postulacion()` | `V3` | plpgsql / VOLATILE | `trg_auditar_postulacion`. |
| `auditar_creacion_postulacion()` | `V3` | plpgsql / VOLATILE | `trg_auditar_creacion_postulacion`. |
| `registrar_recomendacion_historial()` | `V3` | plpgsql / VOLATILE | `trg_registrar_recomendacion_historial`. |
| `cerrar_oferta_si_completa()` | `V3` | plpgsql / VOLATILE | `trg_cerrar_oferta_si_completa`. |
| `verificar_postulacion_con_practica()` | `V3` | plpgsql / VOLATILE | Los dos `CONSTRAINT TRIGGER` diferidos: `trg_postulacion_requiere_practica` y `trg_practica_mantiene_postulacion`. |
| `validar_practica_postulacion()` | `V3` | plpgsql / VOLATILE | `trg_validar_practica_postulacion`. |
| `sincronizar_postulacion_con_practica()` | `V3` | plpgsql / VOLATILE | `trg_sincronizar_postulacion_practica`. |
| `cerrar_ofertas_vencidas()` | `V3` | plpgsql / VOLATILE | Sin trigger; la invoca el backend periódicamente (`SELECT cerrar_ofertas_vencidas();`). Devuelve `INT`. |

### 7.3. Contexto de sesión requerido por los triggers (ya no se fija desde la aplicación)

La aplicación eliminó el disparador de parámetros de sesión: no ejecuta `set_config(..., true)` ni fija `praxis.usuario_id`, `praxis.motivo` ni `praxis.sistema`. No se inventa un usuario fijo ni un usuario \"sistema\" para evitar un rechazo. El detalle y el pendiente están en `CONTEXTO_ACTOR.md`.

Los triggers de la base pueden leer estos parámetros según el DDL de solo lectura:

| Parámetro | Uso en la base |
|---|---|
| `praxis.usuario_id` | Usuario autenticado; obligatorio en operaciones auditadas, ofertas, postulaciones, membresías y cambios de perfiles. |
| `praxis.motivo` | Motivo de rechazos, cancelaciones y correcciones. |
| `praxis.sistema` | `'true'` solo en procesos automáticos sin usuario. |
| `praxis.desvincular_postulacion` | `'true'` solo en la operación administrativa que desvincula una práctica de su postulación. |
| `praxis.correccion_decision` | `'true'` solo en la corrección administrativa de una postulación resuelta. |
| `praxis.sync_practica` | Uso interno del trigger de sincronización con la práctica. |

Como la aplicación ya no fija el contexto, las escrituras dependientes de esos parámetros pueden ser rechazadas por la base. El esquema, los triggers y los parámetros no se modifican.

## 8. Índices

Además de los índices implícitos de las claves primarias y restricciones únicas.

| Tabla | Índice | Columnas | Condición |
|---|---|---|---|
| usuario_rol | idx_usuario_rol_rol | rol_id | – |
| carrera | idx_carrera_facultad | facultad_id | – |
| usuario_carrera | idx_usuario_carrera_carrera | carrera_id | – |
| periodo_academico | uq_periodo_unico_activo | UNIQUE (activo) | `activo = TRUE` |
| estudiante | idx_estudiante_carrera | carrera_id | – |
| tutor_empresarial | idx_tutor_empresa | empresa_id | – |
| requisito | idx_requisito_carrera | carrera_id | `carrera_id IS NOT NULL` |
| requisito_estudiante | idx_req_estudiante_estado | estudiante_id, estado | – |
| requisito_estudiante | idx_req_estudiante_req | requisito_id | – |
| requisito_estudiante | idx_req_estudiante_periodo | periodo_id | – |
| convenio | idx_convenio_empresa_estado | empresa_id, estado | – |
| practica | idx_practica_estudiante_periodo | estudiante_id, periodo_id | – |
| practica | idx_practica_estado_periodo | estado, periodo_id | – |
| practica | idx_practica_carrera_estado | carrera_id, estado | – |
| practica | idx_practica_docente | docente_id | `docente_id IS NOT NULL` |
| practica | idx_practica_tutor | tutor_empresarial_id | `tutor_empresarial_id IS NOT NULL` |
| practica | idx_practica_empresa | empresa_id | – |
| practica | idx_practica_periodo | periodo_id | – |
| practica | idx_practica_tipo | tipo_practica_id | – |
| practica | idx_practica_convenio | convenio_id, empresa_id | – |
| practica | idx_practica_creado_por | creado_por | – |
| practica | uq_practica_activa_por_periodo | UNIQUE (estudiante_id, periodo_id) | `estado NOT IN ('RECHAZADA','CANCELADA','REPROBADA')` |
| practica_estado_historial | idx_hist_practica_fecha | practica_id, fecha DESC | – |
| actividad_practica | idx_actividad_practica | practica_id, fecha | – |
| informe | idx_informe_practica_estado | practica_id, estado | – |
| informe | idx_informe_subido_por | subido_por | – |
| informe | uq_informe_final_aprobado | UNIQUE (practica_id) | `tipo = 'FINAL' AND estado = 'APROBADO'` |
| evaluacion | idx_eval_evaluador | evaluador_usuario_id | – |
| visita_seguimiento | idx_visita_practica_fecha | practica_id, fecha | – |
| visita_seguimiento | idx_visita_docente | docente_id | – |
| incidencia | idx_incidencia_practica_estado | practica_id, estado | – |
| certificado | uq_certificado_vigente_por_practica | UNIQUE (practica_id) | `estado = 'VIGENTE'` |
| auditoria | idx_auditoria_usuario_fecha | usuario_id, fecha DESC | – |
| auditoria | idx_auditoria_entidad | entidad, entidad_id | – |
| empresa_usuario | idx_empresa_usuario_usuario | usuario_id | – |
| oferta_practica | idx_oferta_empresa_estado | empresa_id, estado | – |
| oferta_practica | idx_oferta_estado_cierre | estado, fecha_cierre | – |
| oferta_practica | idx_oferta_convenio | convenio_id, empresa_id | – |
| oferta_practica | idx_oferta_periodo | periodo_id | – |
| oferta_practica | idx_oferta_tipo | tipo_practica_id | – |
| oferta_practica | idx_oferta_creador | creada_por_usuario_id | – |
| oferta_estado_historial | idx_ofhist_oferta_fecha | oferta_id, fecha DESC | – |
| oferta_perfil | idx_perfil_oferta | oferta_id | – |
| oferta_perfil_carrera | idx_perfil_carrera_carrera | carrera_id | – |
| oferta_perfil_requisito | idx_perfil_req_perfil | perfil_id | – |
| postulacion | uq_postulacion_perfil_estudiante | UNIQUE (perfil_id, estudiante_id) | `estado <> 'RETIRADA'` |
| postulacion | idx_postulacion_perfil_estado | perfil_id, estado | – |
| postulacion | idx_postulacion_estudiante | estudiante_id, estado | – |
| postulacion_recomendacion_historial | idx_posthist_postulacion | postulacion_id, fecha DESC | – |
| oferta_perfil_cambio_historial | idx_oferta_perfil_hist_oferta_fecha | oferta_id, fecha DESC | – |

## 9. Enums y catálogos

Los valores son exactamente los definidos en la base de datos. No hay tipos `ENUM` nativos; todos son `CHECK` sobre `VARCHAR`.

| Tabla.columna | Valores |
|---|---|
| rol.codigo | `ESTUDIANTE`, `DOCENTE`, `TUTOR_EMPRESARIAL`, `COORDINADOR`, `ADMINISTRADOR`, `AUTORIDAD`, `REPRESENTANTE_EMPRESA` |
| usuario.tipo_documento | `CEDULA`, `PASAPORTE` |
| requisito_estudiante.estado | `PENDIENTE`, `CUMPLIDO`, `RECHAZADO` |
| convenio.estado | `VIGENTE`, `SUSPENDIDO`, `TERMINADO` |
| v_convenio_estado.estado_efectivo | Los de `convenio.estado` más `VENCIDO` (derivado). |
| practica.estado, practica_estado_historial.estado_anterior / estado_nuevo | `POSTULADA`, `APROBADA`, `EN_CURSO`, `HORAS_COMPLETAS`, `EVALUADA`, `FINALIZADA`, `REPROBADA`, `RECHAZADA`, `CANCELADA` |
| informe.tipo | `PARCIAL`, `FINAL` |
| informe.estado | `ENTREGADO`, `OBSERVADO`, `APROBADO` |
| evaluacion.rol_evaluador | `DOCENTE`, `TUTOR_EMPRESARIAL` |
| incidencia.estado | `ABIERTA`, `RESUELTA` |
| certificado.estado | `VIGENTE`, `ANULADO` |
| empresa_usuario.rol_empresa | `ADMIN_EMPRESA`, `PUBLICADOR` |
| oferta_practica.estado, oferta_estado_historial.estado_anterior / estado_nuevo | `BORRADOR`, `PENDIENTE_REVISION`, `PUBLICADA`, `RECHAZADA`, `CERRADA`, `CANCELADA` |
| postulacion.estado | `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `RETIRADA`, `CANCELADA` |
| postulacion.recomendacion_empresa | `SIN_EVALUAR`, `RECOMENDADO`, `NO_RECOMENDADO` |
| postulacion_recomendacion_historial.recomendacion | `RECOMENDADO`, `NO_RECOMENDADO` |
| oferta_perfil_cambio_historial.operacion | `INSERT`, `UPDATE`, `DELETE` |

Columnas de texto libre sin `CHECK`: `auditoria.accion` y `auditoria.entidad`; `oferta_perfil_cambio_historial.tabla` (lo escribe el trigger con el nombre de la tabla modificada).

## 10. DDL de referencia

El DDL completo es de solo lectura y se mantiene como archivos separados en `docs/db/`, en este orden:

1. [`V1__esquema.sql`](docs/db/V1__esquema.sql)
2. [`V2__catalogos.sql`](docs/db/V2__catalogos.sql)
3. [`V3__ofertas_practicas.sql`](docs/db/V3__ofertas_practicas.sql)
4. [`V4__perfiles_docentes_automaticos.sql`](docs/db/V4__perfiles_docentes_automaticos.sql)

## 11. Observaciones y pendientes

Observaciones sobre la estructura
1. Las claves `id` son `GENERATED ALWAYS`: no se puede insertar un identificador explícito.
2. Tienen clave primaria compuesta `usuario_rol`, `usuario_carrera` y `oferta_perfil_carrera`; `informe_archivo` usa `informe_id` como clave primaria y foránea.
3. Hay claves foráneas compuestas en `practica` y `oferta_practica`. Estas columnas no tienen clave foránea simple: `practica.convenio_id`, `practica.tutor_empresarial_id`, `oferta_practica.convenio_id` y `oferta_practica.creada_por_usuario_id`. Las cuatro columnas participan en FK compuestas que incluyen `empresa_id`: `(convenio_id, empresa_id)`, `(tutor_empresarial_id, empresa_id)`, `(convenio_id, empresa_id)` y `(empresa_id, creada_por_usuario_id)`.
4. No tienen clave foránea: `oferta_perfil_cambio_historial.perfil_id` y `registro_id`.
5. `practica.postulacion_id` nulo identifica una práctica directa, sin convocatoria.
6. `tipo_practica` mantiene las horas requeridas y la nota mínima; `oferta_practica` y `practica` guardan copias congeladas (`*_snapshot`).
7. `informe_archivo` almacena el PDF como `BYTEA` dentro de la base de datos.
8. `certificado` y las tablas de historial y auditoría no tienen `updated_at`. Tampoco lo tienen `usuario_rol`, `usuario_carrera` y `oferta_perfil_carrera` (solo `created_at`), ni `informe_archivo` (que tampoco tiene `created_at`).

Comportamiento que afecta al acceso a datos
9. Los triggers pueden cambiar valores al guardar: estados (una oferta publicada editada pasa a `PENDIENTE_REVISION`), `motivo_decision`, fechas de resolución y publicación, y snapshots. El valor almacenado puede diferir del enviado.
10. Los triggers pueden rechazar operaciones sin el contexto de sesión de la sección 7.3: la aplicación ya no lo fija y el esquema no se modifica.
11. `trg_postulacion_requiere_practica` y `trg_practica_mantiene_postulacion` son diferidos: sus errores aparecen al confirmar la transacción, no al ejecutar la sentencia.
12. Las columnas gestionadas por trigger no deben editarse directamente: snapshots de oferta, `fecha_primera_publicacion`, `fecha_publicacion`, `resuelta_por_usuario_id` y `fecha_resolucion` de la oferta; en `postulacion`, `fecha_evaluacion_empresa`, `resuelta_por`, `fecha_resolucion`, `motivo_decision`, `cancelada_por` y `fecha_cancelacion`; y en `practica`, los snapshots académicos (congelados por `trg_proteger_snapshots`). La sección 7.3 documenta que el contexto de sesión ya no se fija desde la aplicación.

Pendientes (no resueltos por el esquema)
13. No existe tabla para tokens de recuperación de contraseña; esa función no está vigente y queda pendiente a futuro.
14. El límite de 8 horas diarias (`chk_registro_horas_rango`) y de 2 MB por informe (`chk_archivo_tamano`) están fijos en restricciones.
15. `tipo_practica`, `requisito` y el resto de catálogos académicos no tienen datos iniciales. La aplicación no inserta datos institucionales ni de prueba: gestiona listados vacíos y valida solo los requisitos que existan. La administración de requisitos permanece fuera del MVP (CR-6).
16. `usuario.alcance_global` y `usuario_carrera` existen, pero la base de datos no impone cómo se usan.
17. La cabecera de `V1__esquema.sql` indica "26 tablas + 1 vista"; con V3 el total es 35 tablas y 2 vistas.
18. `MODELO_DATOS.md` no define cómo se implementan las entidades; ver `ARQUITECTURA.md`.
