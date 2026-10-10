# 0012 — Modelo de I/O: Spring MVC + JDBC bloqueante + virtual threads

- **Status:** Proposta
- **Data:** 2026-10-10
- **Fase:** 0 (vale a partir das Fases 2 e 3)

## Contexto
O harness de regras do autor (Kotlin/Quarkus) assume I/O reativo (Vert.x/Mutiny) e `suspend fun` em toda operação de I/O. O money-out usa Spring Boot (ADR-0001), Event Sourcing com controle otimista no Postgres (ADR-0003) e precisa de transações explícitas: append no event store, registro de `Idempotency-Key` na mesma transação do efeito (ADR-0004) e o atalho de dois agregados numa transação (Fase 3). Os agentes que implementam tasks na nuvem precisam de uma regra única, sem precisar conciliar dois modelos.

## Opções consideradas
1. **Spring MVC + JDBC (`JdbcClient`) + virtual threads (Java 21)** — modelo bloqueante simples; `@Transactional`/`TransactionTemplate` funcionam como esperado; testes de concorrência com threads reais; virtual threads tiram o custo de thread por requisição. Contra: não exercita programação reativa.
2. **Spring MVC + coroutines (`suspend`)** — segue o harness, mas transação JDBC atravessando pontos de suspensão é armadilha conhecida (contexto transacional ligado à thread) e não traz ganho sobre virtual threads.
3. **WebFlux + R2DBC** — totalmente reativo; Flyway e Testcontainers continuam JDBC; a transação reativa e o debug de concorrência ficam mais difíceis justamente nas fases em que concorrência é o tema.

## Decisão
Opção 1. Sem `suspend` na aplicação. JDBC via `JdbcClient`, sem JPA. Virtual threads ligadas quando o servidor web entrar (Fase 3). Regras operacionais em `.claude/rules/kotlin.md`.

## Consequências
- Transações e concorrência ficam explícitas e didáticas, que é o objetivo das Fases 2, 3 e 7.
- As partes reativas do harness original não se aplicam (ver a tabela de diferenças em `.claude/rules/README.md`).
- Risco: *pinning* de virtual threads em blocos `synchronized` de drivers antigos. Mitigação: driver PostgreSQL atual e checagem na Fase 3 (`-Djdk.tracePinnedThreads` ou o evento JFR equivalente da JDK em uso — conferir na fonte).
