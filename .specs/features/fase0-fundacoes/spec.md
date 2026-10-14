# Fase 0 — Fundações (fechamento) Specification

> Roteiro e didática da fase: [`docs/PLANO.md`](../../../docs/PLANO.md#fase-0--fundações). Guia do autor (revisão dos ADRs e preparação da entrevista): [`docs/fases/fase0-guia.md`](../../../docs/fases/fase0-guia.md). Na Fase 0 o autor não implementa código: `Money` e `allocate` são do agente (decisão do autor em 2026-10-13).
> Esta spec cobre **o que falta** para fechar a Fase 0; o scaffolding (Gradle, CI, ArchUnit, `Currency`, `IdGenerator`/`UuidV7Generator`) já foi entregue (`docs/experiments/fase-0.md` F0-01 a F0-04).

## Problem Statement

Toda fase seguinte depende de `Money` correto (sem perder centavo, sem somar moedas diferentes) e de tempo/ids injetáveis. Hoje `Money` não existe, o `Clock` exigido pelo plano só existe como regra da ArchUnit (não há bean) e nada impede `UUID.randomUUID()` no domínio. Sem fechar isso, as Fases 1 e 2 nascem com dívida exatamente onde o projeto quer ensinar rigor.

## Goals

- [ ] `Money` com aritmética segura e `allocate` provado por property test.
- [ ] `Clock` e `IdGenerator` disponíveis como beans e proibidos de forma global no código puro.
- [ ] ADRs da fase revisados pelo autor, exercícios de quebra registrados pelo agente e entrevista feita.

## Out of Scope

| Item | Motivo |
|---|---|
| `Document` (CPF/CNPJ) | Fase 1 (o autor implementa lá) |
| Postgres em uso pela aplicação | Fase 2 (o compose já existe, mas sem datasource) |
| Segunda moeda | Escopo do projeto: só BRL (`docs/PLANO.md` §2) |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
|---|---|---|---|
| Fuso do `Clock` de produção | `Clock.systemUTC()` | Eventos e `TIMESTAMPTZ` em UTC; conversão de fuso só na apresentação | n |
| Teste de "moedas diferentes" com uma moeda só | Se o compilador impedir com só BRL, o agente registra o motivo no KDoc do teste e no commit; o teste entra com a 2ª moeda | Não inventar moeda fora do escopo só para testar | n |
| `UUID.randomUUID()` também proibido em `application` | Não; só em `..domain..` e `..sharedkernel..` | `application` recebe `IdGenerator`; a regra global fica no código puro, como a do relógio | n |

**Open questions:** none — all resolved or logged above.

---

## User Stories

### P1: Dinheiro seguro ⭐ MVP

**User Story**: Como engenheiro do money-out, quero um `Money` que torne erros de arredondamento e de moeda impossíveis, para que nenhuma fase perca ou crie centavos.

**Why P1**: É o princípio inegociável nº 1 do plano.

**Acceptance Criteria**:

1. WHEN dois `Money` de mesma moeda são somados ou subtraídos THEN o sistema SHALL devolver o resultado exato em minor units.
2. IF dois `Money` de moedas diferentes são somados, subtraídos ou comparados THEN o sistema SHALL lançar `DomainException`.
3. IF a soma, a subtração ou a negação estoura `Long` THEN o sistema SHALL lançar `DomainException` em vez de dar a volta.
4. WHEN `allocate(ratios)` é chamado com ratios válidos THEN o sistema SHALL devolver uma lista do mesmo tamanho cuja soma é exatamente o total.
5. WHEN `allocate(ratios)` é chamado THEN o sistema SHALL produzir partes que diferem da fração exata em menos de 1 minor unit, com os centavos restantes distribuídos a partir das primeiras partes.
6. IF `ratios` é vazio ou contém valor menor ou igual a zero THEN o sistema SHALL lançar `DomainException`.

**Independent Test**: `MoneyTest` e `MoneyAllocateTest` (property) verdes em `./gradlew build`.

---

### P1: Tempo e ids injetáveis ⭐ MVP

**User Story**: Como engenheiro, quero `Clock` e `IdGenerator` vindos do container e proibidos de serem lidos globalmente no código puro, para que testes sejam determinísticos e ninguém reintroduza `now()` por descuido.

**Why P1**: Promessa da Fase 0 ainda não cumprida (só há a regra da ArchUnit, não o bean).

**Acceptance Criteria**:

1. WHEN o contexto Spring sobe THEN o sistema SHALL expor exatamente um bean `java.time.Clock` com zona UTC.
2. WHEN o contexto Spring sobe THEN o sistema SHALL expor um bean `IdGenerator` implementado por `UuidV7Generator` usando esse `Clock`.
3. IF uma classe em `..domain..` ou `..sharedkernel..` chama `UUID.randomUUID()` THEN o build SHALL falhar na `ArchitectureTest`.

**Independent Test**: teste de contexto verifica os beans; cópia descartável com `UUID.randomUUID()` no `sharedkernel` derruba a regra nova (registrado em `docs/experiments/fase-0.md`).

---

### P2: Fechamento didático

**User Story**: Como autor, quero revisar as decisões, ver as regras quebradas de propósito e ser entrevistado, para fixar os conceitos antes da Fase 1.

**Why P2**: Regra de ouro do plano (critérios + ADRs + entrevista), mas não bloqueia código.

**Acceptance Criteria**:

1. WHEN a fase fecha THEN os ADRs 0001, 0002, 0004, 0011 e 0012 SHALL estar com status Aceita ou Rejeitada, revisados pelo autor.
2. WHEN a fase fecha THEN `docs/experiments/fase-0.md` SHALL conter a seção "Exercícios de quebra" preenchida com a regra que falhou em cada exercício.
3. WHEN a fase fecha THEN `docs/entrevistas/fase0.md` SHALL registrar de 5 a 8 perguntas com a resposta do autor e o feedback.

**Independent Test**: leitura dos três arquivos.

---

## Edge Cases

- WHEN `allocate` recebe total negativo THEN o sistema SHALL aplicar o mesmo contrato com sinal (soma exata, diferença < 1 minor unit).
- IF `total * ratio` estouraria `Long` THEN o sistema SHALL calcular sem overflow ou lançar `DomainException` (nunca resultado errado silencioso).
- IF `-Money(Long.MIN_VALUE)` é avaliado THEN o sistema SHALL lançar `DomainException`.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
|---|---|---|---|
| F0-01 | P1: Dinheiro seguro (ACs 1–3) | Tasks | Done |
| F0-02 | P1: Dinheiro seguro (ACs 4–6 + edge cases) | Tasks | Done |
| F0-03 | P1: Tempo e ids (ACs 1–2) | Tasks | Done |
| F0-04 | P1: Tempo e ids (AC 3) | Tasks | Done |
| F0-05 | P2: Fechamento (AC 1) | Tasks | Pending |
| F0-06 | P2: Fechamento (AC 2) | Tasks | Done |
| F0-07 | P2: Fechamento (AC 3) | Tasks | Pending |

**Coverage:** 7 total, 7 mapped to tasks, 0 unmapped.

---

## Success Criteria

- [ ] `./gradlew build` verde local e no CI do `main`.
- [ ] Critérios de aceitação da Fase 0 em `docs/PLANO.md` todos verdes.
