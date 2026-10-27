-- V4: leva a tabela services ao estado final (categorias, preco, FK RESTRICT, indice parcial).

-- Mapeia as categorias antigas para o novo conjunto.
UPDATE services SET category = CASE category
    WHEN 'MANUTENCAO'  THEN 'OUTROS'
    WHEN 'TECNOLOGIA'  THEN 'SUPORTE_TECNICO'
    WHEN 'BELEZA'      THEN 'OUTROS'
    WHEN 'CONSULTORIA' THEN 'CONSULTORIA_TI'
    WHEN 'EDUCACAO'    THEN 'OUTROS'
    ELSE category END;

-- Guarda: precos fora da regra devem ser corrigidos manualmente.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM services WHERE price <= 0) THEN
        RAISE EXCEPTION 'Existem servicos com price <= 0. Corrija antes de migrar.';
    END IF;
END $$;

UPDATE services SET is_active  = TRUE  WHERE is_active  IS NULL;
UPDATE services SET created_at = NOW() WHERE created_at IS NULL;
UPDATE services SET updated_at = NOW() WHERE updated_at IS NULL;
ALTER TABLE services ALTER COLUMN is_active  SET NOT NULL;
ALTER TABLE services ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE services ALTER COLUMN updated_at SET NOT NULL;

-- ON DELETE CASCADE -> RESTRICT (exclusao e' logica).
ALTER TABLE services DROP CONSTRAINT services_provider_id_fkey;
ALTER TABLE services ADD CONSTRAINT fk_services_provider
    FOREIGN KEY (provider_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE services ADD CONSTRAINT chk_services_price CHECK (price > 0);
ALTER TABLE services ADD CONSTRAINT chk_services_category CHECK (category IN (
    'LIMPEZA', 'DIARISTA', 'ENCANAMENTO', 'ELETRICA', 'JARDINAGEM', 'PINTURA',
    'MARCENARIA', 'PEDREIRO', 'DEDETIZACAO', 'AR_CONDICIONADO', 'CHAVEIRO',
    'MONTAGEM_MOVEIS', 'MUDANCA', 'SUPORTE_TECNICO', 'MANUTENCAO_COMPUTADORES',
    'DESENVOLVIMENTO_SOFTWARE', 'REDES_E_INFRAESTRUTURA', 'SEGURANCA_DIGITAL',
    'RECUPERACAO_DE_DADOS', 'CONSULTORIA_TI', 'OUTROS'));

-- Indice de categoria passa a ser parcial (somente servicos ativos).
DROP INDEX IF EXISTS idx_services_category;
CREATE INDEX idx_services_category ON services (category) WHERE is_active;

COMMENT ON COLUMN services.category IS 'Mesmo conjunto do enum Java ServiceCategory (EnumType.STRING). Nova categoria = nova migração.';
