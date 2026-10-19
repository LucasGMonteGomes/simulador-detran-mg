-- Rastreabilidade das questões: quem escreveu, se já foi revisada e de qual trecho da fonte ela saiu.

ALTER TABLE questao
    -- Os DEFAULTs preenchem linhas já existentes; o importador sempre informa os valores explicitamente.
    ADD COLUMN origem       VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
        CHECK (origem IN ('MANUAL', 'IA')),
    ADD COLUMN status       VARCHAR(10) NOT NULL DEFAULT 'RASCUNHO'
        CHECK (status IN ('RASCUNHO', 'REVISADA', 'REPORTADA')),
    -- Citação literal da fonte (ex.: texto do artigo do CTB) que sustenta o gabarito.
    ADD COLUMN trecho_fonte TEXT;

-- O Modo Prova sorteia só questões ativas e revisadas de cada subtema.
CREATE INDEX idx_questao_sorteio ON questao (subtema_id) WHERE ativa AND status = 'REVISADA';
