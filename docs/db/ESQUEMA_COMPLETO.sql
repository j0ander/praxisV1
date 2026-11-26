
























BEGIN;

DO $$
BEGIN
    IF to_regclass('rol') IS NOT NULL OR to_regclass('usuario') IS NOT NULL THEN
        RAISE EXCEPTION
            'ESQUEMA_COMPLETO.sql requiere una base vacía; no aplicar V1–V4 en ella';
    END IF;
END $$;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;





CREATE TABLE rol (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo      VARCHAR(30) NOT NULL UNIQUE,
    nombre      VARCHAR(60) NOT NULL,
    descripcion TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE usuario (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    correo                VARCHAR(120) NOT NULL UNIQUE,
    hash_password         VARCHAR(72)  NOT NULL,
    nombres               VARCHAR(100) NOT NULL,
    apellidos             VARCHAR(100) NOT NULL,
    numero_documento      VARCHAR(20)  UNIQUE,
    tipo_documento        VARCHAR(20)  NOT NULL DEFAULT 'CEDULA',
    telefono              VARCHAR(20),
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    alcance_global        BOOLEAN      NOT NULL DEFAULT FALSE,
    debe_cambiar_password BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos_fallidos     INT          NOT NULL DEFAULT 0,
    bloqueado_hasta       TIMESTAMPTZ,
    ultimo_login          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    rol_id     BIGINT      NOT NULL REFERENCES rol(id)     ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (usuario_id, rol_id)
);
CREATE INDEX idx_usuario_rol_rol ON usuario_rol(rol_id);

CREATE TABLE facultad (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo     VARCHAR(20)  NOT NULL UNIQUE,
    nombre     VARCHAR(150) NOT NULL UNIQUE,
    activa     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE carrera (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    facultad_id BIGINT       NOT NULL REFERENCES facultad(id) ON DELETE RESTRICT,
    codigo      VARCHAR(20)  NOT NULL UNIQUE,
    nombre      VARCHAR(150) NOT NULL,
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_carrera_facultad_nombre UNIQUE (facultad_id, nombre)
);
CREATE INDEX idx_carrera_facultad ON carrera(facultad_id);

CREATE TABLE usuario_carrera (
    usuario_id BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    carrera_id BIGINT      NOT NULL REFERENCES carrera(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (usuario_id, carrera_id)
);
CREATE INDEX idx_usuario_carrera_carrera ON usuario_carrera(carrera_id);

CREATE TABLE periodo_academico (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo       VARCHAR(20)  NOT NULL UNIQUE,
    nombre       VARCHAR(100) NOT NULL,
    fecha_inicio DATE         NOT NULL,
    fecha_fin    DATE         NOT NULL,
    activo       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE estudiante (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id        BIGINT      NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE RESTRICT,
    carrera_id        BIGINT      NOT NULL REFERENCES carrera(id)        ON DELETE RESTRICT,
    codigo_estudiante VARCHAR(30) UNIQUE,
    fecha_nacimiento  DATE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_estudiante_carrera ON estudiante(carrera_id);

CREATE TABLE docente (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE RESTRICT,
    codigo_docente VARCHAR(20) UNIQUE,
    dedicacion     VARCHAR(50),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE empresa (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruc              VARCHAR(13)  NOT NULL UNIQUE,
    razon_social     VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    direccion        VARCHAR(250),
    telefono         VARCHAR(20),
    correo           VARCHAR(120),
    activa           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE tutor_empresarial (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT      NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE RESTRICT,
    empresa_id BIGINT      NOT NULL REFERENCES empresa(id)        ON DELETE RESTRICT,
    cargo      VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tutor_empresa UNIQUE (id, empresa_id)
);
CREATE INDEX idx_tutor_empresa ON tutor_empresarial(empresa_id);





CREATE TABLE requisito (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT,
    obligatorio BOOLEAN      NOT NULL DEFAULT TRUE,
    carrera_id  BIGINT       REFERENCES carrera(id) ON DELETE RESTRICT,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_requisito_nombre_carrera
        UNIQUE NULLS NOT DISTINCT (nombre, carrera_id)
);
CREATE INDEX idx_requisito_carrera ON requisito(carrera_id) WHERE carrera_id IS NOT NULL;

CREATE TABLE requisito_estudiante (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estudiante_id      BIGINT      NOT NULL REFERENCES estudiante(id)        ON DELETE RESTRICT,
    requisito_id       BIGINT      NOT NULL REFERENCES requisito(id)         ON DELETE RESTRICT,
    periodo_id         BIGINT      NOT NULL REFERENCES periodo_academico(id) ON DELETE RESTRICT,
    estado             VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_verificacion TIMESTAMPTZ,
    verificado_por     BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    observacion        TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_req_estudiante_periodo UNIQUE (estudiante_id, requisito_id, periodo_id)
);
CREATE INDEX idx_req_estudiante_estado  ON requisito_estudiante(estudiante_id, estado);
CREATE INDEX idx_req_estudiante_req     ON requisito_estudiante(requisito_id);
CREATE INDEX idx_req_estudiante_periodo ON requisito_estudiante(periodo_id);





CREATE TABLE convenio (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id    BIGINT      NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT,
    numero        VARCHAR(50) UNIQUE,
    fecha_inicio  DATE        NOT NULL,
    fecha_fin     DATE        NOT NULL,
    estado        VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
    documento_url VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_convenio_empresa UNIQUE (id, empresa_id)
);
CREATE INDEX idx_convenio_empresa_estado ON convenio(empresa_id, estado);

CREATE OR REPLACE VIEW v_convenio_estado AS
SELECT c.id, c.empresa_id, c.numero, c.fecha_inicio, c.fecha_fin, c.estado,
       CASE
         WHEN c.estado <> 'VIGENTE' THEN c.estado
         WHEN c.fecha_fin < (NOW() AT TIME ZONE 'America/Guayaquil')::date THEN 'VENCIDO'
         ELSE 'VIGENTE'
       END AS estado_efectivo
  FROM convenio c;

CREATE TABLE tipo_practica (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre           VARCHAR(80)  NOT NULL UNIQUE,
    horas_requeridas NUMERIC(6,2) NOT NULL,
    nota_minima      NUMERIC(4,2) NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);





CREATE TABLE empresa_usuario (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id  BIGINT      NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    rol_empresa VARCHAR(20) NOT NULL DEFAULT 'PUBLICADOR',
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_empresa_usuario UNIQUE (empresa_id, usuario_id)
);
CREATE INDEX idx_empresa_usuario_usuario ON empresa_usuario(usuario_id);

CREATE TABLE oferta_practica (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id                BIGINT       NOT NULL REFERENCES empresa(id)           ON DELETE RESTRICT,
    convenio_id               BIGINT       NOT NULL,
    tipo_practica_id          BIGINT       NOT NULL REFERENCES tipo_practica(id)     ON DELETE RESTRICT,
    periodo_id                BIGINT       NOT NULL REFERENCES periodo_academico(id) ON DELETE RESTRICT,
    titulo        VARCHAR(200) NOT NULL,
    descripcion               TEXT,
    fecha_inicio_actividades  DATE         NOT NULL,
    fecha_fin_actividades     DATE         NOT NULL,
    fecha_cierre              DATE         NOT NULL,
    horas_requeridas_snapshot NUMERIC(6,2),
    nota_minima_snapshot      NUMERIC(4,2),
    fecha_primera_publicacion TIMESTAMPTZ,
    fecha_publicacion         TIMESTAMPTZ,
    estado        VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    creada_por_usuario_id     BIGINT       NOT NULL,
    resuelta_por_usuario_id   BIGINT       REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_resolucion          TIMESTAMPTZ,
    motivo_decision           TEXT,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_oferta_convenio_empresa
        FOREIGN KEY (convenio_id, empresa_id)
        REFERENCES convenio(id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_oferta_creador_empresa
        FOREIGN KEY (empresa_id, creada_por_usuario_id)
        REFERENCES empresa_usuario(empresa_id, usuario_id) ON DELETE RESTRICT
);
CREATE INDEX idx_oferta_empresa_estado ON oferta_practica(empresa_id, estado);
CREATE INDEX idx_oferta_estado_cierre  ON oferta_practica(estado, fecha_cierre);
CREATE INDEX idx_oferta_convenio       ON oferta_practica(convenio_id, empresa_id);
CREATE INDEX idx_oferta_periodo        ON oferta_practica(periodo_id);
CREATE INDEX idx_oferta_tipo           ON oferta_practica(tipo_practica_id);
CREATE INDEX idx_oferta_creador        ON oferta_practica(creada_por_usuario_id);

CREATE TABLE oferta_estado_historial (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oferta_id       BIGINT      NOT NULL REFERENCES oferta_practica(id) ON DELETE RESTRICT,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20) NOT NULL,
    usuario_id      BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    es_sistema      BOOLEAN     NOT NULL DEFAULT FALSE,
    motivo          TEXT,
    fecha           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ofhist_oferta_fecha ON oferta_estado_historial(oferta_id, fecha DESC);

CREATE TABLE oferta_perfil (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oferta_id   BIGINT       NOT NULL REFERENCES oferta_practica(id) ON DELETE RESTRICT,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT,
    plazas      INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_perfil_oferta_nombre UNIQUE (oferta_id, nombre)
);
CREATE INDEX idx_perfil_oferta ON oferta_perfil(oferta_id);

CREATE TABLE oferta_perfil_carrera (
    perfil_id  BIGINT      NOT NULL REFERENCES oferta_perfil(id) ON DELETE CASCADE,
    carrera_id BIGINT      NOT NULL REFERENCES carrera(id)       ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (perfil_id, carrera_id)
);
CREATE INDEX idx_perfil_carrera_carrera ON oferta_perfil_carrera(carrera_id);

CREATE TABLE oferta_perfil_requisito (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    perfil_id   BIGINT      NOT NULL REFERENCES oferta_perfil(id) ON DELETE CASCADE,
    descripcion TEXT        NOT NULL,
    obligatorio BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_perfil_req_perfil ON oferta_perfil_requisito(perfil_id);

CREATE TABLE oferta_perfil_cambio_historial (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oferta_id   BIGINT REFERENCES oferta_practica(id) ON DELETE RESTRICT,
    perfil_id   BIGINT,
    tabla       VARCHAR(40) NOT NULL,
    operacion   VARCHAR(10) NOT NULL,
    registro_id BIGINT,
    antes       JSONB,
    despues     JSONB,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_oferta_perfil_hist_oferta_fecha
    ON oferta_perfil_cambio_historial(oferta_id, fecha DESC);





CREATE TABLE postulacion (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    perfil_id         BIGINT      NOT NULL REFERENCES oferta_perfil(id) ON DELETE RESTRICT,
    estudiante_id     BIGINT      NOT NULL REFERENCES estudiante(id)    ON DELETE RESTRICT,
    estado            VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    mensaje           TEXT,
    fecha_postulacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    recomendacion_empresa           VARCHAR(20) NOT NULL DEFAULT 'SIN_EVALUAR',
    observacion_empresa             TEXT,
    evaluada_por_empresa_usuario_id BIGINT REFERENCES empresa_usuario(id) ON DELETE RESTRICT,
    fecha_evaluacion_empresa        TIMESTAMPTZ,

    resuelta_por      BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_resolucion  TIMESTAMPTZ,
    motivo_decision   TEXT,

    cancelada_por     BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_cancelacion TIMESTAMPTZ,

    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_postulacion_perfil_estado ON postulacion(perfil_id, estado);
CREATE INDEX idx_postulacion_estudiante    ON postulacion(estudiante_id, estado);

CREATE TABLE postulacion_recomendacion_historial (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    postulacion_id      BIGINT      NOT NULL REFERENCES postulacion(id)     ON DELETE RESTRICT,
    recomendacion       VARCHAR(20) NOT NULL,
    observacion         TEXT,
    empresa_usuario_id  BIGINT      NOT NULL REFERENCES empresa_usuario(id) ON DELETE RESTRICT,
    usuario_id          BIGINT      NOT NULL REFERENCES usuario(id)         ON DELETE RESTRICT,
    fecha               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_posthist_postulacion ON postulacion_recomendacion_historial(postulacion_id, fecha DESC);

CREATE OR REPLACE VIEW v_oferta_perfil_cupo AS
SELECT p.id AS perfil_id, p.oferta_id, p.nombre, p.plazas,
       COUNT(po.id) FILTER (WHERE po.estado = 'ACEPTADA')  AS aceptadas,
       COUNT(po.id) FILTER (WHERE po.estado = 'PENDIENTE') AS pendientes,
       COUNT(po.id) FILTER (WHERE po.estado = 'RECHAZADA') AS rechazadas,
       COUNT(po.id) FILTER (WHERE po.estado = 'RETIRADA')  AS retiradas,
       p.plazas - COUNT(po.id) FILTER (WHERE po.estado = 'ACEPTADA') AS disponibles,
       (COUNT(po.id) FILTER (WHERE po.estado = 'ACEPTADA') >= p.plazas) AS perfil_cerrado
  FROM oferta_perfil p
  LEFT JOIN postulacion po ON po.perfil_id = p.id
 GROUP BY p.id, p.oferta_id, p.nombre, p.plazas;





CREATE TABLE practica (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estudiante_id             BIGINT       NOT NULL REFERENCES estudiante(id)        ON DELETE RESTRICT,
    carrera_id                BIGINT       NOT NULL REFERENCES carrera(id)           ON DELETE RESTRICT,
    empresa_id                BIGINT       NOT NULL REFERENCES empresa(id)           ON DELETE RESTRICT,
    convenio_id               BIGINT       NOT NULL,
    tipo_practica_id          BIGINT       NOT NULL REFERENCES tipo_practica(id)     ON DELETE RESTRICT,
    periodo_id                BIGINT       NOT NULL REFERENCES periodo_academico(id) ON DELETE RESTRICT,
    docente_id                BIGINT       REFERENCES docente(id) ON DELETE RESTRICT,
    tutor_empresarial_id      BIGINT,
    postulacion_id            BIGINT       UNIQUE REFERENCES postulacion(id) ON DELETE RESTRICT,
    estado        VARCHAR(30)  NOT NULL DEFAULT 'POSTULADA',
    horas_requeridas_snapshot NUMERIC(6,2) NOT NULL,
    nota_minima_snapshot      NUMERIC(4,2) NOT NULL,
    fecha_postulacion         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_aprobacion          TIMESTAMPTZ,
    fecha_inicio              DATE,
    fecha_fin                 DATE,
    nota_final                NUMERIC(4,2),
    observaciones             TEXT,
    creado_por                BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    version                   BIGINT       NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_practica_convenio_empresa
        FOREIGN KEY (convenio_id, empresa_id)
        REFERENCES convenio(id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_practica_tutor_empresa
        FOREIGN KEY (tutor_empresarial_id, empresa_id)
        REFERENCES tutor_empresarial(id, empresa_id) ON DELETE RESTRICT
);

CREATE INDEX idx_practica_estudiante_periodo ON practica(estudiante_id, periodo_id);
CREATE INDEX idx_practica_estado_periodo     ON practica(estado, periodo_id);
CREATE INDEX idx_practica_carrera_estado     ON practica(carrera_id, estado);
CREATE INDEX idx_practica_docente            ON practica(docente_id)           WHERE docente_id IS NOT NULL;
CREATE INDEX idx_practica_tutor              ON practica(tutor_empresarial_id) WHERE tutor_empresarial_id IS NOT NULL;
CREATE INDEX idx_practica_empresa            ON practica(empresa_id);
CREATE INDEX idx_practica_periodo            ON practica(periodo_id);
CREATE INDEX idx_practica_tipo               ON practica(tipo_practica_id);
CREATE INDEX idx_practica_convenio           ON practica(convenio_id, empresa_id);
CREATE INDEX idx_practica_creado_por         ON practica(creado_por);

CREATE TABLE practica_estado_historial (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id     BIGINT      NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    estado_anterior VARCHAR(30),
    estado_nuevo    VARCHAR(30) NOT NULL,
    usuario_id      BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    motivo          TEXT,
    fecha           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_hist_practica_fecha ON practica_estado_historial(practica_id, fecha DESC);

CREATE TABLE actividad_practica (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id    BIGINT      NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    fecha          DATE        NOT NULL,
    descripcion    TEXT        NOT NULL,
    registrada_por BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_actividad_practica ON actividad_practica(practica_id, fecha);

CREATE TABLE registro_horas (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id      BIGINT       NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    fecha            DATE         NOT NULL,
    horas            NUMERIC(4,2) NOT NULL,
    descripcion      TEXT,
    validado         BOOLEAN      NOT NULL DEFAULT FALSE,
    validado_por     BIGINT       REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_validacion TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_registro_horas_dia    UNIQUE (practica_id, fecha)
);

CREATE TABLE informe (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id    BIGINT       NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    tipo           VARCHAR(20)  NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'ENTREGADO',
    nombre_archivo VARCHAR(255) NOT NULL,
    nombre_interno VARCHAR(255) NOT NULL UNIQUE,
    content_type   VARCHAR(60)  NOT NULL DEFAULT 'application/pdf',
    subido_por     BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_subida   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    observaciones  TEXT,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_informe_practica_estado ON informe(practica_id, estado);
CREATE INDEX idx_informe_subido_por      ON informe(subido_por);

CREATE TABLE informe_archivo (
    informe_id   BIGINT      PRIMARY KEY REFERENCES informe(id) ON DELETE CASCADE,
    contenido    BYTEA       NOT NULL,
    tamano_bytes INT         NOT NULL,
    sha256       VARCHAR(64) NOT NULL
);





CREATE TABLE evaluacion (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id          BIGINT       NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    evaluador_usuario_id BIGINT       NOT NULL REFERENCES usuario(id)  ON DELETE RESTRICT,
    rol_evaluador        VARCHAR(30)  NOT NULL,
    criterios            JSONB,
    nota                 NUMERIC(4,2) NOT NULL,
    observacion          TEXT,
    fecha_evaluacion     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_eval_practica_rol UNIQUE (practica_id, rol_evaluador)
);
CREATE INDEX idx_eval_evaluador ON evaluacion(evaluador_usuario_id);

CREATE TABLE visita_seguimiento (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id   BIGINT      NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    docente_id    BIGINT      NOT NULL REFERENCES docente(id)  ON DELETE RESTRICT,
    fecha         DATE        NOT NULL,
    observaciones TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_visita_practica_fecha ON visita_seguimiento(practica_id, fecha);
CREATE INDEX idx_visita_docente        ON visita_seguimiento(docente_id);

CREATE TABLE incidencia (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id      BIGINT      NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    reportada_por    BIGINT      NOT NULL REFERENCES usuario(id)  ON DELETE RESTRICT,
    descripcion      TEXT        NOT NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    resuelta_por     BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_resolucion TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_incidencia_practica_estado ON incidencia(practica_id, estado);

CREATE TABLE certificado (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id         BIGINT      NOT NULL REFERENCES practica(id) ON DELETE RESTRICT,
    codigo_verificacion UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    datos_snapshot      JSONB       NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
    fecha_emision       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    emitido_por         BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    motivo_anulacion    TEXT,
    fecha_anulacion     TIMESTAMPTZ,
    anulado_por         BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auditoria (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    accion     VARCHAR(50) NOT NULL,
    entidad    VARCHAR(50) NOT NULL,
    entidad_id BIGINT,
    detalle    JSONB,
    fecha      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_auditoria_usuario_fecha ON auditoria(usuario_id, fecha DESC);
CREATE INDEX idx_auditoria_entidad       ON auditoria(entidad, entidad_id);







INSERT INTO rol (codigo, nombre, descripcion) VALUES
('ESTUDIANTE',        'Estudiante',                'Postula, registra horas e informes'),
('DOCENTE',           'Docente / Tutor académico', 'Evalúa, aprueba informes, registra visitas'),
('TUTOR_EMPRESARIAL', 'Tutor empresarial',         'Valida horas, evalúa desempeño'),
('COORDINADOR',       'Coordinador de prácticas',  'Gestiona convenios, requisitos y certificados'),
('ADMINISTRADOR',     'Administrador',             'Gestiona usuarios, roles y catálogos'),
('AUTORIDAD',         'Autoridad académica',       'Solo lectura: reportes y estadísticas'),
('REPRESENTANTE_EMPRESA', 'Representante de empresa',
 'Publica y administra convocatorias de su empresa; recomienda postulantes');









CREATE TRIGGER trg_facultad_updated
    BEFORE UPDATE ON facultad
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_carrera_updated
    BEFORE UPDATE ON carrera
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_periodo_academico_updated
    BEFORE UPDATE ON periodo_academico
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_estudiante_updated
    BEFORE UPDATE ON estudiante
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_docente_updated
    BEFORE UPDATE ON docente
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_empresa_updated
    BEFORE UPDATE ON empresa
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_tutor_empresarial_updated
    BEFORE UPDATE ON tutor_empresarial
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_convenio_updated
    BEFORE UPDATE ON convenio
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_tipo_practica_updated
    BEFORE UPDATE ON tipo_practica
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_empresa_usuario_updated
    BEFORE UPDATE ON empresa_usuario
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;
