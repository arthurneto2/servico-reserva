-- =====================================================================
-- DDL do mini projeto Reservas de Salas
-- Executado automaticamente na primeira subida do container Postgres.
-- As entidades JPA (pacote output.persistence) DEVEM refletir este schema:
-- a aplicação sobe com spring.jpa.hibernate.ddl-auto=validate.
-- =====================================================================

CREATE TABLE sala (
    id              UUID PRIMARY KEY,
    nome            VARCHAR(100)  NOT NULL UNIQUE,
    capacidade      INTEGER       NOT NULL CHECK (capacidade > 0),
    fuso_horario    VARCHAR(60)   NOT NULL,   -- ID IANA, ex.: America/Sao_Paulo
    ativa           BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE reserva (
    id                        UUID PRIMARY KEY,
    sala_id                   UUID          NOT NULL REFERENCES sala (id),
    organizador_email         VARCHAR(254)  NOT NULL,
    inicio                    TIMESTAMPTZ   NOT NULL,
    fim                       TIMESTAMPTZ   NOT NULL,
    quantidade_participantes  INTEGER       NOT NULL CHECK (quantidade_participantes > 0),
    status                    VARCHAR(20)   NOT NULL
                              CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'CANCELADA', 'EXPIRADA')),
    criada_em                 TIMESTAMPTZ   NOT NULL,
    CONSTRAINT reserva_periodo_valido CHECK (fim > inicio)
);

CREATE INDEX idx_reserva_sala_inicio ON reserva (sala_id, inicio);
CREATE INDEX idx_reserva_organizador ON reserva (organizador_email);

-- ---------------------------------------------------------------------
-- Carga inicial: três salas em fusos diferentes (por isso o relógio é um
-- port que recebe o fuso da sala).
-- ---------------------------------------------------------------------
INSERT INTO sala (id, nome, capacidade, fuso_horario, ativa) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Sala Pirapora',  8, 'America/Sao_Paulo', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'Sala Londres',   4, 'Europe/London',     TRUE),
    ('33333333-3333-3333-3333-333333333333', 'Sala Nova York', 12, 'America/New_York',  TRUE),
    ('44444444-4444-4444-4444-444444444444', 'Sala Desativada', 6, 'America/Sao_Paulo', FALSE);
