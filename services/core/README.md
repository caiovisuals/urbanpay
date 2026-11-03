# urbanpay-core

Núcleo financeiro do UrbanPay em **Java 21 + Spring Boot 4**: identidade (cadastro, login, tokens) e
**ledger de partidas dobradas** (contas, lançamentos, transferências).

É um **monólito modular** ([Spring Modulith](https://spring.io/projects/spring-modulith)): cada módulo
só expõe os tipos do seu pacote raiz e esconde o resto em `internal`. As fronteiras são verificadas
em teste (`ModularityTests`), o que deixa o caminho aberto para extrair microsserviços depois.

```
com.urbanpay.core
├── shared/     Money (centavos em long), UUIDv7, erros RFC 9457, Clock
├── identity/   UserRegistered (evento), PinVerifier (API pública)
│   └── internal/  cadastro, login, JWT RS256, refresh token com rotação, Argon2id
└── ledger/
    └── internal/  contas, lançamentos, transferência idempotente, extrato
```

## Como rodar

```bash
docker compose up -d postgres          # na raiz do repositório
cd services/core
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

- API: http://localhost:8080 · Swagger: http://localhost:8080/swagger-ui.html
- Saúde: `/actuator/health` · Métricas Prometheus: `/actuator/prometheus`
- O perfil `dev` habilita `POST /api/v1/dev/deposits` (dinheiro fictício para testar).

Testes (sobem um Postgres real com Testcontainers, então precisam de Docker):

```bash
mvn verify
```

Além dos testes de exemplo, `LedgerPropertyTests` faz **testes baseados em modelo** com jqwik: gera
sequências aleatórias de depósitos, transferências (inclusive inválidas) e reenvios idempotentes,
executa contra o ledger real e compara com um modelo trivial em memória. Se algo divergir, o jqwik
reduz a sequência ao menor contraexemplo e imprime a seed para reproduzir.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/auth/register` | Cadastro (abre a conta corrente na mesma transação) |
| POST | `/api/v1/auth/login` | Login, devolve access token (15 min) e refresh token (7 dias) |
| POST | `/api/v1/auth/refresh` | Troca o refresh token por um novo par (rotação) |
| POST | `/api/v1/auth/logout` | Revoga a sessão (família de refresh tokens) |
| GET | `/api/v1/me` | Dados do usuário (CPF mascarado) |
| GET | `/.well-known/jwks.json` | Chave pública para validar os tokens |
| GET | `/api/v1/accounts/me` | Conta e saldo |
| GET | `/api/v1/accounts/me/statement?cursor=&limit=` | Extrato com paginação por cursor |
| POST | `/api/v1/transfers` | Transferência (header `Idempotency-Key` obrigatório, exige PIN) |
| GET | `/api/v1/transfers/{id}` | Consulta (só quem participou) |

Valores monetários trafegam como **string decimal** (`"10.50"`) para o JavaScript não convertê-los em float.

## Decisões técnicas

**Ledger de partidas dobradas.** Saldo não é um número que alguém incrementa: todo movimento é um
`journal_entry` com duas ou mais pernas (`postings`) cuja soma é zero. O banco garante as regras
mesmo se a aplicação tiver bug:
- trigger `DEFERRABLE` verifica no COMMIT que cada lançamento fecha em zero;
- triggers bloqueiam UPDATE/DELETE: o ledger é append-only (correção = lançamento de estorno);
- `CHECK` impede saldo negativo em conta de cliente.

**Dinheiro em centavos (`long`).** `Money` usa aritmética exata (`Math.addExact`) e rejeita valores
com mais de 2 casas decimais em vez de arredondar silenciosamente.

**Concorrência.** As contas envolvidas são travadas com `SELECT ... FOR UPDATE` **ordenado por id**
(evita deadlock em A→B concorrente com B→A), e `@Version` faz lock otimista como segunda barreira.
Há teste com 40 transferências simultâneas e saldo para 10: passam exatamente 10.

**Idempotência (padrão Stripe).** `Idempotency-Key` é reservada com `INSERT ... ON CONFLICT DO NOTHING`
na mesma transação da transferência. Repetir a requisição devolve o mesmo resultado
(`Idempotent-Replayed: true`); reusar a chave com outro corpo dá 422; se a operação falhar, a chave
é liberada junto com o rollback.

**Autenticação.**
- Senha e PIN com **Argon2id**.
- Access token **JWT RS256** de 15 minutos. Outros serviços validam com a chave pública (JWKS).
- Refresh token opaco com **rotação** e **detecção de reuso**: guardamos só o SHA-256; reapresentar
  um token já usado revoga a família inteira. Uma janela de 30 s tolera chamadas paralelas legítimas.
- Login com mensagem única e tempo constante (não revela se o e-mail existe).
- PIN bloqueia por 15 min após 5 erros. A contagem roda em transação própria, então é registrada
  mesmo quando a transferência é abortada.

**Rate limiting (Bucket4j).** Token bucket por IP no login (20/min) e no cadastro (10/h), e
contador de **falhas** por e-mail (5 a cada 15 min, zerado no login bem-sucedido). O limite por
e-mail freia ataques distribuídos em vários IPs, e não há bloqueio permanente, para que um atacante
não consiga trancar a conta da vítima. A resposta é 429 com `Retry-After`. O IP do usuário chega
pelo `X-Forwarded-For`, aceito só de proxies internos (o BFF). Os buckets ficam em memória: com
várias réplicas, troque por um `ProxyManager` do Bucket4j sobre Redis.

**IDs UUIDv7.** Ordenados por tempo, mantêm os índices B-tree compactos.

## Próximas fases

3. Outbox transacional + Kafka (notificações, auditoria).
4. Pix: chaves (DICT simulado), máquina de estados, antifraude, limites noturnos, MED.
5. Porquinhos como subcontas, cartões (tokenização), crédito.
6. Rate limit distribuído (Redis), OpenTelemetry, testes de carga.
