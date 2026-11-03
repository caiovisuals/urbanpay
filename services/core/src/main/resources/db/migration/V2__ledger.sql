-- Módulo ledger: razão contábil de partidas dobradas.
-- Valores monetários SEMPRE em centavos (BIGINT). Nunca ponto flutuante.

CREATE TABLE accounts (
    id            UUID        PRIMARY KEY,
    owner_id      UUID,
    kind          VARCHAR(20) NOT NULL,
    code          VARCHAR(40),
    currency      VARCHAR(3)  NOT NULL DEFAULT 'BRL',
    balance_cents BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT      NOT NULL,
    CONSTRAINT ck_accounts_kind CHECK (kind IN ('CUSTOMER', 'SYSTEM')),
    -- Última linha de defesa: conta de cliente nunca fica negativa,
    -- mesmo que algum bug passe pela validação da aplicação.
    CONSTRAINT ck_accounts_customer_non_negative CHECK (kind <> 'CUSTOMER' OR balance_cents >= 0),
    CONSTRAINT ck_accounts_owner CHECK ((kind = 'CUSTOMER') = (owner_id IS NOT NULL)),
    CONSTRAINT uk_accounts_code UNIQUE (code)
);

-- Uma conta corrente por cliente nesta fase (subcontas, como porquinhos, virão depois).
CREATE UNIQUE INDEX uk_accounts_customer_owner ON accounts (owner_id) WHERE kind = 'CUSTOMER';

CREATE TABLE journal_entries (
    id           UUID         PRIMARY KEY,
    type         VARCHAR(30)  NOT NULL,
    description  VARCHAR(140),
    initiated_by UUID,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE postings (
    id                  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entry_id            UUID        NOT NULL REFERENCES journal_entries (id),
    account_id          UUID        NOT NULL REFERENCES accounts (id),
    amount_cents        BIGINT      NOT NULL,
    balance_after_cents BIGINT      NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_postings_amount_non_zero CHECK (amount_cents <> 0)
);

CREATE INDEX ix_postings_account ON postings (account_id, id DESC);
CREATE INDEX ix_postings_entry   ON postings (entry_id);

-- Chaves de idempotência: (dono, chave) -> recurso criado.
CREATE TABLE idempotency_keys (
    owner_id     UUID         NOT NULL,
    idem_key     VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64)  NOT NULL,
    resource_id  UUID,
    created_at   TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (owner_id, idem_key)
);

-- O ledger é append-only: lançamentos nunca são alterados nem apagados.
-- Correções são feitas com um novo lançamento de estorno.
CREATE FUNCTION ledger_forbid_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ledger is append-only: % on % is not allowed', TG_OP, TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_postings_append_only
    BEFORE UPDATE OR DELETE ON postings
    FOR EACH ROW EXECUTE FUNCTION ledger_forbid_mutation();

CREATE TRIGGER trg_journal_entries_append_only
    BEFORE UPDATE OR DELETE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_forbid_mutation();

-- Invariante das partidas dobradas, verificada no COMMIT:
-- todo lançamento tem pelo menos 2 pernas e a soma delas é zero.
CREATE FUNCTION ledger_check_entry_balanced() RETURNS trigger AS $$
DECLARE
    total BIGINT;
    legs  INTEGER;
BEGIN
    SELECT COALESCE(SUM(amount_cents), 0), COUNT(*) INTO total, legs
      FROM postings WHERE entry_id = NEW.entry_id;
    IF legs < 2 OR total <> 0 THEN
        RAISE EXCEPTION 'journal entry % is unbalanced (legs=%, sum=%)', NEW.entry_id, legs, total
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_postings_balanced
    AFTER INSERT ON postings
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ledger_check_entry_balanced();

-- Conta de sistema que representa o dinheiro entrando no banco (depósitos, aportes).
-- Ela pode ficar negativa: seu saldo negativo = total de dinheiro emitido para clientes.
INSERT INTO accounts (id, owner_id, kind, code, currency, balance_cents, created_at, version)
VALUES ('00000000-0000-7000-8000-000000000001', NULL, 'SYSTEM', 'TREASURY', 'BRL', 0, now(), 0);
