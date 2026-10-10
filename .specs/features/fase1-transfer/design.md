# Fase 1 — Domínio `Transfer` Design

**Spec**: `.specs/features/fase1-transfer/spec.md`
**Status**: Draft — aguardando aprovação do autor
**Decisões de projeto que este design segue**: ADR-0002, ADR-0003, ADR-0011, ADR-0012; `.specs/STATE.md` AD-001 e AD-002; regras em `.claude/rules/`.

---

## Architecture Overview

```text
com.moneyout.transfer
├── domain/
│   ├── model/
│   │   ├── Transfer.kt                 # agregado: register / decidir (startRiskAnalysis, approve, ...) / apply / rehydrate
│   │   ├── TransferStateMachine.kt     # 🧑 tabela (estado, gatilho) → destino + allows()
│   │   ├── TransferStatus.kt           # 9 estados + isTerminal
│   │   ├── TransferTrigger.kt          # 8 gatilhos
│   │   ├── TransferId.kt, TransferMethod.kt
│   │   ├── Beneficiary.kt              # nome, documento?, destino (sealed: PixKey | BankAccount)
│   │   ├── Document.kt                 # 🧑 sealed Cpf | Cnpj
│   │   ├── PixKey.kt, BankAccount.kt, EndToEndId.kt
│   │   ├── FailureReason.kt, RejectionReason.kt
│   │   └── TransferExceptions.kt       # subclasses de DomainException
│   ├── event/TransferEvent.kt          # sealed interface + 9 eventos (delta + occurredAt)
│   └── port/
│       ├── input/                      # RegisterTransfer(+UseCase), StartRiskAnalysis, ApproveTransfer, RejectTransfer
│       └── output/Transfers.kt         # load / append(expectedVersion)
├── application/                        # um handler por command
└── infrastructure/
    ├── adapter/output/persistence/InMemoryTransfers.kt
    └── config/TransferConfiguration.kt # @Bean dos handlers
```

Fluxo de um command (ex.: aprovar):

```text
ApproveTransfer ──► ApproveTransferHandler
                     │ transfers.load(id)            → Transfer.rehydrate(history)
                     │ transfer.approve(clock.instant()) → TransferApproved   (consulta TransferStateMachine)
                     └ transfers.append(id, transfer.version, [event])  → ConcurrencyConflict se versão mudou
```

## Code Reuse Analysis

| Existente | Uso |
|---|---|
| `sharedkernel/Money.kt` (Fase 0) | valor da transfer; `isPositive()` para o invariante |
| `sharedkernel/DomainException.kt` | base de todas as exceções de domínio |
| `sharedkernel/IdGenerator.kt` + bean (Fase 0 T1) | `TransferId` no `RegisterTransferHandler` |
| `Clock` bean (Fase 0 T1) | `occurredAt` de todos os eventos |
| `architecture/ArchitectureTest.kt` | já cobre `..domain..`; ganha a regra de imutabilidade |

## Components and Interfaces

- **`TransferStateMachine`** (objeto): `fun next(from: TransferStatus, trigger: TransferTrigger): TransferStatus?` e `fun allows(from, trigger): Boolean`. É a **única** fonte das transições; o agregado nunca codifica `if (status == X)`. O formato interno (mapa, `when`, tabela) é escolha do autor.
- **`Transfer`** (data class): `id`, `amount`, `method`, `beneficiary`, `status`, `endToEndId?`, `completedAt?`, `version`.
  - `companion fun register(id, amount, method, beneficiary, at): TransferRegistered`
  - decisões: `startRiskAnalysis(at)`, `approve(at)`, `reject(reason, at)`, `submit(endToEndId, at)`, `complete(at)`, `fail(reason, at)`, `markUncertain(at)`, `returnFunds(at)`; cada uma consulta a state machine e devolve **um** evento.
  - `fun apply(event: TransferEvent): Transfer` — total, sem validação.
  - `companion fun rehydrate(history: List<TransferEvent>): Transfer`.
- **`Transfers`** (porta de saída): `load(id): Transfer` (lança `TransferNotFound`), `append(id, expectedVersion, events)` (lança `ConcurrencyConflict`).
- **`InMemoryTransfers`**: `ConcurrentHashMap<TransferId, List<TransferEvent>>`; o append usa `compute` para que checagem de versão e escrita sejam atômicas (o edge case de dois aprovadores concorrentes é testável já na Fase 1).

## Data Models

Eventos (todos com `transferId` e `occurredAt: Instant`):

| Evento | Campos extras | Estado após `apply` |
|---|---|---|
| `TransferRegistered` | `amount`, `method`, `beneficiary` | `REGISTERED` |
| `RiskAnalysisStarted` | — | `RISK_ANALYSIS` |
| `TransferApproved` | — | `APPROVED` |
| `TransferRejected` | `reason: RejectionReason` | `REJECTED` |
| `TransferSubmitted` | `endToEndId` | `SUBMITTED` |
| `TransferCompleted` | — | `COMPLETED` (grava `completedAt`) |
| `TransferFailed` | `reason: FailureReason` | `FAILED` |
| `TransferMarkedUncertain` | — | `UNCERTAIN` |
| `TransferReturned` | — | `RETURNED` |

PII: na Fase 1 o `beneficiary` vai no `TransferRegistered` em memória. A Fase 2 (ADR-0007) troca isso por referência antes de persistir; o design de eventos já isola o beneficiário num único evento para facilitar a troca.

## Tech Decisions

| Decisão | Escolha | Motivo |
|---|---|---|
| State machine | custom declarativa (ADR-0005, a escrever em T2) | Spring State Machine acoplaria o domínio a framework e esconderia a matriz que é o objeto de estudo |
| Formato do agregado | decidir/evoluir desde já (AD-002) | A Fase 2 só troca o adapter |
| Eventos | delta + `occurredAt`; envelope na infraestrutura | Event store guarda fatos; snapshot no evento é contrato frágil |
| Motivos de falha/rejeição | enums | Evita PII em texto livre dentro de evento imutável |
| `Document` | `sealed interface Document` com `Cpf` e `Cnpj` | `when` exaustivo; o CNPJ alfanumérico é só outra regra de DV |

## Error Handling Strategy

Todas as exceções são subclasses de `DomainException`, sem PII na mensagem, num único arquivo `TransferExceptions.kt`: `InvalidTransferAmount`, `IncompatibleDestination`, `MissingBeneficiaryDocument`, `InvalidDocument`, `InvalidPixKey`, `InvalidBankAccount`, `InvalidEndToEndId`, `InvalidTransferTransition(from, trigger)`, `CorruptedTransferStream`, `TransferNotFound(id)`, `ConcurrencyConflict(id, expected, actual)`, `ReturnWindowExpired`. Mapeamento HTTP (422/404/409) fica para a Fase 3.

## Risks & Concerns

| Risco | Mitigação |
|---|---|
| Regras do BCB/RFB erradas (formato do `endToEndId`, DV do CNPJ alfanumérico, prazo de devolução) | T1 verifica na fonte oficial antes das tasks de VO; ACs são ajustados se divergir |
| Agregado depende da tabela do autor (T5) | Tasks ☁️ de VOs e eventos não dependem de T5; só `Transfer` (T12) espera |
| `ConcurrencyConflict` também vai servir ao `Account` (Fase 2) | Nasce em `transfer`; sobe para o `sharedkernel` na Fase 2 se cumprir os critérios do ADR-0011 |
| PII no `TransferRegistered` | Aceito só em memória na Fase 1; bloqueante para a Fase 2 (ADR-0007) |
