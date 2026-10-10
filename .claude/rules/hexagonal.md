# Regra: arquitetura hexagonal (ports & adapters)

O domínio é o centro. Tudo que fala com o exterior passa por uma **porta** (interface no domínio) e um **adapter** (implementação na infraestrutura). Estrutura e motivos: ADR-0011.

```text
[HTTP / job agendado / consumer Kafka]
            │  adapter de entrada  (infrastructure/adapter/input)
            ▼
   porta de entrada (domain/port/input)  ← command + interface do caso de uso
            │
            ▼
   handler (application)                 ← orquestra, sem regra de negócio
            │
            ▼
   agregado (domain/model)               ← decide; emite eventos
            │
   porta de saída (domain/port/output)   ← Transfers, TransferServiceProvider, ...
            ▼
   adapter de saída (infrastructure/adapter/output) → Postgres, TSP, Kafka
```

## Estrutura por bounded context

```text
com.moneyout
├── sharedkernel/                     # Money, Currency, DomainException, IdGenerator (critérios no ADR-0011)
├── infrastructure/                   # adapters técnicos transversais (id/, clock/)
└── <contexto>/                       # transfer, account, accounting, ...
    ├── domain/
    │   ├── model/                    # agregado, entidades, VOs, state machine, exceções de domínio
    │   ├── event/                    # sealed interface <Agg>Event + eventos concretos
    │   └── port/
    │       ├── input/                # command + interface do caso de uso (ex.: RegisterTransfer + RegisterTransferUseCase)
    │       └── output/               # Transfers, TransferServiceProvider, ...
    ├── application/                  # <Command>Handler implementa a porta de entrada
    │   └── query/                    # (Fase 4) queries, handlers de leitura e DTOs de leitura
    └── infrastructure/
        ├── adapter/input/
        │   ├── http/<acao>/          # Controller + Request/Response DTOs
        │   ├── scheduling/           # jobs @Scheduled
        │   └── messaging/            # consumers
        ├── adapter/output/
        │   ├── persistence/          # event store, projeções, in-memory
        │   └── <integracao>/         # ex.: tsp/ (ACL + facade)
        └── config/                   # @Configuration que monta handlers e adapters
```

## Regras de isolamento (verificadas pela ArchUnit em `src/test/kotlin/com/moneyout/architecture/`)

| Regra | Faça | Não faça |
|---|---|---|
| Domínio sem framework | Kotlin puro + `java.time`/`java.util` | Spring, Jakarta, Jackson em `..domain..`/`..sharedkernel..` |
| Dependências para dentro | `infrastructure → application → domain` | `domain` importar `application` ou `infrastructure` |
| Application sem infra | Handler depende de portas | Handler injetar `JdbcEventStore` concreto |
| Kernel neutro | `sharedkernel` não conhece contextos | `sharedkernel` importar `transfer.*` |
| Leitura separada | Query side com DTOs próprios (Fase 4) | Reusar o agregado como DTO de leitura |
| Contextos isolados | Integração entre contextos por evento ou porta | `account` importar classes internas de `transfer.domain.model` |

Se precisar de uma regra nova, adicione-a à `ArchitectureTest` na mesma task e prove que ela falha numa cópia descartável (padrão do `docs/experiments/fase-0.md` F0-03).

## Portas

- **Entrada:** um caso de uso por command. O command (`data class`, verbo + agregado: `RegisterTransfer`) e a interface (`RegisterTransferUseCase { fun handle(command: RegisterTransfer): TransferId }`) ficam em `domain/port/input`.
- **Saída:** nome de domínio, no plural para coleções de agregados: `Transfers`, `Accounts`, `Journal`, `TransferServiceProvider`. Anti-padrão: `TransferRepository`, `TransferRepo`.
- Um adapter implementa **uma** porta.

## Adapters de entrada

### HTTP (Spring MVC)

- Toda rota nasce versionada no path, **estilo recurso**: `POST /v1/payouts`, `GET /v1/payouts/{id}`, `POST /v1/transfers/{id}/submission`. Não misture com estilo verbo.
- Pasta por ação: `http/<acao>/` com `<Acao>Controller.kt`, `<Acao>Request.kt` (com `toCommand()`) e `<Acao>Response.kt`.
- O controller só converte request → command, chama a porta de entrada e converte o resultado → response.
- Erros em **RFC 9457** (`ProblemDetail`, `application/problem+json`) com `correlationId`, num `@RestControllerAdvice` por contexto.
- Status: invariante de domínio → `422`; recurso inexistente → `404`; conflito de versão ou idempotência em andamento → `409`; mesma `Idempotency-Key` com corpo diferente → `422` (ADR-0004).
- Operações que movem dinheiro exigem header `Idempotency-Key` (ADR-0004, Fase 3).

### Jobs e consumers

- `@Scheduled` em `adapter/input/scheduling/`; consumers em `adapter/input/messaging/`. Os dois só montam commands e chamam portas de entrada.
- Todo consumer é idempotente (inbox ou `Idempotency-Key`, ADR-0004).

## Adapters de saída

- **Persistência:** event store e projeções em `adapter/output/persistence/` (ver `event-sourcing.md`). Implementações in-memory ficam no mesmo pacote (`InMemoryTransfers`) e servem de fake nos testes.
- **Integração externa (TSP, Fase 6):** é uma **Anti-Corruption Layer**. Os tipos do provedor nunca vazam para o domínio. Para várias chamadas ao mesmo sistema, use uma facade:

```text
adapter/output/tsp/
├── TspAdapter.kt          # implementa TransferServiceProvider; traduz domínio ⇄ provedor
└── client/
    ├── TspClient.kt       # HTTP, timeouts, auth
    ├── SubmitPayment.kt   # uma operação
    ├── QueryPayment.kt
    └── json/              # DTOs do provedor
```

- Chamada externa com efeito: **grave a intenção antes de agir** (`endToEndId` persistido antes da chamada) e use-o como chave de idempotência. Timeout ⇒ `UNCERTAIN`, nunca falha (ADR-0004).
- Resiliência (Resilience4j) vive no adapter, nunca no domínio.

## Observações

- Métricas e tracing são preocupações transversais em `infrastructure`; o domínio não conhece Micrometer nem OpenTelemetry.
- Sem pacotes `shared`, `common` ou `utils` (combinado do `CLAUDE.md`).
