CREATE TABLE services (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    price       NUMERIC(10,2) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    provider_id BIGINT NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_services_provider ON services(provider_id);
CREATE INDEX idx_services_category ON services(category);
