-- Esquema inicial del sistema de tickets.
-- Los CHECK replican los enums de Java: si alguien inserta por fuera de la
-- app, la base de datos igual protege la integridad.

CREATE TABLE usuarios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    correo          VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'SOPORTE', 'USUARIO'))
);

-- Único sin importar mayúsculas: "Laura@empresa.com" y "laura@empresa.com"
-- son la misma persona.
CREATE UNIQUE INDEX ux_usuarios_correo ON usuarios (LOWER(correo));

CREATE TABLE tickets (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo                  VARCHAR(100)  NOT NULL,
    descripcion             VARCHAR(2000) NOT NULL,
    categoria               VARCHAR(20)   NOT NULL,
    prioridad               VARCHAR(20)   NOT NULL,
    estado                  VARCHAR(20)   NOT NULL DEFAULT 'ABIERTO',
    usuario_solicitante_id  BIGINT        NOT NULL,
    tecnico_asignado_id     BIGINT,
    resumen_ia              VARCHAR(300),
    origen_analisis         VARCHAR(10)   NOT NULL,
    fecha_creacion          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion       TIMESTAMP,

    CONSTRAINT fk_tickets_solicitante FOREIGN KEY (usuario_solicitante_id) REFERENCES usuarios (id),
    CONSTRAINT fk_tickets_tecnico     FOREIGN KEY (tecnico_asignado_id)    REFERENCES usuarios (id),

    CONSTRAINT ck_tickets_titulo      CHECK (char_length(titulo) BETWEEN 5 AND 100),
    CONSTRAINT ck_tickets_descripcion CHECK (char_length(descripcion) BETWEEN 20 AND 2000),
    CONSTRAINT ck_tickets_categoria   CHECK (categoria IN ('HARDWARE', 'SOFTWARE', 'CONECTIVIDAD', 'ACCESOS', 'OTRO')),
    CONSTRAINT ck_tickets_prioridad   CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_tickets_estado      CHECK (estado IN ('ABIERTO', 'EN_PROCESO', 'RESUELTO', 'CERRADO')),
    CONSTRAINT ck_tickets_origen      CHECK (origen_analisis IN ('IA', 'REGLAS'))
);

-- Índices pensados para los filtros de GET /api/tickets.
-- Son parciales: los tickets eliminados nunca se consultan.
CREATE INDEX ix_tickets_estado      ON tickets (estado)                 WHERE fecha_eliminacion IS NULL;
CREATE INDEX ix_tickets_prioridad   ON tickets (prioridad)              WHERE fecha_eliminacion IS NULL;
CREATE INDEX ix_tickets_categoria   ON tickets (categoria)              WHERE fecha_eliminacion IS NULL;
CREATE INDEX ix_tickets_solicitante ON tickets (usuario_solicitante_id) WHERE fecha_eliminacion IS NULL;
CREATE INDEX ix_tickets_tecnico     ON tickets (tecnico_asignado_id)    WHERE fecha_eliminacion IS NULL;

CREATE TABLE ticket_etiquetas (
    ticket_id  BIGINT      NOT NULL,
    etiqueta   VARCHAR(40) NOT NULL,

    CONSTRAINT fk_ticket_etiquetas_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE
);

CREATE INDEX ix_ticket_etiquetas_ticket ON ticket_etiquetas (ticket_id);
