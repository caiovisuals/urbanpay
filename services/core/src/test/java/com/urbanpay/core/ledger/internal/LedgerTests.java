package com.urbanpay.core.ledger.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.urbanpay.core.ApiTestSupport;
import com.urbanpay.core.shared.DomainException;
import com.urbanpay.core.shared.Money;

class LedgerTests extends ApiTestSupport {

    @Autowired
    TransferService transfers;

    @Autowired
    DepositService deposits;

    @Autowired
    JdbcClient jdbc;

    @Test
    void transferMovesMoneyAndShowsUpInBothStatements() throws Exception {
        var alice = register();
        var bob = register();
        deposit(alice, "100.00");

        transfer(alice, newKey(), accountId(bob), "30.25", PIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value("30.25"));

        assertBalance(alice, "69.75");
        assertBalance(bob, "30.25");

        mvc.perform(get("/api/v1/accounts/me/statement").header("Authorization", alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$.items[0].amount").value("-30.25"))
                .andExpect(jsonPath("$.items[0].balanceAfter").value("69.75"))
                .andExpect(jsonPath("$.items[1].type").value("DEPOSIT"));
    }

    @Test
    void sameIdempotencyKeyNeverTransfersTwice() throws Exception {
        var alice = register();
        var bob = register();
        deposit(alice, "50.00");
        var key = newKey();

        var first = transfer(alice, key, accountId(bob), "10.00", PIN)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andReturn().getResponse().getContentAsString();
        var replay = transfer(alice, key, accountId(bob), "10.00", PIN)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(replay, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        assertBalance(alice, "40.00");

        // Mesma chave com outro corpo é erro do cliente, não uma nova transferência.
        transfer(alice, key, accountId(bob), "99.00", PIN)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"));
    }

    @Test
    void rejectsInsufficientFundsAndAllowsRetryWithSameKey() throws Exception {
        var alice = register();
        var bob = register();
        deposit(alice, "5.00");
        var key = newKey();

        transfer(alice, key, accountId(bob), "5.01", PIN)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("insufficient_funds"));
        assertBalance(alice, "5.00");

        // A falha não "queima" a chave: depois de receber mais dinheiro, a mesma operação passa.
        deposit(alice, "1.00");
        transfer(alice, key, accountId(bob), "5.01", PIN).andExpect(status().isCreated());
        assertBalance(alice, "0.99");
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        var alice = register();
        deposit(alice, "10.00");

        transfer(alice, newKey(), accountId(alice), "1.00", PIN)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("same_account"));
        transfer(alice, newKey(), UUID.randomUUID().toString(), "1.00", PIN)
                .andExpect(status().isNotFound());
        transfer(alice, newKey(), DepositService.TREASURY_ID.toString(), "1.00", PIN)
                .andExpect(status().isNotFound());
        transfer(alice, newKey(), UUID.randomUUID().toString(), "1.001", PIN)
                .andExpect(status().isBadRequest());
        transfer(alice, newKey(), UUID.randomUUID().toString(), "-1.00", PIN)
                .andExpect(status().isBadRequest());
        // Sem Idempotency-Key
        mvc.perform(post("/api/v1/transfers").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toAccountId\":\"%s\",\"amount\":1,\"pin\":\"%s\"}".formatted(UUID.randomUUID(), PIN)))
                .andExpect(status().isBadRequest());
        assertBalance(alice, "10.00");
    }

