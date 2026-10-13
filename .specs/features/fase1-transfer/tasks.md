# Fase 1 — Domínio `Transfer` + State Machine Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill (vendorizada em `.claude/skills/tlc-spec-driven/`): **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path.

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

Antes de qualquer task, leia [`docs/fluxo-de-trabalho.md`](../../../docs/fluxo-de-trabalho.md) (Owner, branch/PR, desbloqueio) e as regras indicadas no campo **Tools**.

- **Owner 🧑** — do autor. Agentes não implementam; podem oferecer dicas, testes e revisão.
- **Owner 🤖** — de um agente de IA, rodando local ou na nuvem (o protocolo é o mesmo). Tasks marcadas **interativa** exigem o autor na sessão.

**Pré-requisito da fase:** Fase 0 fechada (`.specs/features/fase0-fundacoes/validation.md` com PASS), em especial `Money` e o bean de `Clock`/`IdGenerator`.

---

**Spec**: `.specs/features/fase1-transfer/spec.md`
**Design**: `.specs/features/fase1-transfer/design.md`
**Status**: Approved (autor, 2026-10-13)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.claude/rules/tests.md`, `.claude/rules/domain-model.md`, `CLAUDE.md`, `build.gradle.kts`, `.github/workflows/ci.yml`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
|---|---|---|---|---|
| Value objects (`transfer/domain/model`) | unit (tabelado) | 1:1 com os ACs; todo caso inválido da spec; casos válidos de cada tipo | `src/test/kotlin/com/moneyout/transfer/domain/model/*Test.kt` | `./gradlew test` |
| State machine | unit (parametrizado exaustivo) | todos os 72 pares estado × gatilho | `.../transfer/domain/model/TransferStateMachineTest.kt` | `./gradlew test` |
| Agregado `Transfer` | unit (Given events / When / Then events) + property | cada transição da matriz; um evento por transição; rehidratação por caminho | `.../transfer/domain/model/TransferTest.kt`, `TransferPropertyTest.kt` | `./gradlew test` |
| Eventos, exceções, portas (interfaces) | none | - (build gate; cobertos via agregado/handlers) | - | `./gradlew build` |
| Handlers (`transfer/application`) | unit com `InMemoryTransfers` + `Clock.fixed` | caminho feliz, `TransferNotFound`, `ConcurrencyConflict`, instante do `Clock` | `src/test/kotlin/com/moneyout/transfer/application/*Test.kt` | `./gradlew test` |
| Adapter in-memory | unit + concorrência | append/load, versão errada, dois appends concorrentes na mesma versão | `.../transfer/infrastructure/adapter/output/persistence/*Test.kt` | `./gradlew test` |
| Configuração Spring | integration (contexto) | todos os handlers resolvidos como beans | `.../transfer/infrastructure/config/*Test.kt` | `./gradlew test` |
| Regras de arquitetura | ArchUnit + prova de falha | regra nova falha numa cópia descartável | `src/test/kotlin/com/moneyout/architecture/ArchitectureTest.kt` | `./gradlew test` |
| Documentação | none | - | `docs/**` | - |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
|---|---|---|
| Quick | Tasks com testes unitários | `./gradlew test` |
| Full | Tasks com teste de contexto Spring | `./gradlew test` |
| Build | Fim de toda task (é o que o CI roda) | `./gradlew ktlintFormat && ./gradlew build` |

---

## Execution Plan

As fases rodam em ordem; dentro delas, tasks de donos diferentes podem andar em paralelo quando não há seta entre elas. Gargalo esperado: T5 e T6 (autor) liberam T10 e T13.

- **Phase 1 — preparação:** T1 🤖 → T3 🤖; T2 🤖 independente.
- **Phase 2 — modelo base:** T4 🤖 → T7 🤖 → (T5 🧑, T6 🧑, T8 🤖, T9 🤖); T6 → T10 🤖 → T11 🤖; T9 → T11.
- **Phase 3 — agregado:** T12 🤖 → T13 🤖 → (T14 🤖, T15 🧑, T16 🧑 opcional).
- **Phase 4 — casos de uso:** T17 🤖 → T18 🤖 → (T19 🤖, T20 🤖) → T21 🤖.
- **Phase 5 — fechamento:** T22 🤖.

Os diagramas exatos (validados por script) estão no início de cada fase em "Task Breakdown".

Ordem sugerida para o agente: T1, T2, T4, T7, T8, T9 (sem depender do autor); depois T10–T11 (após T6), T12–T14 (após T5), T17–T21.

---

## Task Breakdown

### Phase 1: Preparação

```
T1 -> T3
T2
```

#### T1: Verificar na fonte as regras externas

**Owner**: 🤖
**What**: Confirmar em fonte oficial o formato do `endToEndId`, o DV de CPF, o DV do CNPJ alfanumérico (pesos e conversão), os tipos de conta usados no PIX e o prazo de devolução PIX; registrar fonte, data e eventuais divergências das premissas da spec.
**Where**: `docs/experiments/fase1.md`
**Depends on**: None
**Requirement**: TRF-14

**Tools**:

- MCP: NONE (busca web)
- Skill: `tlc-spec-driven` (Knowledge Verification Chain)

**Done when**:

- [ ] Cada premissa "a confirmar" da spec com: fonte oficial (BCB, Receita Federal ou Serpro), URL, data da consulta e resultado
- [ ] Se alguma premissa divergir: a tabela de Assumptions da spec é atualizada e o PR avisa o autor (não muda AC sozinho)
- [ ] Conjunto de casos de teste (válidos e inválidos) de CPF, CNPJ numérico e CNPJ alfanumérico, gerados pela regra oficial, sem dado real de pessoa

**Tests**: none
**Gate**: build

**Commit**: `docs(experiments): verifica regras do BCB e da RFB para a Fase 1`

---

#### T2: ADR-0005 — state machine custom

**Owner**: 🤖
**What**: ADR com contexto, opções (Spring State Machine, biblioteca de terceiros, custom declarativa) e decisão proposta, em status Proposta, mais a linha no índice.
**Where**: `docs/adr/0005-state-machine-custom.md`
**Depends on**: None
**Reuses**: `docs/adr/0000-template.md`, seção Tech Decisions do `design.md`

**Done when**:

- [ ] ADR em status Proposta e `docs/adr/README.md` atualizado

**Tests**: none
**Gate**: build

**Commit**: `docs(adr): propoe ADR-0005 state machine custom`

---

#### T3: Guia da trilha do autor (Fase 1)

**Owner**: 🤖
**What**: `docs/fases/fase1-guia.md` com a história de falha, os 📋 (esqueleto de `TransferStateMachine` e de `Document`), os casos de teste de CPF/CNPJ vindos de T1 e as perguntas que voltam na entrevista.
**Where**: `docs/fases/fase1-guia.md`
**Depends on**: T1
**Reuses**: formato de `docs/fases/fase0-guia.md`

**Done when**:

- [ ] Código 📋 do guia compila numa cópia descartável
- [ ] Nenhuma solução das tasks 🧑 entregue pronta (só esqueleto, contrato e dicas)

**Tests**: none
**Gate**: build

**Commit**: `docs(fase1): guia da trilha do autor`

---

### Phase 2: Modelo base

```
T4 -> T7 -> T5
T7 -> T6 -> T10 -> T11
T7 -> T8
T7 -> T9 -> T11
T7 -> T10
```

#### T4: Tipos do ciclo de vida

**Owner**: 🤖
**What**: `TransferStatus` (9 estados, `isTerminal`), `TransferTrigger` (8 gatilhos), `TransferMethod` (PIX, TED) e `TransferId` (`@JvmInline value class` sobre `UUID`).
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/TransferStatus.kt`
**Depends on**: None
**Requirement**: TRF-06

**Tools**:

- Skill: `tlc-spec-driven`; ler `.claude/rules/kotlin.md`, `.claude/rules/domain-model.md`

**Done when**:

- [ ] `TransferTrigger.kt`, `TransferMethod.kt`, `TransferId.kt` no mesmo pacote (um tipo por arquivo)
- [ ] Teste afirma que só `REJECTED`, `FAILED` e `RETURNED` são terminais
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): tipos do ciclo de vida da transfer`

---

#### T7: Exceções de domínio do contexto `transfer`

**Owner**: 🤖
**What**: As exceções da seção "Error Handling Strategy" do design, todas subclasses de `DomainException`, sem PII nas mensagens.
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/TransferExceptions.kt`
**Depends on**: T4
**Requirement**: TRF-06

**Done when**:

- [ ] `InvalidTransferTransition` expõe `from` e `trigger`
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: none
**Gate**: build

**Commit**: `feat(transfer): excecoes de dominio`

---

#### T5: Matriz de transições e teste exaustivo

**Owner**: 🧑
**What**: `TransferStateMachine` com a tabela do AC 1 da state machine + teste parametrizado que percorre os 72 pares e afirma que só os da matriz passam.
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/TransferStateMachine.kt`
**Depends on**: T3, T7
**Reuses**: 📋 do `docs/fases/fase1-guia.md`
**Requirement**: TRF-06

**Done when**:

- [ ] `TransferStateMachineTest.kt` cobre os 72 pares
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): matriz de transicoes da state machine`

---

#### T6: `Document` — CPF e CNPJ com dígitos verificadores

**Owner**: 🧑
**What**: `sealed interface Document` com `Cpf` e `Cnpj` (numérico e alfanumérico), normalização de máscara e rejeição de sequências repetidas, guiado pelos casos de teste do guia.
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/Document.kt`
**Depends on**: T3, T7
**Requirement**: TRF-02

**Done when**:

- [ ] `DocumentTest.kt` com todos os casos do guia + ACs 1–3 dos VOs
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): Document com validacao de CPF e CNPJ`

---

#### T8: `EndToEndId`

**Owner**: 🤖
**What**: VO que aceita só o formato confirmado em T1 e lança `InvalidEndToEndId` caso contrário.
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/EndToEndId.kt`
**Depends on**: T1, T7
**Requirement**: TRF-04

**Done when**:

- [ ] `EndToEndIdTest.kt` tabelado: válido, tamanho errado, prefixo errado, ISPB não numérico, data/hora inválida, sufixo com caractere inválido
- [ ] KDoc cita a fonte registrada em `docs/experiments/fase1.md`
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): EndToEndId do PIX`

---

#### T9: `BankAccount`

**Owner**: 🤖
**What**: VO com ISPB (8 dígitos), agência, número da conta e `AccountType` (tipos confirmados em T1).
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/BankAccount.kt`
**Depends on**: T1, T7
**Requirement**: TRF-05

**Done when**:

- [ ] `BankAccountTest.kt` cobre AC 6 dos VOs
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): BankAccount`

---

#### T10: `PixKey`

**Owner**: 🤖
**What**: VO com `PixKeyType` (CPF, CNPJ, EMAIL, PHONE, EVP); chaves CPF/CNPJ delegam a validação para `Document`.
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/PixKey.kt`
**Depends on**: T6, T7
**Requirement**: TRF-03

**Done when**:

- [ ] `PixKeyTest.kt` tabelado por tipo (válido e inválido) + edge case "chave CPF com CPF inválido"
- [ ] Formatos de e-mail, telefone (E.164) e EVP (UUID) conferidos em T1 ou citados com fonte
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): PixKey`

---

#### T11: `Beneficiary` e motivos categorizados

**Owner**: 🤖
**What**: `Beneficiary` (nome, `Document?`, destino `sealed` `PixKey | BankAccount`) + `RejectionReason` e `FailureReason` (enums).
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/Beneficiary.kt`
**Depends on**: T9, T10
**Requirement**: TRF-01

**Done when**:

- [ ] `BeneficiaryTest.kt`: nome vazio e > 140 caracteres rejeitados
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): Beneficiary e motivos categorizados`

---

### Phase 3: Agregado

```
T12 -> T13 -> T14
T13 -> T15
T13 -> T16
```

#### T12: Eventos de domínio

**Owner**: 🤖
**What**: `sealed interface TransferEvent` e os 9 eventos da tabela "Data Models" do design (delta + `occurredAt`, sem `eventId`).
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/event/TransferEvent.kt`
**Depends on**: T8, T11
**Requirement**: TRF-07

**Done when**:

- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: none
**Gate**: build

**Commit**: `feat(transfer): eventos de dominio`

---

#### T13: Agregado `Transfer`

**Owner**: 🤖
**What**: `register`, os 8 métodos de decisão (consultando `TransferStateMachine`), `apply` e `rehydrate`, conforme o design.
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/Transfer.kt`
**Depends on**: T5, T12
**Requirement**: TRF-01, TRF-07, TRF-08

**Tools**:

- Skill: `tlc-spec-driven`; ler `.claude/rules/domain-model.md`, `.claude/rules/event-sourcing.md`, `.claude/rules/tests.md`

**Done when**:

- [ ] `TransferTest.kt` (Given events / When / Then events, Gherkin pt-BR) cobre ACs 1–4 do registro, ACs 2–6 da state machine e ACs 1–2 da rehidratação
- [ ] Nenhum `if (status == ...)` no agregado: transições só via `TransferStateMachine`
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): agregado Transfer event-sourced`

---

#### T14: Regra ArchUnit de imutabilidade no domínio

**Owner**: 🤖
**What**: Regra `pureCodeHasNoMutableFields` (campos de `..domain..` e `..sharedkernel..` são `final`) + prova de falha registrada.
**Where**: `src/test/kotlin/com/moneyout/architecture/ArchitectureTest.kt`
**Depends on**: T13
**Requirement**: TRF-11

**Done when**:

- [ ] Prova numa cópia descartável (um `var` no domínio derruba só a regra nova) registrada em `docs/experiments/fase1.md`
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `test(arch): exige imutabilidade no codigo puro`

---

#### T15: Property test da state machine

**Owner**: 🧑
**What**: `TransferPropertyTest` com sequências aleatórias de gatilhos sobre o agregado.
**Where**: `src/test/kotlin/com/moneyout/transfer/domain/model/TransferPropertyTest.kt`
**Depends on**: T13
**Requirement**: TRF-12

**Done when**:

- [ ] AC 1 da P3 verificado; terminais absorventes
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `test(transfer): property test da state machine`

---

#### T16: (opcional) Guard do prazo de devolução

**Owner**: 🧑
**What**: `returnFunds(at)` lança `ReturnWindowExpired` depois do prazo confirmado em T1.
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/model/Transfer.kt`
**Depends on**: T13
**Requirement**: TRF-13

**Done when**:

- [ ] Teste com `Clock.fixed` no último instante permitido e um instante depois
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): guard do prazo de devolucao`

---

### Phase 4: Portas e casos de uso

```
T17 -> T18 -> T19 -> T21
T18 -> T20 -> T21
```

#### T17: Porta de saída `Transfers`

**Owner**: 🤖
**What**: Interface `Transfers` com `load` e `append(expectedVersion)`, documentada como em `.claude/rules/event-sourcing.md`.
**Where**: `src/main/kotlin/com/moneyout/transfer/domain/port/output/Transfers.kt`
**Depends on**: T13
**Requirement**: TRF-10

**Done when**:

- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: none
**Gate**: build

**Commit**: `feat(transfer): porta de saida Transfers`

---

#### T18: `InMemoryTransfers`

**Owner**: 🤖
**What**: Adapter in-memory com checagem de versão atômica (`ConcurrentHashMap.compute`).
**Where**: `src/main/kotlin/com/moneyout/transfer/infrastructure/adapter/output/persistence/InMemoryTransfers.kt`
**Depends on**: T17
**Requirement**: TRF-10

**Done when**:

- [ ] `InMemoryTransfersTest.kt`: append/load, stream inexistente, versão errada sem gravar nada, e N threads anexando na mesma versão ⇒ exatamente 1 sucesso
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): adapter InMemoryTransfers com controle de versao`

---

#### T19: Caso de uso `RegisterTransfer`

**Owner**: 🤖
**What**: Command + `RegisterTransferUseCase` (porta de entrada) + `RegisterTransferHandler` (usa `IdGenerator` e `Clock`).
**Where**: `src/main/kotlin/com/moneyout/transfer/application/RegisterTransferHandler.kt`
**Depends on**: T18
**Requirement**: TRF-09

**Tools**:

- Skill: `tlc-spec-driven`; ler `.claude/rules/command-handler.md`, `.claude/rules/tests.md`

**Done when**:

- [ ] Command e porta em `transfer/domain/port/input/`
- [ ] `RegisterTransferHandlerTest.kt`: stream criado com versão 1, id do `IdGenerator`, `occurredAt` do `Clock.fixed`
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): caso de uso RegisterTransfer`

---

#### T20: Casos de uso da análise de risco

**Owner**: 🤖
**What**: `StartRiskAnalysis`, `ApproveTransfer` e `RejectTransfer` (command + porta + handler cada).
**Where**: `src/main/kotlin/com/moneyout/transfer/application/ApproveTransferHandler.kt`
**Depends on**: T18
**Requirement**: TRF-09

**Done when**:

- [ ] Três handlers (`StartRiskAnalysisHandler`, `ApproveTransferHandler`, `RejectTransferHandler`), um por command
- [ ] Testes de cada um: caminho feliz, `TransferNotFound`, `InvalidTransferTransition` (aprovar duas vezes), `ConcurrencyConflict`
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(transfer): casos de uso da analise de risco`

---

#### T21: Wiring Spring do contexto `transfer`

**Owner**: 🤖
**What**: `TransferConfiguration` com `@Bean` de `InMemoryTransfers` e dos quatro handlers (expostos pelas portas de entrada), usando os beans de `Clock` e `IdGenerator` da Fase 0.
**Where**: `src/main/kotlin/com/moneyout/transfer/infrastructure/config/TransferConfiguration.kt`
**Depends on**: T19, T20
**Requirement**: TRF-09

**Done when**:

- [ ] Teste de contexto resolve as quatro portas de entrada
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: integration
**Gate**: full

**Commit**: `feat(transfer): wiring Spring do contexto transfer`

---

### Phase 5: Fechamento

```
T22
```

#### T22: Validação, exercícios de quebra e entrevista

**Owner**: 🤖 (**interativa**: precisa do autor)
**What**: Verifier independente contra a spec (com discrimination sensor), exercícios de quebra (ex.: liberar `COMPLETED -> REGISTERED` na tabela, remover a checagem de versão do in-memory), entrevista da fase e atualização dos ADRs.
**Where**: `docs/entrevistas/fase1.md`
**Depends on**: T14, T15, T21
**Reuses**: perguntas sugeridas em `docs/PLANO.md` (Fase 1, 🎤) + perguntas fracas da Fase 0

**Done when**:

- [ ] `.specs/features/fase1-transfer/validation.md` com PASS e evidência `arquivo:linha`
- [ ] Quebras registradas em `docs/experiments/fase1.md`
- [ ] ADR-0005 com status final
- [ ] `python3 .claude/skills/tlc-spec-driven/scripts/validate_state.py fase1-transfer` passa

**Tests**: none
**Gate**: build

**Commit**: `docs(fase1): validacao, quebras e entrevista`
