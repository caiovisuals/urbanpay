package com.urbanpay.core.ledger.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.urbanpay.core.ApiTestSupport;
import com.urbanpay.core.CoreApplication;
import com.urbanpay.core.TestcontainersConfiguration;
import com.urbanpay.core.identity.internal.IdentityTestSupport;
import com.urbanpay.core.shared.DomainException;
import com.urbanpay.core.shared.Money;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;

/**
 * Teste baseado em modelo (model-based testing): o jqwik gera sequências aleatórias de
 * operações, executa cada uma contra o ledger real (Postgres) e contra um modelo trivial em
 * memória (um array de saldos), e exige que os dois concordem. Se algo falhar, o jqwik
 * "encolhe" a sequência até o menor contraexemplo e imprime a seed para reproduzir.
 */
class LedgerPropertyTests {

    private static final int USERS = 4;
    private static final String PIN = ApiTestSupport.PIN;

    private static ConfigurableApplicationContext ctx;
    private static TransferService transfers;
    private static DepositService deposits;
    private static AccountRepository accounts;
    private static JdbcClient jdbc;

    sealed interface Op permits Deposit, Transfer, RetryLastTransfer {
    }

    record Deposit(int user, long cents) implements Op {
    }

    record Transfer(int from, int to, long cents) implements Op {
    }

    /** Reenvia a última transferência com a MESMA Idempotency-Key (ex.: timeout no app). */
    record RetryLastTransfer() implements Op {
    }

    @BeforeContainer
    static void startApplication() {
        ctx = new SpringApplicationBuilder(CoreApplication.class, TestcontainersConfiguration.class)
                .profiles("dev")
                // Argumentos de linha de comando têm precedência sobre o application.yml.
                .run("--server.port=0",
                        "--urbanpay.security.rate-limit.register-per-ip-per-hour=100000");
        transfers = ctx.getBean(TransferService.class);
        deposits = ctx.getBean(DepositService.class);
        accounts = ctx.getBean(AccountRepository.class);
        jdbc = ctx.getBean(JdbcClient.class);
    }

    @AfterContainer
    static void stopApplication() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Property(tries = 30)
    void ledgerAgreesWithASimpleBalanceModel(@ForAll("operations") @Size(min = 1, max = 25) List<Op> ops) {
        var users = new ArrayList<UUID>();
        var accountIds = new ArrayList<UUID>();
        for (int i = 0; i < USERS; i++) {
            var userId = IdentityTestSupport.registerUser(ctx, ApiTestSupport.nextCpf(), PIN);
            users.add(userId);
            accountIds.add(accounts.findCustomerAccountId(userId).orElseThrow());
        }

        long[] model = new long[USERS];
        Transfer lastTransfer = null;
        String lastKey = null;
        UUID lastTransferId = null;

        for (var op : ops) {
            switch (op) {
                case Deposit d -> {
                    deposits.deposit(users.get(d.user()), key(), Money.ofCents(d.cents()));
                    model[d.user()] += d.cents();
                }
                case Transfer t -> {
                    var key = key();
                    var request = request(accountIds.get(t.to()), t.cents());
                    if (t.from() == t.to()) {
                        assertRejected(() -> transfers.transfer(users.get(t.from()), key, request), "same_account");
                    } else if (model[t.from()] < t.cents()) {
                        assertRejected(() -> transfers.transfer(users.get(t.from()), key, request), "insufficient_funds");
                    } else {
                        var result = transfers.transfer(users.get(t.from()), key, request);
                        assertThat(result.replayed()).isFalse();
                        model[t.from()] -= t.cents();
                        model[t.to()] += t.cents();
                        lastTransfer = t;
                        lastKey = key;
                        lastTransferId = result.transfer().id();
                    }
                }
                case RetryLastTransfer r -> {
                    if (lastTransfer != null) {
                        var result = transfers.transfer(users.get(lastTransfer.from()), lastKey,
                                request(accountIds.get(lastTransfer.to()), lastTransfer.cents()));
                        // Mesma resposta, nenhum dinheiro a mais se move.
                        assertThat(result.replayed()).isTrue();
                        assertThat(result.transfer().id()).isEqualTo(lastTransferId);
                    }
                }
            }
        }

        for (int i = 0; i < USERS; i++) {
            assertThat(accounts.findById(accountIds.get(i)).orElseThrow().balance().cents())
                    .as("saldo do usuário %d", i).isEqualTo(model[i]);
        }
        assertGlobalInvariants();
    }

    @Provide
    Arbitrary<List<Op>> operations() {
        var user = Arbitraries.integers().between(0, USERS - 1);
        // Valores pequenos e grandes misturados, para provocar tanto sucesso quanto saldo insuficiente.
        var cents = Arbitraries.longs().between(1, 50_000).edgeCases(c -> c.includeOnly(1L, 50_000L));

        Arbitrary<Op> deposit = Combinators.combine(user, cents).as(Deposit::new);
        Arbitrary<Op> transfer = Combinators.combine(user, user, cents).as(Transfer::new);
        Arbitrary<Op> retry = Arbitraries.just(new RetryLastTransfer());

        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(3, deposit),
                net.jqwik.api.Tuple.of(6, transfer),
                net.jqwik.api.Tuple.of(1, retry)).list();
    }

    private static void assertGlobalInvariants() {
        long total = jdbc.sql("SELECT COALESCE(SUM(amount_cents), 0) FROM postings").query(Long.class).single();
        assertThat(total).as("soma de todos os lançamentos").isZero();

        long mismatches = jdbc.sql("""
                SELECT count(*) FROM accounts a
                WHERE a.balance_cents <> (SELECT COALESCE(SUM(p.amount_cents), 0) FROM postings p WHERE p.account_id = a.id)
                """).query(Long.class).single();
        assertThat(mismatches).as("contas com saldo materializado divergente").isZero();

        long negative = jdbc.sql("SELECT count(*) FROM accounts WHERE kind = 'CUSTOMER' AND balance_cents < 0")
                .query(Long.class).single();
        assertThat(negative).as("contas de cliente negativas").isZero();
    }

    private static TransferRequest request(UUID to, long cents) {
        return new TransferRequest(to, BigDecimal.valueOf(cents, 2), null, PIN);
    }

    private static void assertRejected(Runnable call, String code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    private static String key() {
        return "prop-" + UUID.randomUUID();
    }
}
