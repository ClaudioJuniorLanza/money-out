# Fase 1 — Domínio `Transfer` + State Machine Specification

> Roteiro e didática: [`docs/PLANO.md`](../../../docs/PLANO.md#fase-1--domínio-transfer--state-machine-dddhexagonal). Design: [`design.md`](design.md). Pré-requisito: Fase 0 fechada (`.specs/features/fase0-fundacoes/`).
> **Status:** Approved (autor, 2026-10-13). As premissas marcadas "a confirmar" dependem da task de verificação na fonte (T1) e podem mudar ACs.

## Problem Statement

Um payout tem um ciclo de vida com estados que movem dinheiro de verdade (`SUBMITTED`, `COMPLETED`, `RETURNED`). Se o status for uma string solta ou se uma transição inválida for possível (`COMPLETED → REGISTERED`, aprovar duas vezes), o sistema paga em duplicidade ou perde rastro do dinheiro. A Fase 1 modela `Transfer` com invariantes fortes, uma state machine declarativa e eventos de domínio, já no formato que a Fase 2 vai persistir num event store.

## Goals

- [ ] Nenhuma transição fora da matriz é possível (prova exaustiva estado × gatilho).
- [ ] Toda transição válida emite exatamente um evento, e o estado é reconstruível desses eventos.
- [ ] Value objects de beneficiário (documento, chave PIX, conta, `endToEndId`) rejeitam valores inválidos na construção.
- [ ] Casos de uso de registro e de decisão de risco funcionando sobre um adapter in-memory, sem framework no domínio.

## Out of Scope

| Item | Motivo |
|---|---|
| Event store em Postgres, concorrência real entre processos | Fase 2 |
| API HTTP, `Idempotency-Key` | Fase 3 |
| Geração do `endToEndId` e chamada ao TSP | Fase 6 (aqui o `endToEndId` só é validado e chega como parâmetro do `SUBMIT`) |
| Motor de análise de risco | Fora do plano; aprovar/rejeitar chega como command |
| Devolução parcial | Fase 7 (aqui só devolução total) |
| Limites de valor e horário noturno PIX | Fase 8 |
| Saldo/reserva (`Account`) | Fase 2 |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
|---|---|---|---|
| Formato do `endToEndId` | `E` + ISPB (8 dígitos) + `yyyyMMddHHmm` (12) + 11 alfanuméricos = 32 caracteres | Fontes secundárias concordam; **a confirmar** no Manual de Padrões para Iniciação do Pix (T1) | n |
| CNPJ alfanumérico | 14 posições; 12 primeiras `[0-9A-Z]`, 2 últimas (DV) numéricas; DV por módulo 11 com valor do caractere = código ASCII − 48 | IN RFB nº 2.229/2024 segundo fontes secundárias; **a confirmar** no material oficial RFB/Serpro, incluindo pesos (T1) | n |
| CPF/CNPJ com todos os dígitos iguais | Rejeitado mesmo com DV "válido" | Regra usual de validação; pedida no plano | n |
| Prazo de devolução PIX | 90 dias a partir da liquidação | Valor usual citado; **a confirmar** no Regulamento do Pix (T1). Só afeta a task opcional | n |
| Destino por método | PIX aceita chave PIX **ou** conta; TED exige conta | PIX por agência/conta existe; TED não usa chave | n |
| Documento do beneficiário | Obrigatório para TED e PIX por conta; opcional para PIX por chave | No PIX por chave o DICT resolve o titular | n |
| Tipos de conta | `CHECKING`, `SAVINGS`, `SALARY`, `PAYMENT` | Mapeiam os tipos usados no PIX (CACC, SVGS, SLRY, TRAN), **a confirmar** em T1 | n |
| Valor da transfer | `Money` estritamente positivo, só BRL | Escopo do projeto; `Money` é assinado de propósito (Fase 0) | n |
| Nome do beneficiário | Não vazio, até 140 caracteres | Limite prático; ajustável quando houver contrato de API | n |
| Gatilho `RETURN` | Só de `COMPLETED`, valor igual ao da transfer | Devolução parcial fica para a Fase 7 | n |
| Formato do evento | Delta do fato + `occurredAt`; sem `eventId`/versão (envelope é infraestrutura) | `.claude/rules/domain-model.md`; prepara a Fase 2 | n |

**Open questions:** none — all resolved or logged above (premissas "a confirmar" têm a task T1 como dona).

---

## User Stories

### P1: Registrar uma transfer válida ⭐ MVP

**User Story**: Como operador de payout, quero registrar uma transfer PIX ou TED com beneficiário e valor validados, para que nada inválido entre no ciclo de vida.

**Why P1**: Sem registro não há ciclo de vida.

**Acceptance Criteria**:

1. WHEN uma transfer válida é registrada THEN o sistema SHALL emitir um único `TransferRegistered` e o agregado SHALL ficar em `REGISTERED` com versão 1.
2. IF o valor não é estritamente positivo THEN o sistema SHALL lançar `InvalidTransferAmount` e não emitir evento.
3. IF o método é TED e o destino é uma chave PIX THEN o sistema SHALL lançar `IncompatibleDestination`.
4. IF o destino é conta (TED ou PIX) e o beneficiário não tem documento THEN o sistema SHALL lançar `MissingBeneficiaryDocument`.

**Independent Test**: `TransferTest` registra transfers válidas e inválidas sem nenhum adapter.

---

### P1: Validar documento, chave PIX, conta e `endToEndId` ⭐ MVP

**User Story**: Como engenheiro, quero value objects que se recusem a existir com valor inválido, para que nenhuma camada precise revalidar.

**Why P1**: Invariantes na construção são o que torna o resto confiável.

**Acceptance Criteria**:

1. IF um CPF tem dígito verificador incorreto, tamanho diferente de 11 ou todos os dígitos iguais THEN o sistema SHALL lançar `InvalidDocument` sem o número na mensagem.
2. IF um CNPJ (numérico ou alfanumérico) tem DV incorreto, caractere fora de `[0-9A-Z]` nas 12 primeiras posições, DV não numérico ou todos os caracteres iguais THEN o sistema SHALL lançar `InvalidDocument`.
3. WHEN um documento é criado a partir de texto com máscara (`.`, `-`, `/`) THEN o sistema SHALL normalizar antes de validar.
4. IF uma chave PIX não respeita o formato do seu tipo (CPF, CNPJ, e-mail, telefone, aleatória) THEN o sistema SHALL lançar `InvalidPixKey` sem o valor na mensagem.
5. IF um `endToEndId` não segue o formato confirmado em T1 THEN o sistema SHALL lançar `InvalidEndToEndId`.
6. IF uma conta tem ISPB diferente de 8 dígitos ou agência/conta vazias THEN o sistema SHALL lançar `InvalidBankAccount`.

**Independent Test**: testes de VO tabelados (`@ParameterizedTest`) com casos válidos e inválidos.

---

### P1: Ciclo de vida protegido pela state machine ⭐ MVP

**User Story**: Como engenheiro de pagamentos, quero que só as transições da matriz sejam possíveis e que cada uma gere um fato, para que nenhum payout volte a um estado anterior ou seja processado duas vezes.

**Why P1**: É a história de falha da fase.

**Acceptance Criteria**:

1. The system SHALL permitir apenas as transições: `REGISTERED -START_RISK_ANALYSIS→ RISK_ANALYSIS`; `RISK_ANALYSIS -APPROVE→ APPROVED`; `RISK_ANALYSIS -REJECT→ REJECTED`; `APPROVED -SUBMIT→ SUBMITTED`; `SUBMITTED -COMPLETE→ COMPLETED`; `SUBMITTED -FAIL→ FAILED`; `SUBMITTED -MARK_UNCERTAIN→ UNCERTAIN`; `UNCERTAIN -COMPLETE→ COMPLETED`; `UNCERTAIN -FAIL→ FAILED`; `COMPLETED -RETURN→ RETURNED`.
2. IF um gatilho não é permitido no estado atual THEN o sistema SHALL lançar `InvalidTransferTransition` (com estado e gatilho) e não emitir evento.
3. WHEN uma transição permitida é decidida THEN o sistema SHALL emitir exatamente um evento do tipo correspondente ao gatilho.
4. WHILE a transfer está em `REJECTED`, `FAILED` ou `RETURNED` the system SHALL rejeitar todos os gatilhos.
5. WHEN `SUBMIT` é decidido THEN o evento `TransferSubmitted` SHALL carregar o `endToEndId` recebido.
6. WHEN `FAIL` ou `REJECT` é decidido THEN o evento SHALL carregar um motivo categorizado (enum), nunca texto livre com PII.

**Independent Test**: teste parametrizado sobre todos os 72 pares (9 estados × 8 gatilhos) a partir da tabela.

---

### P1: Estado reconstruível dos eventos ⭐ MVP

**User Story**: Como engenheiro, quero reconstruir qualquer transfer a partir dos seus eventos, para que a Fase 2 só troque o adapter.

**Why P1**: Pré-condição do Event Sourcing (ADR-0003).

**Acceptance Criteria**:

1. WHEN `Transfer.rehydrate(eventos)` é chamado com um histórico válido THEN o sistema SHALL devolver o mesmo estado e a versão igual ao número de eventos.
2. IF o histórico está vazio ou não começa com `TransferRegistered` THEN o sistema SHALL lançar `CorruptedTransferStream`.

**Independent Test**: para cada caminho da matriz, decidir passo a passo e comparar com `rehydrate` dos eventos emitidos.

---

### P2: Casos de uso sobre adapter in-memory

**User Story**: Como engenheiro, quero registrar, iniciar análise, aprovar e rejeitar por portas de entrada, com persistência in-memory com controle de versão, para exercitar hexagonal antes do banco.

**Why P2**: Demonstra as portas; a persistência real é da Fase 2.

**Acceptance Criteria**:

1. WHEN `RegisterTransfer` é tratado THEN o sistema SHALL anexar o stream com `expectedVersion = 0` e devolver o `TransferId` gerado pelo `IdGenerator`.
2. IF a transfer não existe THEN `StartRiskAnalysis`, `ApproveTransfer` e `RejectTransfer` SHALL lançar `TransferNotFound`.
3. IF o append é feito com `expectedVersion` diferente da versão atual THEN o adapter in-memory SHALL lançar `ConcurrencyConflict` e não gravar nada.
4. WHEN um handler decide uma transição THEN o instante do evento SHALL vir do `Clock` injetado.
5. The system SHALL manter `..domain..` sem dependência de framework e sem campos mutáveis (ArchUnit).

**Independent Test**: testes de handler com `InMemoryTransfers` e `Clock.fixed`.

---

### P3: Propriedades e guard de devolução

**User Story**: Como autor, quero provar por propriedade que nenhuma sequência de gatilhos quebra a máquina e limitar a devolução ao prazo, para treinar property testing e guards com tempo.

**Why P3**: Aprofunda o aprendizado; não bloqueia a Fase 2.

**Acceptance Criteria**:

1. WHEN qualquer sequência aleatória de gatilhos é aplicada (ignorando os rejeitados) THEN o estado final SHALL estar entre os 9 estados válidos e estados terminais SHALL permanecer absorventes.
2. IF `RETURN` é decidido depois do prazo confirmado em T1, contado de `TransferCompleted.occurredAt` THEN o sistema SHALL lançar `ReturnWindowExpired`.

**Independent Test**: `TransferPropertyTest` com `checkAll`; teste do guard com `Clock.fixed` antes e depois do prazo.

---

## Edge Cases

- IF a mesma transfer recebe `APPROVE` duas vezes THEN a segunda SHALL lançar `InvalidTransferTransition` (já está em `APPROVED`).
- IF dois handlers aprovam a mesma transfer a partir da mesma versão THEN apenas um append SHALL ter sucesso e o outro SHALL lançar `ConcurrencyConflict`.
- WHEN o CNPJ é numérico antigo (só dígitos) THEN o sistema SHALL continuar aceitando com o mesmo DV de antes.
- IF a chave PIX de tipo CPF tem CPF inválido THEN o sistema SHALL lançar `InvalidPixKey`.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
|---|---|---|---|
| TRF-01 | P1: Registrar (ACs 1–4) | Tasks | Pending |
| TRF-02 | P1: VOs — documento CPF/CNPJ (ACs 1–3) | Tasks | Pending |
| TRF-03 | P1: VOs — chave PIX (AC 4) | Tasks | Pending |
| TRF-04 | P1: VOs — `endToEndId` (AC 5) | Tasks | Pending |
| TRF-05 | P1: VOs — conta bancária (AC 6) | Tasks | Pending |
| TRF-06 | P1: State machine (ACs 1, 2, 4) | Tasks | Pending |
| TRF-07 | P1: State machine (ACs 3, 5, 6) | Tasks | Pending |
| TRF-08 | P1: Rehidratação (ACs 1–2) | Tasks | Pending |
| TRF-09 | P2: Casos de uso (ACs 1, 2, 4) | Tasks | Pending |
| TRF-10 | P2: Adapter in-memory (AC 3 + edge case de concorrência) | Tasks | Pending |
| TRF-11 | P2: Arquitetura (AC 5) | Tasks | Pending |
| TRF-12 | P3: Property test (AC 1) | Tasks | Pending |
| TRF-13 | P3: Guard de devolução (AC 2) | Tasks | Pending |
| TRF-14 | Premissas "a confirmar" verificadas na fonte | Tasks | Pending |

**Coverage:** 14 total, 14 mapped to tasks, 0 unmapped.

---

## Success Criteria

- [ ] Critérios de aceitação da Fase 1 em `docs/PLANO.md` todos verdes.
- [ ] Nenhum `if (status == ...)` fora do agregado/state machine (revisão).
- [ ] ADR-0005 aceito; entrevista registrada em `docs/entrevistas/fase1.md`.
