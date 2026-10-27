-- V6: avaliacao de uma ordem concluida (no maximo uma por ordem).
CREATE TABLE reviews (
    id           BIGSERIAL  PRIMARY KEY,
    request_id   BIGINT     NOT NULL,
    reviewer_id  BIGINT     NOT NULL,
    rating       SMALLINT   NOT NULL,
    comment      TEXT,
    created_at   TIMESTAMP  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_reviews_request   UNIQUE (request_id),
    CONSTRAINT fk_reviews_request   FOREIGN KEY (request_id)  REFERENCES service_requests (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_reviewer  FOREIGN KEY (reviewer_id) REFERENCES users (id)            ON DELETE RESTRICT,
    CONSTRAINT chk_reviews_rating   CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_reviewer ON reviews (reviewer_id);
