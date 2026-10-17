# Regra: testes

Testes derivam dos **critérios de aceitação** da spec (`.specs/features/<fase>/spec.md`), afirmam o resultado esperado pela spec e nunca espelham a implementação. Nunca enfraqueça, pule ou apague um teste para o build passar.

## Stack

JUnit 5 (Jupiter) + Kotest assertions (`shouldBe`, `shouldThrow`) + **Kotest property** (`checkAll`, `Arb`) + ArchUnit. A partir da Fase 2: Testcontainers (Postgres com tag fixa) e Spring Boot Test. HTTP: MockMvc (`@WebMvcTest`). MockK só em último caso: prefira fakes.

## O que testar em cada camada

| Camada | Tipo | Profundidade esperada |
|---|---|---|
| VO e `sharedkernel` | unit + property | todos os ramos; bordas (zero, negativo, overflow, `Long.MIN_VALUE`); propriedades do contrato via `checkAll` |
| Agregado e state machine | unit (Given events / When command / Then events) + property | 1:1 com os critérios da spec; matriz exaustiva estado × gatilho; sequências aleatórias nunca levam a estado inválido |
| Handler (`application`) | unit com fakes in-memory | caminho feliz, não encontrado, conflito de versão, idempotência |
| Adapter de persistência | integração (Testcontainers) | ver `event-sourcing.md`, "Testes obrigatórios do adapter" |
| Controller HTTP | `@WebMvcTest` | status de cada cenário, `ProblemDetail` (RFC 9457), conversão request → command |
| Adapter externo (TSP) | integração com fake/servidor simulado | cada cenário do TSP fake (Fase 6) |
| Arquitetura | ArchUnit | regra nova sempre acompanhada de prova de que falha (cópia descartável) |
| Concorrência | integração | N threads; asserta invariantes (saldo ≥ 0, conservação, 1 efeito por chave) |

## Formato

- Nome da função em **inglês** com crases, descrevendo o comportamento: `` `Should throw InvalidTransferTransition when approving a completed transfer` ``.
- Comentários **Gherkin em pt-BR** (`/** @Dado */`, `/** @Quando */`, `/** @Então */`, `/** @E */`) em **todo** teste, sem exceção por tamanho (só as regras do ArchUnit ficam fora). Regras completas e anti-padrões: [`tests/gherkin.md`](tests/gherkin.md).
- Cada bloco Gherkin vem **imediatamente antes** do código que descreve; asserções só depois de `@Então`/`@E`.

```kotlin
@Test
fun `Should emit TransferApproved when approving a transfer under risk analysis`() {
    /** @Dado uma transfer em análise de risco */
    val transfer = Transfer.rehydrate(listOf(TRANSFER_REGISTERED, RISK_ANALYSIS_STARTED))

    /** @Quando a aprovação for decidida */
    val event = transfer.approve(at = NOW)

    /** @Então um único TransferApproved é emitido com o instante informado */
    event shouldBe TransferApproved(transferId = TRANSFER_ID, occurredAt = NOW)
}
```

## Property tests

- Toda função de dinheiro ou de decisão pura tem pelo menos uma propriedade (`allocate`, `requireBalanced`, decisão de idempotência, matching da reconciliação).
- Use `runBlocking { checkAll(...) }` (o `checkAll` do Kotest é `suspend`; `kotlinx-coroutines-core` já está em `testImplementation`).
- Geradores com faixas que incluam as bordas.

## Fakes, fixtures e tempo

- **Fakes:** prefira os adapters in-memory de produção (`InMemoryTransfers`). Se o teste precisar espionar chamadas, crie um fake manual em `src/test/kotlin/com/moneyout/<ctx>/fakes/` que implementa a porta e guarda o que recebeu (`appended`, `submitted`).
- **Fixtures:** `object` por contexto em `src/test/kotlin/com/moneyout/<ctx>/fixtures/` (`Models`, `Commands`, `Events`), constantes em `SCREAMING_SNAKE_CASE`, variantes via `copy()`.
- **Tempo:** `Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC)`; nunca `Instant.now()` em teste de domínio.
- **Dados pessoais de teste:** só CPFs/CNPJs gerados para teste (válidos no DV, não pertencentes a ninguém conhecido); nunca dado real.

## Localização

Os testes espelham o pacote de produção (`src/test/kotlin/com/moneyout/<ctx>/domain/model/TransferTest.kt`). Integração com Testcontainers termina em `IntegrationTest` (ex.: `JdbcTransfersIntegrationTest`).

## Gate

`./gradlew build` (compila, ktlint, testes unitários, integração e ArchUnit). Registre a contagem de testes antes e depois de cada task: a contagem só cresce.
