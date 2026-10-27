-- V3: leva a tabela users ao estado final do servicehub_schema.sql
-- (autenticacao, papel, e-mail normalizado, constraints).

-- Guarda: aborta se normalizar e-mails para minusculas gerar duplicados.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users GROUP BY LOWER(email) HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'E-mails duplicados ao normalizar para minusculas. Resolva (mescle/renomeie) antes de migrar.';
    END IF;
END $$;

ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN role          VARCHAR(20) NOT NULL DEFAULT 'CLIENT';
ALTER TABLE users ADD COLUMN bio           TEXT;
ALTER TABLE users ADD COLUMN avatar_url    VARCHAR(500);

-- Usuarios legados nao tinham senha: marcador que nunca casa com BCrypt (exige redefinicao de senha).
UPDATE users SET password_hash = '{legacy-sem-senha}' WHERE password_hash IS NULL;
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;

-- Quem ja oferece servicos e' PROVIDER.
UPDATE users SET role = 'PROVIDER' WHERE id IN (SELECT DISTINCT provider_id FROM services);

UPDATE users SET email = LOWER(email);
ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(255);

UPDATE users SET is_active  = TRUE  WHERE is_active  IS NULL;
UPDATE users SET created_at = NOW() WHERE created_at IS NULL;
UPDATE users SET updated_at = NOW() WHERE updated_at IS NULL;
ALTER TABLE users ALTER COLUMN is_active  SET NOT NULL;
ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE users RENAME CONSTRAINT users_email_key TO uk_users_email;
ALTER TABLE users ADD CONSTRAINT chk_users_email_low CHECK (email = LOWER(email));
ALTER TABLE users ADD CONSTRAINT chk_users_role      CHECK (role IN ('ADMIN', 'PROVIDER', 'CLIENT'));

-- idx_users_email era redundante com a UNIQUE (uk_users_email ja cria indice): superindexacao.
DROP INDEX IF EXISTS idx_users_email;

COMMENT ON TABLE  users               IS 'Usuários da plataforma. Papel único por usuário.';
COMMENT ON COLUMN users.password_hash IS 'Hash BCrypt (60 caracteres). Nunca sai em respostas da API.';
COMMENT ON COLUMN users.is_active     IS 'FALSE = usuário excluído logicamente: não faz login e o JWT deixa de valer.';
