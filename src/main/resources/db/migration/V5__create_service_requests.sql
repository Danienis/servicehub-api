-- V5: ordens de servico (um usuario contrata um servico).
CREATE TABLE service_requests (
    id            BIGSERIAL      PRIMARY KEY,
    service_id    BIGINT         NOT NULL,
    client_id     BIGINT         NOT NULL,
    status        VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    scheduled_at  TIMESTAMP,
    notes         TEXT,
    total_price   NUMERIC(10,2)  NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_requests_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE RESTRICT,
    CONSTRAINT fk_requests_client  FOREIGN KEY (client_id)  REFERENCES users (id)    ON DELETE RESTRICT,
    CONSTRAINT chk_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_requests_price  CHECK (total_price > 0)
);

CREATE INDEX idx_requests_service ON service_requests (service_id);
CREATE INDEX idx_requests_client  ON service_requests (client_id);
CREATE INDEX idx_requests_status  ON service_requests (status);

COMMENT ON COLUMN service_requests.total_price IS 'Snapshot de services.price na criação. Mudar o preço do serviço não altera ordens existentes.';
COMMENT ON COLUMN service_requests.status      IS 'Máquina de estados validada na camada de serviço; o CHECK só garante valores conhecidos.';
