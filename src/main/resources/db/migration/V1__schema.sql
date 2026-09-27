-- Схема базы данных системы обработки внутренних заявок сотрудников.
-- Справочники (условно-постоянная информация) и оперативные таблицы.

-- ---------- справочники ----------
CREATE TABLE department (
    id          SERIAL PRIMARY KEY,
    code        CHAR(5)      NOT NULL UNIQUE,          -- разрядный иерархический код
    name        VARCHAR(150) NOT NULL,
    parent_id   INTEGER REFERENCES department (id),
    head_id     INTEGER                                 -- руководитель; FK добавляется ниже
);

CREATE TABLE support_group (
    id    SERIAL PRIMARY KEY,
    code  VARCHAR(10)  NOT NULL UNIQUE,
    name  VARCHAR(100) NOT NULL
);

CREATE TABLE employee (
    id               SERIAL PRIMARY KEY,
    personnel_no     CHAR(6)      NOT NULL UNIQUE,      -- код работника из кадровой системы
    full_name        VARCHAR(150) NOT NULL,
    email            VARCHAR(120) NOT NULL UNIQUE,
    position         VARCHAR(120) NOT NULL,
    department_id    INTEGER      NOT NULL REFERENCES department (id),
    support_group_id INTEGER REFERENCES support_group (id),
    role             VARCHAR(16)  NOT NULL
        CHECK (role IN ('EMPLOYEE', 'SUPPORT', 'SECURITY', 'ADMIN')),
    password_hash    VARCHAR(100),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    dismissed_at     TIMESTAMP
);

ALTER TABLE department
    ADD CONSTRAINT fk_department_head FOREIGN KEY (head_id) REFERENCES employee (id);

CREATE TABLE ticket_status (
    code      CHAR(2)     PRIMARY KEY,
    name      VARCHAR(40) NOT NULL,
    is_final  BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE ticket_priority (
    code        SMALLINT     PRIMARY KEY,
    name        VARCHAR(30)  NOT NULL,
    sla_factor  NUMERIC(4,2) NOT NULL                    -- множитель норматива срока
);

CREATE TABLE ticket_type (
    id                SERIAL PRIMARY KEY,
    code              CHAR(3)      NOT NULL UNIQUE,
    name              VARCHAR(150) NOT NULL,
    category          VARCHAR(10)  NOT NULL CHECK (category IN ('INCIDENT', 'ACCESS')),
    description       VARCHAR(500),
    sla_hours         INTEGER      NOT NULL CHECK (sla_hours > 0),   -- норматив, рабочих часов
    default_priority  SMALLINT     NOT NULL REFERENCES ticket_priority (code),
    needs_approval    BOOLEAN      NOT NULL DEFAULT FALSE,
    contains_pd       BOOLEAN      NOT NULL DEFAULT FALSE,           -- содержит персональные данные
    access_action     VARCHAR(10)  CHECK (access_action IN ('GRANT', 'REVOKE')),
    support_group_id  INTEGER      NOT NULL REFERENCES support_group (id)
);

CREATE TABLE info_resource (
    id               SERIAL PRIMARY KEY,
    code             CHAR(4)      NOT NULL UNIQUE,
    name             VARCHAR(150) NOT NULL,
    owner_id         INTEGER      NOT NULL REFERENCES employee (id),
    payment_contour  BOOLEAN      NOT NULL DEFAULT FALSE             -- требует согласования ИБ
);

CREATE TABLE access_role (
    id           SERIAL PRIMARY KEY,
    resource_id  INTEGER      NOT NULL REFERENCES info_resource (id),
    code         CHAR(4)      NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(300),
    UNIQUE (resource_id, code)
);

-- ---------- оперативные таблицы ----------
CREATE SEQUENCE ticket_number_seq START 1;

CREATE TABLE ticket (
    id                BIGSERIAL PRIMARY KEY,
    number            CHAR(8)      NOT NULL UNIQUE,                  -- сквозной регистрационный номер
    type_id           INTEGER      NOT NULL REFERENCES ticket_type (id),
    status_code       CHAR(2)      NOT NULL REFERENCES ticket_status (code),
    priority_code     SMALLINT     NOT NULL REFERENCES ticket_priority (code),
    author_id         INTEGER      NOT NULL REFERENCES employee (id),
    assignee_id       INTEGER REFERENCES employee (id),
    support_group_id  INTEGER      NOT NULL REFERENCES support_group (id),
    subject           VARCHAR(200) NOT NULL,
    description       TEXT         NOT NULL,
    resource_id       INTEGER REFERENCES info_resource (id),
    access_role_id    INTEGER REFERENCES access_role (id),
    equipment_no      VARCHAR(20),
    created_at        TIMESTAMP    NOT NULL,
    due_at            TIMESTAMP    NOT NULL,                         -- контрольный срок по SLA
    taken_at          TIMESTAMP,
    resolved_at       TIMESTAMP,
    closed_at         TIMESTAMP,
    resolution        TEXT,
    sla_breached      BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_ticket_author   ON ticket (author_id);
CREATE INDEX idx_ticket_assignee ON ticket (assignee_id);
CREATE INDEX idx_ticket_status   ON ticket (status_code);
CREATE INDEX idx_ticket_created  ON ticket (created_at);

CREATE TABLE approval (
    id             BIGSERIAL PRIMARY KEY,
    ticket_id      BIGINT      NOT NULL REFERENCES ticket (id),
    step_no        SMALLINT    NOT NULL,
    approver_id    INTEGER     NOT NULL REFERENCES employee (id),
    approver_kind  VARCHAR(10) NOT NULL CHECK (approver_kind IN ('MANAGER', 'OWNER', 'SECURITY')),
    decision       VARCHAR(10) NOT NULL DEFAULT 'WAITING'
        CHECK (decision IN ('WAITING', 'PENDING', 'APPROVED', 'REJECTED')),
    comment        VARCHAR(500),
    created_at     TIMESTAMP   NOT NULL,
    decided_at     TIMESTAMP,
    UNIQUE (ticket_id, step_no)
);
CREATE INDEX idx_approval_approver ON approval (approver_id, decision);

CREATE TABLE ticket_event (
    id          BIGSERIAL PRIMARY KEY,
    ticket_id   BIGINT       NOT NULL REFERENCES ticket (id),
    actor_id    INTEGER REFERENCES employee (id),                    -- NULL — действие системы
    event_type  VARCHAR(20)  NOT NULL,
    old_status  CHAR(2),
    new_status  CHAR(2),
    message     VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_event_ticket ON ticket_event (ticket_id, created_at);

CREATE TABLE ticket_comment (
    id          BIGSERIAL PRIMARY KEY,
    ticket_id   BIGINT     NOT NULL REFERENCES ticket (id),
    author_id   INTEGER    NOT NULL REFERENCES employee (id),
    body        TEXT       NOT NULL,
    created_at  TIMESTAMP  NOT NULL
);

CREATE TABLE access_grant (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     INTEGER    NOT NULL REFERENCES employee (id),
    access_role_id  INTEGER    NOT NULL REFERENCES access_role (id),
    ticket_id       BIGINT REFERENCES ticket (id),                   -- заявка-основание
    granted_at      TIMESTAMP  NOT NULL,
    granted_by      INTEGER    NOT NULL REFERENCES employee (id),
    revoked_at      TIMESTAMP,
    revoked_by      INTEGER REFERENCES employee (id),
    revoke_reason   VARCHAR(300)
);
-- у работника не может быть двух действующих записей об одном и том же праве
CREATE UNIQUE INDEX uq_active_grant ON access_grant (employee_id, access_role_id)
    WHERE revoked_at IS NULL;