    @Test
    void locksPinAfterTooManyWrongAttempts() throws Exception {
        var alice = register();
        var bob = register();
        deposit(alice, "10.00");

        for (int i = 0; i < 5; i++) {
            transfer(alice, newKey(), accountId(bob), "1.00", "000001")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("invalid_pin"));
        }
        // Bloqueado: nem o PIN correto passa durante o período de bloqueio.
        transfer(alice, newKey(), accountId(bob), "1.00", PIN)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("pin_locked"));
        assertBalance(alice, "10.00");
    }

    @Test
    void cannotReadSomeoneElsesTransfer() throws Exception {
        var alice = register();
        var bob = register();
        var mallory = register();
        deposit(alice, "10.00");
        var json = transfer(alice, newKey(), accountId(bob), "1.00", PIN)
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.id");

        mvc.perform(get("/api/v1/transfers/" + id).header("Authorization", bob.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/transfers/" + id).header("Authorization", mallory.bearer()))
                .andExpect(status().isNotFound());
    }

    /**
     * 40 transferências de R$ 10 simultâneas saindo de uma conta com R$ 100:
     * exatamente 10 passam, o saldo nunca fica negativo e nenhum centavo some.
     */
    @Test
    void concurrentTransfersNeverOverdraw() throws Exception {
        var alice = register();
        var bob = register();
        deposits.deposit(alice.userId(), newKey(), Money.of(new BigDecimal("100.00")));
        var to = UUID.fromString(accountId(bob));

        var tasks = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < 40; i++) {
            tasks.add(() -> {
                try {
                    transfers.transfer(alice.userId(), newKey(), new TransferRequest(to, new BigDecimal("10.00"), null, PIN));
                    return true;
                } catch (DomainException e) {
                    assertThat(e.code()).isEqualTo("insufficient_funds");
                    return false;
                }
            });
        }
        long succeeded;
        try (var pool = Executors.newFixedThreadPool(8)) {
            succeeded = pool.invokeAll(tasks).stream().filter(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }).count();
        }

        assertThat(succeeded).isEqualTo(10);
        assertBalance(alice, "0.00");
        assertBalance(bob, "100.00");
        assertLedgerInvariants();
    }

    /** A->B e B->A ao mesmo tempo: o lock ordenado por id evita deadlock. */
    @Test
    void opposingConcurrentTransfersDoNotDeadlock() throws Exception {
        var alice = register();
        var bob = register();
        deposits.deposit(alice.userId(), newKey(), Money.of(new BigDecimal("500.00")));
        deposits.deposit(bob.userId(), newKey(), Money.of(new BigDecimal("500.00")));
        var aliceAcc = UUID.fromString(accountId(alice));
        var bobAcc = UUID.fromString(accountId(bob));

        var tasks = new ArrayList<Callable<Object>>();
        for (int i = 0; i < 30; i++) {
            boolean even = i % 2 == 0;
            tasks.add(() -> transfers.transfer(even ? alice.userId() : bob.userId(), newKey(),
                    new TransferRequest(even ? bobAcc : aliceAcc, new BigDecimal("1.00"), null, PIN)));
        }
        try (var pool = Executors.newFixedThreadPool(8)) {
            for (var f : pool.invokeAll(tasks)) {
                f.get();
            }
        }
        assertBalance(alice, "500.00");
        assertBalance(bob, "500.00");
        assertLedgerInvariants();
    }

    @Test
    void ledgerIsAppendOnlyAndMustBalance() {
        assertThatThrownBy(() -> jdbc.sql("UPDATE postings SET amount_cents = amount_cents * 2").update())
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM journal_entries").update())
                .hasMessageContaining("append-only");

        // Lançamento com uma perna só (dinheiro "criado do nada") é barrado pelo banco no commit.
        var entryId = UUID.randomUUID();
        jdbc.sql("INSERT INTO journal_entries (id, type, created_at) VALUES (:id, 'DEPOSIT', now())")
                .param("id", entryId).update();
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO postings (entry_id, account_id, amount_cents, balance_after_cents, created_at)
                VALUES (:entry, :account, 1000, 1000, now())
                """).param("entry", entryId).param("account", DepositService.TREASURY_ID).update())
                .hasMessageContaining("unbalanced");
    }

    private void assertLedgerInvariants() {
        // 1) Partidas dobradas: a soma de TODOS os lançamentos do sistema é zero.
        long total = jdbc.sql("SELECT COALESCE(SUM(amount_cents), 0) FROM postings").query(Long.class).single();
        assertThat(total).isZero();
        // 2) O saldo materializado de cada conta bate com a soma dos seus lançamentos.
        long mismatches = jdbc.sql("""
                SELECT count(*) FROM accounts a
                WHERE a.balance_cents <> (SELECT COALESCE(SUM(p.amount_cents), 0) FROM postings p WHERE p.account_id = a.id)
                """).query(Long.class).single();
        assertThat(mismatches).isZero();
    }

    private ResultActions transfer(TestUser from, String key, String toAccountId, String amount, String pin)
            throws Exception {
        return mvc.perform(post("/api/v1/transfers")
                .header("Authorization", from.bearer())
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"toAccountId":"%s","amount":%s,"description":"teste","pin":"%s"}
                        """.formatted(toAccountId, amount, pin)));
    }

    private void deposit(TestUser user, String amount) throws Exception {
        mvc.perform(post("/api/v1/dev/deposits")
                        .header("Authorization", user.bearer())
                        .header("Idempotency-Key", newKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(amount)))
                .andExpect(status().isOk());
    }

    private String accountId(TestUser user) throws Exception {
        var json = mvc.perform(get("/api/v1/accounts/me").header("Authorization", user.bearer()))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private void assertBalance(TestUser user, String expected) throws Exception {
        mvc.perform(get("/api/v1/accounts/me").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.balance").value(expected));
    }
}
